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
package dmanager

import (
	"fmt"
	"strings"
	"time"

	"github.com/ONLYOFFICE/document-server-integration/config"
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"github.com/ONLYOFFICE/document-server-integration/utils"
	"go.uber.org/zap"
)

type DefaultDocumentManager struct {
	config        config.ApplicationConfig
	specification config.SpecificationConfig
	logger        *zap.SugaredLogger
	managers.StorageManager
	managers.UserManager
	managers.ConversionManager
	managers.JwtManager
}

const (
	onlyoffice_permission_edit         = "edit"
	onlyoffice_permission_view         = "view"
	onlyoffice_permission_fill_forms   = "fillForms"
	onlyoffice_permission_embedded     = "embedded"
	onlyoffice_permission_blockcontent = "blockcontent"
	onlyoffice_permission_filter       = "filter"
	onlyoffice_permission_review       = "review"
	onlyoffice_permission_comment      = "comment"
)

func NewDefaultDocumentManager(config config.ApplicationConfig, specification config.SpecificationConfig,
	logger *zap.SugaredLogger, smanager managers.StorageManager, umanager managers.UserManager,
	cmanager managers.ConversionManager, jmanager managers.JwtManager) managers.DocumentManager {
	return &DefaultDocumentManager{
		config,
		specification,
		logger,
		smanager,
		umanager,
		cmanager,
		jmanager,
	}
}

// TODO: Use 'enums' instead of strings
func (dm DefaultDocumentManager) sanitizeEditorParameters(parameters *managers.Editor) {
	parameters.PermissionsMode = parameters.Mode
	parameters.Mode = "view"

	if parameters.PermissionsMode == "" {
		parameters.PermissionsMode = "edit"
	}

	parameters.CanEdit = utils.IsInList(utils.GetFileExt(parameters.Filename, true), dm.specification.Extensions.Edited)

	if parameters.CanEdit && parameters.PermissionsMode != "view" {
		parameters.Mode = "edit"
	}

	if parameters.Type == "" {
		parameters.Type = "desktop"
	}
}

func (dm DefaultDocumentManager) BuildDocumentConfig(parameters managers.Editor, remoteAddress string) (*models.Config, error) {
	user, err := dm.GetUserById(parameters.UserId)
	if err != nil {
		return nil, err
	}

	dm.logger.Debugf("Generating file %s config", parameters.Filename)
	dm.sanitizeEditorParameters(&parameters)
	furi := dm.StorageManager.GeneratePublicFileUri(parameters.Filename, remoteAddress, managers.FileMeta{})
	docKey, err := dm.StorageManager.GenerateFileHash(parameters.Filename)
	if err != nil {
		return nil, err
	}

	config := models.Config{
		Type:         parameters.Type,
		DocumentType: dm.ConversionManager.GetFileType(parameters.Filename),
		Document: models.Document{
			Title:    parameters.Filename,
			Url:      furi,
			FileType: utils.GetFileExt(parameters.Filename, true),
			Key:      docKey,
			Info: models.MetaInfo{
				Author:  user.Username,
				Created: time.Now().Format(time.RFC3339),
			},
			Permissions: models.Permissions{
				Comment: parameters.PermissionsMode != onlyoffice_permission_view && parameters.PermissionsMode != onlyoffice_permission_fill_forms &&
					parameters.PermissionsMode != onlyoffice_permission_embedded && parameters.PermissionsMode != onlyoffice_permission_blockcontent,
				Download: true,
				Edit: parameters.CanEdit && (parameters.PermissionsMode == onlyoffice_permission_edit ||
					parameters.PermissionsMode == onlyoffice_permission_filter || parameters.PermissionsMode == onlyoffice_permission_blockcontent),
				FillForms: parameters.PermissionsMode != onlyoffice_permission_view && parameters.PermissionsMode != onlyoffice_permission_comment &&
					parameters.PermissionsMode != onlyoffice_permission_embedded && parameters.PermissionsMode != onlyoffice_permission_blockcontent,
				ModifyFilter:         parameters.PermissionsMode != onlyoffice_permission_filter,
				ModifyContentControl: parameters.PermissionsMode != onlyoffice_permission_blockcontent,
				Review:               parameters.PermissionsMode == onlyoffice_permission_edit || parameters.PermissionsMode == onlyoffice_permission_review,
			},
		},
		EditorConfig: models.EditorConfig{
			Mode:        parameters.Mode,
			Lang:        parameters.Language,
			CallbackUrl: dm.generateCallbackUrl(parameters.Filename, remoteAddress),
			User:        dm.GetUserInfoById(user.Id, remoteAddress),
			Embedded: models.Embedded{
				SaveUrl:       furi,
				EmbedUrl:      furi,
				ShareUrl:      furi,
				ToolbarDocked: "top",
			},
			Customization: models.Customization{
				About:    true,
				Feedback: true,
				Goback: models.Goback{
					RequestClose: false,
				},
			},
		},
	}

	secret := strings.TrimSpace(dm.config.JwtSecret)
	if secret != "" && dm.config.JwtEnabled {
		token, _ := dm.JwtManager.JwtSign(config, []byte(secret))
		config.Token = token
	}

	return &config, nil
}

func (dm DefaultDocumentManager) IsDocumentConvertable(filename string) bool {
	ext := utils.GetFileExt(filename, true)

	return utils.IsInList(ext, dm.specification.Extensions.Viewed) ||
		utils.IsInList(ext, dm.specification.Extensions.Edited) || utils.IsInList(ext, dm.specification.Extensions.Converted)
}

func (dm DefaultDocumentManager) generateCallbackUrl(filename string, remoteAddress string) string {
	return fmt.Sprintf(
		"%s/callback?filename=%s&user_address=%s",
		remoteAddress,
		filename,
		remoteAddress,
	)
}
