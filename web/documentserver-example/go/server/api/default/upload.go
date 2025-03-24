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
	"fmt"
	"net/http"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
)

func (srv *DefaultServerEndpointsHandler) Upload(w http.ResponseWriter, r *http.Request) {
	err := r.ParseMultipartForm(32 << 20)
	if err != nil {
		srv.logger.Error(err.Error())
		return
	}

	file, handler, err := r.FormFile("uploadedFile")
	w.Header().Set("Content-Type", "application/json")

	if err != nil {
		srv.logger.Error(err.Error())
		return
	}

	srv.logger.Debug("A new upload call")
	if !srv.DocumentManager.IsDocumentConvertable(handler.Filename) {
		srv.logger.Errorf("File %s is not supported", handler.Filename)
		shared.SendResponse(w, map[string]string{"error": "File type is not supported"})
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

	err = srv.HistoryManager.CreateMeta(fileName, models.History{
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
		srv.logger.Errorf("could not create meta")
	}

	fmt.Fprintf(w, "{\"filename\":\"%s\"}", fileName)
}
