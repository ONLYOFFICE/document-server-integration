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
    def __init__(self, id, name, email, group, reviewGroups, favorite, deniedPermissions):
        self.id = id
        self.name = name
        self.email = email
        self.group = group
        self.reviewGroups = reviewGroups
        self.favorite = favorite
        self.deniedPermissions = deniedPermissions

USERS = [
    User('uid-1', 'John Smith', 'smith@mail.ru', None, None, None, []),
    User('uid-2', 'Mark Pottato', 'pottato@mail.ru', 'group-2', ['group-2', ''], True, []),
    User('uid-3', 'Hamish Mitchell', 'mitchell@mail.ru', 'group-3', ['group-2'], False, ["copy", "download", "print"]),
    User('uid-0', None, None, None, None, None, [])
]

DEFAULT_USER = USERS[0]

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
