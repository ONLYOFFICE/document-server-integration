/**
 *
 * (c) Copyright Ascensio System SIA 2023
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

// get all the necessary values and modules
var urlModule = require("url");
var urllib = require("urllib");
var jwt = require("jsonwebtoken");
var fileUtility = require("./fileUtility");
var guidManager = require("./guidManager");
var configServer = require('config').get('server');
var siteUrl = configServer.get('siteUrl');  // the path to the editors installation
var cfgSignatureEnable = configServer.get('token.enable');
var cfgSignatureUseForRequest = configServer.get('token.useforrequest');
var cfgSignatureAuthorizationHeader = configServer.get('token.authorizationHeader');
var cfgSignatureAuthorizationHeaderPrefix = configServer.get('token.authorizationHeaderPrefix');
var cfgSignatureSecretExpiresIn = configServer.get('token.expiresIn');
var cfgSignatureSecret = configServer.get('token.secret');
var cfgSignatureSecretAlgorithmRequest = configServer.get('token.algorithmRequest');

var documentService = {};

documentService.userIp = null;

// get the url of the converted file (synchronous)
documentService.getConvertedUriSync = function (documentUri, fromExtension, toExtension, documentRevisionId, callback) {
    documentService.getConvertedUri(documentUri, fromExtension, toExtension, documentRevisionId, false, function (err, data) {
        callback(err, data);
    });
};

// get the url of the converted file
documentService.getConvertedUri = function (documentUri, fromExtension, toExtension, documentRevisionId, async, callback, filePass = null, lang = null) {
    fromExtension = fromExtension || fileUtility.getFileExtension(documentUri);  // get the current document extension

    var title = fileUtility.getFileName(documentUri) || guidManager.newGuid();  // get the current document name or uuid

    documentRevisionId = documentService.generateRevisionId(documentRevisionId || documentUri);  // generate the document key value

    var params = {  // write all the conversion parameters to the params dictionary
        async: async,
        url: documentUri,
        outputtype: toExtension.replace(".", ""),
        filetype: fromExtension.replace(".", ""),
        title: title,
        key: documentRevisionId,
        password: filePass,
        region: lang,
    };

    var uri = siteUrl + configServer.get('converterUrl');  // get the absolute converter url
    var headers = {
        'Content-Type': 'application/json',
        "Accept": "application/json"
    };

    if (cfgSignatureEnable && cfgSignatureUseForRequest) {  // if the signature is enabled and it can be used for request
        headers[cfgSignatureAuthorizationHeader] = cfgSignatureAuthorizationHeaderPrefix + this.fillJwtByUrl(uri, params);  // write signature authorization header
        params.token = documentService.getToken(params);  // get token and save it to the parameters
    }

    //parse url to allow request by relative url after https://github.com/node-modules/urllib/pull/321/commits/514de1924bf17a38a6c2db2a22a6bc3494c0a959
    urllib.request(urlModule.parse(uri),
        {
            method: "POST",
            headers: headers,
            data: params
        },
        callback);
};

// generate the document key value
documentService.generateRevisionId = function (expectedKey) {
    let maxKeyLength = 128;  // the max key length is 128
    if (expectedKey.length > maxKeyLength) {  // if the expected key length is greater than the max key length
        expectedKey = expectedKey.hashCode().toString();  // the expected key is hashed and a fixed length value is stored in the string format
    } 

    var key = expectedKey.replace(new RegExp("[^0-9-.a-zA-Z_=]", "g"), "_");

    return key.substring(0, Math.min(key.length, maxKeyLength));  // the resulting key is of the max key length or less
};

// create an error message for the error code
documentService.processConvertServiceResponceError = function (errorCode) {
    var errorMessage = "";
    var errorMessageTemplate = "Error occurred in the ConvertService: ";

    // add the error message to the error message template depending on the error code
    switch (errorCode) {
        case -20:
            errorMessage = errorMessageTemplate + "Error encrypt signature";
            break;
        case -8:
            errorMessage = errorMessageTemplate + "Error document signature";
            break;
        case -7:
            errorMessage = errorMessageTemplate + "Error document request";
            break;
        case -6:
            errorMessage = errorMessageTemplate + "Error database";
            break;
        case -5:
            errorMessage = errorMessageTemplate + "Incorrect password";
            break;
        case -4:
            errorMessage = errorMessageTemplate + "Error download error";
            break;
        case -3:
            errorMessage = errorMessageTemplate + "Error convertation error";
            break;
        case -2:
            errorMessage = errorMessageTemplate + "Error convertation timeout";
            break;
        case -1:
            errorMessage = errorMessageTemplate + "Error convertation unknown";
            break;
        case 0:  // if the error code is equal to 0, the error message is empty
            break;
        default:
            errorMessage = "ErrorCode = " + errorCode;  // default value for the error message
            break;
    }

    throw { message: errorMessage };
};

// get the response url
documentService.getResponseUri = function (json) {
    var fileResult = JSON.parse(json);

    if (fileResult.error)  // if an error occurs
        documentService.processConvertServiceResponceError(parseInt(fileResult.error));  // get an error message

    var isEndConvert = fileResult.endConvert  // check if the conversion is completed

    var percent = parseInt(fileResult.percent);  // get the conversion percentage
    var uri = null;
    var fileType = null;

    if (isEndConvert) {  // if the conversion is completed
        if (!fileResult.fileUrl)  // and the file url doesn't exist
            throw { message: "FileUrl is null" };  // the file url is null

        uri = fileResult.fileUrl;  // otherwise, get the file url
        fileType = fileResult.fileType;  // get the file type
        percent = 100;
    } else {  // if the conversion isn't completed
        percent = percent >= 100 ? 99 : percent;  // get the percentage value
    }

    return {
        percent : percent,
        uri : uri,
        fileType : fileType
    };
};

// create a command request
documentService.commandRequest = function (method, documentRevisionId, meta = null, callback) {

    documentRevisionId = documentService.generateRevisionId(documentRevisionId);  // generate the document key value
    params = {  // create a parameter object with command method and the document key value in it
        c: method,
        key: documentRevisionId
    };

    if (meta) {
        params.meta = meta;
    }

    var uri = siteUrl + configServer.get('commandUrl');  // get the absolute command url
    var headers = {  // create a headers object
        'Content-Type': 'application/json'
    };
    if (cfgSignatureEnable && cfgSignatureUseForRequest) {
        headers[cfgSignatureAuthorizationHeader] = cfgSignatureAuthorizationHeaderPrefix + this.fillJwtByUrl(uri, params);
        params.token = documentService.getToken(params);
    }

    //parse url to allow request by relative url after https://github.com/node-modules/urllib/pull/321/commits/514de1924bf17a38a6c2db2a22a6bc3494c0a959
    urllib.request(urlModule.parse(uri),
        {
            method: "POST",
            headers: headers,
            data: params
        },
        callback);
};

// check jwt token headers
documentService.checkJwtHeader = function (req) {
  var decoded = null;
  var authorization = req.get(cfgSignatureAuthorizationHeader);  // get signature authorization header from the request
  if (authorization && authorization.startsWith(cfgSignatureAuthorizationHeaderPrefix)) {  // if authorization header exists and it starts with the authorization header prefix
    var token = authorization.substring(cfgSignatureAuthorizationHeaderPrefix.length);  // the resulting token starts after the authorization header prefix
    try {
      decoded = jwt.verify(token, cfgSignatureSecret);  // verify signature on jwt token using signature secret
    } catch (err) {
        console.log('checkJwtHeader error: name = ' + err.name + ' message = ' + err.message + ' token = ' + token)  // print debug information to the console
    }
  }
  return decoded;
}

// get jwt token using url information
documentService.fillJwtByUrl = function (uri, opt_dataObject) {
  var parseObject = urlModule.parse(uri, true);  // get parse object from the url
  var payload = {query: parseObject.query, payload: opt_dataObject};  // create payload object

  var options = {algorithm: cfgSignatureSecretAlgorithmRequest, expiresIn: cfgSignatureSecretExpiresIn};
  return jwt.sign(payload, cfgSignatureSecret, options);  // sign token with given data using signature secret and options parameters
}

// get token
documentService.getToken = function (data) {
    var options = {algorithm: cfgSignatureSecretAlgorithmRequest, expiresIn: cfgSignatureSecretExpiresIn};
    return jwt.sign(data, cfgSignatureSecret, options);  // sign token with given data using signature secret and options parameters
};

// read and verify token
documentService.readToken = function (token) {
    try {
        return jwt.verify(token, cfgSignatureSecret);  // verify signature on jwt token using signature secret
    } catch (err) {
        console.log('checkJwtHeader error: name = ' + err.name + ' message = ' + err.message + ' token = ' + token)
    }
    return null;
};

// save all the functions to the documentService module to export it later in other files
module.exports = documentService;
