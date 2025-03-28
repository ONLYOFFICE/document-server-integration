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
	"net/url"
	"strings"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
	"github.com/ONLYOFFICE/document-server-integration/utils"
	"github.com/golang-jwt/jwt"
)

func (srv *DefaultServerEndpointsHandler) Reference(w http.ResponseWriter, r *http.Request) {
	var body models.Reference
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		srv.logger.Error("Reference body decoding error")
		shared.SendDocumentServerRespose(w, true)
		return
	}

	remoteAddr := generateUrl(r)
	var fileName /*, userAddress*/ string

	var fileKey map[string]string
	err := json.Unmarshal([]byte(body.ReferenceData.FileKey), &fileKey)
	if err == nil {
		path, _ := srv.GenerateFilePath(fileKey["fileName"])
		if body.ReferenceData.InstanceId == remoteAddr && srv.PathExists(path) {
			fileName = fileKey["fileName"]
		}
	}

	if fileName == "" && body.Link != "" {
		if strings.Contains(body.Link, remoteAddr) {
			res := map[string]interface{}{
				"url": body.Link,
			}
			shared.SendResponse(w, res)
			return
		}

		urlObj, _ := url.Parse(body.Link)
		params, _ := url.ParseQuery(urlObj.RawQuery)
		if len(params["filename"]) != 0 {
			fileName = params["filename"][0]
		}
		path, _ := srv.GenerateFilePath(fileName)
		if !srv.PathExists(path) {
			shared.SendCustomErrorResponse(w, "File is not exist")
			return
		}
	}

	if fileName == "" && body.Path != "" {
		filePath := utils.GetFileName(body.Path)
		path, _ := srv.GenerateFilePath(filePath)
		if srv.PathExists(path) {
			fileName = filePath
		}
	}

	if fileName == "" {
		shared.SendCustomErrorResponse(w, "File is not found")
		return
	}

	docKey, _ := srv.GenerateFileHash(fileName)
	data := models.Reference{
		FileType: srv.GetFileType(fileName),
		Key:      docKey,
		Url:      srv.GeneratePublicFileUri(fileName, remoteAddr, managers.FileMeta{}),
		ReferenceData: models.ReferenceData{
			FileKey:    fmt.Sprintf("{\"fileName\":\"%s\"}", fileName),
			InstanceId: remoteAddr,
		},
		Link: remoteAddr + "/editor?filename=" + url.QueryEscape(fileName),
		Path: fileName,
		StandardClaims: jwt.StandardClaims{
			ExpiresAt: time.Now().Add(time.Minute * srv.config.JwtExpiresIn).Unix(),
			IssuedAt:  time.Now().Unix(),
		},
	}

	secret := strings.TrimSpace(srv.config.JwtSecret)
	if secret != "" && srv.config.JwtEnabled {
		token, _ := srv.JwtSign(data, []byte(secret))
		data.Token = token
	}

	shared.SendResponse(w, data)
}
