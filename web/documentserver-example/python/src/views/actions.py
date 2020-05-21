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

from datetime import datetime
from django.http import HttpResponse, HttpResponseRedirect
from django.shortcuts import render
from src.utils import docManager, fileUtils, serviceConverter, users, jwtManager, historyManager


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
        filename = request.GET['filename']
        fileUri = docManager.getFileUri(filename, request)
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
    filename = request.GET['filename']

    ext = fileUtils.getFileExt(filename)

    fileUri = docManager.getFileUri(filename, request)
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
            'author': meta['uname'],
            'created': meta['created']
        }
    else:
        infObj = {
            'author': 'Me',
            'created': datetime.today().strftime('%d.%m.%Y %H:%M:%S')
        }

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
                'saveUrl': fileUri,
                'embedUrl': fileUri,
                'shareUrl': fileUri,
                'toolbarDocked': 'top'
            },
            'customization': {
                'about': True,
                'feedback': True,
                'goback': {
                    'url': config.EXAMPLE_DOMAIN
                }
            }
        }
    }

    if jwtManager.isEnabled():
        edConfig['token'] = jwtManager.encode(edConfig)

    hist = historyManager.getHistoryObject(storagePath, filename, docKey, fileUri, request)

    context = {
        'cfg': json.dumps(edConfig),
        'history': json.dumps(hist['history']) if 'history' in hist else None,
        'historyData': json.dumps(hist['historyData']) if 'historyData' in hist else None,
        'fileType': fileType,
        'apiUrl': config.DOC_SERV_API_URL
    }
    return render(request, 'editor.html', context)

def track(request):
    filename = request.GET['filename']
    usAddr = request.GET['userAddress']

    response = {}

    try:
        body = json.loads(request.body)

        if jwtManager.isEnabled():
            token = body.get('token')

            if (not token):
                token = request.headers.get('Authorization')
                if token:
                    token = token[len('Bearer '):]

            if (not token):
                raise Exception('Expected JWT')

            body = jwtManager.decode(token)
            if (body.get('payload')):
                body = body['payload']

        status = body['status']
        download = body.get('url')

        if (status == 2) | (status == 3): # mustsave, corrupted
            path = docManager.getStoragePath(filename, usAddr)
            histDir = historyManager.getHistoryDir(path)
            versionDir = historyManager.getNextVersionDir(histDir)
            changesUri = body.get('changesurl')

            os.rename(path, historyManager.getPrevFilePath(versionDir, fileUtils.getFileExt(filename)))
            docManager.saveFileFromUri(download, path)
            docManager.saveFileFromUri(changesUri, historyManager.getChangesZipPath(versionDir))

            hist = None
            hist = body.get('changeshistory')
            if (not hist) & ('history' in body):
                hist = json.dumps(body.get('history'))
            if hist:
                historyManager.writeFile(historyManager.getChangesHistoryPath(versionDir), hist)

            historyManager.writeFile(historyManager.getKeyPath(versionDir), body.get('key'))

    except Exception as e:
        response.setdefault('error', 1)
        response.setdefault('message', e.args[0])

    response.setdefault('error', 0)
    return HttpResponse(json.dumps(response), content_type='application/json', status=200 if response['error'] == 0 else 500)

def remove(request):
    filename = request.GET['filename']

    response = {}

    docManager.removeFile(filename, request)

    response.setdefault('success', True)
    return HttpResponse(json.dumps(response), content_type='application/json')