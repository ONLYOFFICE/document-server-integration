/**
 *
 * (c) Copyright Ascensio System SIA 2020
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

var path = require("path");
var urlModule = require("url");
var urllib = require("urllib");
var jwt = require("jsonwebtoken");
var jwa = require("jwa");
var fileUtility = require("./fileUtility");
var guidManager = require("./guidManager");
var configServer = require('config').get('server');
var siteUrl = configServer.get('siteUrl');
var cfgSignatureEnable = configServer.get('token.enable');
var cfgSignatureUseForRequest = configServer.get('token.useforrequest');
var cfgSignatureAuthorizationHeader = configServer.get('token.authorizationHeader');
var cfgSignatureAuthorizationHeaderPrefix = configServer.get('token.authorizationHeaderPrefix');
var cfgSignatureSecretExpiresIn = configServer.get('token.expiresIn');
var cfgSignatureSecret = configServer.get('token.secret');
var cfgSignatureSecretAlgorithmRequest = configServer.get('token.algorithmRequest');

var documentService = {};

documentService.userIp = null;

documentService.getConvertedUriSync = function (documentUri, fromExtension, toExtension, documentRevisionId, callback) {
    documentService.getConvertedUri(documentUri, fromExtension, toExtension, documentRevisionId, false, function (err, data) {
        if (err) {
            callback();
            return;
        }
        var res = documentService.getResponseUri(data);
        callback(res.value);
    });
};

documentService.getConvertedUri = function (documentUri, fromExtension, toExtension, documentRevisionId, async, callback) {
    fromExtension = fromExtension || fileUtility.getFileExtension(documentUri);

    var title = fileUtility.getFileName(documentUri) || guidManager.newGuid();

    documentRevisionId = documentService.generateRevisionId(documentRevisionId || documentUri);

    var params = {
        async: async,
        url: documentUri,
        outputtype: toExtension.replace(".", ""),
        filetype: fromExtension.replace(".", ""),
        title: title,
        key: documentRevisionId
    };

    var uri = siteUrl + configServer.get('converterUrl');
    var headers = {
        'Content-Type': 'application/json',
        "Accept": "application/json"
    };

    if (cfgSignatureEnable && cfgSignatureUseForRequest) {
        headers[cfgSignatureAuthorizationHeader] = cfgSignatureAuthorizationHeaderPrefix + this.fillJwtByUrl(uri, params);
        params.token = documentService.getToken(params);
    }

    urllib.request(uri,
        {
            method: "POST",
            headers: headers,
            data: params
        },
        callback);
};

documentService.generateRevisionId = function (expectedKey) {
    let maxKeyLength = 128;
    if (expectedKey.length > maxKeyLength) {
        expectedKey = expectedKey.hashCode().toString();
    }

    var key = expectedKey.replace(new RegExp("[^0-9-.a-zA-Z_=]", "g"), "_");

    return key.substring(0, Math.min(key.length, maxKeyLength));
};

documentService.processConvertServiceResponceError = function (errorCode) {
    var errorMessage = "";
    var errorMessageTemplate = "Error occurred in the ConvertService: ";

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
            errorMessage = errorMessageTemplate + "Error unexpected guid";
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
        case 0:
            break;
        default:
            errorMessage = "ErrorCode = " + errorCode;
            break;
    }

    throw { message: errorMessage };
};

documentService.getResponseUri = function (json) {
    var fileResult = JSON.parse(json);

    if (fileResult.error)
        documentService.processConvertServiceResponceError(parseInt(fileResult.error));

    var isEndConvert = fileResult.endConvert

    var percent = parseInt(fileResult.percent);
    var uri = null;

    if (isEndConvert) {
        if (!fileResult.fileUrl)
            throw { message: "FileUrl is null" };

        uri = fileResult.fileUrl;
        percent = 100;
    } else {
        percent = percent >= 100 ? 99 : percent;
    }

    return {
        key: percent,
        value: uri
    };
};

documentService.commandRequest = function (method, documentRevisionId, callback) {
    documentRevisionId = documentService.generateRevisionId(documentRevisionId);
    var params = {
        c: method,
        key: documentRevisionId
    };

    var uri = siteUrl + configServer.get('commandUrl');
    var headers = {
        'Content-Type': 'application/json'
    };
    if (cfgSignatureEnable && cfgSignatureUseForRequest) {
        headers[cfgSignatureAuthorizationHeader] = cfgSignatureAuthorizationHeaderPrefix + this.fillJwtByUrl(uri, params);
        params.token = documentService.getToken(params);
    }

    urllib.request(uri,
        {
            method: "POST",
            headers: headers,
            data: params
        },
        callback);
};

documentService.checkJwtHeader = function (req) {
  var decoded = null;
  var authorization = req.get(cfgSignatureAuthorizationHeader);
  if (authorization && authorization.startsWith(cfgSignatureAuthorizationHeaderPrefix)) {
    var token = authorization.substring(cfgSignatureAuthorizationHeaderPrefix.length);
    try {
      decoded = jwt.verify(token, cfgSignatureSecret);
    } catch (err) {
        console.log('checkJwtHeader error: name = ' + err.name + ' message = ' + err.message + ' token = ' + token)
    }
  }
  return decoded;
}

documentService.fillJwtByUrl = function (uri, opt_dataObject, opt_iss, opt_payloadhash) {
  var parseObject = urlModule.parse(uri, true);
  var payload = {query: parseObject.query, payload: opt_dataObject, payloadhash: opt_payloadhash};

  var options = {algorithm: cfgSignatureSecretAlgorithmRequest, expiresIn: cfgSignatureSecretExpiresIn, issuer: opt_iss};
  return jwt.sign(payload, cfgSignatureSecret, options);
}

documentService.getToken = function (data) {
    var options = {algorithm: cfgSignatureSecretAlgorithmRequest, expiresIn: cfgSignatureSecretExpiresIn};
    return jwt.sign(data, cfgSignatureSecret, options);
};

documentService.readToken = function (token) {
    try {
        return jwt.verify(token, cfgSignatureSecret);
    } catch (err) {
        console.log('checkJwtHeader error: name = ' + err.name + ' message = ' + err.message + ' token = ' + token)
    }
    return null;
};

module.exports = documentService;
