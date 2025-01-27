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

	"github.com/ONLYOFFICE/document-server-integration/server/shared"
)

func (srv *DefaultServerEndpointsHandler) Remove(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodDelete {
		if err := srv.StorageManager.RemoveAll(); err != nil {
			shared.SendDocumentServerRespose(w, true)
			return
		}

		r := map[string]interface{}{
			"success": true,
		}
		shared.SendResponse(w, r)
		return
	}

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
