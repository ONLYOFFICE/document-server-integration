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
        'text': '.docx',
        'spreadsheet': '.xlsx',
        'presentation': '.pptx'
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

def getFileUri(filename, req):
    host = config.EXAMPLE_DOMAIN.rstrip('/')
    curAdr = req.META['REMOTE_ADDR']
    return f'{host}{settings.STATIC_URL}{curAdr}/{filename}'

def getCallbackUrl(filename, req):
    host = config.EXAMPLE_DOMAIN
    curAdr = req.META['REMOTE_ADDR']
    return f'{host}track?filename={filename}&userAddress={curAdr}'

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

    return os.path.join(directory, filename)

def getStoredFiles(req):
    directory = getRootFolder(req)

    files = os.listdir(directory)
    files.sort(key=lambda x: os.path.getmtime(os.path.join(directory, x)), reverse=True)

    fileInfos = []

    for f in files:
        if os.path.isfile(os.path.join(directory, f)):
            fileInfos.append({ 'type': fileUtils.getFileType(f), 'title': f, 'url': getFileUri(f, req) })

    return fileInfos

def createFile(stream, path, req = None, meta = False):
    bufSize = 8196
    with io.open(path, 'wb') as out:
        read = stream.read(bufSize)

        while len(read) > 0:
            out.write(read)
            read = stream.read(bufSize)

    if meta:
        historyManager.createMeta(path, req)
    return

def saveFileFromUri(uri, path, req = None, meta = False):
    resp = requests.get(uri, stream=True)
    createFile(resp.raw, path, req, meta)
    return

def createSample(fileType, sample, req):
    ext = getInternalExtension(fileType)

    if not sample:
        sample = 'false'

    sampleName = 'sample' if sample == 'true' else 'new'

    filename = getCorrectName(f'{sampleName}{ext}', req)
    path = getStoragePath(filename, req)

    with io.open(os.path.join('samples', f'{sampleName}{ext}'), 'rb') as stream:
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
    uri = getFileUri(filename, req)
    stat = os.stat(path)

    h = str(hash(f'{uri}_{stat.st_mtime_ns}'))
    replaced = re.sub(r'[^0-9-.a-zA-Z_=]', '_', h)
    return replaced[:20]
