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
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"path"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/server/shared"
	"github.com/ONLYOFFICE/document-server-integration/utils"
	"github.com/golang-jwt/jwt"
	"go.uber.org/zap"
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

func (hm DefaultHistoryManager) readHistory(cpath string) (models.History, error) {
	var hist models.History
	changes, err := hm.StorageManager.ReadFile(cpath)
	if err != nil {
		return hist, err
	}

	if err := json.Unmarshal(changes, &hist); err != nil {
		return hist, err
	}

	return hist, nil
}

func (hm DefaultHistoryManager) readHistoryFileKey(keyPath string) (string, error) {
	key, err := hm.StorageManager.ReadFile(keyPath)
	if err != nil {
		return "", err
	}

	return string(key), nil
}

func (hm DefaultHistoryManager) buildNextHistory(hist models.History, key string, version int) models.History {
	if len(hist.Changes) == 0 {
		return models.History{
			Changes: []models.Changes{{
				Created: hist.Created,
				User:    *hist.User,
			}},
			Key:           key,
			Created:       hist.Created,
			User:          hist.User,
			ServerVersion: hist.ServerVersion,
			Version:       version,
		}
	}
	if version == 1 {
		return models.History{
			Changes:       nil,
			Key:           key,
			Created:       hist.Changes[len(hist.Changes)-1].Created,
			User:          &hist.Changes[len(hist.Changes)-1].User,
			ServerVersion: hist.ServerVersion,
			Version:       version,
		}
	}
	return models.History{
		Changes:       hist.Changes,
		Key:           key,
		Created:       hist.Changes[len(hist.Changes)-1].Created,
		User:          &hist.Changes[len(hist.Changes)-1].User,
		ServerVersion: hist.ServerVersion,
		Version:       version,
	}
}

func (hm DefaultHistoryManager) signHistorySet(set *managers.HistorySet) error {
	var err error
	if hm.config.JwtSecret != "" && hm.config.JwtEnabled {
		set.Token, err = hm.JwtManager.JwtSign(set, []byte(hm.config.JwtSecret))
		if err != nil {
			return err
		}
	}

	return nil
}

func (hm DefaultHistoryManager) fetchNextHistoryEntry(
	remoteAddress,
	filename string,
	version int,
) (models.History, managers.HistorySet, error) {
	var (
		hresp  models.History
		hsresp managers.HistorySet
	)

	storagePath, err := hm.StorageManager.GetRootFolder()
	if err != nil {
		return hresp, hsresp, err
	}

	histPath := path.Join(storagePath, filename+shared.ONLYOFFICE_HISTORY_POSTFIX, fmt.Sprint(version))
	mchanges, err := hm.readHistory(path.Join(histPath, "changes.json"))
	if err != nil {
		meta := hm.GetFileData(filename)
		mchanges = models.History{
			Created: meta["created"],
			User: &models.User{
				Username: meta["name"],
				Id:       meta["id"],
			},
			ServerVersion: meta["serverVersion"],
		}
		// return hresp, hsresp, err
	}

	key, err := hm.readHistoryFileKey(path.Join(histPath, "key.txt"))
	if err != nil {
		return hresp, hsresp, err
	}

	var hset managers.HistorySet
	url := hm.StorageManager.GeneratePublicFileUri(filename, remoteAddress, managers.FileMeta{
		Version:         version,
		DestinationPath: "prev" + utils.GetFileExt(filename, false),
	})

	if version > 1 {
		prevHistPath := path.Join(storagePath, filename+shared.ONLYOFFICE_HISTORY_POSTFIX, fmt.Sprint(version-1))
		prevKey, err := hm.readHistoryFileKey(path.Join(prevHistPath, "key.txt"))
		if err != nil {
			return hresp, hsresp, err
		}

		prevUrl := hm.StorageManager.GeneratePublicFileUri(filename, remoteAddress, managers.FileMeta{
			Version:         version - 1,
			DestinationPath: "prev" + utils.GetFileExt(filename, false),
		})

		var changesUrl string
		if hm.StorageManager.PathExists(
			path.Join(storagePath, filename+shared.ONLYOFFICE_HISTORY_POSTFIX, fmt.Sprint(version-1), "diff.zip"),
		) {
			changesUrl = hm.StorageManager.GeneratePublicFileUri(filename, remoteAddress, managers.FileMeta{
				Version:         version - 1,
				DestinationPath: "diff.zip",
			})
		}
		hset = managers.HistorySet{
			ChangesUrl: changesUrl,
			Key:        key,
			Url:        url,
			Version:    version,
			Previous: &managers.HistoryPrevious{
				Key: prevKey,
				Url: prevUrl,
			},
		}
	} else {
		hset = managers.HistorySet{
			Key:     key,
			Url:     url,
			Version: version,
			StandardClaims: jwt.StandardClaims{
				ExpiresAt: time.Now().Add(time.Minute * hm.config.JwtExpiresIn).Unix(),
				IssuedAt:  time.Now().Unix(),
			},
		}
	}

	if err := hm.signHistorySet(&hset); err != nil {
		return hresp, hset, err
	}

	return hm.buildNextHistory(mchanges, key, version), hset, nil
}

func (hm DefaultHistoryManager) GetHistory(
	filename,
	remoteAddress string,
) (managers.HistoryRefresh, []managers.HistorySet, error) {
	var (
		version = 1
		rhist   managers.HistoryRefresh
		setHist []managers.HistorySet
	)

	rootPath, err := hm.StorageManager.GetRootFolder()
	if err != nil {
		return rhist, setHist, err
	}

	for {
		hpath := path.Join(rootPath, filename+shared.ONLYOFFICE_HISTORY_POSTFIX, fmt.Sprint(version))
		if hm.StorageManager.PathExists(hpath) {
			hist, histSet, err := hm.fetchNextHistoryEntry(remoteAddress, filename, version)
			if err != nil {
				return rhist, setHist, err
			}

			rhist.History = append(rhist.History, hist)
			setHist = append(setHist, histSet)
			version += 1
		} else {
			break
		}
	}

	rhist.CurrentVersion = fmt.Sprint(version)
	currMeta, err := hm.readHistory(path.Join(rootPath, filename+shared.ONLYOFFICE_HISTORY_POSTFIX, filename+".json"))
	if err != nil {
		return rhist, setHist, err
	}

	docKey, err := hm.StorageManager.GenerateFileHash(filename)
	if err != nil {
		return rhist, setHist, err
	}

	var changesUrl string
	if hm.StorageManager.PathExists(
		path.Join(rootPath, filename+shared.ONLYOFFICE_HISTORY_POSTFIX, fmt.Sprint(version-1), "diff.zip"),
	) {
		changesUrl = hm.StorageManager.GeneratePublicFileUri(filename, remoteAddress, managers.FileMeta{
			Version:         version - 1,
			DestinationPath: "diff.zip",
		})
	}
	currSet := managers.HistorySet{
		Key:        docKey,
		Url:        hm.StorageManager.GeneratePublicFileUri(filename, remoteAddress, managers.FileMeta{}),
		Version:    version,
		ChangesUrl: changesUrl,
		StandardClaims: jwt.StandardClaims{
			ExpiresAt: time.Now().Add(time.Minute * hm.config.JwtExpiresIn).Unix(),
			IssuedAt:  time.Now().Unix(),
		},
	}

	rhist.History = append(rhist.History, models.History{
		Changes:       currMeta.Changes,
		User:          &currMeta.Changes[len(currMeta.Changes)-1].User,
		Created:       currMeta.Changes[len(currMeta.Changes)-1].Created,
		Key:           docKey,
		Version:       version,
		ServerVersion: currMeta.ServerVersion,
	})
	if version > 1 {
		currSet.Previous = &managers.HistoryPrevious{
			Key: setHist[len(setHist)-1].Key,
			Url: setHist[len(setHist)-1].Url,
		}
	}

	if err := hm.signHistorySet(&currSet); err != nil {
		return rhist, setHist, err
	}

	setHist = append(setHist, currSet)
	return rhist, setHist, nil
}

func (hm DefaultHistoryManager) CreateMeta(filename string, history models.History) error {
	rootPath, err := hm.StorageManager.GetRootFolder()
	if err != nil {
		return err
	}

	hpath := path.Join(rootPath, filename+shared.ONLYOFFICE_HISTORY_POSTFIX)
	bdata, err := json.MarshalIndent(history, " ", "")
	if err != nil {
		return err
	}

	if err := hm.StorageManager.CreateDirectory(hpath); err != nil {
		return err
	}

	return hm.StorageManager.CreateFile(bytes.NewReader(bdata), path.Join(hpath, filename+".json"))
}

func (hm DefaultHistoryManager) isMeta(filename string) bool {
	rootPath, err := hm.StorageManager.GetRootFolder()
	if err != nil {
		return false
	}

	hpath := path.Join(rootPath, filename+shared.ONLYOFFICE_HISTORY_POSTFIX)
	return hm.StorageManager.PathExists(path.Join(hpath, filename+".json"))
}

func (hm DefaultHistoryManager) GetFileData(filename string) map[string]string {
	empty := map[string]string{
		"created": "2017-01-01",
		"id":      "uid-1",
		"name":    "John Smith",
	}
	if !hm.isMeta(filename) {
		return empty
	}
	root, err := hm.StorageManager.GetRootFolder()
	if err != nil {
		return empty
	}
	file, err := hm.StorageManager.ReadFile(path.Join(root, filename+shared.ONLYOFFICE_HISTORY_POSTFIX, filename+".json"))
	if err != nil {
		return empty
	}
	var meta models.History
	err = json.Unmarshal(file, &meta)
	if err != nil {
		return empty
	}
	return map[string]string{
		"created":       meta.Changes[0].Created,
		"id":            meta.Changes[0].User.Id,
		"name":          meta.Changes[0].User.Username,
		"serverVersion": meta.ServerVersion,
	}
}

func (hm DefaultHistoryManager) CreateHistory(cbody models.Callback) error {
	var version = 1
	spath, err := hm.StorageManager.GetRootFolder()
	if err != nil {
		return err
	}

	prevFilePath, err := hm.StorageManager.GenerateFilePath(cbody.Filename)
	if err != nil {
		return err
	}

	hdir := path.Join(spath, cbody.Filename+shared.ONLYOFFICE_HISTORY_POSTFIX)
	if !hm.isMeta(cbody.Filename) {
		return fmt.Errorf("file %s no longer exists", cbody.Filename)
	}

	for {
		histDirVersion := path.Join(hdir, fmt.Sprint(version))
		if !hm.StorageManager.PathExists(histDirVersion) {
			err = hm.StorageManager.CreateDirectory(histDirVersion)
			if err != nil {
				return err
			}

			err = hm.StorageManager.MoveFile(path.Join(hdir, cbody.Filename+".json"), path.Join(histDirVersion, "changes.json"))
			if err != nil {
				return err
			}

			cbytes, err := json.Marshal(cbody.History)
			if err != nil {
				return err
			}

			err = hm.StorageManager.CreateFile(bytes.NewReader(cbytes), path.Join(hdir, cbody.Filename+".json"))
			if err != nil {
				return err
			}

			err = hm.StorageManager.CreateFile(bytes.NewReader([]byte(cbody.Key)), path.Join(histDirVersion, "key.txt"))
			if err != nil {
				return err
			}

			err = hm.StorageManager.MoveFile(
				prevFilePath,
				path.Join(histDirVersion, "prev"+utils.GetFileExt(cbody.Filename, false)),
			)
			if err != nil {
				return err
			}

			resp, err := http.Get(cbody.ChangesUrl)
			if err != nil {
				return err
			}

			defer resp.Body.Close()
			err = hm.StorageManager.CreateFile(resp.Body, path.Join(histDirVersion, "diff.zip"))
			if err != nil {
				return err
			}

			break
		}
		version += 1
	}

	return nil
}

func (hm DefaultHistoryManager) CountVersion(directory string) int {
	ver := 1
	for {
		if hm.StorageManager.DirExists(path.Join(directory, fmt.Sprint(ver))) {
			ver += 1
		} else {
			return ver
		}
	}
}
