/**
 *
 * (c) Copyright Ascensio System SIA 2023
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package dapi

import (
	"encoding/json"
	"fmt"
	"html/template"
	"net/http"
	"net/url"
	"os"
	"path"
	"strconv"
	"strings"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/api"
	"github.com/ONLYOFFICE/document-server-integration/server/handlers"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
	"github.com/ONLYOFFICE/document-server-integration/utils"
	"github.com/gorilla/schema"
	"go.uber.org/zap"
)

var decoder = schema.NewDecoder()
var indexTemplate = template.Must(template.ParseFiles("templates/index.html"))
var editorTemplate = template.Must(template.ParseFiles("templates/editor.html"))

type DefaultServerEndpointsHandler struct {
	logger        *zap.SugaredLogger
	config        config.ApplicationConfig
	specification config.SpecificationConfig
	*handlers.CallbackRegistry
	*managers.Managers
}

func NewDefaultServerEndpointsHandler(logger *zap.SugaredLogger, config config.ApplicationConfig,
	spec config.SpecificationConfig, reg *handlers.CallbackRegistry,
	managers *managers.Managers) api.ServerEndpointsHandler {
	return &DefaultServerEndpointsHandler{
		logger,
		config,
		spec,
		reg,
		managers,
	}
}

func generateUrl(r *http.Request) string {
	scheme := "http"
	if r.Header.Get("X-Forwarded-Proto") != "" {
		scheme = r.Header.Get("X-Forwarded-Proto")
	}

	if r.TLS != nil {
		scheme = "https"
	}

	return fmt.Sprintf("%s://%s", scheme, r.Host)
}

func (srv *DefaultServerEndpointsHandler) Upload(w http.ResponseWriter, r *http.Request) {
	r.ParseMultipartForm(32 << 20)
	file, handler, err := r.FormFile("uploadedFile")
	w.Header().Set("Content-Type", "application/json")

	if err != nil {
		srv.logger.Error(err.Error())
		return
	}

	srv.logger.Debug("A new upload call")
	if !srv.DocumentManager.IsDocumentConvertable(handler.Filename) {
		srv.logger.Errorf("File %s is not supported", handler.Filename)
		shared.SendCustomErrorResponse(w, "File type is not supported")
		return
	}

	fileName, err := srv.StorageManager.GenerateVersionedFilename(handler.Filename)
	if err != nil {
		srv.logger.Error(err.Error())
		shared.SendCustomErrorResponse(w, err.Error())
		return
	}

	fpath, err := srv.StorageManager.GenerateFilePath(fileName)
	if err != nil {
		srv.logger.Error(err.Error())
		shared.SendCustomErrorResponse(w, err.Error())
		return
	}

	if err = srv.StorageManager.CreateFile(file, fpath); err != nil {
		srv.logger.Error(err.Error())
		shared.SendCustomErrorResponse(w, err.Error())
		return
	}

	_, uid := shared.GetCookiesInfo(r.Cookies())
	user, err := srv.UserManager.GetUserById(uid)
	if err != nil {
		srv.logger.Errorf("could not find user with id: %s", uid)
		shared.SendCustomErrorResponse(w, err.Error())
		return
	}

	srv.HistoryManager.CreateMeta(fileName, models.History{
		ServerVersion: "0.0.0",
		Changes: []models.Changes{
			{
				Created: time.Now().UTC().Format("2006-02-1 15:04:05"),
				User: models.User{
					Id:       user.Id,
					Username: user.Username,
				},
			},
		},
	})

	fmt.Fprintf(w, "{\"filename\":\"%s\"}", fileName)
}

func (srv *DefaultServerEndpointsHandler) Index(w http.ResponseWriter, r *http.Request) {
	srv.logger.Debug("A new index call")
	files, err := srv.Managers.StorageManager.GetStoredFiles(r.Host)
	if err != nil {
		srv.logger.Errorf("could not fetch files: %s", err.Error())
	}

	data := map[string]interface{}{
		"Extensions": srv.specification.Extensions,
		"Users":      srv.Managers.UserManager.GetUsers(),
		"Files":      files,
		"Preloader":  srv.config.DocumentServerHost + srv.config.DocumentServerPreloader,
	}

	indexTemplate.Execute(w, data)
}

func (srv *DefaultServerEndpointsHandler) Download(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("fileName")

	srv.logger.Debugf("A new download call (%s)", filename)
	if filename == "" {
		shared.SendDocumentServerRespose(w, true)
		return
	}

	var meta managers.FileMeta
	fileUrl := srv.StorageManager.GenerateFilestoreUri(filename, meta)
	if fileUrl == "" {
		shared.SendDocumentServerRespose(w, true)
		return
	}

	http.Redirect(w, r, fileUrl, http.StatusMovedPermanently)
}

func (srv *DefaultServerEndpointsHandler) Remove(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")
	if filename == "" {
		shared.SendDocumentServerRespose(w, true)
		return
	}

	if err := srv.StorageManager.RemoveFile(filename); err != nil {
		srv.logger.Error(err.Error())
		shared.SendDocumentServerRespose(w, true)
		return
	}

	srv.logger.Debug("A new remove call")
	shared.SendDocumentServerRespose(w, false)
}

func (srv *DefaultServerEndpointsHandler) Editor(w http.ResponseWriter, r *http.Request) {
	var editorParameters managers.Editor
	if err := decoder.Decode(&editorParameters, r.URL.Query()); err != nil {
		srv.logger.Error("Invalid query parameters")
		return
	}

	if err := editorParameters.IsValid(); err != nil {
		srv.logger.Errorf("Editor parameters are invalid: %s", err.Error())
		return
	}

	srv.logger.Debug("A new editor call")
	editorParameters.Language, editorParameters.UserId = shared.GetCookiesInfo(r.Cookies())

	remoteAddr := generateUrl(r)
	if srv.config.ServerAddress != "" {
		remoteAddr = srv.config.ServerAddress
	}

	config, err := srv.Managers.DocumentManager.BuildDocumentConfig(editorParameters, remoteAddr)
	if err != nil {
		srv.logger.Errorf("A document manager error has occured: %s", err.Error())
		return
	}

	refHist, setHist, err := srv.Managers.HistoryManager.GetHistory(config.Document.Title, remoteAddr)
	if err != nil {
		srv.logger.Warnf("could not get file history: %s", err.Error())
	}

	data := map[string]interface{}{
		"apijs":      srv.config.DocumentServerHost + srv.config.DocumentServerApi,
		"config":     config,
		"actionLink": editorParameters.ActionLink,
		"docType":    config.DocumentType,
		"refHist":    refHist,
		"setHist":    setHist,
	}

	editorTemplate.Execute(w, data)
}

func (srv *DefaultServerEndpointsHandler) Reference(w http.ResponseWriter, r *http.Request) {
	var body models.Reference
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		srv.logger.Error("Reference body decoding error")
		shared.SendDocumentServerRespose(w, true)
		return
	}

	remoteAddr := generateUrl(r)
	var fileName /*, userAddress*/ string

	var fileKey map[string]string
	json.Unmarshal([]byte(body.ReferenceData.FileKey), &fileKey)
	path, _ := srv.GenerateFilePath(fileKey["fileName"])
	if body.ReferenceData.InstanceId == remoteAddr && srv.PathExists(path) {
		fileName = fileKey["fileName"]
	}

	if fileName == "" && body.Link != "" {
		if strings.Contains(body.Link, remoteAddr) {
			res := map[string]interface{}{
				"url": body.Link,
			}
			shared.SendResponse(w, res)
			return
		}

		urlObj, _ := url.Parse(body.Link)
		params, _ := url.ParseQuery(urlObj.RawQuery)
		if len(params["filename"]) != 0 {
			fileName = params["filename"][0]
		}
		path, _ := srv.GenerateFilePath(fileName)
		if !srv.PathExists(path) {
			shared.SendCustomErrorResponse(w, "File is not exist")
			return
		}
	}

	if fileName == "" && body.Path != "" {
		filePath := utils.GetFileName(body.Path)
		path, _ := srv.GenerateFilePath(filePath)
		if srv.PathExists(path) {
			fileName = filePath
		}
	}

	if fileName == "" {
		shared.SendCustomErrorResponse(w, "File is not found")
		return
	}

	docKey, _ := srv.GenerateFileHash(fileName)
	data := models.Reference{
		FileType: srv.GetFileType(fileName),
		Key:      docKey,
		Url:      srv.GeneratePublicFileUri(fileName, remoteAddr, managers.FileMeta{}),
		ReferenceData: models.ReferenceData{
			FileKey:    fmt.Sprintf("{\"fileName\":\"%s\"}", fileName),
			InstanceId: remoteAddr,
		},
		Link: remoteAddr + "/editor?filename=" + url.QueryEscape(fileName),
		Path: fileName,
	}

	secret := strings.TrimSpace(srv.config.JwtSecret)
	if secret != "" && srv.config.JwtEnabled {
		token, _ := srv.JwtSign(data, []byte(secret))
		data.Token = token
	}

	shared.SendResponse(w, data)
}

func (srv *DefaultServerEndpointsHandler) History(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("fileName")
	file := r.URL.Query().Get("file")
	version, err := strconv.Atoi(r.URL.Query().Get("ver"))
	if err != nil {
		srv.logger.Errorf("Could not parse file version: %s", err.Error())
		shared.SendDocumentServerRespose(w, true)
		return
	}

	srv.logger.Debugf("A new history call (%s)", filename)
	if filename == "" {
		srv.logger.Errorf("filename parameter is blank")
		shared.SendDocumentServerRespose(w, true)
		return
	}

	fileUrl := srv.StorageManager.GenerateFilestoreUri(filename, managers.FileMeta{
		Version:         version,
		DestinationPath: file,
	})

	if fileUrl == "" {
		srv.logger.Errorf("file url is blank")
		shared.SendDocumentServerRespose(w, true)
		return
	}

	http.Redirect(w, r, fileUrl, http.StatusSeeOther)
}

func (srv *DefaultServerEndpointsHandler) Convert(w http.ResponseWriter, r *http.Request) {
	_, uid := shared.GetCookiesInfo(r.Cookies())
	if uid == "" {
		srv.logger.Errorf("invalid user id")
		shared.SendDocumentServerRespose(w, true)
		return
	}

	user, err := srv.UserManager.GetUserById(uid)
	if err != nil {
		srv.logger.Errorf("could not find user with id: %s", uid)
		shared.SendDocumentServerRespose(w, true)
		return
	}

	err = r.ParseForm()
	srv.logger.Debug("A new convert call")
	if err != nil {
		srv.logger.Error(err.Error())
		shared.SendDocumentServerRespose(w, true)
		return
	}

	var payload managers.ConvertRequest
	err = decoder.Decode(&payload, r.PostForm)
	if err != nil {
		srv.logger.Error(err.Error())
		shared.SendDocumentServerRespose(w, true)
		return
	}

	filename := payload.Filename
	response := managers.ConvertResponse{Filename: filename}
	defer shared.SendResponse(w, &response)

	remoteAddr := generateUrl(r)
	if srv.config.ServerAddress != "" {
		remoteAddr = srv.config.ServerAddress
	}

	fileUrl := srv.StorageManager.GeneratePublicFileUri(filename, remoteAddr, managers.FileMeta{})
	fileExt := utils.GetFileExt(filename, true)
	fileType := srv.ConversionManager.GetFileType(filename)
	newExt := srv.ConversionManager.GetInternalExtension(fileType)

	if srv.DocumentManager.IsDocumentConvertable(filename) {
		key, err := srv.StorageManager.GenerateFileHash(filename)
		if err != nil {
			response.Error = err.Error()
			srv.logger.Errorf("File conversion error: %s", err.Error())
			return
		}

		newUrl, err := srv.ConversionManager.GetConverterUri(fileUrl, fileExt, newExt, key, true)
		if err != nil {
			response.Error = err.Error()
			srv.logger.Errorf("File conversion error: %s", err.Error())
			return
		}

		if newUrl == "" {
			response.Step = 1
		} else {
			correctName, err := srv.StorageManager.GenerateVersionedFilename(utils.GetFileNameWithoutExt(filename) + newExt)
			if err != nil {
				response.Error = err.Error()
				srv.logger.Errorf("File conversion error: %s", err.Error())
				return
			}

			srv.StorageManager.SaveFileFromUri(models.Callback{
				Url:         newUrl,
				Filename:    correctName,
				UserAddress: r.Host,
			})
			srv.StorageManager.RemoveFile(filename)
			response.Filename = correctName
			srv.HistoryManager.CreateMeta(response.Filename, models.History{
				ServerVersion: "0.0.0",
				Changes: []models.Changes{
					{
						Created: time.Now().UTC().Format("2006-02-1 15:04:05"),
						User: models.User{
							Id:       user.Id,
							Username: user.Username,
						},
					},
				},
			})
		}
	}
}

func (srv *DefaultServerEndpointsHandler) Callback(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query()
	filename, userAddress := query.Get("filename"), query.Get("user_address")
	if filename == "" || userAddress == "" {
		shared.SendDocumentServerRespose(w, true)
		return
	}

	var body models.Callback
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		srv.logger.Error("Callback body decoding error")
		shared.SendDocumentServerRespose(w, true)
		return
	}

	if err := srv.Managers.JwtManager.ParseCallback(&body, r.Header.Get(srv.config.JwtHeader)); err != nil {
		srv.logger.Error(err.Error())
		shared.SendDocumentServerRespose(w, true)
		return
	}

	body.Filename = filename
	body.UserAddress = userAddress
	if err := srv.CallbackRegistry.HandleIncomingCode(&body); err != nil {
		shared.SendDocumentServerRespose(w, true)
		return
	}

	shared.SendDocumentServerRespose(w, false)
}

func (srv *DefaultServerEndpointsHandler) Create(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query()
	fileExt, isSample := query.Get("fileExt"), query.Get("sample")

	if strings.TrimSpace(fileExt) == "" || !utils.IsInList(fileExt, srv.specification.Extensions.Edited) {
		srv.logger.Errorf("%s extension is not supported", fileExt)
		http.Redirect(w, r, "/", http.StatusSeeOther)
		return
	}

	_, uid := shared.GetCookiesInfo(r.Cookies())
	if uid == "" {
		srv.logger.Errorf("user id is blank")
		http.Redirect(w, r, "/", http.StatusSeeOther)
		return
	}

	user, err := srv.UserManager.GetUserById(uid)
	if err != nil {
		srv.logger.Errorf("could not find user with id: %s", uid)
		http.Redirect(w, r, "/", http.StatusSeeOther)
		return
	}

	srv.logger.Debugf("Creating a new %s file", fileExt)
	sampleType := "new"
	if strings.TrimSpace(isSample) != "" {
		sampleType = "sample"
	}

	filename := fmt.Sprintf("%s.%s", sampleType, fileExt)
	file, err := os.Open(path.Join("static/assets/document-templates", sampleType, filename))
	if err != nil {
		srv.logger.Errorf("could not create a new file: %s", err.Error())
		http.Redirect(w, r, "/", http.StatusSeeOther)
		return
	}

	defer file.Close()
	filename, err = srv.StorageManager.GenerateVersionedFilename(filename)
	if err != nil {
		srv.logger.Errorf("could not generated versioned filename: %s", filename)
		http.Redirect(w, r, "/", http.StatusSeeOther)
		return
	}

	fpath, err := srv.StorageManager.GenerateFilePath(filename)
	if err != nil {
		srv.logger.Errorf("could not generated file path: %s", filename)
		http.Redirect(w, r, "/", http.StatusSeeOther)
		return
	}

	srv.StorageManager.CreateFile(file, fpath)
	srv.HistoryManager.CreateMeta(filename, models.History{
		ServerVersion: "0.0.0",
		Changes: []models.Changes{
			{
				Created: time.Now().UTC().Format("2006-02-1 15:04:05"),
				User: models.User{
					Id:       user.Id,
					Username: user.Username,
				},
			},
		},
	})

	http.Redirect(w, r, "/editor?filename="+filename, http.StatusSeeOther)
}
