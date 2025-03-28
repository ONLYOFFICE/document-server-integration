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
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"go.uber.org/zap"
)

type DefaultNoChangesHandler struct {
	logger *zap.SugaredLogger
	reg    *handlers.CallbackRegistry
}

func NewDefaultNoChangesHandler(logger *zap.SugaredLogger, reg *handlers.CallbackRegistry) *DefaultNoChangesHandler {
	handler := DefaultNoChangesHandler{
		logger,
		reg,
	}
	handler.reg.RegisterCallbackHandler(handler) // nolint: errcheck
	return &handler
}

func (nh DefaultNoChangesHandler) GetCode() int {
	return 4
}

func (nh DefaultNoChangesHandler) Handle(cbody *models.Callback) error {
	nh.logger.Debugf("No %s changes", cbody.Filename)
	return nil
}
