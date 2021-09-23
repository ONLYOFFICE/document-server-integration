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
	"crypto/md5"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"os"
	"path"
	"sort"
	"strings"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
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
	logger *zap.SugaredLogger, conversion_manager managers.ConversionManager) managers.StorageManager {
	return &DefaultStorageManager{
		config,
		specification,
		logger,
		conversion_manager,
	}
}

func (sm DefaultStorageManager) GetRootFolder(remote_address string) string {
	remote_address = sanitize_remote_address(remote_address)
	dir := path.Join("./static", sm.config.StoragePath, remote_address)

	sm.CreateDirectory(dir)

	return dir
}

func (sm DefaultStorageManager) GenerateFilePath(file_name string, remote_address string) string {
	remote_address = sanitize_remote_address(remote_address)
	dir := sm.GetRootFolder(remote_address)
	return path.Join(dir, file_name)
}

func (sm DefaultStorageManager) GetStoredFiles(remote_address string) []models.Document {
	remote_address = sanitize_remote_address(remote_address)
	dir := sm.GetRootFolder(remote_address)

	files, err := ioutil.ReadDir(dir)

	if err != nil {
		sm.logger.Error(err.Error())
		return nil
	}

	sort.Slice(files, func(i, j int) bool {
		return files[i].ModTime().Before(files[j].ModTime())
	})

	documents := []models.Document{}

	for _, v := range files {
		if v.IsDir() {
			continue
		}
		file_name := v.Name()
		documents = append(documents, models.Document{
			FileType: sm.ConversionManager.GetFileType(file_name),
			Title:    file_name,
			Url:      sm.GenerateFileUri(file_name, remote_address, managers.FileMeta{}),
			CanEdit:  !sm.ConversionManager.IsCanConvert(utils.GetFileExt(file_name)),
		})
	}

	sm.logger.Debug("Fetched ", len(documents), " document(s)")

	return documents
}

func (sm DefaultStorageManager) GenerateFileHash(file_name string, remote_address string) string {
	remote_address = sanitize_remote_address(remote_address)
	stat, err := os.Stat(sm.GenerateFilePath(file_name, remote_address))

	if err != nil {
		sm.logger.Errorf("File key generation error: %s", err.Error())
		return ""
	}

	return fmt.Sprintf("%x", md5.Sum([]byte(file_name+stat.ModTime().Format(time.RFC3339))))
}

func (sm DefaultStorageManager) GenerateFileUri(original_filename string, remote_address string, meta managers.FileMeta) string {
	remote_address = sanitize_remote_address(remote_address)
	if (managers.FileMeta{}) == meta {
		sm.logger.Debugf("Generating file %s uri", original_filename)
		return fmt.Sprintf(
			"%s://%s:%s/static/%s/%s/%s",
			sm.config.ServerProtocol,
			sm.config.ServerHost,
			sm.config.ServerPort,
			sm.config.StoragePath,
			remote_address,
			original_filename,
		)
	}
	sm.logger.Debugf("Generating file %s uri", meta.DestinationPath)
	return fmt.Sprintf(
		"%s://%s:%s/static/%s/%s/%s/%s/%s",
		sm.config.ServerProtocol,
		sm.config.ServerHost,
		sm.config.ServerPort,
		sm.config.StoragePath,
		remote_address,
		original_filename+ONLYOFFICE_HISTORY_POSTFIX,
		fmt.Sprint(meta.Version),
		meta.DestinationPath,
	)
}

func (sm DefaultStorageManager) GenerateVersionedFilename(filename string, remote_address string) string {
	basename := utils.GetFileNameWithoutExt(filename)
	ext := utils.GetFileExt(filename)
	name := fmt.Sprintf("%s%s", basename, ext)

	i := 1

	for {
		if _, err := os.Stat(sm.GenerateFilePath(name, remote_address)); os.IsNotExist(err) {
			break
		}

		name = fmt.Sprintf("%s(%d)%s", basename, i, ext)
		i++
	}

	return name
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
		os.MkdirAll(path, 0777)
		return nil
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

func (sm DefaultStorageManager) RemoveFile(filename string, remote_address string) error {
	file_path := sm.GenerateFilePath(filename, remote_address)

	if _, err := os.Stat(file_path); os.IsNotExist(err) {
		sm.logger.Errorf(err.Error())
		return err
	}

	hist_path := path.Join(sm.GetRootFolder(remote_address), filename+ONLYOFFICE_HISTORY_POSTFIX)

	os.Remove(file_path)
	os.RemoveAll(hist_path)

	return nil
}

func (sm DefaultStorageManager) ReadFile(file_path string) ([]byte, error) {
	file, err := ioutil.ReadFile(file_path)

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

	return sm.CreateFile(resp.Body, sm.GenerateFilePath(body.Filename, body.UserAddress))
}

func sanitize_remote_address(remote_address string) string {
	ind := strings.LastIndex(remote_address, ":")
	if ind != -1 {
		return remote_address[:ind]
	}
	return remote_address
}
