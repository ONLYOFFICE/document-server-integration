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

package handlers

import (
	"errors"
	"fmt"
	"sync"

	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"go.uber.org/zap"
)

type CallbackHandler interface {
	GetCode() int
	Handle(body *models.Callback)
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

func (cr *CallbackRegistry) RegisterCallbackHandler(callback_handler CallbackHandler) error {
	if _, exists := cr.CallbackHandlers[callback_handler.GetCode()]; exists {
		return errors.New("A Handler with code " + fmt.Sprint(callback_handler.GetCode()) + " exists")
	}
	cr.CallbackHandlers[callback_handler.GetCode()] = callback_handler
	return nil
}

func (cr *CallbackRegistry) HandleIncomingCode(callback_body *models.Callback) {
	for _, handler := range cr.CallbackHandlers {
		if handler.GetCode() == callback_body.Status {
			cr.logger.Debugf("Processing an incoming callback request with code %d", callback_body.Status)
			go handler.Handle(callback_body)
		}
	}
}
