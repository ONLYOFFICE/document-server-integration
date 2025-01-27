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

	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
)

func (srv *DefaultServerEndpointsHandler) Forgotten(w http.ResponseWriter, r *http.Request) {
	srv.logger.Debug("A new forgotten call")
	if !srv.config.ForgottenEnabled {
		shared.SendCustomErrorResponse(w, "The forgotten page is disabled")
		return
	}

	if r.Method == http.MethodDelete {
		filename := r.URL.Query().Get("fileName")
		if filename == "" {
			shared.SendCustomErrorResponse(w, "No filename")
		} else {
			r, err := srv.Managers.CommandManager.CommandRequest("deleteForgotten", filename, nil)
			if err != nil {
				srv.logger.Errorf("could not delete forgotten file: %s", err.Error())
				shared.SendDocumentServerRespose(w, true)
			} else {
				defer r.Body.Close()
				w.WriteHeader(http.StatusNoContent)
				shared.SendDocumentServerRespose(w, false)
			}
		}
		return
	}

	var forgottenList models.ForgottenList
	res, err := srv.Managers.CommandManager.CommandRequest("getForgottenList", "", nil)
	if err != nil {
		srv.logger.Errorf("could not fetch forgotten files: %s", err.Error())
	}
	defer res.Body.Close()
	if err = json.NewDecoder(res.Body).Decode(&forgottenList); err != nil {
		srv.logger.Errorf("could not parse forgotten files: %s", err.Error())
	}

	var files []models.ForgottenFile
	for _, key := range forgottenList.Keys {
		var file models.ForgottenFile
		res, err = srv.CommandRequest("getForgotten", key, nil)
		if err != nil {
			srv.logger.Errorf("could not fetch forgotten file[%s]: %s", file.Key, err.Error())
		} else {
			defer res.Body.Close()
			if err = json.NewDecoder(res.Body).Decode(&file); err != nil {
				srv.logger.Errorf("could not parse forgotten file[%s]: %s", file.Key, err.Error())
			} else {
				file.Type = srv.Managers.ConversionManager.GetFileType(file.Url)
				files = append(files, file)
			}
		}
	}

	data := map[string]interface{}{
		"Files": files,
	}

	forgottenTemplate.Execute(w, data) // nolint: errcheck
}
