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
package dhandlers

import (
	"github.com/ONLYOFFICE/document-server-integration/server/handlers"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"go.uber.org/zap"
)

type DefaultSaveHandler struct {
	logger *zap.SugaredLogger
	managers.StorageManager
	managers.HistoryManager
	reg *handlers.CallbackRegistry
}

func NewDefaultSaveHandler(logger *zap.SugaredLogger, smanager managers.StorageManager,
	hmanager managers.HistoryManager, reg *handlers.CallbackRegistry) *DefaultSaveHandler {
	shandler := DefaultSaveHandler{
		logger,
		smanager,
		hmanager,
		reg,
	}
	shandler.reg.RegisterCallbackHandler(shandler) // nolint: errcheck
	return &shandler
}

func (sh DefaultSaveHandler) GetCode() int {
	return 2
}

func (sh DefaultSaveHandler) Handle(cbody *models.Callback) error {
	sh.logger.Debugf("Trying to save %s", cbody.Filename)
	if err := sh.HistoryManager.CreateHistory(*cbody); err != nil {
		return err
	}

	if err := sh.StorageManager.SaveFileFromUri(*cbody); err != nil {
		return err
	}

	sh.logger.Debugf("Saved %s successfully", cbody.Filename)
	return nil
}
