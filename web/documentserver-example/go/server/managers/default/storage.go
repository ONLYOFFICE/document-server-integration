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
	"crypto/md5"
	"fmt"
	"io"
	"net/http"
	"os"
	"path"
	"sort"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
	"github.com/ONLYOFFICE/document-server-integration/utils"
	"go.uber.org/zap"
)

type DefaultStorageManager struct {
	config        config.ApplicationConfig
	specification config.SpecificationConfig
	logger        *zap.SugaredLogger
	managers.ConversionManager
}

func NewDefaultStorageManager(config config.ApplicationConfig, specification config.SpecificationConfig,
	logger *zap.SugaredLogger, conversionManager managers.ConversionManager) managers.StorageManager {
	return &DefaultStorageManager{
		config,
		specification,
		logger,
		conversionManager,
	}
}

func (sm DefaultStorageManager) GetRootFolder() (string, error) {
	dir := path.Join("./static", sm.config.StoragePath)

	if err := sm.CreateDirectory(dir); err != nil {
		return "", err
	}

	return dir, nil
}

func (sm DefaultStorageManager) GenerateFilePath(fileName string) (string, error) {
	dir, err := sm.GetRootFolder()
	if err != nil {
		return "", err
	}

	return path.Join(dir, fileName), nil
}

func (sm DefaultStorageManager) GetStoredFiles(remoteAddress string) ([]models.Document, error) {
	var documents []models.Document
	dir, err := sm.GetRootFolder()
	if err != nil {
		return documents, err
	}

	directory, err := os.ReadDir(dir)
	if err != nil {
		return documents, err
	}

	var files []os.FileInfo
	for _, d := range directory {
		f, err := d.Info()
		if err != nil {
			return documents, err
		}

		files = append(files, f)
	}

	sort.Slice(files, func(i, j int) bool {
		return !files[i].ModTime().Before(files[j].ModTime())
	})

	for _, v := range files {
		if v.IsDir() {
			continue
		}
		filename := v.Name()

		version := 1
		hpath := path.Join(dir, filename+shared.ONLYOFFICE_HISTORY_POSTFIX, fmt.Sprint(version))
		for {
			if sm.PathExists(hpath) {
				version++
				hpath = path.Join(dir, filename+shared.ONLYOFFICE_HISTORY_POSTFIX, fmt.Sprint(version))
			} else {
				break
			}
		}

		documents = append(documents, models.Document{
			FileType: sm.ConversionManager.GetFileType(filename),
			Title:    filename,
			Url:      sm.GeneratePublicFileUri(filename, remoteAddress, managers.FileMeta{}),
			CanEdit:  !sm.ConversionManager.IsCanConvert(utils.GetFileExt(filename, true)),
			CanFill:  sm.ConversionManager.IsCanFill(utils.GetFileExt(filename, true)),
			Version:  fmt.Sprint(version),
		})
	}

	sm.logger.Debugf("Fetched %d document(s)", len(documents))
	return documents, nil
}

func (sm DefaultStorageManager) GenerateFileHash(filename string) (string, error) {
	fpath, err := sm.GenerateFilePath(filename)
	if err != nil {
		return "", err
	}

	stat, err := os.Stat(fpath)
	if err != nil {
		return "", err
	}

	return fmt.Sprintf("%x", md5.Sum([]byte(filename+stat.ModTime().Format(time.RFC3339)))), nil
}

func (sm DefaultStorageManager) GenerateFilestoreUri(originalName string, meta managers.FileMeta) string {
	if (managers.FileMeta{}) == meta {
		sm.logger.Debugf("Generating file %s uri", originalName)
		return fmt.Sprintf(
			"/static/%s/%s",
			sm.config.StoragePath,
			originalName,
		)
	}
	sm.logger.Debugf("Generating file %s uri", meta.DestinationPath)
	return fmt.Sprintf(
		"/static/%s/%s/%s/%s",
		sm.config.StoragePath,
		originalName+shared.ONLYOFFICE_HISTORY_POSTFIX,
		fmt.Sprint(meta.Version),
		meta.DestinationPath,
	)
}

func (sm DefaultStorageManager) GeneratePublicFileUri(
	originalName string,
	remoteAddress string,
	meta managers.FileMeta,
) string {
	if meta.Version == 0 || meta.DestinationPath == "" {
		sm.logger.Debugf("Generating file %s uri", originalName)
		return fmt.Sprintf(
			"%s/download?%s",
			remoteAddress,
			"fileName="+originalName,
		)
	}

	sm.logger.Debugf("Generating file %s uri", meta.DestinationPath)
	return fmt.Sprintf(
		"%s/history?%s&%s&%s",
		remoteAddress,
		"fileName="+originalName,
		"file="+meta.DestinationPath,
		"ver="+fmt.Sprint(meta.Version),
	)
}

func (sm DefaultStorageManager) GenerateVersionedFilename(filename string) (string, error) {
	basename := utils.GetFileNameWithoutExt(filename)
	ext := utils.GetFileExt(filename, false)
	name := fmt.Sprintf("%s%s", basename, ext)

	i := 1

	for {
		fpath, err := sm.GenerateFilePath(name)
		if err != nil {
			return "", err
		}

		if _, err := os.Stat(fpath); os.IsNotExist(err) {
			break
		}

		name = fmt.Sprintf("%s(%d)%s", basename, i, ext)
		i++
	}

	return name, nil
}

func (sm DefaultStorageManager) CreateFile(stream io.Reader, path string) error {
	newFile, err := os.Create(path)
	if err != nil {
		return err
	}
	defer newFile.Close()

	_, err = io.Copy(newFile, stream)
	if err != nil {
		return err
	}

	return nil
}

func (sm DefaultStorageManager) CreateDirectory(path string) error {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		err = os.MkdirAll(path, 0777)

		return err
	} else {
		return err
	}
}

func (sm DefaultStorageManager) PathExists(path string) bool {
	if _, err := os.Stat(path); !os.IsNotExist(err) {
		return true
	}
	return false
}

func (sm DefaultStorageManager) DirExists(path string) bool {
	if stat, err := os.Stat(path); err == nil && stat.IsDir() {
		return true
	}
	return false
}

func (sm DefaultStorageManager) RemoveFile(filename string) error {
	fpath, err := sm.GenerateFilePath(filename)
	if err != nil {
		return err
	}

	if _, err := os.Stat(fpath); os.IsNotExist(err) {
		return err
	}

	rootPath, err := sm.GetRootFolder()
	if err != nil {
		return err
	}

	hpath := path.Join(rootPath, filename+shared.ONLYOFFICE_HISTORY_POSTFIX)

	os.Remove(fpath)
	os.RemoveAll(hpath)

	return nil
}

func (sm DefaultStorageManager) RemoveAll() error {
	rf, err := sm.GetRootFolder()

	if err != nil {
		return err
	}

	os.RemoveAll(rf)

	return nil
}

func (sm DefaultStorageManager) ReadFile(fpath string) ([]byte, error) {
	file, err := os.ReadFile(fpath)

	if err != nil {
		return nil, err
	}

	return file, nil
}

func (sm DefaultStorageManager) MoveFile(from string, to string) error {
	return os.Rename(from, to)
}

func (sm DefaultStorageManager) SaveFileFromUri(body models.Callback) error {
	resp, err := http.Get(body.Url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	fpath, err := sm.GenerateFilePath(body.Filename)
	if err != nil {
		return err
	}

	return sm.CreateFile(resp.Body, fpath)
}
