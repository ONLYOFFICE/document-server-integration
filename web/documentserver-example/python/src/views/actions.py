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


def upload(request):
    response = {}

    try:
        fileInfo = request.FILES['uploadedFile']

        if fileInfo.size > config.FILE_SIZE_MAX:
            raise Exception('File size is too big')

        curExt = fileUtils.getFileExt(fileInfo.name)
        if not docManager.isSupportedExt(curExt):
            raise Exception('File type is not supported')

        name = docManager.getCorrectName(fileInfo.name, request)
        path = docManager.getStoragePath(name, request)

        docManager.createFile(fileInfo.file, path, request, True)

        response.setdefault('filename', name)

    except Exception as e:
        response.setdefault('error', e.args[0])

    return HttpResponse(json.dumps(response), content_type='application/json')

def convert(request):
    response = {}

    try:
        filename = fileUtils.getFileName(request.GET['filename'])
        fileUri = docManager.getFileUri(filename, True,request)
        fileExt = fileUtils.getFileExt(filename)
        fileType = fileUtils.getFileType(filename)
        newExt = docManager.getInternalExtension(fileType)

        if docManager.isCanConvert(fileExt):
            key = docManager.generateFileKey(filename, request)

            newUri = serviceConverter.getConverterUri(fileUri, fileExt, newExt, key, True)

            if not newUri:
                response.setdefault('step', '0')
                response.setdefault('filename', filename)
            else:
                correctName = docManager.getCorrectName(fileUtils.getFileNameWithoutExt(filename) + newExt, request)
                path = docManager.getStoragePath(correctName, request)
                docManager.saveFileFromUri(newUri, path, request, True)
                docManager.removeFile(filename, request)
                response.setdefault('filename', correctName)
        else:
            response.setdefault('filename', filename)

    except Exception as e:
        response.setdefault('error', e.args[0])

    return HttpResponse(json.dumps(response), content_type='application/json')

def createNew(request):
    response = {}

    try:
        fileType = request.GET['fileType']
        sample = request.GET.get('sample', False)

        filename = docManager.createSample(fileType, sample, request)

        return HttpResponseRedirect(f'edit?filename={filename}')

    except Exception as e:
        response.setdefault('error', e.args[0])

    return HttpResponse(json.dumps(response), content_type='application/json')

def edit(request):
    filename = fileUtils.getFileName(request.GET['filename'])

    ext = fileUtils.getFileExt(filename)

    fileUri = docManager.getFileUri(filename, True, request)
    fileUriUser = docManager.getFileUri(filename, False, request)
    docKey = docManager.generateFileKey(filename, request)
    fileType = fileUtils.getFileType(filename)
    user = users.getUserFromReq(request)

    edMode = request.GET.get('mode') if request.GET.get('mode') else 'edit'
    canEdit = docManager.isCanEdit(ext)
    mode = 'edit' if canEdit & (edMode != 'view') else 'view'

    edType = request.GET.get('type') if request.GET.get('type') else 'desktop'
    lang = request.COOKIES.get('ulang') if request.COOKIES.get('ulang') else 'en'

    storagePath = docManager.getStoragePath(filename, request)
    meta = historyManager.getMeta(storagePath)
    infObj = None

    actionData = request.GET.get('actionLink')
    actionLink = json.loads(actionData) if actionData else None

    if (meta):
        infObj = {
            'owner': meta['uname'],
            'uploaded': meta['created']
        }
    else:
        infObj = {
            'owner': 'Me',
            'uploaded': datetime.today().strftime('%d.%m.%Y %H:%M:%S')
        }
    infObj['favorite'] = request.COOKIES.get('uid') == 'uid-2' if request.COOKIES.get('uid') else None
    edConfig = {
        'type': edType,
        'documentType': fileType,
        'document': {
            'title': filename,
            'url': fileUri,
            'fileType': ext[1:],
            'key': docKey,
            'info': infObj,
            'permissions': {
                'comment': (edMode != 'view') & (edMode != 'fillForms') & (edMode != 'embedded') & (edMode != "blockcontent"),
                'download': True,
                'edit': canEdit & ((edMode == 'edit') | (edMode == 'filter') | (edMode == "blockcontent")),
                'fillForms': (edMode != 'view') & (edMode != 'comment') & (edMode != 'embedded') & (edMode != "blockcontent"),
                'modifyFilter': edMode != 'filter',
                'modifyContentControl': edMode != "blockcontent",
                'review': (edMode == 'edit') | (edMode == 'review')
            }
        },
        'editorConfig': {
            'actionLink': actionLink,
            'mode': mode,
            'lang': lang,
            'callbackUrl': docManager.getCallbackUrl(filename, request),
            'user': {
                'id': user['uid'],
                'name': user['uname']
            },
            'embedded': {
                'saveUrl': fileUriUser,
                'embedUrl': fileUriUser,
                'shareUrl': fileUriUser,
                'toolbarDocked': 'top'
            },
            'customization': {
                'about': True,
                'feedback': True,
                'forcesave': False,
                'goback': {
                    'url': docManager.getServerUrl(False, request)
                }
            }
        }
    }

    dataInsertImage = {
        'fileType': 'png',
        'url': docManager.getServerUrl(True, request) + 'static/images/logo.png'
    }

    dataCompareFile = {
        'fileType': 'docx',
        'url': docManager.getServerUrl(True, request) + 'static/sample.docx'
    }

    dataMailMergeRecipients = {
        'fileType': 'csv',
        'url': docManager.getServerUrl(True, request) + 'csv'
    }

    if jwtManager.isEnabled():
        edConfig['token'] = jwtManager.encode(edConfig)
        dataInsertImage['token'] = jwtManager.encode(dataInsertImage)
        dataCompareFile['token'] = jwtManager.encode(dataCompareFile)
        dataMailMergeRecipients['token'] = jwtManager.encode(dataMailMergeRecipients)

    hist = historyManager.getHistoryObject(storagePath, filename, docKey, fileUri, request)

    context = {
        'cfg': json.dumps(edConfig),
        'history': json.dumps(hist['history']) if 'history' in hist else None,
        'historyData': json.dumps(hist['historyData']) if 'historyData' in hist else None,
        'fileType': fileType,
        'apiUrl': config.DOC_SERV_SITE_URL + config.DOC_SERV_API_URL,
        'dataInsertImage': json.dumps(dataInsertImage)[1 : len(json.dumps(dataInsertImage)) - 1],
        'dataCompareFile': dataCompareFile,
        'dataMailMergeRecipients': json.dumps(dataMailMergeRecipients)
    }
    return render(request, 'editor.html', context)

def track(request):
    response = {}

    try:
        body = trackManager.readBody(request)
        status = body['status']

        if (status == 1): # Editing
            if (body['actions'] and body['actions'][0]['type'] == 0):# finished edit
                user = body['actions'][0]['userid']
                if (not user in body['users']):
                    trackManager.commandRequest('forcesave', body['key'])

        filename = fileUtils.getFileName(request.GET['filename'])
        usAddr = request.GET['userAddress']

        if (status == 2) | (status == 3): # mustsave, corrupted
            trackManager.processSave(body, filename, usAddr, request)
        if (status == 6) | (status == 7): # mustforcesave, corruptedforcesave
            trackManager.processForceSave(body, filename, usAddr, request)

    except Exception as e:
        response.setdefault('error', 1)
        response.setdefault('message', e.args[0])

    response.setdefault('error', 0)
    return HttpResponse(json.dumps(response), content_type='application/json', status=200 if response['error'] == 0 else 500)

def remove(request):
    filename = fileUtils.getFileName(request.GET['filename'])

    response = {}

    docManager.removeFile(filename, request)

    response.setdefault('success', True)
    return HttpResponse(json.dumps(response), content_type='application/json')

def files(request):
    try:
        response = docManager.getFilesInfo(request)
    except Exception as e:
        response = {}
        response.setdefault('error', e.args[0])
    return HttpResponse(json.dumps(response), content_type='application/json')

def csv(request):
    filePath = os.path.join('assets', 'sample', "csv.csv")
    response = FileResponse(open(filePath, 'rb'), True)
    response['Content-Length'] =  os.path.getsize(filePath)
    response['Content-Disposition'] = "attachment;filename*=UTF-8\'\'" + urllib.parse.unquote(os.path.basename(filePath))
    response['Content-Type'] = magic.from_file(filePath, mime=True)
    return response