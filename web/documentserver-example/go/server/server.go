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
package server

import (
	"net/http"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/api"
	"github.com/ONLYOFFICE/document-server-integration/server/handlers"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/gorilla/mux"
	"go.uber.org/zap"
)

type Server struct {
	Http                *http.Server
	Config              config.ApplicationConfig
	ServerSpecification config.SpecificationConfig
	*api.ServerAPI
	Logger *zap.SugaredLogger
	*managers.Managers
	*handlers.CallbackRegistry
	isRunning bool
}

func New(config config.ApplicationConfig, specification config.SpecificationConfig,
	logger *zap.SugaredLogger, managers *managers.Managers, api *api.ServerAPI, reg *handlers.CallbackRegistry) *Server {
	srv := Server{
		Config:              config,
		ServerSpecification: specification,
		ServerAPI:           api,
		Logger:              logger,
		isRunning:           false,
		Managers:            managers,
		CallbackRegistry:    reg,
	}

	r := srv.configureRouter()

	server := &http.Server{
		Handler:      r,
		Addr:         ":" + config.ServerPort,
		WriteTimeout: 15 * time.Second,
		ReadTimeout:  15 * time.Second,
	}

	srv.Http = server

	return &srv
}

func (server *Server) Run() {
	err := server.Http.ListenAndServe()
	if err == nil {
		server.isRunning = true
	}
}

func initFileserver(allow_origins ...string) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		for _, origin := range allow_origins {
			w.Header().Set("Access-Control-Allow-Origin", origin)
		}
		http.FileServer(http.Dir("./static/")).ServeHTTP(w, r)
	})
}

func (srv *Server) configureRouter() *mux.Router {
	r := mux.NewRouter()

	r.PathPrefix("/static/").Handler(http.StripPrefix("/static/", initFileserver(srv.Config.DocumentServerHost)))

	r.HandleFunc("/", srv.ServerAPI.Index).Methods(http.MethodGet)
	r.HandleFunc("/editor", srv.ServerAPI.Editor).Methods(http.MethodGet)
	r.HandleFunc("/callback", srv.ServerAPI.Callback).Methods(http.MethodPost)
	r.HandleFunc("/remove", srv.ServerAPI.Remove).Methods(http.MethodGet)
	r.HandleFunc("/remove", srv.ServerAPI.Remove).Methods(http.MethodDelete)
	r.HandleFunc("/upload", srv.ServerAPI.Upload).Methods(http.MethodPost)
	r.HandleFunc("/convert", srv.ServerAPI.Convert).Methods(http.MethodPost)
	r.HandleFunc("/download", srv.ServerAPI.Download).Methods(http.MethodGet)
	r.HandleFunc("/history", srv.ServerAPI.History).Methods(http.MethodGet)
	r.HandleFunc("/create", srv.ServerAPI.Create).Methods(http.MethodGet, http.MethodPost)
	r.HandleFunc("/reference", srv.ServerAPI.Reference).Methods(http.MethodPost)
	r.HandleFunc("/files", srv.ServerAPI.Files).Methods(http.MethodGet)
	r.HandleFunc("/rename", srv.ServerAPI.Rename).Methods(http.MethodPost)
	r.HandleFunc("/historyObj", srv.ServerAPI.HistoryObj).Methods(http.MethodPost)
	r.HandleFunc("/restore", srv.ServerAPI.Restore).Methods(http.MethodPut)
	r.HandleFunc("/formats", srv.ServerAPI.Formats).Methods(http.MethodGet)
	r.HandleFunc("/config", srv.ServerAPI.Config).Methods(http.MethodGet)
	r.HandleFunc("/forgotten", srv.ServerAPI.Forgotten).Methods(http.MethodGet, http.MethodDelete)

	return r
}
