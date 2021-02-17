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


import config
import requests
import os
import json
from . import jwtManager, docManager, historyManager, fileUtils, serviceConverter

def readBody(request):
    body = json.loads(request.body)
    if (jwtManager.isEnabled()):
        token = body.get('token')

        if (not token):
            jwtHeader = 'Authorization' if config.DOC_SERV_JWT_HEADER is None or config.DOC_SERV_JWT_HEADER == '' else config.DOC_SERV_JWT_HEADER
            token = request.headers.get(jwtHeader)
            if token:
                token = token[len('Bearer '):]

        if (not token):
            raise Exception('Expected JWT')

        body = jwtManager.decode(token)
        if (body.get('payload')):
            body = body['payload']
    return body

def processSave(body, filename, usAddr, request):
    download = body.get('url')
    changesUri = body.get('changesurl')
    newFilename = filename

    curExt = fileUtils.getFileExt(filename)
    downloadExt = fileUtils.getFileExt(download)

    if (curExt != downloadExt):
        try:
            newUri = serviceConverter.getConverterUri(download, downloadExt, curExt, docManager.generateRevisionId(download), False)
            if not newUri:
                newFilename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, request)
            else:
                download = newUri
        except Exception:
            newFilename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, request)

    path = docManager.getStoragePath(newFilename, usAddr)

    histDir = historyManager.getHistoryDir(path)
    if not os.path.exists(histDir):
        os.makedirs(histDir)

    versionDir = historyManager.getNextVersionDir(histDir)

    os.rename(docManager.getStoragePath(filename, usAddr), historyManager.getPrevFilePath(versionDir, curExt))
    docManager.saveFileFromUri(download, path)
    docManager.saveFileFromUri(changesUri, historyManager.getChangesZipPath(versionDir))

    hist = None
    hist = body.get('changeshistory')
    if (not hist) & ('history' in body):
        hist = json.dumps(body.get('history'))
    if hist:
        historyManager.writeFile(historyManager.getChangesHistoryPath(versionDir), hist)

    historyManager.writeFile(historyManager.getKeyPath(versionDir), body.get('key'))

    forcesavePath = docManager.getForcesavePath(newFilename, request, False)
    if (forcesavePath != ""):
       os.remove(forcesavePath)

    return

def processForceSave(body, filename, usAddr, request):
    download = body.get('url')

    curExt = fileUtils.getFileExt(filename)
    downloadExt = fileUtils.getFileExt(download)

    if (curExt != downloadExt):
        try:
            newUri = serviceConverter.getConverterUri(download, downloadExt, curExt, docManager.generateRevisionId(download), False)
            if not newUri:
                filename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, request)
            else:
                download = newUri
        except Exception:
            filename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, request)

    forcesavePath = docManager.getForcesavePath(filename, request, False)
    if (forcesavePath == ""):
       forcesavePath = docManager.getForcesavePath(filename, request, True)

    docManager.saveFileFromUri(download, forcesavePath)

    return

def commandRequest(method, key):
    documentCommandUrl = config.DOC_SERV_SITE_URL + config.DOC_SERV_COMMAND_URL

    payload = {
        'c': method,
        'key': key
    }

    headers={'accept': 'application/json'}

    if jwtManager.isEnabled():
        jwtHeader = 'Authorization' if config.DOC_SERV_JWT_HEADER is None or config.DOC_SERV_JWT_HEADER == '' else config.DOC_SERV_JWT_HEADER
        headerToken = jwtManager.encode({'payload': payload})
        headers[jwtHeader] = f'Bearer {headerToken}'

        payload['token'] = jwtManager.encode(payload)
        
    response = requests.post(documentCommandUrl, json=payload, headers=headers)

    return

