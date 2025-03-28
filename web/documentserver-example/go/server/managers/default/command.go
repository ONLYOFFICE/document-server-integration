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
package dmanager

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/golang-jwt/jwt"
)

type DefaultCommandManager struct {
	config config.ApplicationConfig
	managers.JwtManager
}

type CommandPayload struct {
	C     string      `json:"c"`
	Key   string      `json:"key"`
	Meta  interface{} `json:"meta"`
	Token string      `json:"token"`
	jwt.StandardClaims
}

type CommandRequestHeaderPayload struct {
	Query   map[string]string `json:"query"`
	Payload CommandPayload    `json:"payload"`
	jwt.StandardClaims
}

func NewDefaultCommandManager(config config.ApplicationConfig, jmanager managers.JwtManager) managers.CommandManager {
	return &DefaultCommandManager{
		config,
		jmanager,
	}
}

func (cm DefaultCommandManager) CommandRequest(method string, docKey string, meta interface{}) (*http.Response, error) {
	payload := CommandPayload{
		C:   method,
		Key: docKey,
		StandardClaims: jwt.StandardClaims{
			ExpiresAt: time.Now().Add(time.Minute * cm.config.JwtExpiresIn).Unix(),
			IssuedAt:  time.Now().Unix(),
		},
	}
	if meta != nil {
		payload.Meta = meta
	}
	uri := cm.config.DocumentServerHost + cm.config.DocumentServerCommandUrl
	var err error
	var headerToken string
	secret := strings.TrimSpace(cm.config.JwtSecret)
	if secret != "" && cm.config.JwtEnabled {
		headerPayload := fillJwtByUrl(uri, payload, cm.config)
		headerToken, err = cm.JwtManager.JwtSign(headerPayload, []byte(secret))
		if err != nil {
			return nil, err
		}
		payload.Token, err = cm.JwtManager.JwtSign(payload, []byte(secret))
		if err != nil {
			return nil, err
		}
	}

	requestBody, err := json.Marshal(payload)
	if err != nil {
		return nil, err
	}

	req, err := http.NewRequest(http.MethodPost, uri, bytes.NewReader(requestBody))
	if err != nil {
		return nil, err
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	if headerToken != "" {
		req.Header.Set(cm.config.JwtHeader, "Bearer "+headerToken)
	}

	response, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, err
	}

	return response, nil
}

func fillJwtByUrl(uri string, payload CommandPayload, config config.ApplicationConfig) CommandRequestHeaderPayload {
	urlObj, _ := url.Parse(uri)
	query, _ := url.ParseQuery(urlObj.RawQuery)
	queryMap := make(map[string]string)
	for k, v := range query {
		queryMap[k] = v[0]
	}

	return CommandRequestHeaderPayload{
		Query:   queryMap,
		Payload: payload,
		StandardClaims: jwt.StandardClaims{
			ExpiresAt: time.Now().Add(time.Minute * config.JwtExpiresIn).Unix(),
			IssuedAt:  time.Now().Unix(),
		},
	}
}
