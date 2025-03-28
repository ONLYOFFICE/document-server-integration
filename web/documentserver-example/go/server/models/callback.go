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

type Callback struct {
	Actions []struct {
		Type   int    `json:"type"`
		UserID string `json:"userid"`
	} `json:"actions"`
	ChangesUrl  string   `json:"changesurl"`
	History     History  `json:"history"`
	Key         string   `json:"key"`
	Status      int      `json:"status"`
	Users       []string `json:"users"`
	Url         string   `json:"url"`
	FileId      string   `json:"-"`
	Token       string   `json:"token"`
	Filename    string   `json:"filename,omitempty"`
	UserAddress string   `json:"userAddress,omitempty"`
}

type History struct {
	Changes       []Changes `json:"changes,omitempty"`
	ServerVersion string    `json:"serverVersion,omitempty"`
	Created       string    `json:"created,omitempty"`
	Key           string    `json:"key,omitempty"`
	User          *User     `json:"user,omitempty"`
	Version       int       `json:"version,omitempty"`
}

type Changes struct {
	Created string `json:"created"`
	User    User   `json:"user"`
}
