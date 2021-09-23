/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

package default_managers

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/utils"
	"go.uber.org/zap"
)

const (
	ONLYOFFICE_DOCUMENT     = "word"
	ONLYOFFICE_SPREADSHEET  = "cell"
	ONLYOFFICE_PRESENTATION = "slide"
)

type DefaultConversionManager struct {
	config        config.ApplicationConfig
	specification config.SpecificationConfig
	logger        *zap.SugaredLogger
	managers.JwtManager
}

func NewDefaultConversionManager(config config.ApplicationConfig, spec config.SpecificationConfig,
	logger *zap.SugaredLogger, jwt_manager managers.JwtManager) managers.ConversionManager {
	return &DefaultConversionManager{
		config,
		spec,
		logger,
		jwt_manager,
	}
}

func (cm DefaultConversionManager) GetFileType(filename string) string {
	ext := utils.GetFileExt(filename)

	exts := cm.specification.ExtensionTypes

	if utils.IsInList(ext, exts.Document) {
		return ONLYOFFICE_DOCUMENT
	}
	if utils.IsInList(ext, exts.Spreadsheet) {
		return ONLYOFFICE_SPREADSHEET
	}
	if utils.IsInList(ext, exts.Presentation) {
		return ONLYOFFICE_PRESENTATION
	}

	return ONLYOFFICE_DOCUMENT
}

func (cm DefaultConversionManager) GetInternalExtension(fileType string) string {
	switch fileType {
	case ONLYOFFICE_DOCUMENT:
		return ".docx"
	case ONLYOFFICE_SPREADSHEET:
		return ".xlsx"
	case ONLYOFFICE_PRESENTATION:
		return ".pptx"
	default:
		return ".docx"
	}
}

func (cm DefaultConversionManager) IsCanConvert(ext string) bool {
	return utils.IsInList(ext, cm.specification.Extensions.Converted)
}

//TODO: Refactoring
func (cm DefaultConversionManager) GetConverterUri(docUri string, fromExt string, toExt string, docKey string, isAsync bool) (string, error) {
	if fromExt == "" {
		fromExt = utils.GetFileExt(docUri)
	}

	title := utils.GetFileName(docUri)

	payload := managers.ConvertRequestPayload{
		DocUrl:     docUri,
		OutputType: strings.Replace(toExt, ".", "", -1),
		FileType:   strings.Replace(fromExt, ".", "", -1),
		Title:      title,
		Key:        docKey,
		Async:      isAsync,
	}

	var headerToken string
	secret := strings.TrimSpace(cm.config.JwtSecret)
	if secret != "" && cm.config.JwtEnabled {
		headerPayload := managers.ConvertRequestHeaderPayload{Payload: payload}
		var err error
		headerToken, err = cm.JwtManager.JwtSign(headerPayload, []byte(secret))
		if err != nil {
			return "", err
		}
		bodyToken, err := cm.JwtManager.JwtSign(payload, []byte(secret))
		if err != nil {
			return "", err
		}
		payload.JwtToken = bodyToken
	}

	requestBody, err := json.Marshal(payload)

	if err != nil {
		return "", err
	}

	req, err := http.NewRequest("POST", cm.config.DocumentServerHost+cm.config.DocumentServerConverter, bytes.NewReader(requestBody))
	if err != nil {
		return "", err
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	if headerToken != "" {
		req.Header.Set(cm.config.JwtHeader, "Bearer "+headerToken)
	}

	client := &http.Client{}
	response, err := client.Do(req)
	defer client.CloseIdleConnections()

	if err != nil {
		return "", err
	}

	jsonBody, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return "", err
	}

	var body managers.ConvertPayload
	json.Unmarshal(jsonBody, &body)

	return getResponseUri(body)
}

func getResponseUri(json managers.ConvertPayload) (string, error) {
	if json.Error < 0 {
		return "", fmt.Errorf("error occurred in the ConvertService: %d", json.Error)
	}

	return json.FileUrl, nil
}
