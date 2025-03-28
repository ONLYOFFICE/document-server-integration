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
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
	"github.com/golang-jwt/jwt"
)

func (srv *DefaultServerEndpointsHandler) Config(w http.ResponseWriter, r *http.Request) {
	filename := r.URL.Query().Get("fileName")
	var permissions models.Permissions
	err := json.Unmarshal([]byte(r.URL.Query().Get("permissions")), &permissions)
	if err != nil {
		shared.SendDocumentServerRespose(w, true)
		return
	}

	path, err := srv.GenerateFilePath(filename)
	if err != nil || filename == "" || !srv.PathExists(path) {
		shared.SendCustomErrorResponse(w, "File not found")
		return
	}

	remoteAddr := generateUrl(r)
	docKey, err := srv.StorageManager.GenerateFileHash(filename)
	if err != nil {
		shared.SendDocumentServerRespose(w, true)
		return
	}

	config := models.Config{
		Document: models.Document{
			Title: filename,
			Url: srv.StorageManager.GeneratePublicFileUri(
				filename, remoteAddr, managers.FileMeta{}),
			Key:         docKey,
			Permissions: permissions,
			ReferenceData: models.ReferenceData{
				FileKey:    fmt.Sprintf("{\"fileName\":\"%s\"}", filename),
				InstanceId: remoteAddr,
			},
		},
		EditorConfig: models.EditorConfig{
			CallbackUrl: fmt.Sprintf(
				"%s/callback?filename=%s&user_address=%s",
				remoteAddr,
				filename,
				remoteAddr,
			),
			Mode: "edit",
		},
		StandardClaims: jwt.StandardClaims{
			ExpiresAt: time.Now().Add(time.Minute * srv.config.JwtExpiresIn).Unix(),
			IssuedAt:  time.Now().Unix(),
		},
	}

	secret := strings.TrimSpace(srv.config.JwtSecret)
	if secret != "" && srv.config.JwtEnabled {
		token, _ := srv.JwtManager.JwtSign(config, []byte(secret))
		config.Token = token
	}

	shared.SendResponse(w, config)
}
