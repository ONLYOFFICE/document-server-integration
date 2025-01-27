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
package shared

import (
	"encoding/json"
	"fmt"
	"net/http"
)

func SendDocumentServerRespose(w http.ResponseWriter, isError bool) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	if isError {
		w.Write([]byte("{\"error\": 1}"))
	} else {
		w.Write([]byte("{\"error\": 0}"))
	}
}

func SendCustomErrorResponse(w http.ResponseWriter, msg string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusBadRequest)
	fmt.Fprintf(w, "{\"error\":\"%s\"}", msg)
}

func SendResponse(w http.ResponseWriter, data interface{}) {
	body, _ := json.Marshal(data)
	fmt.Fprint(w, string(body))
}
