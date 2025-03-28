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
package dmanager

import (
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"go.uber.org/zap"
)

type DefaultUserManager struct {
	users  []models.User
	logger *zap.SugaredLogger
}

func NewDefaultUserManager(logger *zap.SugaredLogger) managers.UserManager {
	users := []models.User{
		{
			Id:                "uid-0",
			Username:          "",
			Email:             "",
			Group:             "",
			ReviewGroups:      nil,
			CommentGroups:     nil,
			UserInfoGroups:    nil,
			Favorite:          -1,
			DeniedPermissions: []string{"protect"},
			Description:       descriptionUser0,
			Templates:         false,
			Avatar:            false,
			Goback:            nil,
			Close:             nil,
		},
		{
			Id:                "uid-1",
			Username:          "John Smith",
			Email:             "smith@example.com",
			Group:             "",
			ReviewGroups:      nil,
			CommentGroups:     nil,
			UserInfoGroups:    nil,
			Favorite:          -1,
			DeniedPermissions: nil,
			Description:       descriptionUser1,
			Templates:         true,
			Avatar:            true,
			Goback: map[string]interface{}{
				"blank": false,
			},
			Close: map[string]interface{}{
				"visible": false,
			},
		},
		{
			Id:           "uid-2",
			Username:     "Mark Pottato",
			Email:        "pottato@example.com",
			Group:        "group-2",
			ReviewGroups: []string{"group-2", ""},
			CommentGroups: map[string]interface{}{
				"view":   "",
				"edit":   []string{"group-2", ""},
				"remove": []string{"group-2"},
			},
			UserInfoGroups:    []string{"group-2", ""},
			Favorite:          1,
			DeniedPermissions: nil,
			Description:       descriptionUser2,
			Templates:         false,
			Avatar:            true,
			Goback: map[string]interface{}{
				"text": "Go to Documents",
			},
			Close: map[string]interface{}{
				"visible": true,
			},
		},
		{
			Id:           "uid-3",
			Username:     "Hamish Mitchell",
			Email:        "mitchell@example.com",
			Group:        "group-3",
			ReviewGroups: []string{"group-2"},
			CommentGroups: map[string]interface{}{
				"view":   []string{"group-3", "group-2"},
				"edit":   []string{"group-2"},
				"remove": "",
			},
			UserInfoGroups:    []string{"group-2"},
			Favorite:          0,
			DeniedPermissions: []string{"copy", "download", "print"},
			Description:       descriptionUser3,
			Templates:         false,
			Avatar:            false,
			Goback:            nil,
			Close: map[string]interface{}{
				"visible": true,
			},
		},
	}
	return &DefaultUserManager{
		users,
		logger,
	}
}

var descriptionUser0 []string = []string{
	"The name is requested when the editor is opened",
	"Doesn't belong to any group",
	"Can review all the changes",
	"Can perform all actions with comments",
	"The file favorite state is undefined",
	"Can't mention others in comments",
	"Can't create new files from the editor",
	"Can't see anyone's information",
	"Can't rename files from the editor",
	"Can't view chat",
	"Can't protect file",
	"View file without collaboration",
	"Can't submit forms",
	"Can't refresh outdated file",
}
var descriptionUser1 []string = []string{
	"File author by default",
	"Doesn't belong to any group",
	"Can review all the changes",
	"Can perform all actions with comments",
	"The file favorite state is undefined",
	"Can create files from templates using data from the editor",
	"Can see the information about all users",
	"Can submit forms",
	"Has an avatar",
}
var descriptionUser2 []string = []string{
	"Belongs to Group2",
	"Can review only his own changes or changes made by users with no group",
	"Can view comments, edit his own comments and comments left by users with no group. Can remove his own comments only",
	"This file is marked as favorite",
	"Can create new files from the editor",
	"Can see the information about users from Group2 and users who don’t belong to any group",
	"Can't submit forms",
	"Has an avatar",
}
var descriptionUser3 []string = []string{
	"Belongs to Group3",
	"Can review changes made by Group2 users",
	"Can view comments left by Group2 and Group3 users. Can edit comments left by the Group2 users",
	"This file isn't marked as favorite",
	"Can't copy data from the file to clipboard",
	"Can't download the file",
	"Can't print the file",
	"Can create new files from the editor",
	"Can see the information about Group2 users",
	"Can't submit forms",
	"Can't close history",
	"Can't restore the file version",
}

func (um DefaultUserManager) GetUsers() []models.User {
	um.logger.Debug("Fetching all users")
	return um.users
}

func (um DefaultUserManager) GetUserById(uid string) (models.User, error) {
	um.logger.Debugf("Trying to get a user by id: %s", uid)
	for _, user := range um.users {
		if user.Id == uid {
			return user, nil
		}
	}

	return um.users[0], nil
}

func (um DefaultUserManager) GetUserInfoById(uid string, serverAddress string) models.UserInfo {
	for _, user := range um.users {
		if user.Id == uid {
			var image string
			if user.Avatar {
				image = serverAddress + "/static/images/" + user.Id + ".png"
			}
			return models.UserInfo{
				Id:    user.Id,
				Name:  user.Username,
				Email: user.Email,
				Image: image,
			}
		}
	}
	return models.UserInfo{}
}

func (um DefaultUserManager) GetUsersForMentions(uid string) (usersForMentions []models.UserInfo) {
	for _, user := range um.users {
		if user.Id != uid && user.Username != "" && user.Email != "" {
			u := models.UserInfo{
				Name:  user.Username,
				Email: user.Email,
			}
			usersForMentions = append(usersForMentions, u)
		}
	}
	return
}

func (um DefaultUserManager) GetUsersForProtect(uid string, serverAddress string) (usersForProtect []models.UserInfo) {
	for _, user := range um.users {
		if user.Id != uid && user.Username != "" {
			var image string
			if user.Avatar {
				image = serverAddress + "/static/images/" + user.Id + ".png"
			}
			u := models.UserInfo{
				Id:    user.Id,
				Name:  user.Username,
				Email: user.Email,
				Image: image,
			}
			usersForProtect = append(usersForProtect, u)
		}
	}
	return
}

func (um DefaultUserManager) GetUsersInfo(serverAddress string) (usersInfo []models.UserInfo) {
	for _, user := range um.users {
		var image string
		if user.Avatar {
			image = serverAddress + "/static/images/" + user.Id + ".png"
		}
		u := models.UserInfo{
			Id:    user.Id,
			Name:  user.Username,
			Email: user.Email,
			Image: image,
		}
		usersInfo = append(usersInfo, u)
	}
	return
}
