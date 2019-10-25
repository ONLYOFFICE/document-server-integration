import config
import json

from django.http import HttpResponse
from src.utils import docManager, fileUtils, serviceConverter


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

        #redirect to edit

    except Exception as e:
        response.setdefault('error', e.args[0])

    return HttpResponse(json.dumps(response), content_type='application/json')