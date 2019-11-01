import config
import os
import io
import re
import requests

from src import settings
from . import fileUtils

LANGUAGES = {
    'en': 'English',
    'bg': 'Bulgarian',
    'zh': 'Chinese',
    'cs': 'Czech',
    'nl': 'Dutch',
    'fr': 'French',
    'de': 'German',
    'hu': 'Hungarian',
    'it': 'Italian',
    'ja': 'Japanese',
    'ko': 'Korean',
    'lv': 'Latvian',
    'pl': 'Polish',
    'pt': 'Portuguese',
    'ru': 'Russian',
    'sk': 'Slovak',
    'sl': 'Slovenian',
    'es': 'Spanish',
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
    fileInfos = []
    for f in files:
        fileInfos.append({ 'type': fileUtils.getFileType(f), 'title': f, 'url': getFileUri(f, req) })

    return fileInfos

def createFile(stream, path, meta = False):
    bufSize = 8196
    with io.open(path, 'wb') as out:
        read = stream.read(bufSize)

        while len(read) > 0:
            out.write(read)
            read = stream.read(bufSize)

    #createmeta
    return

def saveFileFromUri(uri, path, meta = False):
    resp = requests.get(uri, stream=True)
    createFile(resp.raw, path, meta)
    return

def createSample(fileType, sample, req):
    ext = getInternalExtension(fileType)

    if not sample:
        sample = 'false'

    sampleName = 'sample' if sample == 'true' else 'new'

    filename = getCorrectName(f'{sampleName}{ext}', req)
    path = getStoragePath(filename, req)

    with io.open(os.path.join('samples', f'{sampleName}{ext}'), 'rb') as stream:
        createFile(stream, path, True)
    return filename

def removeFile(filename, req):
    path = getStoragePath(filename, req)
    if os.path.exists(path):
        os.remove(path)
    #if history path exists

def generateFileKey(filename, req):
    path = getStoragePath(filename, req)
    uri = getFileUri(filename, req)
    stat = os.stat(path)

    h = str(hash(f'{uri}_{stat.st_mtime_ns}'))
    replaced = re.sub(r'[^0-9-.a-zA-Z_=]', '_', h)
    return replaced[:20]
