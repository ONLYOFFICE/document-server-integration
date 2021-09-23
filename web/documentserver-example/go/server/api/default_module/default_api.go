/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

package default_api

import (
	"encoding/json"
	"fmt"
	"html/template"
	"net/http"
	"os"
	"path"
	"strings"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/api"
	"github.com/ONLYOFFICE/document-server-integration/server/handlers"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/utils"
	"github.com/gorilla/schema"
	"go.uber.org/zap"
)

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

var decoder = schema.NewDecoder()

var index_template = template.Must(template.ParseFiles("templates/index.html"))
var editor_template = template.Must(template.ParseFiles("templates/editor.html"))

func (srv *DefaultServerEndpointsHandler) Index(w http.ResponseWriter, r *http.Request) {
	srv.logger.Debug("A new index call")
	data := map[string]interface{}{
		"Extensions": srv.specification.Extensions,
		"Users":      srv.Managers.UserManager.GetUsers(),
		"Files":      srv.Managers.StorageManager.GetStoredFiles(r.RemoteAddr),
		"Preloader":  srv.config.DocumentServerHost + srv.config.DocumentServerPreloader,
	}

	index_template.Execute(w, data)
}

func (srv *DefaultServerEndpointsHandler) Editor(w http.ResponseWriter, r *http.Request) {
	editorParameters := managers.Editor{}

	decodingErr := decoder.Decode(&editorParameters, r.URL.Query())
	validationErr := editorParameters.IsValid()

	if decodingErr != nil || validationErr != nil {
		srv.logger.Error("Invalid query parameters")
		return
	}

	srv.logger.Debug("A new editor call")

	editorParameters.Language, editorParameters.UserId = getCookiesInfo(r)
	config, configErr := srv.Managers.DocumentManager.BuildDocumentConfig(editorParameters, r.RemoteAddr)

	ref_hist, set_hist := srv.Managers.HistoryManager.GetHistory(config.Document.Title, r.RemoteAddr)

	if configErr != nil {
		srv.logger.Errorf("A document manager error has occured: ", configErr)
		return
	}

	data := map[string]interface{}{
		"apijs":      srv.config.DocumentServerHost + srv.config.DocumentServerApi,
		"config":     config,
		"actionLink": editorParameters.ActionLink,
		"docType":    config.DocumentType,
		"refHist":    ref_hist,
		"setHist":    set_hist,
	}

	editor_template.Execute(w, data)
}

func (srv *DefaultServerEndpointsHandler) Remove(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("filename")

	if filename == "" {
		sendDocumentServerRespose(w, true)
		return
	}

	err := srv.StorageManager.RemoveFile(filename, r.RemoteAddr)

	if err != nil {
		srv.logger.Error(err.Error())
		sendDocumentServerRespose(w, true)
		return
	}

	srv.logger.Debug("A new remove call")

	sendDocumentServerRespose(w, false)
}

func (srv *DefaultServerEndpointsHandler) Upload(w http.ResponseWriter, r *http.Request) {
	r.ParseMultipartForm(32 << 20)
	file, handler, err := r.FormFile("uploadedFile")
	w.Header().Set("Content-Type", "application/json")

	if err != nil {
		srv.logger.Error(err.Error())
		fmt.Fprintf(w, "{\"error\":\"%s\"}", err.Error())
		return
	}

	srv.logger.Debug("A new upload call")

	if !srv.DocumentManager.IsDocumentConvertable(handler.Filename) {
		srv.logger.Warnf("File %s is not supported", handler.Filename)
		fmt.Fprint(w, "{\"error\":\"File type is not supported\"}")
		return
	}

	fileName := srv.StorageManager.GenerateVersionedFilename(handler.Filename, r.RemoteAddr)

	err = srv.StorageManager.CreateFile(file, srv.StorageManager.GenerateFilePath(fileName, r.RemoteAddr))
	srv.HistoryManager.CreateMeta(fileName, r.RemoteAddr, []models.Changes{
		{
			Created: time.Now().Format("2006-02-1 15:04:05"),
			User:    srv.getUserFromCookies(r),
		},
	})

	if err != nil {
		srv.logger.Error(err.Error())
		fmt.Fprintf(w, "{\"error\":\"%s\"}", err.Error())
	} else {
		fmt.Fprintf(w, "{\"filename\":\"%s\"}", fileName)
	}
}

func (srv *DefaultServerEndpointsHandler) Download(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("fileName")

	srv.logger.Debugf("A new download call (%s)", filename)

	if filename == "" {
		sendDocumentServerRespose(w, true)
		return
	}

	fileUrl := srv.StorageManager.GenerateFileUri(filename, r.RemoteAddr, managers.FileMeta{})

	if fileUrl == "" {
		sendDocumentServerRespose(w, true)
		return
	}

	http.Redirect(w, r, fileUrl, http.StatusSeeOther)
}

func sendResponse(w http.ResponseWriter, data interface{}) {
	body, _ := json.Marshal(data)
	fmt.Fprint(w, string(body))
}

//TODO: Refactoring
func (srv *DefaultServerEndpointsHandler) Convert(w http.ResponseWriter, r *http.Request) {
	err := r.ParseForm()

	srv.logger.Debug("A new convert call")

	if err != nil {
		srv.logger.Error(err.Error())
		sendDocumentServerRespose(w, true)
		return
	}

	var payload managers.ConvertRequest
	err = decoder.Decode(&payload, r.PostForm)

	if err != nil {
		srv.logger.Error(err.Error())
		sendDocumentServerRespose(w, true)
		return
	}

	filename := payload.Filename

	response := managers.ConvertResponse{Filename: filename}
	defer sendResponse(w, &response)

	fileUrl := srv.StorageManager.GenerateFileUri(filename, r.RemoteAddr, managers.FileMeta{})
	fileExt := utils.GetFileExt(filename)
	fileType := srv.ConversionManager.GetFileType(filename)
	newExt := srv.ConversionManager.GetInternalExtension(fileType)

	if srv.DocumentManager.IsDocumentConvertable(filename) {
		key := srv.StorageManager.GenerateFileHash(filename, r.RemoteAddr)
		newUrl, err := srv.ConversionManager.GetConverterUri(fileUrl, fileExt, newExt, key, true)
		if err != nil {
			response.Error = err.Error()
			srv.logger.Errorf("File conversion error: %s", err.Error())
			return
		}

		if newUrl == "" {
			response.Step = 1
		} else {
			correct_name := srv.StorageManager.GenerateVersionedFilename(utils.GetFileNameWithoutExt(filename)+newExt, r.RemoteAddr)
			srv.StorageManager.SaveFileFromUri(models.Callback{
				Url:         newUrl,
				Filename:    correct_name,
				UserAddress: r.RemoteAddr,
			})
			srv.StorageManager.RemoveFile(filename, r.RemoteAddr)
			response.Filename = correct_name
			srv.HistoryManager.CreateMeta(response.Filename, r.RemoteAddr, []models.Changes{
				{
					Created: time.Now().Format("2006-02-1 15:04:05"),
					User:    srv.getUserFromCookies(r),
				},
			})
		}
	}
}

func (srv *DefaultServerEndpointsHandler) Callback(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query()
	filename := query.Get("filename")
	user_address := query.Get("user_address")

	if filename == "" || user_address == "" {
		sendDocumentServerRespose(w, true)
		return
	}

	body := models.Callback{}

	decErr := json.NewDecoder(r.Body).Decode(&body)

	if decErr != nil {
		srv.logger.Error("Callback body decoding error")
		sendDocumentServerRespose(w, true)
		return
	}

	jwtErr := srv.Managers.JwtManager.ParseCallback(&body, r.Header.Get(srv.config.JwtHeader))

	if jwtErr != nil {
		srv.logger.Error(jwtErr.Error())
		sendDocumentServerRespose(w, true)
		return
	}

	body.Filename = filename
	body.UserAddress = user_address

	srv.CallbackRegistry.HandleIncomingCode(&body)

	sendDocumentServerRespose(w, false)
}

func (srv *DefaultServerEndpointsHandler) Create(w http.ResponseWriter, r *http.Request) {

	query := r.URL.Query()
	fileExt := query.Get("fileExt")
	isSample := query.Get("sample")

	if strings.TrimSpace(fileExt) == "" || !utils.IsInList("."+fileExt, srv.specification.Extensions.Edited) {
		srv.logger.Errorf("%s extension is not supported", fileExt)
		http.Redirect(w, r, "/", http.StatusSeeOther)
		return
	}

	srv.logger.Debugf("Creating a new %s file", fileExt)

	var filename string

	if strings.TrimSpace(isSample) != "" {
		filename = "sample." + fileExt
	} else {
		filename = "new." + fileExt
	}

	file, _ := os.Open(path.Join("assets", filename))
	defer file.Close()

	filename = srv.StorageManager.GenerateVersionedFilename(filename, r.RemoteAddr)

	srv.StorageManager.CreateFile(file, srv.StorageManager.GenerateFilePath(filename, r.RemoteAddr))

	srv.HistoryManager.CreateMeta(filename, r.RemoteAddr, []models.Changes{
		{
			Created: time.Now().Format("2006-02-1 15:04:05"),
			User:    srv.getUserFromCookies(r),
		},
	})

	http.Redirect(w, r, "/editor?filename="+filename, http.StatusSeeOther)
}

func sendDocumentServerRespose(w http.ResponseWriter, isError bool) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(200)
	if isError {
		w.Write([]byte("{\"error\": 1}"))
	} else {
		w.Write([]byte("{\"error\": 0}"))
	}
}

func getCookiesInfo(req *http.Request) (string, string) {
	lang := "en"
	user_id := "uid-1"
	for _, cookie := range req.Cookies() {
		if cookie.Name == "ulang" {
			lang = cookie.Value
		}
		if cookie.Name == "uid" {
			user_id = cookie.Value
		}
	}
	return lang, user_id
}

func (srv DefaultServerEndpointsHandler) getUserFromCookies(req *http.Request) models.User {
	_, user_id := getCookiesInfo(req)
	user, _ := srv.UserManager.GetUserById(user_id)

	return user
}
