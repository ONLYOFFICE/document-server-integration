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
package handlers

import (
	"sync"

	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"go.uber.org/zap"
)

type CallbackHandler interface {
	GetCode() int
	Handle(body *models.Callback) error
}

type CallbackRegistry struct {
	Locker           *sync.Mutex
	logger           *zap.SugaredLogger
	CallbackHandlers map[int]CallbackHandler
}

func New(logger *zap.SugaredLogger) *CallbackRegistry {
	reg := &CallbackRegistry{
		Locker:           &sync.Mutex{},
		logger:           logger,
		CallbackHandlers: make(map[int]CallbackHandler),
	}
	return reg
}

func (cr *CallbackRegistry) RegisterCallbackHandler(chandler CallbackHandler) error {
	if _, exists := cr.CallbackHandlers[chandler.GetCode()]; exists {
		return &HandlerExistsError{
			Code: chandler.GetCode(),
		}
	}
	cr.CallbackHandlers[chandler.GetCode()] = chandler
	return nil
}

func (cr *CallbackRegistry) HandleIncomingCode(cbody *models.Callback) error {
	for _, handler := range cr.CallbackHandlers {
		if handler.GetCode() == cbody.Status {
			cr.logger.Debugf("Processing an incoming callback request with code %d", cbody.Status)
			if err := handler.Handle(cbody); err != nil {
				return err
			}
		}
	}

	return nil
}
