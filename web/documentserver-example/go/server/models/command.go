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
package models

import "github.com/golang-jwt/jwt"

type Command int

const (
	ONLYOFFICE_COMMAND_DROP Command = iota
	ONLYOFFICE_COMMAND_FORCESAVE
	ONLYOFFICE_COMMAND_INFO
	ONLYOFFICE_COMMAND_META
	ONLYOFFICE_COMMAND_VERSION
)

func (c Command) String() string {
	return [...]string{"drop", "forcesave", "info", "meta", "version"}[c]
}

func (c Command) Ordinal() int {
	return int(c)
}

type CommandBody struct {
	Command            string `json:"c"`
	Token              string `json:"token,omitempty"`
	jwt.StandardClaims `json:"-"`
}

type CommandResponse struct {
	Error   int    `json:"error"`
	Version string `json:"version,omitempty"`
}

type ForgottenList struct {
	Error int      `json:"error"`
	Keys  []string `json:"keys"`
}

type ForgottenFile struct {
	Error int    `json:"error"`
	Key   string `json:"key"`
	Url   string `json:"url"`
	Type  string
}
