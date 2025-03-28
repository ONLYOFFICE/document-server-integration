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
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
	"github.com/ONLYOFFICE/document-server-integration/utils"
	"github.com/golang-jwt/jwt"
	"go.uber.org/zap"
)

type DefaultConversionManager struct {
	config        config.ApplicationConfig
	specification config.SpecificationConfig
	logger        *zap.SugaredLogger
	managers.JwtManager
}

func NewDefaultConversionManager(config config.ApplicationConfig, spec config.SpecificationConfig,
	logger *zap.SugaredLogger, jmanager managers.JwtManager) managers.ConversionManager {
	return &DefaultConversionManager{
		config,
		spec,
		logger,
		jmanager,
	}
}

func (cm DefaultConversionManager) GetFileType(filename string) string {
	ext := utils.GetFileExt(filename, true)

	exts := cm.specification.ExtensionTypes

	if utils.IsInList(ext, exts.Pdf) {
		return shared.ONLYOFFICE_PDF
	}
	if utils.IsInList(ext, exts.Document) {
		return shared.ONLYOFFICE_DOCUMENT
	}
	if utils.IsInList(ext, exts.Spreadsheet) {
		return shared.ONLYOFFICE_SPREADSHEET
	}
	if utils.IsInList(ext, exts.Presentation) {
		return shared.ONLYOFFICE_PRESENTATION
	}

	return shared.ONLYOFFICE_DOCUMENT
}

func (cm DefaultConversionManager) GetInternalExtension(fileType string) string {
	switch fileType {
	case shared.ONLYOFFICE_DOCUMENT:
		return ".docx"
	case shared.ONLYOFFICE_SPREADSHEET:
		return ".xlsx"
	case shared.ONLYOFFICE_PRESENTATION:
		return ".pptx"
	default:
		return ".docx"
	}
}

func (cm DefaultConversionManager) IsCanFill(ext string) bool {
	return utils.IsInList(ext, cm.specification.Extensions.Filled)
}

func (cm DefaultConversionManager) IsCanConvert(ext string) bool {
	return utils.IsInList(ext, cm.specification.Extensions.Converted)
}

func (cm DefaultConversionManager) GetConverterUri(
	docUri string,
	fromExt string,
	toExt string,
	docKey string,
	isAsync bool,
	title string,
) (string, string, error) {
	if fromExt == "" {
		fromExt = utils.GetFileExt(docUri, true)
	}

	payload := managers.ConvertRequestPayload{
		DocUrl:     docUri,
		OutputType: strings.ReplaceAll(toExt, ".", ""),
		FileType:   fromExt,
		Title:      title,
		Key:        docKey,
		Async:      isAsync,
		StandardClaims: jwt.StandardClaims{
			ExpiresAt: time.Now().Add(time.Minute * cm.config.JwtExpiresIn).Unix(),
			IssuedAt:  time.Now().Unix(),
		},
	}

	var headerToken string
	var err error

	secret := strings.TrimSpace(cm.config.JwtSecret)
	if secret != "" && cm.config.JwtEnabled {
		headerPayload := managers.ConvertRequestHeaderPayload{
			Payload: payload,
			StandardClaims: jwt.StandardClaims{
				ExpiresAt: time.Now().Add(time.Minute * cm.config.JwtExpiresIn).Unix(),
				IssuedAt:  time.Now().Unix(),
			},
		}
		headerToken, err = cm.JwtManager.JwtSign(headerPayload, []byte(secret))
		if err != nil {
			return "", "", err
		}

		bodyToken, err := cm.JwtManager.JwtSign(payload, []byte(secret))
		if err != nil {
			return "", "", err
		}

		payload.JwtToken = bodyToken
	}

	requestBody, err := json.Marshal(payload)
	if err != nil {
		return "", "", err
	}

	req, err := http.NewRequest(
		http.MethodPost,
		cm.config.DocumentServerHost+cm.config.DocumentServerConverter,
		bytes.NewReader(requestBody),
	)
	if err != nil {
		return "", "", err
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	if headerToken != "" {
		req.Header.Set(cm.config.JwtHeader, "Bearer "+headerToken)
	}

	response, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", "", err
	}

	defer response.Body.Close()
	jsonBody, err := io.ReadAll(response.Body)
	if err != nil {
		return "", "", err
	}

	var body managers.ConvertPayload
	if err := json.Unmarshal(jsonBody, &body); err != nil {
		return "", "", err
	}

	return getResponseUri(body)
}

func getResponseUri(json managers.ConvertPayload) (string, string, error) {
	if json.Error < 0 {
		return "", "", fmt.Errorf("error occurred in the ConvertService: %d", json.Error)
	}

	return json.FileUrl, json.FileType, nil
}
