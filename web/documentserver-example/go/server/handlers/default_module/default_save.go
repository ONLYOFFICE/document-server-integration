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

package default_handlers

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

func NewDefaultSaveHandler(logger *zap.SugaredLogger, storage_manager managers.StorageManager,
	history_manager managers.HistoryManager, reg *handlers.CallbackRegistry) *DefaultSaveHandler {
	save_handler := DefaultSaveHandler{
		logger,
		storage_manager,
		history_manager,
		reg,
	}
	save_handler.reg.RegisterCallbackHandler(save_handler)
	return &save_handler
}

func (sh DefaultSaveHandler) GetCode() int {
	return 2
}

func (sh DefaultSaveHandler) Handle(callback_body *models.Callback) {
	sh.logger.Debugf("Trying to save %s", callback_body.Filename)
	hist_err := sh.HistoryManager.CreateHistory(*callback_body, callback_body.UserAddress)

	if hist_err != nil {
		sh.logger.Errorf("An error occured while trying to save: %s", hist_err.Error())
		return
	}

	file_err := sh.StorageManager.SaveFileFromUri(*callback_body)

	if file_err != nil {
		sh.logger.Errorf("An error occured while trying to save: %s", file_err.Error())
		return
	}

	sh.logger.Debugf("Saved %s successfully", callback_body.Filename)
}
