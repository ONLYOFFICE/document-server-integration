"""

 (c) Copyright Ascensio System SIA 2020
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

USERS = [
    {
        'uid': 'uid-1',
        'uname': 'John Smith'
    },
    {
        'uid': 'uid-2',
        'uname': 'Mark Pottato'
    },
    {
        'uid': 'uid-3',
        'uname': 'Hamish Mitchell'
    },
    {
        'uid': 'uid-0',
        'uname': 'anonymous'
    }
]

DEFAULT_USER = USERS[0]

# get user information from the request
def getUserFromReq(req):
    uid = req.COOKIES.get('uid')
    uname = req.COOKIES.get('uname')

    if (not uid) | (not uname): # check if we got both the user id and name parameters
        return DEFAULT_USER # if not, return default user values
    else:
        return { 'uid': unquote(uid), 'uname': unquote(uname) }