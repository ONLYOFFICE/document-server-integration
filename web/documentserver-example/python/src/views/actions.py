import config
import json

from datetime import datetime
from django.http import HttpResponse, HttpResponseRedirect
from django.shortcuts import render
from src.utils import docManager, fileUtils, serviceConverter, users


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

        docManager.createFile(fileInfo.file, path, True)

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
                docManager.saveFileFromUri(newUri, path, True)
                docManager.removeFile(filename, request)
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
    lang = request.GET.get('ulang') if request.GET.get('ulang') else 'en'

    edConfig = {
        'type': edType,
        'documentType': fileType,
        'document': {
            'title': filename,
            'url': fileUri,
            'fileType': ext[1:],
            'key': docKey,
            'info': {
                'author': 'Me',
                'created': datetime.today().strftime('%d.%m.%Y')
            },
            'permissions': {
                'comment': (edMode != 'view') & (edMode != 'fillForms') & (edMode != 'embedded'),
                'download': True,
                'edit': canEdit & ((edMode == 'edit') | (edMode == 'filter')),
                'fillForms': (edMode != 'view') & (edMode != 'comment') & (edMode != 'embedded'),
                'modifyFilter': edMode != 'filter',
                'review': (edMode == 'edit') | (edMode == 'review')
            }
        },
        'editorConfig': {
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
                    'url': request.META['HTTP_REFERER']
                }
            }
        }
    }

    #jwt

    context = {
        'cfg': json.dumps(edConfig),
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

        #jwt

        status = body['status']
        download = body.get('url')

        if (status == 2) | (status == 3): # mustsave, corrupted
            path = docManager.getStoragePath(filename, usAddr)
            docManager.saveFileFromUri(download, path)

    except Exception as e:
        response.setdefault('error', 1)
        response.setdefault('message', e.args[0])

    response.setdefault('error', 0)
    return HttpResponse(json.dumps(response), content_type='application/json')