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
package utils

import (
	"encoding/json"
	"io"
	"os"
	"path/filepath"
	"runtime"
	"slices"
)

type Format struct {
	Name       string   `json:"name"`
	FormatType string   `json:"type"`
	Actions    []string `json:"actions"`
	Convert    []string `json:"convert"`
	Mime       []string `json:"mime"`
}

type DefaultFormatManager struct {
	formats []Format
}

type FormatManager interface {
	GetFormats() []Format
	GetViewedExtensions() []string
	GetEditedExtensions() []string
	GetConvertedExtensions() []string
	GetFilledExtensions() []string
	GetDocumentExtensions() []string
	GetSpreadsheetExtensions() []string
	GetPresentationExtensions() []string
	GetPdfExtensions() []string
}

func NewFormatManager() (FormatManager, error) {
	_, b, _, _ := runtime.Caller(0)
	parentDir := filepath.Dir(filepath.Dir(b))
	path := filepath.Join(parentDir, "static", "assets", "document-formats", "onlyoffice-docs-formats.json")

	fileContent, err := os.Open(path)
	if err != nil {
		return DefaultFormatManager{}, err
	}
	defer fileContent.Close()

	byteResult, _ := io.ReadAll(fileContent)
	var formats []Format
	err = json.Unmarshal(byteResult, &formats)

	if err != nil {
		return DefaultFormatManager{}, err
	}

	return DefaultFormatManager{
		formats,
	}, err
}

func (fm DefaultFormatManager) GetFormats() []Format {
	return fm.formats
}

func (fm DefaultFormatManager) GetViewedExtensions() (viewed []string) {
	for _, f := range fm.formats {
		if slices.Contains(f.Actions, "view") {
			viewed = append(viewed, f.Name)
		}
	}
	return
}

func (fm DefaultFormatManager) GetEditedExtensions() (edited []string) {
	for _, f := range fm.formats {
		if slices.Contains(f.Actions, "edit") {
			edited = append(edited, f.Name)
		}
	}
	return
}

func (fm DefaultFormatManager) GetConvertedExtensions() (converted []string) {
	for _, f := range fm.formats {
		if slices.Contains(f.Actions, "auto-convert") {
			converted = append(converted, f.Name)
		}
	}
	return converted
}

func (fm DefaultFormatManager) GetFilledExtensions() (filled []string) {
	for _, f := range fm.formats {
		if slices.Contains(f.Actions, "fill") {
			filled = append(filled, f.Name)
		}
	}
	return filled
}

func (fm DefaultFormatManager) GetDocumentExtensions() (word []string) {
	for _, f := range fm.formats {
		if f.FormatType == "word" {
			word = append(word, f.Name)
		}
	}
	return
}

func (fm DefaultFormatManager) GetSpreadsheetExtensions() (cell []string) {
	for _, f := range fm.formats {
		if f.FormatType == "cell" {
			cell = append(cell, f.Name)
		}
	}
	return
}

func (fm DefaultFormatManager) GetPresentationExtensions() (slide []string) {
	for _, f := range fm.formats {
		if f.FormatType == "slide" {
			slide = append(slide, f.Name)
		}
	}
	return
}

func (fm DefaultFormatManager) GetPdfExtensions() (slide []string) {
	for _, f := range fm.formats {
		if f.FormatType == "pdf" {
			slide = append(slide, f.Name)
		}
	}
	return
}
