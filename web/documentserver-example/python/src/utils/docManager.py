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


import os
import shutil
import io
import re
import requests
import time
import urllib.parse
import magic

from django.conf import settings
from django.http import HttpResponse, HttpResponseRedirect, FileResponse
from src.configuration import ConfigurationManager
from src.format import FormatManager
from . import fileUtils, historyManager

config_manager = ConfigurationManager()
format_manager = FormatManager()

def isCanFillForms(ext):
    return ext in format_manager.fillable_extensions()

# check if the file extension can be viewed
def isCanView(ext):
    return ext in format_manager.viewable_extensions()

# check if the file extension can be edited
def isCanEdit(ext):
    return ext in format_manager.editable_extensions()

# check if the file extension can be converted
def isCanConvert(ext):
    return ext in format_manager.convertible_extensions()

# check if the file extension is supported by the editor (it can be viewed or edited or converted)
def isSupportedExt(ext):
    return isCanView(ext) | isCanEdit(ext) | isCanConvert(ext) | isCanFillForms(ext)

# get internal extension for a given file type
def getInternalExtension(fileType):
    mapping = {
        'word': '.docx',
        'cell': '.xlsx',
        'slide': '.pptx',
        'docxf': '.docxf'
    }

    return mapping.get(fileType, '.docx') # the default file type is .docx

# get image url for templates
def getTemplateImageUrl(fileType, request):
    path = getServerUrl(True, request) + '/static/images/'
    mapping = {
        'word': path + 'file_docx.svg',
        'cell': path + 'file_xlsx.svg',
        'slide': path + 'file_pptx.svg'
    }

    return mapping.get(fileType, path + 'file_docx.svg') # the default file type

# get file name with an index if such a file name already exists
def getCorrectName(filename, req):
    basename = fileUtils.getFileNameWithoutExt(filename)
    ext = fileUtils.getFileExt(filename)
    name = f'{basename}{ext}'

    i = 1
    while os.path.exists(getStoragePath(name, req)): # if file with such a name already exists
        name = f'{basename} ({i}){ext}'  # add an index to its name
        i += 1

    return name

# get server url
def getServerUrl (forDocumentServer, req):
    example_url = config_manager.example_url()
    if (forDocumentServer and example_url is not None):
        return example_url.geturl()
    else:
        return req.headers.get("x-forwarded-proto") or req.scheme + "://" + req.get_host()

# get file url
def getFileUri(filename, forDocumentServer, req):
    host = getServerUrl(forDocumentServer, req)
    curAdr = req.META['REMOTE_ADDR']
    return f'{host}{settings.STATIC_URL}{curAdr}/{filename}'

# get absolute URL to the document storage service
def getCallbackUrl(filename, req):
    host = getServerUrl(True, req)
    curAdr = req.META['REMOTE_ADDR']
    return f'{host}/track?filename={filename}&userAddress={curAdr}'

# get url to the created file
def getCreateUrl(fileType, req):
    host = getServerUrl(False, req)
    return f'{host}/create?fileType={fileType}'

# get url to download a file
def getDownloadUrl(filename, req, isServerUrl = True):
    host = getServerUrl(isServerUrl, req)
    curAdr = f'&userAddress={req.META["REMOTE_ADDR"]}' if isServerUrl else ""
    return f'{host}/download?fileName={filename}{curAdr}'

# get root folder for the current file
def getRootFolder(req):
    if isinstance(req, str):
        curAdr = req
    else:
        curAdr = req.META['REMOTE_ADDR']

    storage_directory = config_manager.storage_path()
    directory = storage_directory.joinpath(curAdr)

    if not os.path.exists(directory): # if such a directory does not exist, make it
        os.makedirs(directory)

    return directory

# get the file history path
def getHistoryPath(filename, file, version, req):
    if isinstance(req, str):
        curAdr = req
    else:
        curAdr = req.META['REMOTE_ADDR']

    storage_directory = config_manager.storage_path()
    directory = storage_directory.joinpath(curAdr)
    if not os.path.exists(directory): # the directory with host address doesn't exist
        filePath = os.path.join(getRootFolder(req), f'{filename}-hist', version, file)
    else:
        filePath = os.path.join(directory, f'{filename}-hist', version, file)

    return filePath

# get the file path
def getStoragePath(filename, req):
    directory = getRootFolder(req)

    return os.path.join(directory, fileUtils.getFileName(filename))

# get the path to the forcesaved file version
def getForcesavePath(filename, req, create):
    if isinstance(req, str):
        curAdr = req
    else:
        curAdr = req.META['REMOTE_ADDR']

    storage_directory = config_manager.storage_path()
    directory = storage_directory.joinpath(curAdr)
    if not os.path.exists(directory): # the directory with host address doesn't exist
        return ""
 
    directory = os.path.join(directory, f'{filename}-hist') # get the path to the history of the given file
    if (not os.path.exists(directory)):
        if create: # if the history directory doesn't exist
            os.makedirs(directory) # create history directory if it doesn't exist
        else: # the history directory doesn't exist and we are not supposed to create it
            return ""

    directory = os.path.join(directory, filename) # and get the path to the given file
    if (not os.path.exists(directory) and not create):
        return ""

    return directory

# get information about all the stored files
def getStoredFiles(req):
    directory = getRootFolder(req)

    files = os.listdir(directory)
    files.sort(key=lambda x: os.path.getmtime(os.path.join(directory, x)), reverse=True) # sort files by time of last modification

    fileInfos = []

    for f in files:
        if os.path.isfile(os.path.join(directory, f)):
            fileInfos.append({'isFillFormDoc': isCanFillForms(fileUtils.getFileExt(f)),'version':historyManager.getFileVersion(historyManager.getHistoryDir(getStoragePath(f, req))), 'type': fileUtils.getFileType(f), 'title': f, 'url': getFileUri(f, True, req), 'canEdit': isCanEdit(fileUtils.getFileExt(f))}) # write information about file type, title and url

    return fileInfos

# create a file
def createFile(stream, path, req = None, meta = False):
    bufSize = 8192
    with io.open(path, 'wb') as out: # write data to the file by streams
        read = stream.read(bufSize)
        while len(read) > 0:
            out.write(read)
            read = stream.read(bufSize)
    if meta:
        historyManager.createMeta(path, req) # create meta data for the file if needed
    return

# save file
def saveFile(response, path):
    with open(path, 'wb') as file:
        for chunk in response.iter_content(chunk_size=8192):
            file.write(chunk)
    return

# download file from the given url 
def downloadFileFromUri(uri, path = None, withSave = False):
    resp = requests.get(uri, stream=True, verify = config_manager.ssl_verify_peer_mode_enabled(), timeout=5)
    status_code = resp.status_code
    if status_code != 200:  # checking status code
        raise RuntimeError('Document editing service returned status: %s' % status_code)
    if withSave:
        if path is None:
            raise RuntimeError('Path for saving file is null')
        saveFile(resp, path)
    return resp

# create sample file
def createSample(fileType, sample, req):
    ext = getInternalExtension(fileType) # get the internal extension of the given file type

    if not sample:
        sample = 'false'

    sampleName = 'sample' if sample == 'true' else 'new' # create sample or new template 

    filename = getCorrectName(f'{sampleName}{ext}', req) # get file name with an index if such a file name already exists
    path = getStoragePath(filename, req)

    with io.open(os.path.join('assets', 'document-templates', 'sample' if sample == 'true' else 'new', f'{sampleName}{ext}'), 'rb') as stream: # create sample file of the necessary extension in the directory
        createFile(stream, path, req, True)
    return filename

# remove file from the directory
def removeFile(filename, req):
    path = getStoragePath(filename, req)
    if os.path.exists(path):
        os.remove(path)
    histDir = historyManager.getHistoryDir(path) # get history directory
    if os.path.exists(histDir): # remove all the history information about this file
        shutil.rmtree(histDir)

# generate file key
def generateFileKey(filename, req):
    path = getStoragePath(filename, req)
    uri = getFileUri(filename, False, req)
    stat = os.stat(path) # get the directory parameters

    h = str(hash(f'{uri}_{stat.st_mtime_ns}')) # get the hash value of the file url and the date of its last modification and turn it into a string format
    replaced = re.sub(r'[^0-9-.a-zA-Z_=]', '_', h)
    return replaced[:20] # take the first 20 characters for the key

# generate the document key value
def generateRevisionId(expectedKey):
    if (len(expectedKey) > 20):
        expectedKey = str(hash(expectedKey))

    key = re.sub(r'[^0-9-.a-zA-Z_=]', '_', expectedKey)
    return key[:20]

# get files information
def getFilesInfo(req):
    fileId = req.GET.get('fileId') if req.GET.get('fileId') else None

    result = []
    resultID = []
    for f in getStoredFiles(req): # run through all the files from the directory
        stats = os.stat(os.path.join(getRootFolder(req), f.get("title"))) # get file information
        result.append( # write file parameters to the file object
            {   "version" : historyManager.getFileVersion(historyManager.getHistoryDir(getStoragePath(f.get("title"), req))),
                "id" :  generateFileKey(f.get("title"), req),   
                "contentLength" : "%.2f KB" % (stats.st_size/1024),
                "pureContentLength" : stats.st_size,
                "title" :  f.get("title"),
                "updated" : time.strftime("%Y-%m-%dT%X%z",time.gmtime(stats.st_mtime))
        })
        if fileId : # if file id is defined
            if fileId == generateFileKey(f.get("title"), req) : # and it is equal to the file key value
                resultID.append(result[-1]) # add file object to the response array

    if fileId :
        if len(resultID) > 0 : return resultID
        else : return "File not found"     
    else :
        return result

# download the file
def download(filePath):
    response = FileResponse(open(filePath, 'rb'), True) # write headers to the response object
    response['Content-Length'] =  os.path.getsize(filePath)
    response['Content-Disposition'] = "attachment;filename*=UTF-8\'\'" + urllib.parse.quote_plus(os.path.basename(filePath))
    response['Content-Type'] = magic.from_file(filePath, mime=True)
    response['Access-Control-Allow-Origin'] = "*"
    return response