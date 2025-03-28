/**
 *
 * (c) Copyright Ascensio System SIA 2025
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
	"net/http"
	"os"
	"path"
	"strings"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
	"github.com/ONLYOFFICE/document-server-integration/utils"
)

func (srv *DefaultServerEndpointsHandler) Create(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodPost {
		var body map[string]string
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			srv.logger.Error("Reference body decoding error")
			shared.SendDocumentServerRespose(w, true)
			return
		}

		fileName, url := body["title"], body["url"]
		if fileName == "" || url == "" {
			srv.logger.Error("empty url or title")
			shared.SendCustomErrorResponse(w, "empty url or title")
			return
		}

		_, uid := shared.GetCookiesInfo(r.Cookies())
		user, err := srv.UserManager.GetUserById(uid)
		if err != nil {
			srv.logger.Errorf("could not find user with id: %s", uid)
			shared.SendCustomErrorResponse(w, err.Error())
			return
		}

		fileExt := utils.GetFileExt(fileName, true)
		if strings.TrimSpace(fileExt) == "" || !utils.IsInList(fileExt, srv.specification.Extensions.Viewed) {
			srv.logger.Errorf("%s extension is not supported", fileExt)
			shared.SendCustomErrorResponse(w, "extension is not supported")
			return
		}

		correctName, err := srv.StorageManager.GenerateVersionedFilename(fileName)
		if err != nil {
			srv.logger.Errorf("file saving error: %s", err.Error())
			shared.SendCustomErrorResponse(w, "file saving error")
			return
		}

		err = srv.StorageManager.SaveFileFromUri(models.Callback{
			Url:         url,
			Filename:    correctName,
			UserAddress: r.Host,
		})
		if err != nil {
			srv.logger.Errorf("file saving error: %s", err.Error())
			shared.SendCustomErrorResponse(w, "file saving error")
			return
		}

		err = srv.HistoryManager.CreateMeta(correctName, models.History{
			ServerVersion: srv.config.Version,
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
		if err != nil {
			srv.logger.Errorf("meta creation error: %s", err.Error())
		}

		res := map[string]interface{}{
			"file": correctName,
		}
		shared.SendResponse(w, res)
		return
	}

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

	err = srv.StorageManager.CreateFile(file, fpath)
	if err != nil {
		srv.logger.Errorf("could not create file: %s", filename)
		http.Redirect(w, r, "/", http.StatusSeeOther)
		return
	}

	err = srv.HistoryManager.CreateMeta(filename, models.History{
		ServerVersion: srv.config.Version,
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
	if err != nil {
		srv.logger.Errorf("could not create file meta: %s", filename)
	}

	http.Redirect(w, r, "/editor?filename="+filename, http.StatusSeeOther)
}
