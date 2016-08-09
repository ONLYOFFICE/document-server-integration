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

var express = require("express");
var path = require("path");
var favicon = require("serve-favicon");
var bodyParser = require("body-parser");
var fileSystem = require("fs");
var formidable = require("formidable");
var syncRequest = require("sync-request");
var config = require('config');
var configServer = config.get('server');
var docManager = require("./helpers/docManager");
var documentService = require("./helpers/documentService");
var fileUtility = require("./helpers/fileUtility");
var siteUrl = configServer.get('siteUrl');
var fileChoiceUrl = configServer.has('fileChoiceUrl') ? configServer.get('fileChoiceUrl') : "";
var plugins = config.get('plugins');

process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";


String.prototype.hashCode = function () {
    for (var ret = 0, i = 0, len = this.length; i < len; i++) {
        ret = (31 * ret + this.charCodeAt(i)) << 0;
    }
    return ret;
};
String.prototype.format = function () {
    var text = this.toString();

    if (!arguments.length) return text;

    for (var i = 0; i < arguments.length; i++) {
        text = text.replace(new RegExp("\\{" + i + "\\}", "gi"), arguments[i]);
    }

    return text;
};


var app = express();


app.set("views", path.join(__dirname, "views"));
app.set("view engine", "ejs")


app.use(function (req, res, next) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    next();
});

app.use(express.static(path.join(__dirname, "public")));
app.use(favicon(__dirname + "/public/images/favicon.ico"));


app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));


app.get("/", function (req, res) {
    try {

        docManager.init(__dirname, req, res);

        res.render("index", {
            preloaderUrl: siteUrl + configServer.get('preloaderUrl'),
            convertExts: configServer.get('convertedDocs').join(","),
            editedExts: configServer.get('editedDocs').join(","),
            storedFiles: docManager.getStoredFiles(),
            params: docManager.getCustomParams()
        });

    }
    catch (ex) {
        res.status(500);
        res.render("error", { message: ex.message, error: ex });
        return;
    }
});

app.post("/upload", function (req, res) {

    docManager.init(__dirname, req, res);
    docManager.storagePath(""); //mkdir if not exist

    var userIp = docManager.curUserHostAddress();
    var uploadDir = "./public/" + configServer.get('storageFolder') + "/" + userIp;

    var form = new formidable.IncomingForm();
    form.uploadDir = uploadDir;
    form.keepExtensions = true;

    form.parse(req, function (err, fields, files) {

        var file = files.uploadedFile;

        file.name = docManager.getCorrectName(file.name);

        if (configServer.get('maxFileSize') < file.size || file.size <= 0) {
            fileSystem.unlinkSync(file.path);
            res.writeHead(200, { "Content-Type": "text/plain" });
            res.write("{ \"error\": \"File size is incorrect\"}");
            res.end();
            return;
        }

        var exts = [].concat(configServer.get('viewedDocs'), configServer.get('editedDocs'), configServer.get('convertedDocs'));
        var curExt = fileUtility.getFileExtension(file.name);

        if (exts.indexOf(curExt) == -1) {
            fileSystem.unlinkSync(file.path);
            res.writeHead(200, { "Content-Type": "text/plain" });
            res.write("{ \"error\": \"File type is not supported\"}");
            res.end();
            return;
        }

        fileSystem.rename(file.path, uploadDir + "/" + file.name, function (err) {
            res.writeHead(200, { "Content-Type": "text/plain" });
            if (err) {
                res.write("{ \"error\": \"" + err + "\"}");
            } else {
                res.write("{ \"filename\": \"" + file.name + "\"}");

                var userid = req.query.userid ? req.query.userid : "uid-1";
                var firstname = req.query.firstname ? req.query.firstname : "Jonn";
                var lastname = req.query.lastname ? req.query.lastname : "Smith";

                docManager.saveFileData(file.name, userid, firstname + " " + lastname);
                docManager.getFileData(file.name, docManager.curUserHostAddress());
            }
            res.end();
        });
    });
});

app.get("/convert", function (req, res) {

    var fileName = fileUtility.getFileName(req.query.filename);
    var fileUri = docManager.getFileUri(fileName);
    var fileExt = fileUtility.getFileExtension(fileName);
    var fileType = fileUtility.getFileType(fileName);
    var internalFileExt = docManager.getInternalExtension(fileType);
    var response = res;

    var writeResult = function (filename, step, error) {
        var result = {};

        if (filename != null)
            result["filename"] = filename;

        if (step != null)
            result["step"] = step;

        if (error != null)
            result["error"] = error;

        response.write(JSON.stringify(result));
        response.end();
    };

    var callback = function (err, data) {
        if (err) {
            if (err.name === "ConnectionTimeoutError" || err.name === "ResponseTimeoutError") {
                writeResult(fileName, 0, null);
            } else {
                writeResult(null, null, JSON.stringify(err));
            }
            return;
        }

        try {
            var responseUri = documentService.getResponseUri(data.toString());
            var result = responseUri.key;
            var newFileUri = responseUri.value;

            if (result != 100) {
                writeResult(fileName, result, null);
                return;
            }

            var correctName = docManager.getCorrectName(fileUtility.getFileName(fileName, true) + internalFileExt);

            var file = syncRequest("GET", newFileUri);
            fileSystem.writeFileSync(docManager.storagePath(correctName), file.getBody());

            fileSystem.unlinkSync(docManager.storagePath(fileName));

            var userAddress = docManager.curUserHostAddress();
            var historyPath = docManager.historyPath(fileName, userAddress, true);
            var correctHistoryPath = docManager.historyPath(correctName, userAddress, true);

            fileSystem.renameSync(historyPath, correctHistoryPath);

            fileSystem.renameSync(path.join(correctHistoryPath, fileName + ".txt"), path.join(correctHistoryPath, correctName + ".txt"));

            writeResult(correctName, null, null);
        } catch (e) {
            writeResult(null, null, e.message);
        }
    };

    try {
        if (configServer.get('convertedDocs').indexOf(fileExt) != -1) {
            var key = documentService.generateRevisionId(fileUri);
            documentService.getConvertedUriAsync(fileUri, fileExt, internalFileExt, key, callback);
        } else {
            writeResult(fileName, null, null);
        }
    } catch (ex) {
        writeResult(null, null, ex.message);
    }
});

app.delete("/file", function (req, res) {
    try {
        docManager.init(__dirname, req, res);

        var fileName = fileUtility.getFileName(req.query.filename);

        var filePath = docManager.storagePath(fileName)
        fileSystem.unlinkSync(filePath);

        var userAddress = docManager.curUserHostAddress();
        var historyPath = docManager.historyPath(fileName, userAddress, true);

        var deleteFolderRecursive = function (path) {
            if (fileSystem.existsSync(path)) {
                var files = fileSystem.readdirSync(path);
                files.forEach(function (file, index) {
                    var curPath = path + "/" + file;
                    if (fileSystem.lstatSync(curPath).isDirectory()) {
                        deleteFolderRecursive(curPath);
                    } else {
                        fileSystem.unlinkSync(curPath);
                    }
                });
                fileSystem.rmdirSync(path);
            }
        };
        deleteFolderRecursive(historyPath);

        res.write("{\"success\":true}");
    } catch (ex) {
        res.write(JSON.stringify(ex));
    }
    res.end();
});

app.post("/track", function (req, res) {

    docManager.init(__dirname, req, res);

    var userAddress = req.query.useraddress;
    var fileName = fileUtility.getFileName(req.query.filename);
    var version = 0;

    var processTrack = function (response, body, fileName, userAddress) {

        var processSave = function (body, fileName, userAddress, newVersion) {

            var downloadUri = body.url;
            var curExt = fileUtility.getFileExtension(fileName);
            var downloadExt = fileUtility.getFileExtension(downloadUri);

            if (downloadExt != curExt) {
                var key = documentService.generateRevisionId(downloadUri);

                try {
                    downloadUri = documentService.getConvertedUri(downloadUri, downloadExt, curExt, key);
                } catch (ex) {
                    fileName = docManager.getCorrectName(fileUtility.getFileName(fileName, true) + downloadExt, userAddress)
                }
            }

            try {

                var path = docManager.storagePath(fileName, userAddress);

                if (newVersion) {
                    var historyPath = docManager.historyPath(fileName, userAddress);
                    if (historyPath == "") {
                        historyPath = docManager.historyPath(fileName, userAddress, true);
                        docManager.createDirectory(historyPath);
                    }

                    var count_version = docManager.countVersion(historyPath);
                    version = count_version + 1;
                    versionPath = docManager.versionPath(fileName, userAddress, version);
                    docManager.createDirectory(versionPath);

                    var downloadZip = body.changesurl;
                    if (downloadZip) {
                        var path_changes = docManager.diffPath(fileName, userAddress, version);
                        var diffZip = syncRequest("GET", downloadZip);
                        fileSystem.writeFileSync(path_changes, diffZip.getBody());
                    }

                    var changeshistory = body.changeshistory;
                    if (changeshistory) {
                        var path_changes_json = docManager.changesPath(fileName, userAddress, version);
                        fileSystem.writeFileSync(path_changes_json, body.changeshistory);
                    }

                    var path_key = docManager.keyPath(fileName, userAddress, version);
                    fileSystem.writeFileSync(path_key, body.key);

                    var path_prev = docManager.prevFilePath(fileName, userAddress, version);
                    fileSystem.writeFileSync(path_prev, fileSystem.readFileSync(path));
                }

                var file = syncRequest("GET", downloadUri);
                fileSystem.writeFileSync(path, file.getBody());
            } catch (ex) {
            }
        }

        if (body.status == 1) { //Editing
            if (body.actions && body.actions[0].type == 0) { //finished edit
                var user = body.actions[0].userid;
                if (body.users.indexOf(user) == -1) {
                    var key = body.key;
                    try {
                        documentService.commandRequest("forcesave", key);
                    } catch (ex) {
                    }
                }
            }
        } else if (body.status == 2 || body.status == 3) { //MustSave, Corrupted
            processSave(body, fileName, userAddress, true);
        } else if (body.status == 6 || body.status == 7) { //MustForceSave, CorruptedForceSave
            processSave(body, fileName, userAddress);
        }

        response.write("{\"error\":0}");
        response.end();
    }

    var readbody = function (request, response, fileName, userAddress) {
        var content = "";
        request.on('data', function (data) {
            content += data;
        });
        request.on('end', function () {
            var body = JSON.parse(content);
            processTrack(response, body, fileName, userAddress);
        });
    }

    if (req.body.hasOwnProperty("status")) {
        processTrack(res, req.body, fileName, userAddress);
    } else {
        readbody(req, res, fileName, userAddress);
    }
});

app.get("/editor", function (req, res) {
    try {

        docManager.init(__dirname, req, res);

        var fileExt = req.query.fileExt;
        var history = [];
        var prevUrl = [];
        var diff = [];
        var lang = docManager.getLang();
        var userid = req.query.userid ? req.query.userid : "uid-1";
        var firstname = req.query.firstname ? req.query.firstname : "Jonn";
        var lastname = req.query.lastname ? req.query.lastname : "Smith";

        if (fileExt != null) {
            var fileName = docManager.createDemo((req.query.sample ? "sample." : "new.") + fileExt, userid, firstname + " " + lastname);

            var redirectPath = docManager.getProtocol() + "://" + docManager.req.get("host") + "/editor?fileName=" + encodeURIComponent(fileName) + docManager.getCustomParams();
            res.redirect(redirectPath);
            return;
        }

        var userAddress = docManager.curUserHostAddress();
        fileName = fileUtility.getFileName(req.query.fileName);
        var key = docManager.getKey(fileName);
        var url = docManager.getFileUri(fileName);
        var mode = req.query.mode || "edit"; //mode: view/edit 
        var type = req.query.type || "desktop"; //type: embedded/mobile/desktop
        var canEdit = configServer.get('editedDocs').indexOf(fileUtility.getFileExtension(fileName)) != -1;

        var countVersion = 1;

        var historyPath = docManager.historyPath(fileName, userAddress);
        changes = null;

        if (historyPath != '') {

            countVersion = docManager.countVersion(historyPath) + 1;
            var prevPath = docManager.getlocalFileUri(fileName, 1) + "/prev" + fileUtility.getFileExtension(fileName);
            var diffPath = null;
            for (var i = 1; i < countVersion; i++) {
                var keyPath = docManager.keyPath(fileName, userAddress, i);
                var keyVersion = "" + fileSystem.readFileSync(keyPath);
                history.push(docManager.getHistory(fileName, changes, keyVersion, i));

                prevUrl.push(prevPath);
                prevPath = docManager.getlocalFileUri(fileName, i) + "/prev" + fileUtility.getFileExtension(fileName);

                diff.push(diffPath);
                diffPath = docManager.getlocalFileUri(fileName, i) + "/diff.zip";

                var changesFile = docManager.changesPath(fileName, userAddress, i);
                var changes = docManager.getChanges(changesFile);
            }
            prevUrl.push(prevPath);
            diff.push(diffPath);
        } else {
            prevUrl.push(url);
        }
        history.push(docManager.getHistory(fileName, changes, key, countVersion));

        var argss = {
            apiUrl: siteUrl + configServer.get('apiUrl'),
            file: {
                name: fileName,
                ext: fileUtility.getFileExtension(fileName, true),
                uri: url,
                version: countVersion
            },
            editor: {
                type: type,
                documentType: fileUtility.getFileType(fileName),
                key: key,
                callbackUrl: docManager.getCallback(fileName),
                isEdit: canEdit,
                mode: canEdit && mode != "view" ? "edit" : "view",
                canBackToFolder: type != "embedded",
                getServerUrl: docManager.getServerUrl(),
                curUserHostAddress: docManager.curUserHostAddress(),
                lang: lang,
                userid: userid,
                firstname: firstname,
                lastname: lastname,
                fileChoiceUrl: fileChoiceUrl,
                plugins: plugins
            },
            history: history,
            setHistoryData: {
                url: prevUrl,
                urlDiff: diff
            }
        };

        res.render("editor", argss);
    }
    catch (ex) {
        res.status(500);
        res.render("error", { message: ex.message, error: ex });
    }
});

app.use(function (req, res, next) {
    var err = new Error("Not Found");
    err.status = 404;
    next(err);
});

app.use(function (err, req, res, next) {
    res.status(err.status || 500);
    res.render("error", {
        message: err.message
    });
});

module.exports = app;
