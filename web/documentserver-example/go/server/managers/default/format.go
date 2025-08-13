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
	"encoding/json"
	"io"
	"os"
	"path/filepath"
	"runtime"
	"slices"

	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/utils"
)

type DefaultFormatManager struct {
	formats []models.Format
}

func NewDefaultFormatManager() (managers.FormatManager, error) {
	_, b, _, _ := runtime.Caller(0)
	parentDir := filepath.Dir(filepath.Dir(filepath.Dir(filepath.Dir(b))))
	path := filepath.Join(parentDir, "static", "assets", "document-formats", "onlyoffice-docs-formats.json")

	fileContent, err := os.Open(path)
	if err != nil {
		return DefaultFormatManager{}, err
	}
	defer fileContent.Close()

	byteResult, _ := io.ReadAll(fileContent)
	var formats []models.Format
	err = json.Unmarshal(byteResult, &formats)

	if err != nil {
		return DefaultFormatManager{}, err
	}

	return DefaultFormatManager{
		formats,
	}, err
}

func (fm DefaultFormatManager) GetAllFormats() []models.Format {
	return fm.formats
}

func (fm DefaultFormatManager) GetFormat(ext string) models.Format {
	for _, f := range fm.formats {
		if f.Name == ext {
			return f
		}
	}
	return models.Format{}
}

func (fm DefaultFormatManager) GetActions(ext string) []string {
	return fm.GetFormat(ext).Actions
}

func (fm DefaultFormatManager) GetTypeByExtension(ext string) string {
	return fm.GetFormat(ext).FormatType
}

func (fm DefaultFormatManager) GetFileType(filename string) string {
	ext := utils.GetFileExt(filename, true)
	return fm.GetTypeByExtension(ext)
}

func (fm DefaultFormatManager) HasAction(ext string, action string) bool {
	return slices.Contains(fm.GetActions(ext), action)
}
