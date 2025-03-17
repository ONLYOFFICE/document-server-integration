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
	"net/http"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
	"github.com/ONLYOFFICE/document-server-integration/utils"
)

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

	srv.logger.Debug("A new convert call")

	var payload managers.ConvertRequest
	err = json.NewDecoder(r.Body).Decode(&payload)
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
	toExt := "ooxml"
	if payload.Filetype != "" {
		toExt = payload.Filetype
	}

	keepOriginal := payload.Keeporiginal

	if srv.DocumentManager.IsDocumentConvertable(filename) || payload.Filetype != "" {
		key, err := srv.StorageManager.GenerateFileHash(filename)
		if err != nil {
			response.Error = err.Error()
			srv.logger.Errorf("File conversion error: %s", err.Error())
			return
		}

		newUrl, newExt, err := srv.ConversionManager.GetConverterUri(fileUrl, fileExt, toExt, key, true, filename)
		if err != nil {
			response.Error = err.Error()
			srv.logger.Errorf("File conversion error: %s", err.Error())
			return
		}

		if newUrl == "" {
			response.Step = 1
		} else {
			response.Step = 100

			supportedExt := true
			fm, err := utils.NewFormatManager()
			for _, f := range fm.GetFormats() {
				if f.Name == newExt && len(f.Actions) == 0 {
					supportedExt = false
					break
				}
			}

			if !supportedExt && err == nil {
				response.Error = "FileTypeIsNotSupported"
				response.Filename = newUrl
				return
			} else if err != nil {
				response.Error = err.Error()
				return
			}

			correctName, err := srv.StorageManager.GenerateVersionedFilename(
				utils.GetFileNameWithoutExt(filename) + "." + newExt,
			)
			if err != nil {
				response.Error = err.Error()
				srv.logger.Errorf("File conversion error: %s", err.Error())
				return
			}

			err = srv.StorageManager.SaveFileFromUri(models.Callback{
				Url:         newUrl,
				Filename:    correctName,
				UserAddress: r.Host,
			})
			if err != nil {
				response.Error = err.Error()
				return
			}

			if !keepOriginal {
				err = srv.StorageManager.RemoveFile(filename)
				if err != nil {
					srv.logger.Errorf("File deletion error: %s", err.Error())
				}
			}
			response.Filename = correctName
			err = srv.HistoryManager.CreateMeta(response.Filename, models.History{
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
				srv.logger.Errorf("Meta creation error: %s", err.Error())
			}
		}
	}
}
