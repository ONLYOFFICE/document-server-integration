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
	"github.com/ONLYOFFICE/document-server-integration/utils"
)

func (srv *DefaultServerEndpointsHandler) Rename(w http.ResponseWriter, r *http.Request) {
	var body map[string]string
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		srv.logger.Error("Rename body decoding error")
		shared.SendCustomErrorResponse(w, err.Error())
		return
	}

	newFileName, ext, docKey := body["newfilename"], body["ext"], body["dockey"]
	if newFileName == "" || docKey == "" {
		srv.logger.Error("No filename or dockey")
		shared.SendCustomErrorResponse(w, "No filename or dockey")
		return
	}

	if curExt := utils.GetFileExt(newFileName, true); curExt != ext {
		newFileName += "." + ext
	}
	meta := map[string]string{
		"title": newFileName,
	}

	commandResponse, err := srv.CommandManager.CommandRequest("meta", docKey, meta)
	if err != nil {
		srv.logger.Error("Command request error")
		shared.SendCustomErrorResponse(w, err.Error())
		return
	}
	defer commandResponse.Body.Close()

	var res map[string]interface{}
	if err := json.NewDecoder(commandResponse.Body).Decode(&res); err != nil {
		srv.logger.Error("Command response body decoding error")
		shared.SendCustomErrorResponse(w, err.Error())
		return
	}
	result := map[string]interface{}{
		"result": res,
	}
	shared.SendResponse(w, result)
}
