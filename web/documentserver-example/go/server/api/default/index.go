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
)

func (srv *DefaultServerEndpointsHandler) Index(w http.ResponseWriter, r *http.Request) {
	srv.logger.Debug("A new index call")
	files, err := srv.Managers.StorageManager.GetStoredFiles(r.Host)
	if err != nil {
		srv.logger.Errorf("could not fetch files: %s", err.Error())
	}

	data := map[string]interface{}{
		"Extensions":       srv.specification.Extensions,
		"Users":            srv.Managers.UserManager.GetUsers(),
		"Files":            files,
		"Preloader":        srv.config.DocumentServerHost + srv.config.DocumentServerPreloader,
		"ForgottenEnabled": srv.config.ForgottenEnabled,
		"Languages":        srv.config.Languages,
		"ServerVersion":    srv.config.Version,
	}

	indexTemplate.Execute(w, data) // nolint: errcheck
}
