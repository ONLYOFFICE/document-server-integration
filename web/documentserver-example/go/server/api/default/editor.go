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

	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
)

func (srv *DefaultServerEndpointsHandler) Editor(w http.ResponseWriter, r *http.Request) {
	var editorParameters managers.Editor
	if err := decoder.Decode(&editorParameters, r.URL.Query()); err != nil {
		srv.logger.Error("Invalid query parameters")
		return
	}

	if err := editorParameters.IsValid(); err != nil {
		srv.logger.Errorf("Editor parameters are invalid: %s", err.Error())
		return
	}

	srv.logger.Debug("A new editor call")
	editorParameters.Language, editorParameters.UserId = shared.GetCookiesInfo(r.Cookies())

	remoteAddr := generateUrl(r)
	if srv.config.ServerAddress != "" {
		remoteAddr = srv.config.ServerAddress
	}

	config, err := srv.Managers.DocumentManager.BuildDocumentConfig(editorParameters, remoteAddr)
	if err != nil {
		srv.logger.Errorf("A document manager error has occurred: %s", err.Error())
		return
	}

	var usersForMentions, usersForProtect, usersInfo []models.UserInfo
	if config.EditorConfig.User.Id != "uid-0" {
		usersForMentions = srv.GetUsersForMentions(config.EditorConfig.User.Id)
		usersForProtect = srv.GetUsersForProtect(config.EditorConfig.User.Id, remoteAddr)
		usersInfo = srv.GetUsersInfo(remoteAddr)
	}

	data := map[string]interface{}{
		"apijs":            srv.config.DocumentServerHost + srv.config.DocumentServerApi,
		"config":           config,
		"actionLink":       editorParameters.ActionLink,
		"docType":          config.DocumentType,
		"usersForProtect":  usersForProtect,
		"usersForMentions": usersForMentions,
		"usersInfo":        usersInfo,
		"dataInsertImage": map[string]interface{}{
			"fileType": "svg",
			"url":      remoteAddr + "/static/images/logo.svg",
		},
		"dataDocument": map[string]interface{}{
			"fileType": "docx",
			"url":      remoteAddr + "/static/assets/document-templates/sample/sample.docx",
		},
		"dataSpreadsheet": map[string]interface{}{
			"fileType": "csv",
			"url":      remoteAddr + "/static/assets/document-templates/sample/csv.csv",
		},
	}

	editorTemplate.Execute(w, data) // nolint: errcheck
}
