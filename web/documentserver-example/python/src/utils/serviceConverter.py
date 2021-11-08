"""

 (c) Copyright Ascensio System SIA 2021
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

import json
import requests
import config

from . import fileUtils, jwtManager

# convert file and give url to a new file
def getConverterUri(docUri, fromExt, toExt, docKey, isAsync, filePass = None, lang = None):
    if not fromExt: # check if the extension from the request matches the real file extension
        fromExt = fileUtils.getFileExt(docUri) # if not, overwrite the extension value

    title = fileUtils.getFileName(docUri)

    payload = { # write all the necessary data to the payload object
        'url': docUri,
        'outputtype': toExt.replace('.', ''),
        'filetype': fromExt.replace('.', ''),
        'title': title,
        'key': docKey,
        'password': filePass,
        'region': lang
    }

    headers={'accept': 'application/json'}

    if (isAsync): # check if the operation is asynchronous
        payload.setdefault('async', True) # and write this information to the payload object

    if jwtManager.isEnabled(): # check if a secret key to generate token exists or not
        jwtHeader = 'Authorization' if config.DOC_SERV_JWT_HEADER is None or config.DOC_SERV_JWT_HEADER == '' else config.DOC_SERV_JWT_HEADER # get jwt header
        headerToken = jwtManager.encode({'payload': payload}) # encode a payload object into a header token
        payload['token'] = jwtManager.encode(payload) # encode a payload object into a body token
        headers[jwtHeader] = f'Bearer {headerToken}' # add a header Authorization with a header token with Authorization prefix in it

    response = requests.post(config.DOC_SERV_SITE_URL + config.DOC_SERV_CONVERTER_URL, json=payload, headers=headers ) # send the headers and body values to the converter and write the result to the response
    json = response.json()

    return getResponseUri(json)

# get response url
def getResponseUri(json):
    isEnd = json.get('endConvert')
    error = json.get('error')
    if error:
        processError(error)

    if isEnd:
        return json.get('fileUrl')

# display an error that occurs during conversion
def processError(error):
    prefix = 'Error occurred in the ConvertService: '

    mapping = {
        '-8': f'{prefix}Error document VKey',
        '-7': f'{prefix}Error document request',
        '-6': f'{prefix}Error database',
        '-5': f'{prefix}Incorrect password',
        '-4': f'{prefix}Error download error',
        '-3': f'{prefix}Error convertation error',
        '-2': f'{prefix}Error convertation timeout',
        '-1': f'{prefix}Error convertation unknown'
    }

    raise Exception(mapping.get(str(error), f'Error Code: {error}'))