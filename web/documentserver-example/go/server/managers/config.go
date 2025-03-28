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
	"io"
	"net/http"

	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/golang-jwt/jwt"
)

type DocumentManager interface {
	BuildDocumentConfig(parameters Editor, storageAddress string) (*models.Config, error)
	IsDocumentConvertable(filename string) bool
}

type HistoryManager interface {
	GetHistory(filename, storageAddress string) (HistoryRefresh, []HistorySet, error)
	CreateMeta(filename string, history models.History) error
	CreateHistory(cbody models.Callback) error
	CountVersion(directory string) int
	GetFileData(filename string) map[string]string
}

type StorageManager interface {
	GetRootFolder() (string, error)
	GenerateFilePath(filename string) (string, error)
	GetStoredFiles(storageAddress string) ([]models.Document, error)
	GenerateFileHash(filename string) (string, error)
	GenerateFilestoreUri(originalName string, meta FileMeta) string
	GeneratePublicFileUri(originalName, storageAddress string, meta FileMeta) string
	GenerateVersionedFilename(filename string) (string, error)
	CreateFile(stream io.Reader, path string) error
	CreateDirectory(path string) error
	PathExists(path string) bool
	DirExists(path string) bool
	RemoveFile(filename string) error
	RemoveAll() error
	ReadFile(filePath string) ([]byte, error)
	MoveFile(from, to string) error
	SaveFileFromUri(body models.Callback) error
}

type UserManager interface {
	GetUsers() []models.User
	GetUserById(uid string) (models.User, error)
	GetUserInfoById(uid string, serverAddress string) models.UserInfo
	GetUsersForMentions(uid string) []models.UserInfo
	GetUsersForProtect(uid string, serverAddress string) []models.UserInfo
	GetUsersInfo(serverAddress string) []models.UserInfo
}

type JwtManager interface {
	JwtSign(claims jwt.Claims, key []byte) (string, error)
	JwtDecode(jwtString string, key []byte) (jwt.MapClaims, error)
	ParseCallback(body *models.Callback, tokenHeader string) error
}

type ConversionManager interface {
	GetFileType(filename string) string
	GetInternalExtension(fileType string) string
	IsCanConvert(ext string) bool
	IsCanFill(ext string) bool
	GetConverterUri(docUri, fromExt, toExt, docKey string, isAsync bool, title string) (string, string, error)
}

type CommandManager interface {
	CommandRequest(method string, docKey string, meta interface{}) (*http.Response, error)
}

type Managers struct {
	DocumentManager
	HistoryManager
	StorageManager
	UserManager
	JwtManager
	ConversionManager
	CommandManager
}

func New(umanager UserManager, smanager StorageManager,
	hmanager HistoryManager, dmanager DocumentManager,
	jmanager JwtManager, cmanager ConversionManager,
	commanager CommandManager) *Managers {
	return &Managers{
		HistoryManager:    hmanager,
		StorageManager:    smanager,
		DocumentManager:   dmanager,
		UserManager:       umanager,
		JwtManager:        jmanager,
		ConversionManager: cmanager,
		CommandManager:    commanager,
	}
}
