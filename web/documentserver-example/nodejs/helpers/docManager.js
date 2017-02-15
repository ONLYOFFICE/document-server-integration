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

var path = require("path");
var fileSystem = require("fs");
var fileUtility = require("./fileUtility");
var documentService = require("./documentService");
var cacheManager = require("./cacheManager");
var guidManager = require("./guidManager");
var configServer = require('config').get('server');
var storageFolder = configServer.get('storageFolder');
var os = require("os");
const readline = require('readline');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

var docManager = {};

docManager.dir = null;
docManager.req = null;
docManager.res = null;

docManager.existsSync = function(path) {
    var res = true;
    try {
        fileSystem.accessSync(path, fileSystem.F_OK);
    } catch (e) {
        res = false;
    }
    return res;
};
docManager.createDirectory = function(path) {
    if (!this.existsSync(path)) {
        fileSystem.mkdirSync(path);
    }
};

docManager.init = function (dir, req, res) {
    docManager.dir = dir;
    docManager.req = req;
    docManager.res = res;

    this.createDirectory(path.join(docManager.dir, "public", storageFolder));
};

docManager.getLang = function () {
    if (docManager.req.query.lang) {
        return docManager.req.query.lang;
    } else {
        return "en"
    }
};

docManager.getCustomParams = function () {
    var params = "";

    var userid = docManager.req.query.userid;
    params += (userid ? "&userid=" + userid : "");

    var name = docManager.req.query.name;
    params += (name ? "&name=" + name : "");

    var lang = docManager.req.query.lang;
    params += (lang ? "&lang=" + docManager.getLang() : "");

    var fileName = docManager.req.query.fileName;
    params += (fileName ? "&fileName=" + fileName : "");

    var mode = docManager.req.query.mode;
    params += (mode ? "&mode=" + mode : "");

    var type = docManager.req.query.type;
    params += (type ? "&type=" + type : "");

    return params;
};

docManager.getCorrectName = function (fileName, userAddress) {
    var baseName = fileUtility.getFileName(fileName, true);
    var ext = fileUtility.getFileExtension(fileName);
    var name = baseName + ext;
    var index = 1;

    while (this.existsSync(docManager.storagePath(name, userAddress))) {
        name = baseName + " (" + index + ")" + ext;
        index++;
    }

    return name;
};

docManager.createDemo = function (demoName, userid, username) {
    var fileName = docManager.getCorrectName(demoName);

    docManager.copyFile(path.join(docManager.dir, "public", "samples", demoName), docManager.storagePath(fileName));

    docManager.saveFileData(fileName, userid, username);

    return fileName;
};

docManager.saveFileData = function (fileName, userid, username) {
    var userAddress = docManager.curUserHostAddress();
    var date_create = fileSystem.statSync(docManager.storagePath(fileName)).mtime;
    var minutes = (date_create.getMinutes() < 10 ? '0' : '') + date_create.getMinutes().toString();
    var month = (date_create.getMonth() < 10 ? '0' : '') + (parseInt(date_create.getMonth().toString()) + 1);
    var sec = (date_create.getSeconds() < 10 ? '0' : '') + date_create.getSeconds().toString();
    var date_format = date_create.getFullYear() + "-" + month + "-" + date_create.getDate() + " " + date_create.getHours() + ":" + minutes + ":" + sec;

    var file_info = docManager.historyPath(fileName, userAddress, true);
    this.createDirectory(file_info);

    fileSystem.writeFileSync(path.join(file_info, fileName + ".txt"), date_format + "," + userid + "," + username);
};

docManager.getFileData = function (fileName, userAddress) {
    const history = path.join(docManager.historyPath(fileName, userAddress, true), fileName + ".txt");
    if (!this.existsSync(history)) {
        return ["2016-01-01", "uid-1", "John Smith"];
    }

    return ((fileSystem.readFileSync(history)).toString()).split(",");
};

docManager.getFileUri = function (fileName) {
	return docManager.getlocalFileUri(fileName);
};

docManager.getlocalFileUri = function (fileName, version) {
    var serverPath = docManager.getServerUrl();
    var storagePath = storageFolder.length ? storageFolder + "/" : "";
    var hostAddress = docManager.curUserHostAddress();
    var url = serverPath + "/" + storagePath + hostAddress + "/" + encodeURIComponent(fileName);
    if (!version) {
        return url;
    }
    return url + "-history/" + version;
};

docManager.getServerUrl = function () {
    return docManager.getProtocol() + "://" + docManager.req.get("host");
};

docManager.getCallback = function (fileName) {
    var server = docManager.getServerUrl();
    var hostAddress = docManager.curUserHostAddress();
    var handler = "/track?filename=" + encodeURIComponent(fileName) + "&useraddress=" + encodeURIComponent(hostAddress);

    return server + handler;
};

docManager.storagePath = function (fileName, userAddress) {
    fileName = fileUtility.getFileName(fileName);
    var directory = path.join(docManager.dir, "public", storageFolder, docManager.curUserHostAddress(userAddress));
    this.createDirectory(directory);
    return path.join(directory, fileName);
};

docManager.historyPath = function (fileName, userAddress, create) {
    var directory = path.join(docManager.dir, "public", storageFolder, docManager.curUserHostAddress(userAddress));
    if (!this.existsSync(directory)) {
        return "";
    }
    directory = path.join(directory, fileName + "-history");
    if (!create && !this.existsSync(path.join(directory, "1"))) {
        return "";
    }
    return directory;
};

docManager.versionPath = function (fileName, userAddress, version) {
    var historyPath = docManager.historyPath(fileName, userAddress, true);
    return path.join(historyPath, "" + version);
};

docManager.prevFilePath = function (fileName, userAddress, version) {
    return path.join(docManager.versionPath(fileName, userAddress, version), "prev" + fileUtility.getFileExtension(fileName));
};

docManager.diffPath = function (fileName, userAddress, version) {
    return path.join(docManager.versionPath(fileName, userAddress, version), "diff.zip");
};

docManager.changesPath = function (fileName, userAddress, version) {
    return path.join(docManager.versionPath(fileName, userAddress, version), "changes.txt");
};

docManager.keyPath = function (fileName, userAddress, version) {
    return path.join(docManager.versionPath(fileName, userAddress, version), "key.txt");
};

docManager.changesUser = function (fileName, userAddress, version) {
    return path.join(docManager.versionPath(fileName, userAddress, version), "user.txt");
};

docManager.getStoredFiles = function () {
    var directory = path.join(docManager.dir, "public", storageFolder, docManager.curUserHostAddress());
    this.createDirectory(directory);
    var result = [];
    var storedFiles = fileSystem.readdirSync(directory);
    for (var i = 0; i < storedFiles.length; i++) {
        var stats = fileSystem.lstatSync(path.join(directory, storedFiles[i]));

        if (!stats.isDirectory()) {

            var time = stats.mtime.getTime();
            var item = {
                time: time,
                name: storedFiles[i],
                url: docManager.getlocalFileUri(storedFiles[i]),
                documentType: fileUtility.getFileType(storedFiles[i])
            };

            if (!result.length) {
                result.push(item);
            } else {
                for (var j = 0; j < result.length; j++) {
                    if (time > result[j].time) {
                        break;
                    }
                }
                result.splice(j, 0, item);
            }
        }
    }
    return result;
};

docManager.getProtocol = function () {
    return docManager.req.headers["x-forwarded-proto"] || docManager.req.protocol;
};

docManager.curUserHostAddress = function (userAddress) {
    if (!userAddress)
        userAddress = docManager.req.headers["x-forwarded-for"] || docManager.req.connection.remoteAddress;

    return userAddress.replace(new RegExp("[^0-9a-zA-Z.=]", "g"), "_");
};

docManager.copyFile = function (exist, target) {
    fileSystem.writeFileSync(target, fileSystem.readFileSync(exist));
};

docManager.getInternalExtension = function (fileType) {
    if (fileType == fileUtility.fileType.text)
        return ".docx";

    if (fileType == fileUtility.fileType.spreadsheet)
        return ".xlsx";

    if (fileType == fileUtility.fileType.presentation)
        return ".pptx";

    return ".docx";
};

docManager.getKey = function (fileName) {
    var userAddress = docManager.curUserHostAddress();
    var key = userAddress + docManager.getlocalFileUri(fileName);

    var historyPath = docManager.historyPath(fileName, userAddress);
    if (historyPath != ""){
        key += docManager.countVersion(historyPath);
    }

    historyPath = docManager.historyPath(fileName, userAddress, true);
    var stat = fileSystem.statSync(historyPath);
    key += stat.mtime.toString();

    return documentService.generateRevisionId(key);
};

docManager.getDate = function (date) {
    var minutes = (date.getMinutes() < 10 ? '0' : '') + date.getMinutes().toString();
    return date.getMonth() + "/" + date.getDate() + "/" + date.getFullYear() + " " + date.getHours() + ":" + minutes;
};

docManager.getChanges = function (fileName) {
    return JSON.parse(fileSystem.readFileSync(fileName));
};

docManager.countVersion = function(directory) {
    var i = 0;
    while (this.existsSync(path.join(directory, '' + (i + 1)))) {
        i++;
    }
    return i;
};

docManager.getHistory = function (fileName, content, keyVersion, version) {
    var oldVersion = false;
    var contentJson = null;
    if (content) {
        if (content.changes) {
            contentJson = content.changes[0];
        } else {
            contentJson = content[0];
            oldVersion = true;
        }
    }

    var userAddress = docManager.curUserHostAddress();
    var username = content ? (oldVersion ? contentJson.username : contentJson.user.name) : (docManager.getFileData(fileName, userAddress))[2];
    var userid = content ? (oldVersion ? contentJson.userid : contentJson.user.id) : (docManager.getFileData(fileName, userAddress))[1];
    var created = content ? (oldVersion ? contentJson.date : contentJson.created) : (docManager.getFileData(fileName, userAddress))[0];
    var res = (content && !oldVersion) ? content : {changes: content};
    res.key = keyVersion;
    res.version = version;
    res.created = created;
    res.user = {
        id: userid,
        name: username
    };

    return res;
};

module.exports = docManager;
