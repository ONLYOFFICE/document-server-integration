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
	"net/http"
	"path"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/utils"
	"go.uber.org/zap"
)

const (
	ONLYOFFICE_HISTORY_POSTFIX = "-hist"
)

type DefaultHistoryManager struct {
	logger *zap.SugaredLogger
	managers.StorageManager
	managers.JwtManager
	config config.ApplicationConfig
}

func NewDefaultHistoryManager(logger *zap.SugaredLogger, sm managers.StorageManager,
	jwt managers.JwtManager, config config.ApplicationConfig) managers.HistoryManager {
	return &DefaultHistoryManager{
		logger,
		sm,
		jwt,
		config,
	}
}

func (hm DefaultHistoryManager) read_history_changes(changes_path string) []models.Changes {
	changes_bytes, _ := hm.StorageManager.ReadFile(changes_path)
	var changes_model []models.Changes = []models.Changes{}

	json.Unmarshal(changes_bytes, &changes_model)

	return changes_model
}

func (hm DefaultHistoryManager) read_history_file_key(key_path string) string {
	key_bytes, _ := hm.StorageManager.ReadFile(key_path)
	return string(key_bytes[:])
}

func (hm DefaultHistoryManager) build_next_history(changes_model []models.Changes, key string, version int) models.History {
	return models.History{
		Changes: changes_model,
		Key:     key,
		Created: changes_model[len(changes_model)-1].Created,
		User:    changes_model[len(changes_model)-1].User,
		Version: version,
	}
}

func (hm DefaultHistoryManager) sign_history_set(set *managers.HistorySet) {
	if hm.config.JwtSecret != "" && hm.config.JwtEnabled {
		set.Token, _ = hm.JwtManager.JwtSign(set, []byte(hm.config.JwtSecret))
	}
}

//TODO: Refactoring
func (hm DefaultHistoryManager) fetch_next_history_entry(remote_address string, filename string, version int) (models.History, managers.HistorySet) {
	storage_path := hm.StorageManager.GetRootFolder(remote_address)
	hist_path := path.Join(storage_path, filename+ONLYOFFICE_HISTORY_POSTFIX, fmt.Sprint(version))

	changes_model := hm.read_history_changes(path.Join(hist_path, "changes.json"))
	key := hm.read_history_file_key(path.Join(hist_path, "key.txt"))

	var history_set managers.HistorySet

	url := hm.StorageManager.GenerateFileUri(filename, remote_address, managers.FileMeta{
		Version:         version,
		DestinationPath: "prev" + utils.GetFileExt(filename),
	})

	changes_url := hm.StorageManager.GenerateFileUri(filename, remote_address, managers.FileMeta{
		Version:         version,
		DestinationPath: "diff.zip",
	})

	if version > 1 {
		prev_hist_path := path.Join(storage_path, filename+ONLYOFFICE_HISTORY_POSTFIX, fmt.Sprint(version-1))
		prev_key := hm.read_history_file_key(prev_hist_path)
		prev_url := hm.StorageManager.GenerateFileUri(filename, remote_address, managers.FileMeta{
			Version:         version - 1,
			DestinationPath: "prev" + utils.GetFileExt(filename),
		})

		history_set = managers.HistorySet{
			ChangesUrl: changes_url,
			Key:        key,
			Url:        url,
			Version:    version,
			Previous: managers.HistoryPrevious{
				Key: prev_key,
				Url: prev_url,
			},
		}
	} else {
		history_set = managers.HistorySet{
			ChangesUrl: changes_url,
			Key:        key,
			Url:        url,
			Version:    version,
		}
	}

	hm.sign_history_set(&history_set)

	return hm.build_next_history(changes_model, key, version), history_set
}

//TODO: Refactoring
func (hm DefaultHistoryManager) GetHistory(filename string, remote_address string) (managers.HistoryRefresh, []managers.HistorySet) {
	version := 1
	refresh_history := managers.HistoryRefresh{}
	set_histories := []managers.HistorySet{}

	for {
		hist_path := path.Join(hm.StorageManager.GetRootFolder(remote_address), filename+ONLYOFFICE_HISTORY_POSTFIX, fmt.Sprint(version))
		if hm.StorageManager.PathExists(hist_path) {

			hist, set_hist := hm.fetch_next_history_entry(remote_address, filename, version)

			refresh_history.History = append(refresh_history.History, hist)
			set_histories = append(set_histories, set_hist)

			version += 1
		} else {
			break
		}
	}

	refresh_history.CurrentVersion = fmt.Sprint(version)
	curr_meta := hm.read_history_changes(path.Join(hm.StorageManager.GetRootFolder(remote_address), filename+ONLYOFFICE_HISTORY_POSTFIX, filename+".json"))

	refresh_history.History = append(refresh_history.History, models.History{
		Changes: curr_meta,
		User:    curr_meta[len(curr_meta)-1].User,
		Created: curr_meta[len(curr_meta)-1].Created,
		Key:     hm.StorageManager.GenerateFileHash(filename, remote_address),
		Version: version,
	})

	curr_set := managers.HistorySet{
		Key:     hm.StorageManager.GenerateFileHash(filename, remote_address),
		Url:     hm.StorageManager.GenerateFileUri(filename, remote_address, managers.FileMeta{}),
		Version: version,
	}

	hm.sign_history_set(&curr_set)

	set_histories = append(set_histories, curr_set)

	return refresh_history, set_histories
}

func (hm DefaultHistoryManager) CreateMeta(filename string, remote_address string, changes []models.Changes) {
	history_path := path.Join(hm.StorageManager.GetRootFolder(remote_address), filename+ONLYOFFICE_HISTORY_POSTFIX)
	byte_data, _ := json.MarshalIndent(changes, " ", "")

	hm.StorageManager.CreateDirectory(history_path)
	hm.StorageManager.CreateFile(bytes.NewReader(byte_data), path.Join(history_path, filename+".json"))
}

func (hm DefaultHistoryManager) is_meta(filename string, remote_address string) bool {
	history_path := path.Join(hm.StorageManager.GetRootFolder(remote_address), filename+ONLYOFFICE_HISTORY_POSTFIX)
	return hm.StorageManager.PathExists(path.Join(history_path, filename+".json"))
}

func (hm DefaultHistoryManager) CreateHistory(callback_body models.Callback, remote_address string) error {
	var version int = 1
	storage_path := hm.StorageManager.GetRootFolder(remote_address)
	prev_file_path := hm.StorageManager.GenerateFilePath(callback_body.Filename, remote_address)
	hist_dir := path.Join(storage_path, callback_body.Filename+ONLYOFFICE_HISTORY_POSTFIX)

	if !hm.is_meta(callback_body.Filename, remote_address) {
		return fmt.Errorf("file %s no longer exists", callback_body.Filename)
	}

	for {
		hist_dir_version := path.Join(hist_dir, fmt.Sprint(version))
		if !hm.StorageManager.PathExists(hist_dir_version) {
			hm.StorageManager.CreateDirectory(hist_dir_version)

			hm.StorageManager.MoveFile(path.Join(hist_dir, callback_body.Filename+".json"), path.Join(hist_dir_version, "changes.json"))

			changes_bytes, _ := json.Marshal(callback_body.History.Changes)
			hm.StorageManager.CreateFile(bytes.NewReader(changes_bytes), path.Join(hist_dir, callback_body.Filename+".json"))

			hm.StorageManager.CreateFile(bytes.NewReader([]byte(callback_body.Key)), path.Join(hist_dir_version, "key.txt"))

			hm.StorageManager.MoveFile(prev_file_path, path.Join(hist_dir_version, "prev"+utils.GetFileExt(callback_body.Filename)))

			resp, err := http.Get(callback_body.ChangesUrl)
			if err != nil {
				return err
			}
			defer resp.Body.Close()

			hm.StorageManager.CreateFile(resp.Body, path.Join(hist_dir_version, "diff.zip"))

			break
		}
		version += 1
	}

	return nil
}
