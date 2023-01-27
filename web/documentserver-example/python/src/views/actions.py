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
import requests

import config
import json
import os
import urllib.parse
import magic

from datetime import datetime
from django.http import HttpResponse, HttpResponseRedirect, FileResponse
from django.shortcuts import render
from src.utils import docManager, fileUtils, serviceConverter, users, jwtManager, historyManager, trackManager


# upload a file from the document storage service to the document editing service
def upload(request):
    response = {}

    try:
        fileInfo = request.FILES['uploadedFile']
        if ((fileInfo.size > config.FILE_SIZE_MAX) | (fileInfo.size <= 0)):  # check if the file size exceeds the maximum size allowed (5242880)
            raise Exception('File size is incorrect')

        curExt = fileUtils.getFileExt(fileInfo.name)
        if not docManager.isSupportedExt(curExt):  # check if the file extension is supported by the document manager
            raise Exception('File type is not supported')

        name = docManager.getCorrectName(fileInfo.name, request)  # get file name with an index if such a file name already exists
        path = docManager.getStoragePath(name, request)

        docManager.createFile(fileInfo.file, path, request, True)  # create file with meta information in the storage directory

        response.setdefault('filename', name)
        response.setdefault('documentType', fileUtils.getFileType(name))

    except Exception as e:  # if an error occurs
        response.setdefault('error', e.args[0])  # save an error message to the response variable

    return HttpResponse(json.dumps(response), content_type='application/json')  # return http response in json format

# convert a file from one format to another
def convert(request):
    response = {}

    try:
        body = json.loads(request.body)
        filename = fileUtils.getFileName(body.get("filename"))
        filePass = body.get("filePass")
        lang = request.COOKIES.get('ulang') if request.COOKIES.get('ulang') else 'en'
        fileUri = docManager.getDownloadUrl(filename,request)
        fileExt = fileUtils.getFileExt(filename)
        fileType = fileUtils.getFileType(filename)
        newExt = docManager.getInternalExtension(fileType)  # internal editor extensions: .docx, .xlsx or .pptx

        if docManager.isCanConvert(fileExt):  # check if the file extension is available for converting
            key = docManager.generateFileKey(filename, request)  # generate the file key

            newUri = serviceConverter.getConverterUri(fileUri, fileExt, newExt, key, True, filePass, lang)  # get the url of the converted file

            if not newUri:  # if the converter url is not received, the original file name is passed to the response
                response.setdefault('step', '0')
                response.setdefault('filename', filename)
            else:
                correctName = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + newExt, request)  # otherwise, create a new name with the necessary extension
                path = docManager.getStoragePath(correctName, request)
                docManager.saveFileFromUri(newUri, path, request, True)  # save the file from the new url in the storage directory
                docManager.removeFile(filename, request)  # remove the original file
                response.setdefault('filename', correctName)  # pass the name of the converted file to the response
        else:
            response.setdefault('filename', filename)  # if the file can't be converted, the original file name is passed to the response

    except Exception as e:
        response.setdefault('error', e.args[0])

    return HttpResponse(json.dumps(response), content_type='application/json')

# create a new file
def createNew(request):
    response = {}

    try:
        fileType = request.GET['fileType']
        sample = request.GET.get('sample', False)

        filename = docManager.createSample(fileType, sample, request)  # create a new sample file of the necessary type

        return HttpResponseRedirect(f'edit?filename={filename}')  # return http response with redirection url

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
        resp = requests.get(saveAsFileUrl, verify = config.DOC_SERV_VERIFY_PEER)

        if ((len(resp.content) > config.FILE_SIZE_MAX) | (len(resp.content) <= 0)):  # check if the file size exceeds the maximum size allowed (5242880)
            response.setdefault('error', 'File size is incorrect')
            raise Exception('File size is incorrect')

        curExt = fileUtils.getFileExt(filename)
        if not docManager.isSupportedExt(curExt):  # check if the file extension is supported by the document manager
            response.setdefault('error', 'File type is not supported')
            raise Exception('File type is not supported')

        docManager.saveFileFromUri(saveAsFileUrl, path, request, True)  # save the file from the new url in the storage directory

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

    response.setdefault('result', trackManager.commandRequest('meta', dockey, meta).json())

    return HttpResponse(json.dumps(response), content_type='application/json')

# edit a file
def edit(request):
    filename = fileUtils.getFileName(request.GET['filename'])
    isEnableDirectUrl = request.GET['directUrl'].lower() in ("true")  if 'directUrl' in request.GET else False

    ext = fileUtils.getFileExt(filename)

    fileUri = docManager.getFileUri(filename, True, request)
    fileUriUser = docManager.getDownloadUrl(filename, request) + "&dmode=emb" if os.path.isabs(config.STORAGE_PATH) else docManager.getFileUri(filename, False, request)
    directUrl = docManager.getDownloadUrl(filename, request, False)
    docKey = docManager.generateFileKey(filename, request)
    fileType = fileUtils.getFileType(filename)
    user = users.getUserFromReq(request)  # get user

    edMode = request.GET.get('mode') if request.GET.get('mode') else 'edit'  # get the editor mode: view/edit/review/comment/fillForms/embedded (the default mode is edit)
    canEdit = docManager.isCanEdit(ext)  # check if the file with this extension can be edited

    if (((not canEdit) and edMode == 'edit') or edMode == 'fillForms') and docManager.isCanFillForms(ext) :
        edMode = 'fillForms'
        canEdit = True
    submitForm = edMode == 'fillForms' and user.id == 'uid-1' and False  # if the Submit form button is displayed or hidden
    mode = 'edit' if canEdit & (edMode != 'view') else 'view'  # if the file can't be edited, the mode is view

    types = ['desktop', 'mobile', 'embedded']
    edType = request.GET.get('type') if request.GET.get('type') in types else 'desktop'  # get the editor type: embedded/mobile/desktop (the default type is desktop)
    lang = request.COOKIES.get('ulang') if request.COOKIES.get('ulang') else 'en'  # get the editor language (the default language is English)

    storagePath = docManager.getStoragePath(filename, request)
    meta = historyManager.getMeta(storagePath)  # get the document meta data
    infObj = None

    actionData = request.GET.get('actionLink')  # get the action data that will be scrolled to (comment or bookmark)
    actionLink = json.loads(actionData) if actionData else None

    templatesImageUrl = docManager.getTemplateImageUrl(fileType, request) # templates image url in the "From Template" section
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
                'chat': user.id !='uid-0',
                'reviewGroups': user.reviewGroups,
                'commentGroups': user.commentGroups,
                'userInfoGroups': user.userInfoGroups
            }
        },
        'editorConfig': {
            'actionLink': actionLink,
            'mode': mode,
            'lang': lang,
            'callbackUrl': docManager.getCallbackUrl(filename, request),  # absolute URL to the document storage service
            'coEditing': {
                            "mode": "strict", 
                            "change": False
                         } 
                         if edMode == 'view' and user.id =='uid-0' else None,
            'createUrl' : createUrl if user.id !='uid-0' else None,
            'templates' : templates if user.templates else None,
            'user': {  # the user currently viewing or editing the document
                'id': user.id if user.id !='uid-0' else None,
                'name': user.name,
                'group': user.group
            },
            'embedded': {  # the parameters for the embedded document type
                'saveUrl': directUrl,  # the absolute URL that will allow the document to be saved onto the user personal computer
                'embedUrl': directUrl,  # the absolute URL to the document serving as a source file for the document embedded into the web page
                'shareUrl': directUrl,  # the absolute URL that will allow other users to share this document
                'toolbarDocked': 'top'  # the place for the embedded viewer toolbar (top or bottom)
            },
            'customization': {  # the parameters for the editor interface
                'about': True,  # the About section display
                'comments': True,  
                'feedback': True,  # the Feedback & Support menu button display
                'forcesave': False,  # adds the request for the forced file saving to the callback handler
                'submitForm': submitForm,  # if the Submit form button is displayed or not
                'goback': {  # settings for the Open file location menu button and upper right corner button 
                    'url': docManager.getServerUrl(False, request)  # the absolute URL to the website address which will be opened when clicking the Open file location menu button
                }
            }
        }
    }

    # an image which will be inserted into the document
    dataInsertImage = {
        'fileType': 'png',
        'url': docManager.getServerUrl(True, request) + 'static/images/logo.png',
        'directUrl': docManager.getServerUrl(False, request) + 'static/images/logo.png'
    } if isEnableDirectUrl else {
        'fileType': 'png',
        'url': docManager.getServerUrl(True, request) + 'static/images/logo.png'
    }

    # a document which will be compared with the current document
    dataCompareFile = {
        'fileType': 'docx',
        'url': docManager.getServerUrl(True, request) + 'static/sample.docx',
        'directUrl': docManager.getServerUrl(False, request) + 'static/sample.docx'
    } if isEnableDirectUrl else {
        'fileType': 'docx',
        'url': docManager.getServerUrl(True, request) + 'static/sample.docx'
    }

    # recipient data for mail merging
    dataMailMergeRecipients = {
        'fileType': 'csv',
        'url': docManager.getServerUrl(True, request) + 'csv',
        'directUrl': docManager.getServerUrl(False, request) + 'csv'
    } if isEnableDirectUrl else {
        'fileType': 'csv',
        'url': docManager.getServerUrl(True, request) + 'csv'
    }

    # users data for mentions
    usersForMentions = users.getUsersForMentions(user.id) 

    if jwtManager.isEnabled():  # if the secret key to generate token exists
        edConfig['token'] = jwtManager.encode(edConfig)  # encode the edConfig object into a token
        dataInsertImage['token'] = jwtManager.encode(dataInsertImage)  # encode the dataInsertImage object into a token
        dataCompareFile['token'] = jwtManager.encode(dataCompareFile)  # encode the dataCompareFile object into a token
        dataMailMergeRecipients['token'] = jwtManager.encode(dataMailMergeRecipients)  # encode the dataMailMergeRecipients object into a token

    hist = historyManager.getHistoryObject(storagePath, filename, docKey, fileUri, isEnableDirectUrl, request)  # get the document history

    context = {  # the data that will be passed to the template
        'cfg': json.dumps(edConfig),  # the document config in json format
        'history': json.dumps(hist['history']) if 'history' in hist else None,  # the information about the current version
        'historyData': json.dumps(hist['historyData']) if 'historyData' in hist else None,  # the information about the previous document versions if they exist
        'fileType': fileType,  # the file type of the document (text, spreadsheet or presentation)
        'apiUrl': config.DOC_SERV_SITE_URL + config.DOC_SERV_API_URL,  # the absolute URL to the api
        'dataInsertImage': json.dumps(dataInsertImage)[1 : len(json.dumps(dataInsertImage)) - 1],  # the image which will be inserted into the document
        'dataCompareFile': dataCompareFile,  # document which will be compared with the current document
        'dataMailMergeRecipients': json.dumps(dataMailMergeRecipients),  # recipient data for mail merging
        'usersForMentions': json.dumps(usersForMentions) if user.id !='uid-0' else None
    }
    return render(request, 'editor.html', context)  # execute the "editor.html" template with context data

# track the document changes
def track(request):
    response = {}

    try:
        body = trackManager.readBody(request)  # read request body
        status = body['status']  # and get status from it

        if (status == 1): # editing
            if (body['actions'] and body['actions'][0]['type'] == 0):  # finished edit
                user = body['actions'][0]['userid']  # the user who finished editing
                if (not user in body['users']):
                    trackManager.commandRequest('forcesave', body['key'])  # create a command request with the forcasave method

        filename = fileUtils.getFileName(request.GET['filename'])
        usAddr = request.GET['userAddress']

        if (status == 2) | (status == 3):  # mustsave, corrupted
            trackManager.processSave(body, filename, usAddr)
        if (status == 6) | (status == 7):  # mustforcesave, corruptedforcesave
            trackManager.processForceSave(body, filename, usAddr)

    except Exception as e:
        response.setdefault("error", 1)  # set the default error value as 1 (document key is missing or no document with such key could be found)
        response.setdefault("message", str(e.args[0]))

    response.setdefault('error', 0)  # if no exceptions are raised, the default error value is 0 (no errors)
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
    filePath = os.path.join('assets', 'sample', "csv.csv")
    response = docManager.download(filePath)
    return response

# download a file
def download(request):
    try:
        fileName = fileUtils.getFileName(request.GET['fileName'])  # get the file name
        userAddress = request.GET.get('userAddress') if request.GET.get('userAddress') else request
        isEmbedded = request.GET.get('dmode')

        if (jwtManager.isEnabled() and isEmbedded == None):
            jwtHeader = 'Authorization' if config.DOC_SERV_JWT_HEADER is None or config.DOC_SERV_JWT_HEADER == '' else config.DOC_SERV_JWT_HEADER
            token = request.headers.get(jwtHeader)
            if token:
                token = token[len('Bearer '):]
                try:
                    body = jwtManager.decode(token)
                except Exception:
                    return HttpResponse('JWT validation failed', status=403)

        filePath = docManager.getForcesavePath(fileName, userAddress, False)  # get the path to the forcesaved file version
        if (filePath == ""):
            filePath = docManager.getStoragePath(fileName, userAddress)  # get file from the storage directory
        response = docManager.download(filePath)  # download this file
        return response
    except Exception:
        response = {}
        response.setdefault('error', 'File not found')
        return HttpResponse(json.dumps(response), content_type='application/json')

# download a history file
def downloadhistory(request):
    try:
        fileName = fileUtils.getFileName(request.GET['fileName'])  # get the file name
        userAddress = request.GET.get('userAddress') if request.GET.get('userAddress') else request
        file = fileUtils.getFileName(request.GET['file'])
        version = fileUtils.getFileName(request.GET['ver'])
        isEmbedded = request.GET.get('dmode')

        if (jwtManager.isEnabled() and isEmbedded == None):
            jwtHeader = 'Authorization' if config.DOC_SERV_JWT_HEADER is None or config.DOC_SERV_JWT_HEADER == '' else config.DOC_SERV_JWT_HEADER
            token = request.headers.get(jwtHeader)
            if token:
                token = token[len('Bearer '):]
                try:
                    body = jwtManager.decode(token)
                except Exception:
                    return HttpResponse('JWT validation failed', status=403)
            else:
                return HttpResponse('JWT validation failed', status=403)

        filePath = docManager.getHistoryPath(fileName, file, version, userAddress)

        response = docManager.download(filePath)  # download this file
        return response
    except Exception:
        response = {}
        response.setdefault('error', 'File not found')
        return HttpResponse(json.dumps(response), content_type='application/json', status=404)
