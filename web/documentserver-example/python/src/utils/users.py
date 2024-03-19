"""

 (c) Copyright Ascensio System SIA 2024

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

"""

from typing import Optional


class User:
    def __init__(self, uid, name, email, group, reviewGroups, commentGroups, userInfoGroups, favorite,
                 deniedPermissions, descriptions, templates, avatar, goback):
        self.id = uid
        self.name = name
        self.email = email
        self.group = group
        self.reviewGroups = reviewGroups
        self.commentGroups = commentGroups
        self.favorite = favorite
        self.deniedPermissions = deniedPermissions
        self.descriptions = descriptions
        self.templates = templates
        self.userInfoGroups = userInfoGroups
        self.avatar = avatar
        self.goback = goback


descr_user_1 = [
    "File author by default",
    "Doesn’t belong to any group",
    "Can review all the changes",
    "Can perform all actions with comments",
    "The file favorite state is undefined",
    "Can create files from templates using data from the editor",
    "Can see the information about all users",
    "Has an avatar",
    "Can submit forms"
]

descr_user_2 = [
    "Belongs to Group2",
    "Can review only his own changes or changes made by users with no group",
    ("Can view comments, edit his own comments and comments left by users with no group."
     "Can remove his own comments only"),
    "This file is marked as favorite",
    "Can create new files from the editor",
    "Can see the information about users from Group2 and users who don’t belong to any group",
    "Has an avatar",
    "Can’t submit forms"
]

descr_user_3 = [
    "Belongs to Group3",
    "Can review changes made by Group2 users",
    "Can view comments left by Group2 and Group3 users. Can edit comments left by the Group2 users",
    "This file isn’t marked as favorite",
    "Can’t copy data from the file to clipboard",
    "Can’t download the file",
    "Can’t print the file",
    "Can create new files from the editor",
    "Can see the information about Group2 users",
    "Can’t submit forms",
    "Can't close history",
    "Can't restore the file version"
]

descr_user_0 = [
    "The name is requested when the editor is opened",
    "Doesn’t belong to any group",
    "Can review all the changes",
    "Can perform all actions with comments",
    "The file favorite state is undefined",
    "Can't mention others in comments",
    "Can't create new files from the editor",
    "Can’t see anyone’s information",
    "Can't rename files from the editor",
    "Can't view chat",
    "Can't protect file",
    "View file without collaboration",
    "Can’t submit forms"
]

USERS = [
    User('uid-1', 'John Smith', 'smith@example.com',
         '', None, {}, None,
         None, [], descr_user_1, True, True, {'blank': False}),
    User('uid-2', 'Mark Pottato', 'pottato@example.com',
         'group-2', ['group-2', ''], {
             'view': "",
             'edit': ["group-2", ""],
             'remove': ["group-2"]
         },
         ['group-2', ''],
         True, [], descr_user_2, False, True, {'text': "Go to Documents"}),
    User('uid-3', 'Hamish Mitchell', None,
         'group-3', ['group-2'], {
             'view': ["group-3", "group-2"],
             'edit': ["group-2"],
             'remove': []
         }, ['group-2'],
         False, ["copy", "download", "print"], descr_user_3, False, False,
         None),
    User('uid-0', None, None,
         '', None, {}, [],
         None, ["protect"], descr_user_0, False, False, None)
]

DEFAULT_USER = USERS[0]


# get all users
def getAllUsers():
    return USERS


# get user information from the request
def getUserFromReq(req):
    uid = req.COOKIES.get('uid')

    for user in USERS:
        if user.id == uid:
            return user

    return DEFAULT_USER


# get users data for mentions
def getUsersForMentions(uid):
    usersData = []
    for user in USERS:
        if (user.id != uid and user.name is not None and user.email is not None):
            usersData.append({'name': user.name, 'email': user.email})
    return usersData


# get users data for protect
def getUsersForProtect(uid):
    usersData = []
    for user in USERS:
        if (user.id != uid and user.name is not None):
            usersData.append({'id': user.id, 'name': user.name, 'email': user.email})
    return usersData


def find_user(searchId: Optional[str]) -> User:
    if searchId is None:
        return DEFAULT_USER
    for user in USERS:
        if not user.id == searchId:
            continue
        return user
    return DEFAULT_USER
