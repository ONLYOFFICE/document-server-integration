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

var configServer = require('config').get('server');
var siteUrl = configServer.get('siteUrl');
var tempStorageUrl = siteUrl + configServer.get('tempStorageUrl');

var fileUtility = {};

fileUtility.getFileName = function (url, withoutExtension) {
    if (!url) return "";

    var filename;

    if (tempStorageUrl && url.indexOf(tempStorageUrl) == 0) {
        var params = getUrlParams(url);
        filename = params == null ? null : params["filename"];
    } else {
        var parts = url.toLowerCase().split("/");
        fileName = parts.pop();
    }

    if (withoutExtension) {
        var ext = fileUtility.getFileExtension(fileName);
        return fileName.replace(ext, "");
    }

    return fileName;
};

fileUtility.getFileExtension = function (url, withoutDot) {
    if (!url) return null;

    var fileName = fileUtility.getFileName(url);

    var parts = fileName.toLowerCase().split(".");

    return withoutDot ? parts.pop() : "." + parts.pop();
};

fileUtility.getFileType = function (url) {
    var ext = fileUtility.getFileExtension(url);

    if (fileUtility.documentExts.indexOf(ext) != -1) return fileUtility.fileType.text;
    if (fileUtility.spreadsheetExts.indexOf(ext) != -1) return fileUtility.fileType.spreadsheet;
    if (fileUtility.presentationExts.indexOf(ext) != -1) return fileUtility.fileType.presentation;

    return fileUtility.fileType.text;
}

fileUtility.fileType = {
    text: "text",
    spreadsheet: "spreadsheet",
    presentation: "presentation"
}

fileUtility.documentExts = [".doc", ".docx", ".docm", ".dot", ".dotx", ".dotm", ".odt", ".fodt", ".ott", ".rtf", ".txt", ".html", ".htm", ".mht", ".pdf", ".djvu", ".fb2", ".epub", ".xps"];

fileUtility.spreadsheetExts = [".xls", ".xlsx", ".xlsm", ".xlt", ".xltx", ".xltm", ".ods", ".fods", ".ots", ".csv"];

fileUtility.presentationExts = [".pps", ".ppsx", ".ppsm", ".ppt", ".pptx", ".pptm", ".pot", ".potx", ".potm", ".odp", ".fodp", ".otp"];

function getUrlParams(url) {
    try {
        var query = url.split("?").pop();
        var params = query.split("&");
        var map = {};
        for (var i = 0; i < params.length; i++) {
            var parts = param.split("=");
            map[parts[0]] = parts[1];
        }
        return map;
    }
    catch (ex) {
        return null;
    }
}

module.exports = fileUtility;
