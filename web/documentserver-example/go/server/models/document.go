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

package models

type MetaInfo struct {
	Author  string `json:"author"`
	Created string `json:"created"`
}

type Permissions struct {
	Comment                 bool `json:"comment,omitempty"`
	Copy                    bool `json:"copy,omitempty"`
	DeleteCommentAuthorOnly bool `json:"deleteCommentAuthorOnly,omitempty"`
	Download                bool `json:"download,omitempty"`
	Edit                    bool `json:"edit"`
	EditCommentAuthorOnly   bool `json:"editCommentAuthorOnly,omitempty"`
	FillForms               bool `json:"fillForms,omitempty"`
	ModifyContentControl    bool `json:"modifyContentControl,omitempty"`
	ModifyFilter            bool `json:"modifyFilter,omitempty"`
	Print                   bool `json:"print,omitempty"`
	Review                  bool `json:"review,omitempty"`
}

type Document struct {
	FileType    string      `json:"fileType"`
	Key         string      `json:"key,omitempty"`
	Title       string      `json:"title"`
	Url         string      `json:"url"`
	Info        MetaInfo    `json:"info"`
	Permissions Permissions `json:"permissions,omitempty"`
	CanEdit     bool
}
