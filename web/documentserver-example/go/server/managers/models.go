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
package managers

import (
	"errors"

	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/golang-jwt/jwt"
)

var ErrInvalidFilename = errors.New("invalid filename")

type HistoryRefresh struct {
	CurrentVersion string           `json:"currentVersion"`
	History        []models.History `json:"history"`
	HistoryData    []HistorySet     `json:"historyData"`
}

type HistorySet struct {
	ChangesUrl string           `json:"changesUrl,omitempty"`
	Key        string           `json:"key"`
	Previous   *HistoryPrevious `json:"previous,omitempty"`
	Url        string           `json:"url"`
	Version    int              `json:"version"`
	Token      string           `json:"token,omitempty"`
	jwt.StandardClaims
}

type HistoryPrevious struct {
	Key string `json:"key"`
	Url string `json:"url"`
}

type FileMeta struct {
	Version         int
	DestinationPath string
}

type Editor struct {
	Filename        string `json:"filename"`
	Mode            string `json:"mode"`
	Type            string `json:"type"`
	ActionLink      string `json:"actionLink"`
	Language        string `json:"language"`
	UserId          string
	CanEdit         bool
	PermissionsMode string
}

func (ep *Editor) IsValid() error {
	if ep.Filename == "" {
		return ErrInvalidFilename
	}
	return nil
}

type ConvertRequest struct {
	Filename     string `json:"filename"`
	Filepass     string `json:"filePass"`
	Filetype     string `json:"fileExt"`
	Keeporiginal bool   `json:"keepOriginal"`
}

type ConvertResponse struct {
	Error    string `json:"error"`
	Step     int    `json:"step"`
	Filename string `json:"filename"`
}

type ConvertPayload struct {
	IsConverted bool   `json:"endConvert"`
	FileUrl     string `json:"fileUrl"`
	FileType    string `json:"fileType"`
	Percent     int    `json:"percent"`
	Error       int    `json:"error"`
}

type ConvertRequestPayload struct {
	DocUrl     string `json:"url"`
	OutputType string `json:"outputtype"`
	FileType   string `json:"filetype"`
	Title      string `json:"title"`
	Key        string `json:"key"`
	Async      bool   `json:"async"`
	JwtToken   string `json:"token,omitempty"`
	jwt.StandardClaims
}

type ConvertRequestHeaderPayload struct {
	Payload ConvertRequestPayload `json:"payload"`
	jwt.StandardClaims
}
