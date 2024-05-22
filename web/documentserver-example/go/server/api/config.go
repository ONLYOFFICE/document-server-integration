/**
 *
 * (c) Copyright Ascensio System SIA 2023
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
package api

import (
	"net/http"
)

type ServerEndpointsHandler interface {
	Index(w http.ResponseWriter, r *http.Request)
	Editor(w http.ResponseWriter, r *http.Request)
	Remove(w http.ResponseWriter, r *http.Request)
	Upload(w http.ResponseWriter, r *http.Request)
	Download(w http.ResponseWriter, r *http.Request)
	History(w http.ResponseWriter, r *http.Request)
	Convert(w http.ResponseWriter, r *http.Request)
	Callback(w http.ResponseWriter, r *http.Request)
	Create(w http.ResponseWriter, r *http.Request)
	Reference(w http.ResponseWriter, r *http.Request)
}

type ServerAPI struct {
	ServerEndpointsHandler
}

func New(endpointsHandler ServerEndpointsHandler) *ServerAPI {
	return &ServerAPI{
		endpointsHandler,
	}
}
