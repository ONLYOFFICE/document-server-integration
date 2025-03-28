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

type DefaultSavingErrorHandler struct {
	logger *zap.SugaredLogger
	managers.StorageManager
	reg *handlers.CallbackRegistry
}

func NewDefaultSavingErrorHandler(
	logger *zap.SugaredLogger,
	smanager managers.StorageManager,
	reg *handlers.CallbackRegistry,
) *DefaultSavingErrorHandler {
	shandler := DefaultSavingErrorHandler{
		logger,
		smanager,
		reg,
	}
	shandler.reg.RegisterCallbackHandler(shandler) // nolint: errcheck
	return &shandler
}

func (sh DefaultSavingErrorHandler) GetCode() int {
	return 3
}

func (sh DefaultSavingErrorHandler) Handle(cbody *models.Callback) error {
	sh.logger.Debugf("Trying to save %s with callback status 3", cbody.Filename)
	if err := sh.StorageManager.SaveFileFromUri(*cbody); err != nil {
		return err
	}

	sh.logger.Debugf("Saved %s successfully", cbody.Filename)
	return nil
}
