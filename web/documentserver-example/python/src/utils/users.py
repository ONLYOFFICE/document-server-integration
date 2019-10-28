from urllib.parse import unquote

USERS = [
    {
        'uid': 'uid-1',
        'name': 'John Smith'
    },
    {
        'uid': 'uid-2',
        'name': 'Mark Pottato'
    },
    {
        'uid': 'uid-3',
        'name': 'Hamish Mitchell'
    }
]

DEFAULT_USER = USERS[0]

def getUserFromReq(req):
    uid = unquote(req.COOKIES.get('uid'))
    uname = unquote(req.COOKIES.get('uname'))

    if (not uid) | (not uname):
        return DEFAULT_USER
    else:
        return { 'uid': uid, 'uname': uname }