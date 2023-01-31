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
	"github.com/ONLYOFFICE/document-server-integration/server/managers"
	"github.com/ONLYOFFICE/document-server-integration/server/models"
	"go.uber.org/zap"
)

type DefaultUserManager struct {
	logger *zap.SugaredLogger
}

func NewDefaultUserManager(logger *zap.SugaredLogger) managers.UserManager {
	return &DefaultUserManager{
		logger,
	}
}

var anonymous = models.User{Id: "uid-4", Username: "Anonymous", Description: []string{
	"The name is requested when the editor is opened",
	"Doesn’t belong to any group",
	"Can review all the changes",
	"Can perform all actions with comments",
	"The file favorite state is undefined",
	"Can't mention others in comments",
	"Can't create new files from the editor",
}}

func (um DefaultUserManager) GetUsers() []models.User {
	um.logger.Debug("Fetching all users")
	return []models.User{
		{Id: "uid-1", Username: "John Smith", Description: []string{
			"File author by default",
			"Doesn’t belong to any group",
			"Can review all the changes",
			"Can perform all actions with comments",
			"The file favorite state is undefined",
			"Can create files from templates using data from the editor",
		}},
		{Id: "uid-2", Username: "Mark Pottato", Description: []string{
			"Belongs to Group2",
			"Can review only his own changes or changes made by users with no group",
			"Can view comments, edit his own comments and comments left by users with no group. Can remove his own comments only",
			"This file is marked as favorite",
			"Can create new files from the editor",
		}},
		{Id: "uid-3", Username: "Hamish Mitchell", Description: []string{
			"Belongs to Group3",
			"Can review changes made by Group2 users",
			"Can view comments left by Group2 and Group3 users. Can edit comments left by the Group2 users",
			"This file isn’t marked as favorite",
			"Can’t copy data from the file to clipboard",
			"Can’t download the file",
			"Can’t print the file",
			"Can create new files from the editor",
		}},
		anonymous,
	}
}

func (um DefaultUserManager) GetUserById(uid string) (models.User, error) {
	users := um.GetUsers()

	um.logger.Debugf("Trying to get a user by id: %s", uid)
	for _, user := range users {
		if user.Id == uid {
			return user, nil
		}
	}

	return anonymous, nil
}
