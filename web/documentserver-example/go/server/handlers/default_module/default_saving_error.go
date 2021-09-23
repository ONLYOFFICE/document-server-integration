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

type DefaultSavingErrorHandler struct {
	logger *zap.SugaredLogger
	managers.StorageManager
	reg *handlers.CallbackRegistry
}

func NewDefaultSavingErrorHandler(logger *zap.SugaredLogger, storage_manager managers.StorageManager, reg *handlers.CallbackRegistry) *DefaultSavingErrorHandler {
	save_handler := DefaultSavingErrorHandler{
		logger,
		storage_manager,
		reg,
	}
	save_handler.reg.RegisterCallbackHandler(save_handler)
	return &save_handler
}

func (sh DefaultSavingErrorHandler) GetCode() int {
	return 3
}

func (sh DefaultSavingErrorHandler) Handle(callback_body *models.Callback) {
	sh.logger.Debugf("Trying to save %s with callback status 3", callback_body.Filename)
	err := sh.StorageManager.SaveFileFromUri(*callback_body)

	if err != nil {
		sh.logger.Errorf("An error occured while trying to save: %s", err.Error())
		return
	}

	sh.logger.Debugf("Saved %s successfully", callback_body.Filename)
}
