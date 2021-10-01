"""

 (c) Copyright Ascensio System SIA 2021
 *
 The MIT License (MIT)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.

"""

from urllib.parse import unquote

class User:
    def __init__(self, id, name, email, group, reviewGroups, commentGroups, favorite, deniedPermissions, descriptions, templates):
        self.id = id
        self.name = name
        self.email = email
        self.group = group
        self.reviewGroups = reviewGroups
        self.commentGroups = commentGroups
        self.favorite = favorite
        self.deniedPermissions = deniedPermissions
        self.descriptions = descriptions
        self.templates = templates

descr_user_1 = [
    "File author by default",
    "Doesn’t belong to any group",
    "Can review all the changes",
    "Can perform all actions with comments",
    "The file favorite state is undefined",
    "Can create files from templates using data from the editor"
]

descr_user_2 = [
    "Belongs to Group2",
    "Can review only his own changes or changes made by users with no group",
    "Can view comments, edit his own comments and comments left by users with no group. Can remove his own comments only",
    "This file is marked as favorite",
    "Can create new files from the editor"
]

descr_user_3 = [
    "Belongs to Group3",
    "Can review changes made by Group2 users",
    "Can view comments left by Group2 and Group3 users. Can edit comments left by the Group2 users",
    "This file isn’t marked as favorite",
    "Can’t copy data from the file to clipboard",
    "Can’t download the file",
    "Can’t print the file",
    "Can create new files from the editor"
]

descr_user_0 = [
    "The name is requested when the editor is opened",
    "Doesn’t belong to any group",
    "Can review all the changes",
    "Can perform all actions with comments",
    "The file favorite state is undefined",
    "Can't mention others in comments",
    "Can't create new files from the editor"
]

USERS = [
    User('uid-1', 'John Smith', 'smith@example.com',
        None, None, {},
        None, [], descr_user_1, True),
    User('uid-2', 'Mark Pottato', 'pottato@example.com',
        'group-2', ['group-2', ''], {
            'view': "",
            'edit': ["group-2", ""],
            'remove': ["group-2"]
        },
        True, [], descr_user_2, False),
    User('uid-3', 'Hamish Mitchell', 'mitchell@example.com',
        'group-3', ['group-2'], {
            'view': ["group-3", "group-2"],
            'edit': ["group-2"],
            'remove': []
        },
        False, ["copy", "download", "print"], descr_user_3, False),
    User('uid-0', None, None,
        None, None, {},
        None, [], descr_user_0, False)
]

DEFAULT_USER = USERS[0]

# get all users
def getAllUsers():
    return USERS

# get user information from the request
def getUserFromReq(req):
    uid = req.COOKIES.get('uid')

    for user in USERS:
        if (user.id == uid):
            return user

    return DEFAULT_USER

# get users data for mentions
def getUsersForMentions(uid):
    usersData = []
    for user in USERS:
        if(user.id != uid and user.name != None and user.email != None):
            usersData.append({'name':user.name, 'email':user.email})
    return usersData
