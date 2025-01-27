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

	"github.com/ONLYOFFICE/document-server-integration/server/shared"
)

func (srv *DefaultServerEndpointsHandler) HistoryObj(w http.ResponseWriter, r *http.Request) {
	var body map[string]string
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		srv.logger.Error("HistoryObj body decoding error")
		shared.SendCustomErrorResponse(w, err.Error())
		return
	}

	fileName := body["fileName"]
	if fileName == "" {
		srv.logger.Error("No filename in historyObj request")
		shared.SendCustomErrorResponse(w, "No filename")
		return
	}

	remoteAddr := generateUrl(r)
	refHist, setHist, err := srv.Managers.HistoryManager.GetHistory(fileName, remoteAddr)
	if err != nil {
		srv.logger.Warnf("could not get file history: %s", err.Error())
		shared.SendCustomErrorResponse(w, err.Error())
	}

	refHist.HistoryData = setHist
	shared.SendResponse(w, refHist)
}
