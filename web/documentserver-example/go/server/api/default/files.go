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

	"github.com/ONLYOFFICE/document-server-integration/server/shared"
)

func (srv *DefaultServerEndpointsHandler) Files(w http.ResponseWriter, r *http.Request) {
	srv.logger.Debug("A new files call")
	files, err := srv.Managers.StorageManager.GetStoredFiles(r.Host)
	if err != nil {
		srv.logger.Errorf("could not fetch files: %s", err.Error())
		shared.SendCustomErrorResponse(w, fmt.Sprintf("could not fetch files: %s", err.Error()))
		return
	}

	shared.SendResponse(w, files)
}
