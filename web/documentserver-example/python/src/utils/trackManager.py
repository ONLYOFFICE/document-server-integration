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

import shutil

from copy import deepcopy
import requests
import os
import json
from src.configuration import ConfigurationManager
from src.history import HistoryManager, HistoryChanges
from src.storage import StorageManager
from src.proxy import ProxyManager
from . import jwtManager, docManager, historyManager, fileUtils, serviceConverter

# read request body
def readBody(request):
    body = json.loads(request.body)
    if (jwtManager.isEnabled() and jwtManager.useForRequest()): # if the secret key to generate token exists
        token = body.get('token') # get the document token

        if (not token): # if JSON web token is not received
            config = ConfigurationManager()
            token = request.headers.get(config.jwt_header()) # get it from the Authorization header
            if token:
                token = token[len('Bearer '):] # and save it without Authorization prefix

        if (not token): # if the token is not received
            raise Exception('Expected JWT') # an error occurs

        body = jwtManager.decode(token)
        if (body.get('payload')): # get the payload object from the request body
            body = body['payload']
    return body

# file saving process
def processSave(raw_body, filename, usAddr):
    body = resolve_process_save_body(raw_body)

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
            convertedData = serviceConverter.getConvertedData(download, downloadExt, curExt, docManager.generateRevisionId(download), False) # convert file and give url to a new file
            if not convertedData:
                newFilename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, usAddr) # get the correct file name if it already exists
            else:
                download = convertedData['uri']
        except Exception:
            newFilename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, usAddr)

    data = docManager.downloadFileFromUri(download)
    if (data is None):
        raise Exception("Downloaded document is null")

    dataChanges = docManager.downloadFileFromUri(changesUri)
    if (dataChanges is None):
        raise Exception("Downloaded changes is null")

    hist = body.get('changeshistory')
    if (not hist) & ('history' in body):
        hist = json.dumps(body.get('history'))

    config_manager = ConfigurationManager()
    storage_manager = StorageManager(
        config_manager=config_manager,
        user_host=usAddr,
        source_basename=newFilename
    )
    history_manager = HistoryManager(
        storage_manager=storage_manager
    )
    history_changes = HistoryChanges.decode(hist)
    history_manager.save(
        changes=history_changes,
        diff=dataChanges.iter_content(chunk_size=8192),
        item=data.iter_content(chunk_size=8192)
    )

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
            convertedData = serviceConverter.getConvertedData(download, downloadExt, curExt, docManager.generateRevisionId(download), False) # convert file and give url to a new file
            if not convertedData:
                newFilename = True
            else:
                download = convertedData['uri']
        except Exception:
            newFilename = True

    data = docManager.downloadFileFromUri(download)  # download document file
    if (data is None):
        raise Exception("Downloaded document is null")

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

    docManager.saveFile(download, forcesavePath) # save document file

    if(isSubmitForm):
        uid = body['actions'][0]['userid'] # get the user id
        historyManager.createMetaData(filename, uid, "Filling Form", usAddr) # create meta data for forcesaved file
    return

# create a command request
def commandRequest(method, key, meta = None):
    config = ConfigurationManager()

    payload = {
        'c': method,
        'key': key
    }

    if (meta): 
        payload['meta'] = meta


    headers={'accept': 'application/json'}

    if (jwtManager.isEnabled() and jwtManager.useForRequest()): # check if a secret key to generate token exists or not
        config = ConfigurationManager()
        headerToken = jwtManager.encode({'payload': payload}) # encode a payload object into a header token
        headers[config.jwt_header()] = f'Bearer {headerToken}' # add a header Authorization with a header token with Authorization prefix in it

        payload['token'] = jwtManager.encode(payload) # encode a payload object into a body token
    response = requests.post(config.document_server_command_url().geturl(), json=payload, headers=headers, verify = config.ssl_verify_peer_mode_enabled())

    if (meta): 
        return response

    return

def resolve_process_save_body(body):
    copied = deepcopy(body)
    config_manager = ConfigurationManager()
    proxy_manager = ProxyManager(config_manager=config_manager)

    url = copied.get('url')
    if url is not None:
        resolved_url = proxy_manager.resolve_document_server_url(url)
        copied['url'] = resolved_url.geturl()

    changes_url = copied.get('changesurl')
    if changes_url is not None:
        resolved_url = proxy_manager.resolve_document_server_url(changes_url)
        copied['changesurl'] = resolved_url.geturl()

    home = copied.get('home')
    if home is not None:
        url = home.get('url')
        if url is not None:
            resolved_url = proxy_manager.resolve_document_server_url(url)
            home['url'] = resolved_url.geturl()

        changes_url = home.get('changesurl')
        if changes_url is not None:
            resolved_url = proxy_manager.resolve_document_server_url(changes_url)
            home['changesurl'] = resolved_url.geturl()

        copied['home'] = home

    return copied
