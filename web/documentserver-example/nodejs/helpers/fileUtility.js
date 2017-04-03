/*
 *
 * (c) Copyright Ascensio System Limited 2010-2017
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

fileUtility.documentExts = [".docx", ".doc", ".odt", ".rtf", ".txt", ".html", ".htm", ".mht", ".pdf", ".djvu", ".fb2", ".epub", ".xps"];

fileUtility.spreadsheetExts = [".xls", ".xlsx", ".ods", ".csv"];

fileUtility.presentationExts = [".pps", ".ppsx", ".ppt", ".pptx", ".odp"];

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
