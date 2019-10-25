import json
import requests
import config

from . import fileUtils

def getConverterUri(docUri, fromExt, toExt, docKey, isAsync):
    if not fromExt:
        fromExt = fileUtils.getFileExt(docUri)

    title = fileUtils.getFileName(docUri)

    payload = {
        'url': docUri,
        'outputtype': toExt.replace('.', ''),
        'filetype': fromExt.replace('.', ''),
        'title': title,
        'key': docKey
    }

    if (isAsync):
        payload.setdefault('async', True)

    # jwt

    response = requests.post(config.DOC_SERV_CONVERTER_URL, json=payload, headers={'accept': 'application/json'})
    json = response.json()

    return getResponseUri(json)

def getResponseUri(json):
    isEnd = json.get('endConvert')
    error = json.get('error')
    if error:
        processError(error)

    if isEnd:
        return json.get('fileUrl')

def processError(error):
    prefix = 'Error occurred in the ConvertService: '

    mapping = {
        '-8': f'{prefix}Error document VKey',
        '-7': f'{prefix}Error document request',
        '-6': f'{prefix}Error database',
        '-5': f'{prefix}Error unexpected guid',
        '-4': f'{prefix}Error download error',
        '-3': f'{prefix}Error convertation error',
        '-2': f'{prefix}Error convertation timeout',
        '-1': f'{prefix}Error convertation unknown'
    }

    raise Exception(mapping.get(str(error), f'Error Code: {error}'))