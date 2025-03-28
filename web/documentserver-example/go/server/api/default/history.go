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
	"net/http"
	"strconv"

	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
)

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
