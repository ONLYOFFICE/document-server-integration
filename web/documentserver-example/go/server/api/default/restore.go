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
package dapi

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
	"github.com/ONLYOFFICE/document-server-integration/utils"
)

func (srv *DefaultServerEndpointsHandler) Restore(w http.ResponseWriter, r *http.Request) {
	result := map[string]interface{}{
		"success": false,
	}
	var body map[string]interface{}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		srv.logger.Error("Restore body decoding error")
		result["error"] = err.Error()
		shared.SendResponse(w, result)
		return
	}

	fileName := fmt.Sprintf("%v", body["fileName"])
	if fileName == "" {
		srv.logger.Error("Restore filename is empty")
		result["error"] = "Empty filename"
		shared.SendResponse(w, result)
		return
	}

	version := fmt.Sprintf("%v", body["version"])
	url := ""
	v, e := body["url"]
	if s, o := v.(string); o && e {
		url = s
	}

	key, err := srv.GenerateFileHash(fileName)
	if err != nil {
		result["error"] = err.Error()
		shared.SendResponse(w, result)
		return
	}
	filePath, err := srv.GenerateFilePath(fileName)
	if err != nil {
		result["error"] = err.Error()
		shared.SendResponse(w, result)
		return
	}
	rootPath, _ := srv.GetRootFolder()
	historyPath := path.Join(rootPath, fileName+shared.ONLYOFFICE_HISTORY_POSTFIX)
	newVersion := srv.HistoryManager.CountVersion(historyPath)
	versionPath := path.Join(historyPath, version, "prev"+utils.GetFileExt(fileName, false))
	newVersionPath := path.Join(historyPath, fmt.Sprint(newVersion))

	if !srv.Managers.StorageManager.PathExists(versionPath) {
		result["error"] = "Version path does not exist"
		shared.SendResponse(w, result)
		return
	}

	err = srv.Managers.StorageManager.CreateDirectory(newVersionPath)
	if err != nil {
		result["error"] = err.Error()
		shared.SendResponse(w, result)
		return
	}

	currFile, _ := srv.Managers.StorageManager.ReadFile(filePath)
	err = srv.Managers.StorageManager.CreateFile(
		bytes.NewBuffer(currFile),
		path.Join(newVersionPath, "prev"+utils.GetFileExt(fileName, false)))
	if err != nil {
		result["error"] = err.Error()
		shared.SendResponse(w, result)
		return
	}

	err = srv.Managers.StorageManager.CreateFile(bytes.NewBuffer([]byte(key)), path.Join(newVersionPath, "key.txt"))
	if err != nil {
		result["error"] = err.Error()
		shared.SendResponse(w, result)
		return
	}

	var verFile []byte
	if url != "" {
		res, err := http.Get(url)
		if err != nil {
			result["error"] = err.Error()
			shared.SendResponse(w, result)
			return
		}
		defer res.Body.Close()
		verFile, _ = io.ReadAll(res.Body)
	} else {
		verFile, _ = srv.Managers.StorageManager.ReadFile(versionPath)
	}

	err = srv.Managers.StorageManager.CreateFile(bytes.NewBuffer(verFile), filePath)
	if err != nil {
		result["error"] = err.Error()
		shared.SendResponse(w, result)
		return
	}

	fileContent, err := os.Open(path.Join(historyPath, fileName+".json"))
	if err != nil {
		result["error"] = err.Error()
		shared.SendResponse(w, result)
		return
	}

	byteResult, _ := io.ReadAll(fileContent)
	var history models.History
	err = json.Unmarshal(byteResult, &history)
	if err != nil {
		result["error"] = err.Error()
		shared.SendResponse(w, result)
		return
	}
	fileContent.Close()

	changes := history.Changes[len(history.Changes)-1]
	hist := models.History{
		ServerVersion: srv.config.Version,
		Changes: []models.Changes{
			{
				Created: time.Now().UTC().Format("2006-02-1 15:04:05"),
				User: models.User{
					Id:       changes.User.Id,
					Username: changes.User.Username,
				},
			},
		},
	}

	meta, _ := json.MarshalIndent(hist, " ", "")

	err = srv.StorageManager.MoveFile(path.Join(historyPath, fileName+".json"), path.Join(newVersionPath, "changes.json"))
	if err != nil {
		result["error"] = err.Error()
		shared.SendResponse(w, result)
		return
	}

	err = srv.StorageManager.CreateFile(bytes.NewReader(meta), path.Join(historyPath, fileName+".json"))
	if err != nil {
		srv.logger.Errorf("meta creation error: %s", err.Error())
	}

	result["success"] = true
	shared.SendResponse(w, result)
}
