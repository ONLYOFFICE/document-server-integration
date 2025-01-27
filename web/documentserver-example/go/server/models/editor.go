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

type Goback struct {
	RequestClose bool `json:"requestClose"`
}

type Customization struct {
	About      bool                   `json:"about"`
	Comments   bool                   `json:"comments,omitempty"`
	Feedback   bool                   `json:"feedback"`
	Forcesave  bool                   `json:"forcesave,omitempty"`
	SubmitForm bool                   `json:"submitForm,omitempty"`
	Goback     Goback                 `json:"goback,omitempty"`
	Close      map[string]interface{} `json:"close,omitempty"`
}

type Template struct {
	Image string `json:"image,omitempty"`
	Title string `json:"title,omitempty"`
	Url   string `json:"url,omitempty"`
}

type Embedded struct {
	SaveUrl       string `json:"saveUrl"`
	EmbedUrl      string `json:"embedUrl"`
	ShareUrl      string `json:"shareUrl"`
	ToolbarDocked string `json:"toolbarDocked"`
}

type EditorConfig struct {
	User          UserInfo               `json:"user"`
	CallbackUrl   string                 `json:"callbackUrl"`
	Customization Customization          `json:"customization,omitempty"`
	Embedded      Embedded               `json:"embedded,omitempty"`
	Lang          string                 `json:"lang,omitempty"`
	Mode          string                 `json:"mode,omitempty"`
	ActionLink    string                 `json:"actionLink,omitempty"`
	CreateUrl     string                 `json:"createUrl,omitempty"`
	FileChoiceUrl string                 `json:"fileChoiceUrl,omitempty"`
	CoEditing     map[string]interface{} `json:"coEditing,omitempty"`
	Plugins       map[string]interface{} `json:"plugins,omitempty"`
	Templates     []Template             `json:"templates,omitempty"`
}
