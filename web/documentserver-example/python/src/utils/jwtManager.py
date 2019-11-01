import config
import jwt

def isEnabled():
    return bool(config.DOC_SERV_JWT_SECRET)

def encode(payload):
    return jwt.encode(payload, config.DOC_SERV_JWT_SECRET, algorithm='HS256').decode('utf-8')

def decode(string):
    return jwt.decode(string, config.DOC_SERV_JWT_SECRET, algorithms=['HS256'])