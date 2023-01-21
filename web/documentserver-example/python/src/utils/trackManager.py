"""

 (c) Copyright Ascensio System SIA 2023

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


import config
import requests
import os
import json
from . import jwtManager, docManager, historyManager, fileUtils, serviceConverter

# read request body
def readBody(request):
    body = json.loads(request.body)
    if (jwtManager.isEnabled()): # if the secret key to generate token exists
        token = body.get('token') # get the document token

        if (not token): # if JSON web token is not received
            jwtHeader = 'Authorization' if config.DOC_SERV_JWT_HEADER is None or config.DOC_SERV_JWT_HEADER == '' else config.DOC_SERV_JWT_HEADER
            token = request.headers.get(jwtHeader) # get it from the Authorization header
            if token:
                token = token[len('Bearer '):] # and save it without Authorization prefix

        if (not token): # if the token is not received
            raise Exception('Expected JWT') # an error occurs

        body = jwtManager.decode(token)
        if (body.get('payload')): # get the payload object from the request body
            body = body['payload']
    return body

# file saving process
def processSave(body, filename, usAddr):
    download = body.get('url')
    if (download is None):
        raise Exception("DownloadUrl is null")
    changesUri = body.get('changesurl')
    newFilename = filename

    curExt = fileUtils.getFileExt(filename) # get current file extension

    downloadExt = "." + body.get('filetype')  # get the extension of the downloaded file

    # convert downloaded file to the file with the current extension if these extensions aren't equal
    if (curExt != downloadExt):
        try:
            newUri = serviceConverter.getConverterUri(download, downloadExt, curExt, docManager.generateRevisionId(download), False) # convert file and give url to a new file
            if not newUri:
                newFilename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, usAddr) # get the correct file name if it already exists
            else:
                download = newUri
        except Exception:
            newFilename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, usAddr)

    path = docManager.getStoragePath(newFilename, usAddr) # get the file path

    histDir = historyManager.getHistoryDir(path) # get the path to the history direction
    if not os.path.exists(histDir): # if the path doesn't exist
        os.makedirs(histDir) # create it

    versionDir = historyManager.getNextVersionDir(histDir) # get the path to the next file version

    os.rename(docManager.getStoragePath(filename, usAddr), historyManager.getPrevFilePath(versionDir, curExt)) # get the path to the previous file version and rename the storage path with it
    docManager.saveFileFromUri(download, path) # save file to the storage path 
    docManager.saveFileFromUri(changesUri, historyManager.getChangesZipPath(versionDir)) # save file changes to the diff.zip archive

    hist = None
    hist = body.get('changeshistory')
    if (not hist) & ('history' in body):
        hist = json.dumps(body.get('history'))
    if hist:
        historyManager.writeFile(historyManager.getChangesHistoryPath(versionDir), hist) # write the history changes to the changes.json file

    historyManager.writeFile(historyManager.getKeyPath(versionDir), body.get('key')) # write the key value to the key.txt file

    forcesavePath = docManager.getForcesavePath(newFilename, usAddr, False) # get the path to the forcesaved file version
    if (forcesavePath != ""): # if the forcesaved file version exists
       os.remove(forcesavePath) # remove it

    return

# file force saving process
def processForceSave(body, filename, usAddr):
    download = body.get('url')
    if (download is None):
        raise Exception("DownloadUrl is null")
    curExt = fileUtils.getFileExt(filename) # get current file extension

    downloadExt = "." + body.get('filetype')  # get the extension of the downloaded file

    newFilename = False

    # convert downloaded file to the file with the current extension if these extensions aren't equal
    if (curExt != downloadExt):
        try:
            newUri = serviceConverter.getConverterUri(download, downloadExt, curExt, docManager.generateRevisionId(download), False) # convert file and give url to a new file
            if not newUri:
                newFilename = True
            else:
                download = newUri
        except Exception:
            newFilename = True

    isSubmitForm = body.get('forcesavetype') == 3 # SubmitForm

    if(isSubmitForm):
        if (newFilename):
            filename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + "-form" + downloadExt, usAddr) # get the correct file name if it already exists
        else :
            filename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + "-form" + curExt, usAddr)
        forcesavePath = docManager.getStoragePath(filename, usAddr)
    else:
        if (newFilename):
            filename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, usAddr)
        forcesavePath = docManager.getForcesavePath(filename, usAddr, False)
        if (forcesavePath == ""):
            forcesavePath = docManager.getForcesavePath(filename, usAddr, True)

    docManager.saveFileFromUri(download, forcesavePath)

    if(isSubmitForm):
        uid = body['actions'][0]['userid'] # get the user id
        historyManager.createMetaData(filename, uid, "Filling Form", usAddr) # create meta data for forcesaved file
    return

# create a command request
def commandRequest(method, key, meta = None):
    documentCommandUrl = config.DOC_SERV_SITE_URL + config.DOC_SERV_COMMAND_URL

    payload = {
        'c': method,
        'key': key
    }

    if (meta): 
        payload['meta'] = meta


    headers={'accept': 'application/json'}

    if jwtManager.isEnabled(): # check if a secret key to generate token exists or not
        jwtHeader = 'Authorization' if config.DOC_SERV_JWT_HEADER is None or config.DOC_SERV_JWT_HEADER == '' else config.DOC_SERV_JWT_HEADER # get jwt header
        headerToken = jwtManager.encode({'payload': payload}) # encode a payload object into a header token
        headers[jwtHeader] = f'Bearer {headerToken}' # add a header Authorization with a header token with Authorization prefix in it

        payload['token'] = jwtManager.encode(payload) # encode a payload object into a body token
    response = requests.post(documentCommandUrl, json=payload, headers=headers, verify = config.DOC_SERV_VERIFY_PEER)

    if (meta): 
        return response

    return

