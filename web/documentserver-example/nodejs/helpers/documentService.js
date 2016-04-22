/*
 *
 * (c) Copyright Ascensio System Limited 2010-2016
 *
 * This program is freeware. You can redistribute it and/or modify it under the terms of the GNU 
 * General Public License (GPL) version 3 as published by the Free Software Foundation (https://www.gnu.org/copyleft/gpl.html). 
 * In accordance with Section 7(a) of the GNU GPL its Section 15 shall be amended to the effect that 
 * Ascensio System SIA expressly excludes the warranty of non-infringement of any third-party rights.
 *
 * THIS PROGRAM IS DISTRIBUTED WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. For more details, see GNU GPL at https://www.gnu.org/copyleft/gpl.html
 *
 * You can contact Ascensio System SIA by email at sales@onlyoffice.com
 *
 * The interactive user interfaces in modified source and object code versions of ONLYOFFICE must display 
 * Appropriate Legal Notices, as required under Section 5 of the GNU GPL version 3.
 *
 * Pursuant to Section 7 § 3(b) of the GNU GPL you must retain the original ONLYOFFICE logo which contains 
 * relevant author attributions when distributing the software. If the display of the logo in its graphic 
 * form is not reasonably feasible for technical reasons, you must include the words "Powered by ONLYOFFICE" 
 * in every copy of the program you distribute. 
 * Pursuant to Section 7 § 3(e) we decline to grant you any rights under trademark law for use of our trademarks.
 *
*/

var path = require("path");
var urllib = require("urllib");
var syncRequest = require("sync-request");
var xml2js = require("xml2js");
var fileUtility = require("./fileUtility");
var guidManager = require("./guidManager");
var configServer = require('config').get('server');
var siteUrl = configServer.get('siteUrl');

var documentService = {};

documentService.convertParams = "?url={0}&outputtype={1}&filetype={2}&title={3}&key={4}";
documentService.commandParams = "?c={0}&key={1}";
documentService.userIp = null;

documentService.getConvertedUri = function (documentUri, fromExtension, toExtension, documentRevisionId) {
    var xml = documentService.sendRequestToConvertService(documentUri, fromExtension, toExtension, documentRevisionId);

    var res = documentService.getResponseUri(xml);

    return res.value;
};

documentService.getConvertedUriAsync = function (documentUri, fromExtension, toExtension, documentRevisionId, callback) {
    fromExtension = fromExtension || fileUtility.getFileExtension(documentUri);

    var title = fileUtility.getFileName(documentUri) || guidManager.newGuid();

    documentRevisionId = documentService.generateRevisionId(documentRevisionId || documentUri);

    var params = documentService.convertParams.format(
    encodeURIComponent(documentUri),
    toExtension.replace(".", ""),
    fromExtension.replace(".", ""),
    title,
    documentRevisionId);

    urllib.request(siteUrl + configServer.get('converterUrl') + params, callback);
};

documentService.getExternalUri = function (fileStream, contentLength, contentType, documentRevisionId) {
    var params = documentService.convertParams.format("", "", "", "", documentRevisionId);

    var urlTostorage = siteUrl + configServer.get('storageUrl') + params;

    var response = syncRequest("POST", urlTostorage, {
        headers: {
            "Content-Type": contentType == null ? "application/octet-stream" : contentType,
            "Content-Length": contentLength.toString(),
            "charset": "utf-8"
        },
        body: fileStream
    });

    var res = documentService.getResponseUri(response.body.toString());

    return res.value;
};

documentService.generateRevisionId = function (expectedKey) {
    if (expectedKey.length > 20) {
        expectedKey = expectedKey.hashCode().toString();
    }

    var key = expectedKey.replace(new RegExp("[^0-9-.a-zA-Z_=]", "g"), "_");

    return key.substring(0, Math.min(key.length, 20));
};

documentService.sendRequestToConvertService = function (documentUri, fromExtension, toExtension, documentRevisionId) {
    fromExtension = fromExtension || fileUtility.getFileExtension(documentUri);

    var title = fileUtility.getFileName(documentUri) || guidManager.newGuid();

    documentRevisionId = documentService.generateRevisionId(documentRevisionId || documentUri);

    var params = documentService.convertParams.format(
    encodeURIComponent(documentUri),
    toExtension.replace(".", ""),
    fromExtension.replace(".", ""),
    title,
    documentRevisionId);

    var res = syncRequest("GET", siteUrl + configServer.get('converterUrl') + params);
    return res.getBody("utf8");
};

documentService.processConvertServiceResponceError = function (errorCode) {
    var errorMessage = "";
    var errorMessageTemplate = "Error occurred in the ConvertService: ";

    switch (errorCode) {
        case -20:
            errorMessage = errorMessageTemplate + "vkey deciphering error";
            break;
        case -8:
            errorMessage = errorMessageTemplate + "Error document VKey";
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

documentService.getResponseUri = function (xml) {
    var json = documentService.convertXmlStringToJson(xml);

    if (!json.FileResult)
        throw { message: "FileResult node is null" };

    var fileResult = json.FileResult;

    if (fileResult.Error)
        documentService.processConvertServiceResponceError(parseInt(fileResult.Error[0]));

    if (!fileResult.EndConvert)
        throw { message: "EndConvert node is null" };

    var isEndConvert = fileResult.EndConvert[0].toLowerCase() === "true";

    if (!fileResult.Percent)
        throw { message: "Percent node is null" };

    var percent = parseInt(fileResult.Percent[0]);
    var uri = null;

    if (isEndConvert) {
        if (!fileResult.FileUrl)
            throw { message: "FileUrl node is null" };

        uri = fileResult.FileUrl[0];
        percent = 100;
    } else {
        percent = percent >= 100 ? 99 : percent;
    }

    return {
        key: percent,
        value: uri
    };
};

documentService.convertXmlStringToJson = function (xml) {
    var res;

    xml2js.parseString(xml, function (err, result) {
        res = result;
    });

    return res;
};

documentService.commandRequest = function (method, documentRevisionId) {
    documentRevisionId = documentService.generateRevisionId(documentRevisionId);
    var params = documentService.commandParams.format(
    method,
    documentRevisionId);

    var res = syncRequest("GET", siteUrl + configServer.get('commandUrl') + params).getBody("utf8");
    return JSON.parse(res).error;
};

module.exports = documentService;
