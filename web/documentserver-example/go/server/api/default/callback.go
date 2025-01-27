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
