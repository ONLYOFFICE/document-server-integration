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

from datetime import datetime
from http import HTTPStatus
import json
import os
from pathlib import Path
from shutil import copy
from django.http import HttpRequest, HttpResponse, HttpResponseRedirect
from django.shortcuts import render
import requests
from src.common import http
from src.configuration import ConfigurationManager
from src.response import ErrorResponse
from src.utils import (docManager, fileUtils, serviceConverter, users,
                       jwtManager, historyManager, trackManager)

config_manager = ConfigurationManager()


# upload a file from the document storage service to the document editing service
def upload(request):
    response = {}

    try:
        fileInfo = request.FILES['uploadedFile']
        # check if the file size exceeds the maximum size allowed (5242880)
        if ((fileInfo.size > config_manager.maximum_file_size()) | (fileInfo.size <= 0)):
            raise Exception('File size is incorrect')

        curExt = fileUtils.getFileExt(fileInfo.name)
        # check if the file extension is supported by the document manager
        if not docManager.isSupportedExt(curExt):
            raise Exception('File type is not supported')

        # get file name with an index if such a file name already exists
        name = docManager.getCorrectName(fileInfo.name, request)
        path = docManager.getStoragePath(name, request)

        # create file with meta information in the storage directory
        docManager.createFile(fileInfo.file, path, request, True)

        response.setdefault('filename', name)
        response.setdefault('documentType', fileUtils.getFileType(name))

    except Exception as e:  # if an error occurs
        # save an error message to the response variable
        response.setdefault('error', e.args[0])

    # return http response in json format
    return HttpResponse(json.dumps(response), content_type='application/json')


# convert a file from one format to another
def convert(request):
    response = {}

    try:
        body = json.loads(request.body)
        filename = fileUtils.getFileName(body.get("filename"))
        filePass = body.get("filePass")
        lang = request.COOKIES.get(
            'ulang') if request.COOKIES.get('ulang') else 'en'
        fileUri = docManager.getDownloadUrl(filename, request)
        fileExt = fileUtils.getFileExt(filename)
        newExt = 'ooxml'  # convert to .ooxml

        # check if the file extension is available for converting
        if docManager.isCanConvert(fileExt):
            key = docManager.generateFileKey(
                filename, request)  # generate the file key

            # get the url of the converted file
            convertedData = serviceConverter.getConvertedData(
                fileUri, fileExt, newExt, key, True, filePass, lang)

            # if the converter url is not received,
            # the original file name is passed to the response
            if not convertedData:
                response.setdefault('step', '0')
                response.setdefault('filename', filename)
            else:
                # otherwise, create a new name with the necessary extension
                correctName = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(
                    filename) + '.' + convertedData['fileType'], request)
                path = docManager.getStoragePath(correctName, request)

                # save the file from the new url in the storage directory
                docManager.downloadFileFromUri(
                    convertedData['uri'], path, True)
                # remove the original file
                docManager.removeFile(filename, request)
                # pass the name of the converted file to the response
                response.setdefault('filename', correctName)
        else:
            # if the file can't be converted, the original file name is passed to the response
            response.setdefault('filename', filename)

    except Exception as e:
        response.setdefault('error', e.args[0])

    return HttpResponse(json.dumps(response), content_type='application/json')


# create a new file
def createNew(request):
    response = {}

    try:
        fileType = request.GET['fileType']
        sample = request.GET.get('sample', False)

        # create a new sample file of the necessary type
        filename = docManager.createSample(fileType, sample, request)

        # return http response with redirection url
        return HttpResponseRedirect(f'edit?filename={filename}')

    except Exception as e:
        response.setdefault('error', e.args[0])

    return HttpResponse(json.dumps(response), content_type='application/json')


# save file as...
def saveAs(request):
    response = {}

    try:
        body = json.loads(request.body)
        saveAsFileUrl = body.get('url')
        title = body.get('title')

        filename = docManager.getCorrectName(title, request)
        path = docManager.getStoragePath(filename, request)
        resp = requests.get(
            saveAsFileUrl, verify=config_manager.ssl_verify_peer_mode_enabled())

        # check if the file size exceeds the maximum size allowed (5242880)
        if ((len(resp.content) > config_manager.maximum_file_size()) | (len(resp.content) <= 0)):
            response.setdefault('error', 'File size is incorrect')
            raise Exception('File size is incorrect')

        curExt = fileUtils.getFileExt(filename)
        # check if the file extension is supported by the document manager
        if not docManager.isSupportedExt(curExt):
            response.setdefault('error', 'File type is not supported')
            raise Exception('File type is not supported')

        # save the file from the new url in the storage directory
        docManager.downloadFileFromUri(saveAsFileUrl, path, True)

        response.setdefault('file', filename)
    except Exception as e:
        response.setdefault('error', 1)
        response.setdefault('message', e.args[0])

    return HttpResponse(json.dumps(response), content_type='application/json')


# rename file
def rename(request):
    response = {}

    body = json.loads(request.body)
    newfilename = body['newfilename']

    origExt = '.' + body['ext']
    curExt = fileUtils.getFileExt(newfilename)
    if (origExt != curExt):
        newfilename += origExt

    dockey = body['dockey']
    meta = {'title': newfilename}

    trackManager.commandRequest('meta', dockey, meta)

    response.setdefault('result', trackManager.commandRequest(
        'meta', dockey, meta).json())

    return HttpResponse(json.dumps(response), content_type='application/json')


# edit a file
def edit(request):
    filename = fileUtils.getFileName(request.GET['filename'])
    isEnableDirectUrl = request.GET['directUrl'].lower() in (
        "true") if 'directUrl' in request.GET else False

    ext = fileUtils.getFileExt(filename)

    fileUri = docManager.getFileUri(filename, True, request)
    fileUriUser = docManager.getDownloadUrl(filename, request) + "&dmode=emb"
    directUrl = docManager.getDownloadUrl(filename, request, False)
    docKey = docManager.generateFileKey(filename, request)
    fileType = fileUtils.getFileType(filename)
    user = users.getUserFromReq(request)  # get user

    # get the editor mode: view/edit/review/comment/fillForms/embedded (the default mode is edit)
    edMode = request.GET.get('mode') if request.GET.get('mode') else 'edit'
    # check if the file with this extension can be edited
    canEdit = docManager.isCanEdit(ext)

    if (((not canEdit) and edMode == 'edit') or edMode == 'fillForms') and docManager.isCanFillForms(ext):
        edMode = 'fillForms'
        canEdit = True
    # if the Submit form button is displayed or hidden
    submitForm = edMode == 'fillForms' and user.id == 'uid-1' and False
    # if the file can't be edited, the mode is view
    mode = 'edit' if canEdit & (edMode != 'view') else 'view'

    types = ['desktop', 'mobile', 'embedded']
    # get the editor type: embedded/mobile/desktop (the default type is desktop)
    edType = request.GET.get('type') if request.GET.get(
        'type') in types else 'desktop'
    # get the editor language (the default language is English)
    lang = request.COOKIES.get(
        'ulang') if request.COOKIES.get('ulang') else 'en'

    storagePath = docManager.getStoragePath(filename, request)
    meta = historyManager.getMeta(storagePath)  # get the document meta data
    infObj = None

    # get the action data that will be scrolled to (comment or bookmark)
    actionData = request.GET.get('actionLink')
    actionLink = json.loads(actionData) if actionData else None

    # templates image url in the "From Template" section
    templatesImageUrl = docManager.getTemplateImageUrl(fileType, request)
    createUrl = docManager.getCreateUrl(edType, request)
    templates = [
        {
            'image': '',
            'title': 'Blank',
            'url': createUrl
        },
        {
            'image': templatesImageUrl,
            'title': 'With sample content',
            'url': createUrl + '&sample=true'
        }
    ]

    if (meta):  # if the document meta data exists,
        infObj = {  # write author and creation time parameters to the information object
            'owner': meta['uname'],
            'uploaded': meta['created']
        }
    else:  # otherwise, write current meta information to this object
        infObj = {
            'owner': 'Me',
            'uploaded': datetime.today().strftime('%d.%m.%Y %H:%M:%S')
        }
    infObj['favorite'] = user.favorite
    # specify the document config
    edConfig = {
        'type': edType,
        'documentType': fileType,
        'document': {
            'title': filename,
            'url': docManager.getDownloadUrl(filename, request),
            'directUrl': directUrl if isEnableDirectUrl else "",
            'fileType': ext[1:],
            'key': docKey,
            'info': infObj,
            'permissions': {  # the permission for the document to be edited and downloaded or not
                'comment': (edMode != 'view') & (edMode != 'fillForms') & (edMode != 'embedded') & (edMode != "blockcontent"),
                'copy': 'copy' not in user.deniedPermissions,
                'download': 'download' not in user.deniedPermissions,
                'edit': canEdit & ((edMode == 'edit') | (edMode == 'view') | (edMode == 'filter') | (edMode == "blockcontent")),
                'print': 'print' not in user.deniedPermissions,
                'fillForms': (edMode != 'view') & (edMode != 'comment') & (edMode != 'embedded') & (edMode != "blockcontent"),
                'modifyFilter': edMode != 'filter',
                'modifyContentControl': edMode != "blockcontent",
                'review': canEdit & ((edMode == 'edit') | (edMode == 'review')),
                'chat': user.id != 'uid-0',
                'reviewGroups': user.reviewGroups,
                'commentGroups': user.commentGroups,
                'userInfoGroups': user.userInfoGroups,
                'protect': 'protect' not in user.deniedPermissions
            },
            'referenceData': {
                'instanceId': docManager.getServerUrl(False, request),
                'fileKey': json.dumps({'fileName': filename, 'userAddress': request.META['REMOTE_ADDR']}) if user.id != 'uid-0' else None
            }
        },
        'editorConfig': {
            'actionLink': actionLink,
            'mode': mode,
            'lang': lang,
            # absolute URL to the document storage service
            'callbackUrl': docManager.getCallbackUrl(filename, request),
            'coEditing': {
                "mode": "strict",
                "change": False
            }
            if edMode == 'view' and user.id == 'uid-0' else None,
            'createUrl': createUrl if user.id != 'uid-0' else None,
            'templates': templates if user.templates else None,
            'user': {  # the user currently viewing or editing the document
                'id': user.id if user.id != 'uid-0' else None,
                'name': user.name,
                'group': user.group
            },
            'embedded': {  # the parameters for the embedded document type
                # the absolute URL that will allow the document to be saved onto the user personal computer
                'saveUrl': directUrl,
                # the absolute URL to the document serving as a source file for the document embedded into the web page
                'embedUrl': directUrl,
                # the absolute URL that will allow other users to share this document
                'shareUrl': directUrl,
                # the place for the embedded viewer toolbar (top or bottom)
                'toolbarDocked': 'top'
            },
            'customization': {  # the parameters for the editor interface
                'about': True,  # the About section display
                'comments': True,
                'feedback': True,  # the Feedback & Support menu button display
                'forcesave': False,  # adds the request for the forced file saving to the callback handler
                'submitForm': submitForm,  # if the Submit form button is displayed or not
                'goback': {  # settings for the Open file location menu button and upper right corner button
                    # the absolute URL to the website address which will be opened when clicking the Open file location menu button
                    'url': docManager.getServerUrl(False, request)
                }
            }
        }
    }

    # an image which will be inserted into the document
    dataInsertImage = {
        'fileType': 'png',
        'url': docManager.getServerUrl(True, request) + '/static/images/logo.png',
        'directUrl': docManager.getServerUrl(False, request) + '/static/images/logo.png'
    } if isEnableDirectUrl else {
        'fileType': 'png',
        'url': docManager.getServerUrl(True, request) + '/static/images/logo.png'
    }

    # a document which will be compared with the current document
    dataCompareFile = {
        'fileType': 'docx',
        'url': docManager.getServerUrl(True, request) + '/static/sample.docx',
        'directUrl': docManager.getServerUrl(False, request) + '/static/sample.docx'
    } if isEnableDirectUrl else {
        'fileType': 'docx',
        'url': docManager.getServerUrl(True, request) + '/static/sample.docx'
    }

    # recipient data for mail merging
    dataMailMergeRecipients = {
        'fileType': 'csv',
        'url': docManager.getServerUrl(True, request) + '/csv',
        'directUrl': docManager.getServerUrl(False, request) + '/csv'
    } if isEnableDirectUrl else {
        'fileType': 'csv',
        'url': docManager.getServerUrl(True, request) + '/csv'
    }

    # users data for mentions
    usersForMentions = users.getUsersForMentions(user.id)

    if jwtManager.isEnabled():  # if the secret key to generate token exists
        # encode the edConfig object into a token
        edConfig['token'] = jwtManager.encode(edConfig)
        # encode the dataInsertImage object into a token
        dataInsertImage['token'] = jwtManager.encode(dataInsertImage)
        # encode the dataCompareFile object into a token
        dataCompareFile['token'] = jwtManager.encode(dataCompareFile)
        # encode the dataMailMergeRecipients object into a token
        dataMailMergeRecipients['token'] = jwtManager.encode(
            dataMailMergeRecipients)

    hist = historyManager.getHistoryObject(
        # get the document history
        storagePath, filename, docKey, fileUri, isEnableDirectUrl, request)

    context = {  # the data that will be passed to the template
        'cfg': json.dumps(edConfig),  # the document config in json format
        # the information about the current version
        'history': json.dumps(hist['history']) if 'history' in hist else None,
        # the information about the previous document versions if they exist
        'historyData': json.dumps(hist['historyData']) if 'historyData' in hist else None,
        # the file type of the document (text, spreadsheet or presentation)
        'fileType': fileType,
        # the absolute URL to the api
        'apiUrl': config_manager.document_server_api_url().geturl(),
        # the image which will be inserted into the document
        'dataInsertImage': json.dumps(dataInsertImage)[1: len(json.dumps(dataInsertImage)) - 1],
        # document which will be compared with the current document
        'dataCompareFile': dataCompareFile,
        # recipient data for mail merging
        'dataMailMergeRecipients': json.dumps(dataMailMergeRecipients),
        'usersForMentions': json.dumps(usersForMentions) if user.id != 'uid-0' else None
    }
    # execute the "editor.html" template with context data
    return render(request, 'editor.html', context)


# track the document changes
def track(request):
    response = {}

    try:
        body = trackManager.readBody(request)  # read request body
        status = body['status']  # and get status from it

        if (status == 1):  # editing
            if (body['actions'] and body['actions'][0]['type'] == 0):  # finished edit
                # the user who finished editing
                user = body['actions'][0]['userid']
                if (not user in body['users']):
                    # create a command request with the forcasave method
                    trackManager.commandRequest('forcesave', body['key'])

        filename = fileUtils.getFileName(request.GET['filename'])
        usAddr = request.GET['userAddress']

        if (status == 2) | (status == 3):  # mustsave, corrupted
            trackManager.processSave(body, filename, usAddr)
        if (status == 6) | (status == 7):  # mustforcesave, corruptedforcesave
            trackManager.processForceSave(body, filename, usAddr)

    except Exception as e:
        # set the default error value as 1 (document key is missing or no document with such key could be found)
        response.setdefault("error", 1)
        response.setdefault("message", str(e.args[0]))

    # if no exceptions are raised, the default error value is 0 (no errors)
    response.setdefault('error', 0)
    # the response status is 200 if the changes are saved successfully; otherwise, it is equal to 500
    return HttpResponse(json.dumps(response), content_type='application/json', status=200 if response['error'] == 0 else 500)


# remove a file
def remove(request):
    filename = fileUtils.getFileName(request.GET['filename'])

    response = {}

    docManager.removeFile(filename, request)

    response.setdefault('success', True)
    return HttpResponse(json.dumps(response), content_type='application/json')


# get file information
def files(request):
    try:
        response = docManager.getFilesInfo(request)
    except Exception as e:
        response = {}
        response.setdefault('error', e.args[0])
    return HttpResponse(json.dumps(response), content_type='application/json')


# download a csv file
def csv(request):
    filePath = os.path.join(
        'assets', 'document-templates', 'sample', "csv.csv")
    response = docManager.download(filePath)
    return response


# download a file
def download(request):
    try:
        fileName = fileUtils.getFileName(
            request.GET['fileName'])  # get the file name
        userAddress = request.GET.get('userAddress')
        isEmbedded = request.GET.get('dmode')

        if (jwtManager.isEnabled() and isEmbedded == None and userAddress and jwtManager.useForRequest()):
            token = request.headers.get(config_manager.jwt_header())
            if token:
                token = token[len('Bearer '):]

            try:
                body = jwtManager.decode(token)
            except Exception:
                return HttpResponse('JWT validation failed', status=403)

        if (userAddress == None):
            userAddress = request

        # get the path to the forcesaved file version
        filePath = docManager.getForcesavePath(fileName, userAddress, False)
        if (filePath == ""):
            # get file from the storage directory
            filePath = docManager.getStoragePath(fileName, userAddress)
        response = docManager.download(filePath)  # download this file
        return response
    except Exception:
        response = {}
        response.setdefault('error', 'File not found')
        return HttpResponse(json.dumps(response), content_type='application/json')


# download a history file
def downloadhistory(request):
    try:
        fileName = fileUtils.getFileName(
            request.GET['fileName'])  # get the file name
        userAddress = request.GET.get('userAddress') if request.GET.get(
            'userAddress') else request
        file = fileUtils.getFileName(request.GET['file'])
        version = fileUtils.getFileName(request.GET['ver'])
        isEmbedded = request.GET.get('dmode')

        if (jwtManager.isEnabled() and isEmbedded == None and jwtManager.useForRequest()):
            token = request.headers.get(config_manager.jwt_header())
            if token:
                token = token[len('Bearer '):]
                try:
                    body = jwtManager.decode(token)
                except Exception:
                    return HttpResponse('JWT validation failed', status=403)
            else:
                return HttpResponse('JWT validation failed', status=403)

        filePath = docManager.getHistoryPath(
            fileName, file, version, userAddress)

        response = docManager.download(filePath)  # download this file
        return response
    except Exception:
        response = {}
        response.setdefault('error', 'File not found')
        return HttpResponse(json.dumps(response), content_type='application/json', status=404)


# referenceData
def reference(request):
    response = {}
    body = json.loads(request.body)
    referenceData = None
    fileName = None
    userAddress = None

    try:
        referenceData = body['referenceData']
    except Exception:
        pass

    if referenceData is not None:
        instanceId = referenceData['instanceId']
        if instanceId == docManager.getServerUrl(False, request):
            fileKey = json.loads(referenceData['fileKey'])
            userAddress = fileKey['userAddress']
            if userAddress == request.META['REMOTE_ADDR']:
                fileName = fileKey['fileName']

    if fileName is None:
        try:
            path = fileUtils.getFileName(body['path'])
            if os.path.exists(docManager.getStoragePath(path, request)):
                fileName = path
        except KeyError:
            response.setdefault('error', 'Path not found')
            return HttpResponse(json.dumps(response), content_type='application/json', status=404)

    if fileName is None:
        response.setdefault('error', 'File not found')
        return HttpResponse(json.dumps(response), content_type='application/json', status=404)

    data = {
        'fileType': fileUtils.getFileExt(fileName).replace('.', ''),
        'url': docManager.getDownloadUrl(fileName, request),
        'directUrl': docManager.getDownloadUrl(fileName, request, False) if body["directUrl"] else None,
        'referenceData': {
            'instanceId': docManager.getServerUrl(False, request),
            'fileKey': json.dumps({'fileName': fileName, 'userAddress': request.META['REMOTE_ADDR']})
        },
        'path': fileName
    }

    if (jwtManager.isEnabled()):
        data['token'] = jwtManager.encode(data)

    return HttpResponse(json.dumps(data), content_type='application/json')


@http.PUT()
def restore(request: HttpRequest) -> HttpResponse:
    try:
        body = json.loads(request.body)
        source_basename: str = body['fileName']
        version: int = body['version']
        user_id: str = body.get('userId')
        source_extension = Path(source_basename).suffix

        user = users.find_user(user_id)

        source_file = docManager.getStoragePath(source_basename, request)
        history_directory = historyManager.getHistoryDir(source_file)

        recovery_version_directory = historyManager.getVersionDir(
            history_directory, version)
        recovery_file = historyManager.getPrevFilePath(
            recovery_version_directory, source_extension)

        bumped_version_directory = historyManager.getNextVersionDir(
            history_directory)
        bumped_key = docManager.generateFileKey(source_basename, request)
        bumped_key_file = historyManager.getKeyPath(bumped_version_directory)
        bumped_changes_file = historyManager.getChangesHistoryPath(
            bumped_version_directory)
        bumped_changes = {
            'serverVersion': None,
            'changes': [
                {
                    'created': datetime.today().strftime('%Y-%m-%d %H:%M:%S'),
                    'user': {
                        'id': user.id,
                        'name': user.name
                    }
                }
            ]
        }
        bumped_changes_content = json.dumps(bumped_changes)
        bumped_file = historyManager.getPrevFilePath(
            bumped_version_directory, source_extension)

        Path(bumped_key_file).write_text(bumped_key, 'utf-8')
        Path(bumped_changes_file).write_text(bumped_changes_content, 'utf-8')
        copy(source_file, bumped_file)
        copy(recovery_file, source_file)

        return HttpResponse()
    except Exception as error:
        return ErrorResponse(
            message=f'{type(error)}: {error}',
            status=HTTPStatus.INTERNAL_SERVER_ERROR
        )
