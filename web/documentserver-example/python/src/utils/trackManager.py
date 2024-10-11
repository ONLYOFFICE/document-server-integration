"""

 (c) Copyright Ascensio System SIA 2024

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

from copy import deepcopy
import json
import os
from urllib.parse import urlparse
import requests
from src.configuration import ConfigurationManager
from src.proxy import ProxyManager
from . import jwtManager, docManager, historyManager, fileUtils, serviceConverter

config_manager = ConfigurationManager()
proxy_manager = ProxyManager(config_manager=config_manager)


# read request body
def readBody(request):
    body = json.loads(request.body)
    if (jwtManager.isEnabled() and jwtManager.useForRequest()):  # if the secret key to generate token exists
        token = body.get('token')  # get the document token

        if not token:  # if JSON web token is not received
            token = request.headers.get(config_manager.jwt_header())  # get it from the Authorization header
            if token:
                token = token[len('Bearer '):]  # and save it without Authorization prefix

        if not token:  # if the token is not received
            raise Exception('Expected JWT')  # an error occurs

        body = jwtManager.decode(token)
        if body.get('payload'):  # get the payload object from the request body
            body = body['payload']
    return body


# file saving process
def processSave(raw_body, filename, usAddr):
    body = resolve_process_save_body(raw_body)

    download = body.get('url')
    if download is None:
        raise Exception("DownloadUrl is null")
    changesUri = body.get('changesurl')
    newFilename = filename

    curExt = fileUtils.getFileExt(filename)  # get current file extension

    downloadExt = "." + body.get('filetype')  # get the extension of the downloaded file

    # convert downloaded file to the file with the current extension if these extensions aren't equal
    if curExt != downloadExt:
        try:
            # convert file and give url to a new file
            convertedData = serviceConverter.getConvertedData(download, downloadExt, curExt,
                                                              docManager.generateRevisionId(download), False)
            if not convertedData:
                newFilename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt,
                                                        usAddr)  # get the correct file name if it already exists
            else:
                download = convertedData['uri']
        except Exception:
            newFilename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, usAddr)

    path = docManager.getStoragePath(newFilename, usAddr)  # get the file path

    data = docManager.downloadFileFromUri(download)  # download document file
    if data is None:
        raise Exception("Downloaded document is null")

    histDir = historyManager.getHistoryDir(path)  # get the path to the history direction
    if not os.path.exists(histDir):  # if the path doesn't exist
        os.makedirs(histDir)  # create it

    versionDir = historyManager.getNextVersionDir(histDir)  # get the path to the next file version

    # get the path to the previous file version and rename the storage path with it
    os.rename(docManager.getStoragePath(filename, usAddr), historyManager.getPrevFilePath(versionDir, curExt))

    docManager.saveFile(data, path)  # save document file

    dataChanges = docManager.downloadFileFromUri(changesUri)  # download changes file
    if dataChanges is None:
        raise Exception("Downloaded changes is null")
    # save file changes to the diff.zip archive
    docManager.saveFile(dataChanges, historyManager.getChangesZipPath(versionDir))

    hist = None
    hist = body.get('changeshistory')
    if (not hist) & ('history' in body):
        hist = json.dumps(body.get('history'))
    if hist:
        # write the history changes to the changes.json file
        historyManager.writeFile(historyManager.getChangesHistoryPath(versionDir), hist)
    # write the key value to the key.txt file
    historyManager.writeFile(historyManager.getKeyPath(versionDir), body.get('key'))
    # get the path to the forcesaved file version
    forcesavePath = docManager.getForcesavePath(newFilename, usAddr, False)
    if forcesavePath != "":  # if the forcesaved file version exists
        os.remove(forcesavePath)  # remove it


# file force saving process
def processForceSave(body, filename, usAddr):
    download = body.get('url')
    if download is None:
        raise Exception("DownloadUrl is null")
    curExt = fileUtils.getFileExt(filename)  # get current file extension

    downloadExt = "." + body.get('filetype')  # get the extension of the downloaded file

    newFilename = False

    # convert downloaded file to the file with the current extension if these extensions aren't equal
    if curExt != downloadExt:
        try:
            # convert file and give url to a new file
            convertedData = serviceConverter.getConvertedData(download, downloadExt, curExt,
                                                              docManager.generateRevisionId(download), False)
            if not convertedData:
                newFilename = True
            else:
                download = convertedData['uri']
        except Exception:
            newFilename = True

    data = docManager.downloadFileFromUri(download)  # download document file
    if data is None:
        raise Exception("Downloaded document is null")

    isSubmitForm = body.get('forcesavetype') == 3  # SubmitForm

    if isSubmitForm:
        if newFilename:
            filename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + "-form" + downloadExt,
                                                 usAddr)  # get the correct file name if it already exists
        else:
            filename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + "-form" + curExt, usAddr)
        forcesavePath = docManager.getStoragePath(filename, usAddr)
    else:
        if newFilename:
            filename = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + downloadExt, usAddr)
        forcesavePath = docManager.getForcesavePath(filename, usAddr, False)
        if forcesavePath == "":
            forcesavePath = docManager.getForcesavePath(filename, usAddr, True)

    docManager.saveFile(data, forcesavePath)  # save document file

    if isSubmitForm:
        uid = body['actions'][0]['userid']  # get the user id
        historyManager.createMetaData(filename, uid, "Filling Form", usAddr)  # create meta data for forcesaved file

        forms_data_url = body.get('formsdataurl')
        if forms_data_url:
            data_name = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + ".txt", usAddr)
            data_path = docManager.getStoragePath(data_name, usAddr)

            forms_data = docManager.downloadFileFromUri(forms_data_url)

            if forms_data is None:
                raise Exception("Document editing service didn't return forms_data")
            else:
                with open(data_path, 'w') as file:
                    file.write(forms_data.text)
        else:
            raise Exception('Document editing service did not return forms_data_url')


# create a command request
def commandRequest(method, key, meta=None):
    payload = {
        'c': method,
        'key': key
    }

    if meta:
        payload['meta'] = meta

    headers = {'accept': 'application/json'}

    if (jwtManager.isEnabled() and jwtManager.useForRequest()):  # check if a secret key to generate token exists or not
        headerToken = jwtManager.encode({'payload': payload})  # encode a payload object into a header token
        # add a header Authorization with a header token with Authorization prefix in it
        headers[config_manager.jwt_header()] = f'Bearer {headerToken}'

        payload['token'] = jwtManager.encode(payload)  # encode a payload object into a body token
    response = requests.post(config_manager.document_server_command_url().geturl(), json=payload, headers=headers,
                             verify=config_manager.ssl_verify_peer_mode_enabled(), timeout=5)

    error = response.json()["error"]
    if error not in (4, 0):
        raise Exception(f"Command Service Error #{error}")

    return response


def resolve_process_save_body(body):
    copied = deepcopy(body)

    url = copied.get('url')
    if url is not None:
        parsed_url = urlparse(url)
        resolved_url = proxy_manager.resolve_url(parsed_url)
        copied['url'] = resolved_url.geturl()

    changes_url = copied.get('changesurl')
    if changes_url is not None:
        parsed_url = urlparse(changes_url)
        resolved_url = proxy_manager.resolve_url(parsed_url)
        copied['changesurl'] = resolved_url.geturl()

    return copied
