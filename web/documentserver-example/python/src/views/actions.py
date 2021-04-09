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

        if fileInfo.size > config.FILE_SIZE_MAX:  # check if the file size exceeds the maximum size allowed (5242880)
            raise Exception('File size is too big')

        curExt = fileUtils.getFileExt(fileInfo.name)
        if not docManager.isSupportedExt(curExt):  # check if the file extension is supported by the document manager
            raise Exception('File type is not supported')

        name = docManager.getCorrectName(fileInfo.name, request)  # get file name with an index if such a file name already exists
        path = docManager.getStoragePath(name, request)

        docManager.createFile(fileInfo.file, path, request, True)  # create file with meta information in the storage directory

        response.setdefault('filename', name)

    except Exception as e:  # if an error occurs
        response.setdefault('error', e.args[0])  # save an error message to the response variable

    return HttpResponse(json.dumps(response), content_type='application/json')  # return http response in json format

# convert a file from one format to another
def convert(request):
    response = {}

    try:
        filename = fileUtils.getFileName(request.GET['filename'])
        fileUri = docManager.getFileUri(filename, True,request)
        fileExt = fileUtils.getFileExt(filename)
        fileType = fileUtils.getFileType(filename)
        newExt = docManager.getInternalExtension(fileType)  # internal editor extensions: .docx, .xlsx or .pptx

        if docManager.isCanConvert(fileExt):  # check if the file extension is available for converting
            key = docManager.generateFileKey(filename, request)  # generate the file key

            newUri = serviceConverter.getConverterUri(fileUri, fileExt, newExt, key, True)  # get the url of the converted file

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

# edit a file
def edit(request):
    filename = fileUtils.getFileName(request.GET['filename'])

    ext = fileUtils.getFileExt(filename)

    fileUri = docManager.getFileUri(filename, True, request)
    fileUriUser = docManager.getFileUri(filename, False, request)
    docKey = docManager.generateFileKey(filename, request)
    fileType = fileUtils.getFileType(filename)
    user = users.getUserFromReq(request)  # get user id and name
    userGroup = None
    reviewGroups = None
    if (user['uid'] == 'uid-2'):
        userGroup = 'group-2'
        reviewGroups = ['group-2', '']
    if (user['uid'] == 'uid-3'):
        userGroup = 'group-3'
        reviewGroups = ['group-2']

    edMode = request.GET.get('mode') if request.GET.get('mode') else 'edit'  # get the editor mode: view/edit/review/comment/fillForms/embedded (the default mode is edit)
    canEdit = docManager.isCanEdit(ext)  # check if the file with this extension can be edited
    submitForm = canEdit & ((edMode == 'edit') | (edMode == 'fillForms'))  # if the Submit form button is displayed or hidden
    mode = 'edit' if canEdit & (edMode != 'view') else 'view'  # if the file can't be edited, the mode is view

    edType = request.GET.get('type') if request.GET.get('type') else 'desktop'  # get the editor type: embedded/mobile/desktop (the default type is desktop)
    lang = request.COOKIES.get('ulang') if request.COOKIES.get('ulang') else 'en'  # get the editor language (the default language is English)

    storagePath = docManager.getStoragePath(filename, request)
    meta = historyManager.getMeta(storagePath)  # get the document meta data
    infObj = None

    actionData = request.GET.get('actionLink')  # get the action data that will be scrolled to (comment or bookmark)
    actionLink = json.loads(actionData) if actionData else None

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
    infObj['favorite'] = request.COOKIES.get('uid') == 'uid-2' if request.COOKIES.get('uid') else None
    # specify the document config
    edConfig = {
        'type': edType,
        'documentType': fileType,
        'document': {
            'title': filename,
            'url': fileUri,
            'fileType': ext[1:],
            'key': docKey,
            'info': infObj,
            'permissions': {  # the permission for the document to be edited and downloaded or not
                'comment': (edMode != 'view') & (edMode != 'fillForms') & (edMode != 'embedded') & (edMode != "blockcontent"),
                'download': True,
                'edit': canEdit & ((edMode == 'edit') | (edMode == 'filter') | (edMode == "blockcontent")),
                'fillForms': (edMode != 'view') & (edMode != 'comment') & (edMode != 'embedded') & (edMode != "blockcontent"),
                'modifyFilter': edMode != 'filter',
                'modifyContentControl': edMode != "blockcontent",
                'review': (edMode == 'edit') | (edMode == 'review'),
                'reviewGroups': reviewGroups
            }
        },
        'editorConfig': {
            'actionLink': actionLink,
            'mode': mode,
            'lang': lang,
            'callbackUrl': docManager.getCallbackUrl(filename, request),  # absolute URL to the document storage service
            'user': {  # the user currently viewing or editing the document
                'id': user['uid'],
                'name': None if user['uid'] == 'uid-0' else user['uname'],
                'group': userGroup
            },
            'embedded': {  # the parameters for the embedded document type
                'saveUrl': fileUriUser,  # the absolute URL that will allow the document to be saved onto the user personal computer
                'embedUrl': fileUriUser,  # the absolute URL to the document serving as a source file for the document embedded into the web page
                'shareUrl': fileUriUser,  # the absolute URL that will allow other users to share this document
                'toolbarDocked': 'top'  # the place for the embedded viewer toolbar (top or bottom)
            },
            'customization': {  # the parameters for the editor interface
                'about': True,  # the About section display
                'feedback': True,  # the Feedback & Support menu button display
                'forcesave': True,  # adds the request for the forced file saving to the callback handler
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
        'url': docManager.getServerUrl(True, request) + 'static/images/logo.png'
    }

    # a document which will be compared with the current document
    dataCompareFile = {
        'fileType': 'docx',
        'url': docManager.getServerUrl(True, request) + 'static/sample.docx'
    }

    # recipient data for mail merging
    dataMailMergeRecipients = {
        'fileType': 'csv',
        'url': docManager.getServerUrl(True, request) + 'csv'
    }

    if jwtManager.isEnabled():  # if the secret key to generate token exists
        edConfig['token'] = jwtManager.encode(edConfig)  # encode the edConfig object into a token
        dataInsertImage['token'] = jwtManager.encode(dataInsertImage)  # encode the dataInsertImage object into a token
        dataCompareFile['token'] = jwtManager.encode(dataCompareFile)  # encode the dataCompareFile object into a token
        dataMailMergeRecipients['token'] = jwtManager.encode(dataMailMergeRecipients)  # encode the dataMailMergeRecipients object into a token

    hist = historyManager.getHistoryObject(storagePath, filename, docKey, fileUri, request)  # get the document history

    context = {  # the data that will be passed to the template
        'cfg': json.dumps(edConfig),  # the document config in json format
        'history': json.dumps(hist['history']) if 'history' in hist else None,  # the information about the current version
        'historyData': json.dumps(hist['historyData']) if 'historyData' in hist else None,  # the information about the previous document versions if they exist
        'fileType': fileType,  # the file type of the document (text, spreadsheet or presentation)
        'apiUrl': config.DOC_SERV_SITE_URL + config.DOC_SERV_API_URL,  # the absolute URL to the api
        'dataInsertImage': json.dumps(dataInsertImage)[1 : len(json.dumps(dataInsertImage)) - 1],  # the image which will be inserted into the document
        'dataCompareFile': dataCompareFile,  # document which will be compared with the current document
        'dataMailMergeRecipients': json.dumps(dataMailMergeRecipients)  # recipient data for mail merging
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
        response.setdefault('error', 1)  # set the default error value as 1 (document key is missing or no document with such key could be found)
        response.setdefault('message', e.args[0])

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
        fileName = fileUtils.getFileName(request.GET['filename'])  # get the file name
        filePath = docManager.getForcesavePath(fileName, request, False)  # get the path to the forcesaved file version
        if (filePath == ""):  # if this path is empty
            filePath = docManager.getStoragePath(fileName, request)  # get file from the storage directory
        response = docManager.download(filePath)  # download this file
        return response
    except Exception:
        response = {}
        response.setdefault('error', 'File not found')
        return HttpResponse(json.dumps(response), content_type='application/json')