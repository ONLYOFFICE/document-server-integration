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

import os
import io
import json
import config

from . import users, fileUtils
from datetime import datetime
from src import settings
from src.utils import docManager
from src.utils import jwtManager
    
# get the path to the history direction
def getHistoryDir(storagePath):
    return f'{storagePath}-hist'

# get the path to the given file version
def getVersionDir(histDir, version):
    return os.path.join(histDir, str(version))

# get file version of the given history directory
def getFileVersion(histDir):
    if not os.path.exists(histDir): # if the history directory doesn't exist
        return 0 # file version is 0

    cnt = 1

    for f in os.listdir(histDir): # run through all the files in the history directory
        if not os.path.isfile(os.path.join(histDir, f)): # and count the number of files
            cnt += 1
    
    return cnt

# get the path to the next file version
def getNextVersionDir(histDir):
    v = getFileVersion(histDir) # get file version of the given history directory
    path = getVersionDir(histDir, v) # get the path to the next file version

    if not os.path.exists(path): # if this path doesn't exist
        os.makedirs(path) # make the directory for this file version
    return path

# get the path to a file archive with differences in the given file version
def getChangesZipPath(verDir):
    return os.path.join(verDir, 'diff.zip')

# get the path to a json file with changes of the given file version
def getChangesHistoryPath(verDir):
    return os.path.join(verDir, 'changes.json')

# get the path to the previous file version
def getPrevFilePath(verDir, ext):
    return os.path.join(verDir, f'prev{ext}')

# get the path to a txt file with a key information in it
def getKeyPath(verDir):
    return os.path.join(verDir, 'key.txt')

# get the path to a json file with meta data about this file
def getMetaPath(histDir):
    return os.path.join(histDir, 'createdInfo.json')

# create a json file with file meta data using the storage path and request
def createMeta(storagePath, req):
    histDir = getHistoryDir(storagePath)
    path = getMetaPath(histDir) # get the path to a json file with meta data about file

    if not os.path.exists(histDir):
        os.makedirs(histDir)

    user = users.getUserFromReq(req) # get the user information (id and name)

    obj = { # create the meta data object
        'created': datetime.today().strftime('%Y-%m-%d %H:%M:%S'),
        'uid': user.id,
        'uname': user.name
    }

    writeFile(path, json.dumps(obj))
    
    return

# create a json file with file meta data using the file name, user id, user name and user address
def createMetaData(filename, uid, uname, usAddr):
    histDir = getHistoryDir(docManager.getStoragePath(filename, usAddr))
    path = getMetaPath(histDir) # get the path to a json file with meta data about file

    if not os.path.exists(histDir):
        os.makedirs(histDir)

    obj = { # create the meta data object
        'created': datetime.today().strftime('%Y-%m-%d %H:%M:%S'),
        'uid': uid,
        'uname': uname
    }

    writeFile(path, json.dumps(obj))

    return

# create file with a given content in it
def writeFile(path, content):
    with io.open(path, 'w') as out:
        out.write(content)
    return

# read a file
def readFile(path):
    with io.open(path, 'r') as stream:
        return stream.read()

# get the url to the previous file version with a given extension
def getPrevUri(filename, ver, ext, req):
    host = docManager.getServerUrl(True, req)
    curAdr = req.META['REMOTE_ADDR']
    prevUri =  docManager.getDownloadUrl(f'{filename}-hist/{ver}/prev.{ext}', req) if os.path.isabs(config.STORAGE_PATH) else f'{host}{settings.STATIC_URL}{curAdr}/{filename}-hist/{ver}/prev{ext}'
    return prevUri

# get the url to a file archive with changes of the given file version
def getZipUri(filename, ver, req):
    host = docManager.getServerUrl(True, req)
    curAdr = req.META['REMOTE_ADDR']
    zipUri = docManager.getDownloadUrl(f'{filename}-hist/{ver}/diff.zip', req) if os.path.isabs(config.STORAGE_PATH) else f'{host}{settings.STATIC_URL}{curAdr}/{filename}-hist/{ver}/diff.zip'
    return zipUri

# get the meta data of the file
def getMeta(storagePath):
    histDir = getHistoryDir(storagePath)
    path = getMetaPath(histDir)

    if os.path.exists(path): # check if the json file with file meta data exists
        with io.open(path, 'r') as stream:
            return json.loads(stream.read()) # turn meta data into python format
    
    return None

# get the document history of a given file
def getHistoryObject(storagePath, filename, docKey, docUrl, req):
    histDir = getHistoryDir(storagePath)
    version = getFileVersion(histDir)
    if version > 0: # if the file was modified (the file version is greater than 0)
        hist = []
        histData = {}
        
        for i in range(1, version + 1): # run through all the file versions
            obj = {}
            dataObj = {}
            prevVerDir = getVersionDir(histDir, i - 1) # get the path to the previous file version
            verDir = getVersionDir(histDir, i) # get the path to the given file version

            try:
                key = docKey if i == version else readFile(getKeyPath(verDir)) # get document key

                obj['key'] = key
                obj['version'] = i
                dataObj['key'] = key
                dataObj['version'] = i

                if i == 1: # check if the version number is equal to 1
                    meta = getMeta(storagePath) # get meta data of this file
                    if meta: # write meta information to the object (user information and creation date)
                        obj['created'] = meta['created']
                        obj['user'] = {
                            'id': meta['uid'],
                            'name': meta['uname']
                        }
                    
                dataObj['url'] = docUrl if i == version else getPrevUri(filename, i, fileUtils.getFileExt(filename), req) # write file url to the data object

                if i > 1: # check if the version number is greater than 1 (the file was modified)
                    changes = json.loads(readFile(getChangesHistoryPath(prevVerDir))) # get the path to the changes.json file 
                    change = changes['changes'][0]
                    
                    obj['changes'] = changes['changes'] if change else None # write information about changes to the object
                    obj['serverVersion'] = changes['serverVersion']
                    obj['created'] = change['created'] if change else None
                    obj['user'] = change['user'] if change else None

                    prev = histData[str(i - 2)] # get the history data from the previous file version
                    prevInfo = { # write key and url information about previous file version
                        'key': prev['key'],
                        'url': prev['url']
                    }
                    dataObj['previous'] = prevInfo # write information about previous file version to the data object
                    dataObj['changesUrl'] = getZipUri(filename, i - 1, req) # write the path to the diff.zip archive with differences in this file version

                if jwtManager.isEnabled():
                    dataObj['token'] = jwtManager.encode(dataObj) 

                hist.append(obj) # add object dictionary to the hist list
                histData[str(i - 1)] = dataObj # write data object information to the history data
            except Exception:
                return {}
        
        histObj = { # write history information about the current file version to the history object
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