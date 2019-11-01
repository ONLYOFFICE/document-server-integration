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
    }
]

DEFAULT_USER = USERS[0]

def getUserFromReq(req):
    uid = req.COOKIES.get('uid')
    uname = req.COOKIES.get('uname')

    if (not uid) | (not uname):
        return DEFAULT_USER
    else:
        return { 'uid': unquote(uid), 'uname': unquote(uname) }