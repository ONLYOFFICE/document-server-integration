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
	"fmt"
	"html/template"
	"net/http"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/api"
	"github.com/ONLYOFFICE/document-server-integration/server/handlers"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/gorilla/schema"
	"go.uber.org/zap"
)

type DefaultServerEndpointsHandler struct {
	logger        *zap.SugaredLogger
	config        config.ApplicationConfig
	specification config.SpecificationConfig
	*handlers.CallbackRegistry
	*managers.Managers
}

func NewDefaultServerEndpointsHandler(logger *zap.SugaredLogger, config config.ApplicationConfig,
	spec config.SpecificationConfig, reg *handlers.CallbackRegistry,
	managers *managers.Managers) api.ServerEndpointsHandler {
	return &DefaultServerEndpointsHandler{
		logger,
		config,
		spec,
		reg,
		managers,
	}
}

func generateUrl(r *http.Request) string {
	scheme := "http"
	if r.Header.Get("X-Forwarded-Proto") != "" {
		scheme = r.Header.Get("X-Forwarded-Proto")
	}

	if r.TLS != nil {
		scheme = "https"
	}

	return fmt.Sprintf("%s://%s", scheme, r.Host)
}

var decoder = schema.NewDecoder()
var indexTemplate = template.Must(template.ParseFiles("templates/index.html"))
var forgottenTemplate = template.Must(template.ParseFiles("templates/forgotten.html"))
var editorTemplate = template.Must(template.ParseFiles("templates/editor.html"))
