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
import os
import shutil
import io
import re
import requests
import time

from src import settings
from . import fileUtils, historyManager

LANGUAGES = {
    'en': 'English',
    'be': 'Belarusian',
    'bg': 'Bulgarian',
    'ca': 'Catalan',
    'zh': 'Chinese',
    'cs': 'Czech',
    'da': 'Danish',
    'nl': 'Dutch',
    'fi': 'Finnish',
    'fr': 'French',
    'de': 'German',
    'el': 'Greek',
    'hu': 'Hungarian',
    'id': 'Indonesian',
    'it': 'Italian',
    'ja': 'Japanese',
    'ko': 'Korean',
    'lv': 'Latvian',
    'lo': 'Lao',
    'nb': 'Norwegian',
    'pl': 'Polish',
    'pt': 'Portuguese',
    'ro': 'Romanian',
    'ru': 'Russian',
    'sk': 'Slovak',
    'sl': 'Slovenian',
    'es': 'Spanish',
    'sv': 'Swedish',
    'tr': 'Turkish',
    'uk': 'Ukrainian',
    'vi': 'Vietnamese'
}

def isCanView(ext):
    return ext in config.DOC_SERV_VIEWED

def isCanEdit(ext):
    return ext in config.DOC_SERV_EDITED

def isCanConvert(ext):
    return ext in config.DOC_SERV_CONVERT

def isSupportedExt(ext):
    return isCanView(ext) | isCanEdit(ext) | isCanConvert(ext)

def getInternalExtension(fileType):
    mapping = {
        'word': '.docx',
        'cell': '.xlsx',
        'slide': '.pptx'
    }

    return mapping.get(fileType, '.docx')

def getCorrectName(filename, req):
    basename = fileUtils.getFileNameWithoutExt(filename)
    ext = fileUtils.getFileExt(filename)
    name = f'{basename}{ext}'

    i = 1
    while os.path.exists(getStoragePath(name, req)):
        name = f'{basename} ({i}){ext}'
        i += 1

    return name

def getServerUrl (forDocumentServer, req):
    if (forDocumentServer and config.EXAMPLE_DOMAIN is not None):
        return  config.EXAMPLE_DOMAIN 
    else:
        return req.headers.get("x-forwarded-proto") or req.scheme + "://" + req.get_host()

def getFileUri(filename, forDocumentServer, req):
    host = getServerUrl(forDocumentServer, req)
    curAdr = req.META['REMOTE_ADDR']
    return f'{host}{settings.STATIC_URL}{curAdr}/{filename}'

def getCallbackUrl(filename, req):
    host = getServerUrl(True, req)
    curAdr = req.META['REMOTE_ADDR']
    return f'{host}/track?filename={filename}&userAddress={curAdr}'

def getRootFolder(req):
    if isinstance(req, str):
        curAdr = req
    else:
        curAdr = req.META['REMOTE_ADDR']

    directory = os.path.join(config.STORAGE_PATH, curAdr)

    if not os.path.exists(directory):
        os.makedirs(directory)

    return directory

def getStoragePath(filename, req):
    directory = getRootFolder(req)

    return os.path.join(directory, fileUtils.getFileName(filename))

def getForcesavePath(filename, req, create):
    if isinstance(req, str):
        curAdr = req
    else:
        curAdr = req.META['REMOTE_ADDR']

    directory = os.path.join(config.STORAGE_PATH, curAdr)
    if not os.path.exists(directory):
        return ""
 
    directory = os.path.join(directory, f'{filename}-hist')
    if (not os.path.exists(directory)):
        if create:
            os.makedirs(directory)
        else:
            return ""

    directory = os.path.join(directory, filename)
    if (not os.path.exists(directory) and not create):
        return ""

    return directory

def getStoredFiles(req):
    directory = getRootFolder(req)

    files = os.listdir(directory)
    files.sort(key=lambda x: os.path.getmtime(os.path.join(directory, x)), reverse=True)

    fileInfos = []

    for f in files:
        if os.path.isfile(os.path.join(directory, f)):
            fileInfos.append({ 'type': fileUtils.getFileType(f), 'title': f, 'url': getFileUri(f, True, req) })

    return fileInfos

def createFile(stream, path, req = None, meta = False):
    bufSize = 8192
    with io.open(path, 'wb') as out:
        read = stream.read(bufSize)
        while len(read) > 0:
            out.write(read)
            read = stream.read(bufSize)
    if meta:
        historyManager.createMeta(path, req)
    return

def createFileResponse(response, path, req, meta):
    response.raise_for_status()
    with open(path, 'wb') as file:
        for chunk in response.iter_content(chunk_size=8192):
            file.write(chunk)
    return

def saveFileFromUri(uri, path, req = None, meta = False):
    resp = requests.get(uri, stream=True)
    createFileResponse(resp, path, req, meta)
    return

def createSample(fileType, sample, req):
    ext = getInternalExtension(fileType)

    if not sample:
        sample = 'false'

    sampleName = 'sample' if sample == 'true' else 'new'

    filename = getCorrectName(f'{sampleName}{ext}', req)
    path = getStoragePath(filename, req)

    with io.open(os.path.join('assets', 'sample' if sample == 'true' else 'new', f'{sampleName}{ext}'), 'rb') as stream:
        createFile(stream, path, req, True)
    return filename

def removeFile(filename, req):
    path = getStoragePath(filename, req)
    if os.path.exists(path):
        os.remove(path)
    histDir = historyManager.getHistoryDir(path)
    if os.path.exists(histDir):
        shutil.rmtree(histDir)

def generateFileKey(filename, req):
    path = getStoragePath(filename, req)
    uri = getFileUri(filename, False, req)
    stat = os.stat(path)

    h = str(hash(f'{uri}_{stat.st_mtime_ns}'))
    replaced = re.sub(r'[^0-9-.a-zA-Z_=]', '_', h)
    return replaced[:20]

def generateRevisionId(expectedKey):
    if (len(expectedKey) > 20):
        expectedKey = str(hash(expectedKey))

    key = re.sub(r'[^0-9-.a-zA-Z_=]', '_', expectedKey)
    return key[:20]

def getFilesInfo(req):
    fileId = req.GET.get('fileId') if req.GET.get('fileId') else None

    result = []
    resultID = []
    for f in getStoredFiles(req):
        stats = os.stat(os.path.join(getRootFolder(req), f.get("title")))
        result.append(
            {   "version" : historyManager.getFileVersion(historyManager.getHistoryDir(getStoragePath(f.get("title"), req))),
                "id" :  generateFileKey(f.get("title"), req),   
                "contentLength" : "%.2f KB" % (stats.st_size/1024),
                "pureContentLength" : stats.st_size,
                "title" :  f.get("title"),
                "updated" : time.strftime("%Y-%m-%dT%X%z",time.gmtime(stats.st_mtime))
        })
        if fileId :
            if fileId == generateFileKey(f.get("title"), req) :
                resultID.append(result[-1]) 

    if fileId :
        if len(resultID) > 0 : return resultID
        else : return "File not found"     
    else :
        return result