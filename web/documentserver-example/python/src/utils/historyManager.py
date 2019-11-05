import os
import io
import json
import config

from . import users, fileUtils
from datetime import datetime
from src import settings
    
def getHistoryDir(storagePath):
    return f'{storagePath}-hist'

def getVersionDir(histDir, version):
    return os.path.join(histDir, str(version))

def getFileVersion(histDir):
    if not os.path.exists(histDir):
        return 0

    cnt = 0

    for f in os.listdir(histDir):
        if not os.path.isfile(os.path.join(histDir, f)):
            cnt += 1
    
    return cnt

def getNextVersionDir(histDir):
    v = getFileVersion(histDir)
    path = getVersionDir(histDir, v + 1)

    if not os.path.exists(path):
        os.makedirs(path)
    return path

def getChangesZipPath(verDir):
    return os.path.join(verDir, 'diff.zip')

def getChangesHistoryPath(verDir):
    return os.path.join(verDir, 'changes.json')

def getPrevFilePath(verDir, ext):
    return os.path.join(verDir, f'prev{ext}')

def getKeyPath(verDir):
    return os.path.join(verDir, 'key.txt')

def getMetaPath(histDir):
    return os.path.join(histDir, 'createdInfo.json')

def createMeta(storagePath, req):
    histDir = getHistoryDir(storagePath)
    path = getMetaPath(histDir)

    if not os.path.exists(histDir):
        os.makedirs(histDir)

    user = users.getUserFromReq(req)

    obj = {
        'created': datetime.today().strftime('%d.%m.%Y %H:%M:%S'),
        'uid': user['uid'],
        'uname': user['uname']
    }

    writeFile(path, json.dumps(obj))
    
    return

def writeFile(path, content):
    with io.open(path, 'w') as out:
        out.write(content)
    return

def readFile(path):
    with io.open(path, 'r') as stream:
        return stream.read()

def getPrevUri(filename, ver, ext, req):
    host = config.EXAMPLE_DOMAIN.rstrip('/')
    curAdr = req.META['REMOTE_ADDR']
    return f'{host}{settings.STATIC_URL}{curAdr}/{filename}-hist/{ver}/prev{ext}'

def getZipUri(filename, ver, req):
    host = config.EXAMPLE_DOMAIN.rstrip('/')
    curAdr = req.META['REMOTE_ADDR']
    return f'{host}{settings.STATIC_URL}{curAdr}/{filename}-hist/{ver}/diff.zip'

def getMeta(storagePath):
    histDir = getHistoryDir(storagePath)
    path = getMetaPath(histDir)

    if os.path.exists(path):
        with io.open(path, 'r') as stream:
            return json.loads(stream.read())
    
    return None

def getHistoryObject(storagePath, filename, docKey, docUrl, req):
    histDir = getHistoryDir(storagePath)
    version = getFileVersion(histDir)
    if version > 0:
        hist = []
        histData = {}

        for i in range(version + 1):
            obj = {}
            dataObj = {}
            prevVerDir = getVersionDir(histDir, i)
            verDir = getVersionDir(histDir, i + 1)

            try:
                key = docKey if i == version else readFile(getKeyPath(verDir))

                obj['key'] = key
                obj['version'] = i
                dataObj['key'] = key
                dataObj['version'] = i

                if i == 0:
                    meta = getMeta(storagePath)
                    if meta:
                        obj['created'] = meta['created']
                        obj['user'] = {
                            'id': meta['uid'],
                            'name': meta['uname']
                        }
                    
                dataObj['url'] = docUrl if i == version else getPrevUri(filename, i + 1, fileUtils.getFileExt(filename), req)

                if i > 0:
                    changes = json.loads(readFile(getChangesHistoryPath(prevVerDir)))
                    change = changes['changes'][0]
                    
                    obj['changes'] = changes['changes']
                    obj['serverVersion'] = changes['serverVersion']
                    obj['created'] = change['created']
                    obj['user'] = change['user']

                    prev = histData[str(i - 1)]
                    prevInfo = {
                        'key': prev['key'],
                        'url': prev['url']
                    }
                    dataObj['previous'] = prevInfo
                    dataObj['changesUrl'] = getZipUri(filename, i, req)

                hist.append(obj)
                histData[str(i)] = dataObj
            except Exception:
                return {}
        
        histObj = {
            'currentVersion': version,
            'history': hist
        }

        return { 'history': histObj, 'historyData': histData }
    return {}


class CorsHeaderMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        resp = self.get_response(request)
        if request.path.endswith('.zip'):
            resp['Access-Control-Allow-Origin'] = '*'
        return resp