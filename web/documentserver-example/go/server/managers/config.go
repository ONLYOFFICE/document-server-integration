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

package managers

import (
	"io"

	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/golang-jwt/jwt"
)

type DocumentManager interface {
	BuildDocumentConfig(parameters Editor, storage_address string) (*models.Config, error)
	IsDocumentConvertable(filename string) bool
}

type HistoryManager interface {
	GetHistory(filename string, storage_address string) (HistoryRefresh, []HistorySet)
	CreateMeta(filename string, storage_address string, changes []models.Changes)
	CreateHistory(callback_body models.Callback, storage_address string) error
}

//TODO: Refactoring
type StorageManager interface {
	GetRootFolder(storage_address string) string
	GenerateFilePath(file_name string, storage_address string) string
	GetStoredFiles(storage_address string) []models.Document
	GenerateFileHash(file_name string, storage_address string) string
	GenerateFileUri(original_filename string, storage_address string, meta FileMeta) string
	GenerateVersionedFilename(filename string, storage_address string) string
	CreateFile(stream io.Reader, path string) error
	CreateDirectory(path string) error
	PathExists(path string) bool
	RemoveFile(filename string, storage_address string) error
	ReadFile(file_path string) ([]byte, error)
	MoveFile(from string, to string) error
	SaveFileFromUri(body models.Callback) error
}

type UserManager interface {
	GetUsers() []models.User
	GetUserById(user_id string) (models.User, error)
}

type JwtManager interface {
	JwtSign(claims jwt.Claims, key []byte) (string, error)
	JwtDecode(jwtString string, key []byte) (jwt.MapClaims, error)
	ParseCallback(body *models.Callback, token_header string) error
}

type ConversionManager interface {
	GetFileType(filename string) string
	GetInternalExtension(fileType string) string
	IsCanConvert(ext string) bool
	GetConverterUri(docUri string, fromExt string, toExt string, docKey string, isAsync bool) (string, error)
}

type Managers struct {
	DocumentManager
	HistoryManager
	StorageManager
	UserManager
	JwtManager
	ConversionManager
}

func New(user_manager UserManager, storage_manager StorageManager,
	history_manager HistoryManager, document_manager DocumentManager,
	jwt_manager JwtManager, conversion_manager ConversionManager) *Managers {
	return &Managers{
		HistoryManager:    history_manager,
		StorageManager:    storage_manager,
		DocumentManager:   document_manager,
		UserManager:       user_manager,
		JwtManager:        jwt_manager,
		ConversionManager: conversion_manager,
	}
}
