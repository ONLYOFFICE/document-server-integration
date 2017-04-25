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

const express = require("express");
const path = require("path");
const favicon = require("serve-favicon");
const bodyParser = require("body-parser");
const fileSystem = require("fs");
const formidable = require("formidable");
const syncRequest = require("sync-request");
const jwt = require('jsonwebtoken');
const config = require('config');
const configServer = config.get('server');
const mime = require("mime");
const docManager = require("./helpers/docManager");
const documentService = require("./helpers/documentService");
const fileUtility = require("./helpers/fileUtility");
const siteUrl = configServer.get('siteUrl');
const fileChoiceUrl = configServer.has('fileChoiceUrl') ? configServer.get('fileChoiceUrl') : "";
const plugins = config.get('plugins');
const cfgSignatureEnable = configServer.get('token.enable');
const cfgSignatureUseForRequest = configServer.get('token.useforrequest');
const cfgSignatureSecretExpiresIn = configServer.get('token.expiresIn');
const cfgSignatureSecret = configServer.get('token.secret');

process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";


String.prototype.hashCode = function () {
	const len = this.length;
	let ret = 0;
    for (let i = 0; i < len; i++) {
        ret = (31 * ret + this.charCodeAt(i)) << 0;
    }
    return ret;
};
String.prototype.format = function () {
    let text = this.toString();

    if (!arguments.length) return text;

    for (let i = 0; i < arguments.length; i++) {
        text = text.replace(new RegExp("\\{" + i + "\\}", "gi"), arguments[i]);
    }

    return text;
};


const app = express();
app.set("views", path.join(__dirname, "views"));
app.set("view engine", "ejs");


app.use(function (req, res, next) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    next();
});

app.use(express.static(path.join(__dirname, "public")));
if (config.has('server.static')) {
  const staticContent = config.get('server.static');
  for (let i = 0; i < staticContent.length; ++i) {
    const staticContentElem = staticContent[i];
    app.use(staticContentElem['name'], express.static(staticContentElem['path'], staticContentElem['options']));
  }
}
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
        console.log(ex);
        res.status(500);
        res.render("error", { message: "Server error" });
        return;
    }
});

app.get("/download", function(req, res) {
    docManager.init(__dirname, req, res);

    var fileName = fileUtility.getFileName(req.query.fileName);
    var userAddress = docManager.curUserHostAddress();

    var path = docManager.forcesavePath(fileName, userAddress, false);
    if (path == "") {
        path = docManager.storagePath(fileName, userAddress);
    }

    res.setHeader("Content-Length", fileSystem.statSync(path).size);
    res.setHeader("Content-Type", mime.lookup(path));

    res.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

    var filestream = fileSystem.createReadStream(path);
    filestream.pipe(res);
});

app.post("/upload", function (req, res) {

    docManager.init(__dirname, req, res);
    docManager.storagePath(""); //mkdir if not exist

    const userIp = docManager.curUserHostAddress();
    const uploadDir = path.join("./public", configServer.get('storageFolder'), userIp);
    const uploadDirTmp = path.join(uploadDir, 'tmp');
    docManager.createDirectory(uploadDirTmp);

    const form = new formidable.IncomingForm();
    form.uploadDir = uploadDirTmp;
    form.keepExtensions = true;

    form.parse(req, function (err, fields, files) {
    	if (err) {
			docManager.cleanFolderRecursive(uploadDirTmp, true);
			res.writeHead(200, { "Content-Type": "text/plain" });
			res.write("{ \"error\": \"" + err.message + "\"}");
			res.end();
			return;
		}

        const file = files.uploadedFile;

        if (file == undefined) {
            res.writeHead(200, { "Content-Type": "text/plain" });
            res.write("{ \"error\": \"Uploaded file not found\"}");
            res.end();
            return;
        }

        file.name = docManager.getCorrectName(file.name);

        if (configServer.get('maxFileSize') < file.size || file.size <= 0) {
			docManager.cleanFolderRecursive(uploadDirTmp, true);
            res.writeHead(200, { "Content-Type": "text/plain" });
            res.write("{ \"error\": \"File size is incorrect\"}");
            res.end();
            return;
        }

        const exts = [].concat(configServer.get('viewedDocs'), configServer.get('editedDocs'), configServer.get('convertedDocs'));
        const curExt = fileUtility.getFileExtension(file.name);

        if (exts.indexOf(curExt) == -1) {
			docManager.cleanFolderRecursive(uploadDirTmp, true);
            res.writeHead(200, { "Content-Type": "text/plain" });
            res.write("{ \"error\": \"File type is not supported\"}");
            res.end();
            return;
        }

        fileSystem.rename(file.path, uploadDir + "/" + file.name, function (err) {
			docManager.cleanFolderRecursive(uploadDirTmp, true);
            res.writeHead(200, { "Content-Type": "text/plain" });
            if (err) {
                res.write("{ \"error\": \"" + err + "\"}");
            } else {
                res.write("{ \"filename\": \"" + file.name + "\"}");

                const userid = req.query.userid ? req.query.userid : "uid-1";
                const name = req.query.name ? req.query.name : "Jonn Smith";

                docManager.saveFileData(file.name, userid, name);
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
            console.log(e);
            writeResult(null, null, "Server error");
        }
    };

    try {
        if (configServer.get('convertedDocs').indexOf(fileExt) != -1) {
            const key = documentService.generateRevisionId(fileUri);
            documentService.getConvertedUri(fileUri, fileExt, internalFileExt, key, true, callback);
        } else {
            writeResult(fileName, null, null);
        }
    } catch (ex) {
        console.log(ex);
        writeResult(null, null, "Server error");
    }
});

app.delete("/file", function (req, res) {
    try {
    	docManager.init(__dirname, req, res);
        let fileName = req.query.filename;
        if (fileName) {
			fileName = fileUtility.getFileName(fileName);

			const filePath = docManager.storagePath(fileName);
			fileSystem.unlinkSync(filePath);

			const userAddress = docManager.curUserHostAddress();
			const historyPath = docManager.historyPath(fileName, userAddress, true);
			docManager.cleanFolderRecursive(historyPath, true);
		} else {
			docManager.cleanFolderRecursive(docManager.storagePath(''), false);
		}

        res.write("{\"success\":true}");
    } catch (ex) {
        console.log(ex);
        res.write("Server error");
    }
    res.end();
});

app.post("/track", function (req, res) {

    docManager.init(__dirname, req, res);

    var userAddress = req.query.useraddress;
    var fileName = fileUtility.getFileName(req.query.filename);
    var version = 0;

    var processTrack = function (response, body, fileName, userAddress) {

        var processSave = function (downloadUri, body, fileName, userAddress, resp) {
            var curExt = fileUtility.getFileExtension(fileName);
            var downloadExt = fileUtility.getFileExtension(downloadUri);

            if (downloadExt != curExt) {
                var key = documentService.generateRevisionId(downloadUri);

                try {
                    documentService.getConvertedUriSync(downloadUri, downloadExt, curExt, key, function (dUri) {
                        processSave(dUri, body, fileName, userAddress, resp)
                    });
                    return;
                } catch (ex) {
                    console.log(ex);
                    fileName = docManager.getCorrectName(fileUtility.getFileName(fileName, true) + downloadExt, userAddress)
                }
            }

            try {

                var path = docManager.storagePath(fileName, userAddress);

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

                var changeshistory = body.changeshistory || JSON.stringify(body.history);
                if (changeshistory) {
                    var path_changes_json = docManager.changesPath(fileName, userAddress, version);
                    fileSystem.writeFileSync(path_changes_json, changeshistory);
                }

                var path_key = docManager.keyPath(fileName, userAddress, version);
                fileSystem.writeFileSync(path_key, body.key);

                var path_prev = docManager.prevFilePath(fileName, userAddress, version);
                fileSystem.writeFileSync(path_prev, fileSystem.readFileSync(path));

                var file = syncRequest("GET", downloadUri);
                fileSystem.writeFileSync(path, file.getBody());

                var forcesavePath = docManager.forcesavePath(fileName, userAddress, false);
                if (forcesavePath != "") {
                    fileSystem.unlinkSync(forcesavePath);
                }
            } catch (ex) {
                console.log(ex);
            }

            response.write("{\"error\":0}");
            response.end();
        };

        var processForceSave = function (downloadUri, body, fileName, userAddress, resp) {
            var curExt = fileUtility.getFileExtension(fileName);
            var downloadExt = fileUtility.getFileExtension(downloadUri);

            if (downloadExt != curExt) {
                var key = documentService.generateRevisionId(downloadUri);

                try {
                    documentService.getConvertedUriSync(downloadUri, downloadExt, curExt, key, function (dUri) {
                        processForceSave(dUri, body, fileName, userAddress, resp)
                    });
                    return;
                } catch (ex) {
                    console.log(ex);
                    fileName = docManager.getCorrectName(fileUtility.getFileName(fileName, true) + downloadExt, userAddress)
                }
            }

            try {

                var path = docManager.storagePath(fileName, userAddress);

                var forcesavePath = docManager.forcesavePath(fileName, userAddress, false);
                if (forcesavePath == "") {
                    forcesavePath = docManager.forcesavePath(fileName, userAddress, true);
                }

                var file = syncRequest("GET", downloadUri);
                fileSystem.writeFileSync(forcesavePath, file.getBody());
            } catch (ex) {
                console.log(ex);
            }

            response.write("{\"error\":0}");
            response.end();
        };

        if (body.status == 1) { //Editing
            if (body.actions && body.actions[0].type == 0) { //finished edit
                var user = body.actions[0].userid;
                if (body.users.indexOf(user) == -1) {
                    var key = body.key;
                    try {
                        documentService.commandRequest("forcesave", key);
                    } catch (ex) {
                        console.log(ex);
                    }
                }
            }

        } else if (body.status == 2 || body.status == 3) { //MustSave, Corrupted
            processSave(body.url, body, fileName, userAddress, response);
            return;
        } else if (body.status == 6 || body.status == 7) { //MustForceSave, CorruptedForceSave
            processForceSave(body.url, body, fileName, userAddress, response);
            return;
        }

        response.write("{\"error\":0}");
        response.end();
    };

    var readbody = function (request, response, fileName, userAddress) {
        var content = "";
        request.on('data', function (data) {
            content += data;
        });
        request.on('end', function () {
            var body = JSON.parse(content);
            processTrack(response, body, fileName, userAddress);
        });
    };

    //checkjwt
    if (cfgSignatureEnable && cfgSignatureUseForRequest) {
        var checkJwtHeaderRes = documentService.checkJwtHeader(req);
        if (checkJwtHeaderRes) {
            if (checkJwtHeaderRes.payload) {
                body = checkJwtHeaderRes.payload;
            }
            if (checkJwtHeaderRes.query) {
                if (checkJwtHeaderRes.query.useraddress) {
                    userAddress = checkJwtHeaderRes.query.useraddress;
                }
                if (checkJwtHeaderRes.query.filename) {
                    fileName = fileUtility.getFileName(checkJwtHeaderRes.query.filename);
                }
            }
            processTrack(res, body, fileName, userAddress);
        } else {
            res.write("{\"error\":1}");
            res.end();
        }
        return;
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
        var historyData = [];
        var lang = docManager.getLang();
        var userid = req.query.userid ? req.query.userid : "uid-1";
        var name = req.query.name ? req.query.name : "Jonn Smith";

        if (fileExt != null) {
            var fileName = docManager.createDemo((req.query.sample ? "sample." : "new.") + fileExt, userid, name);

            var redirectPath = docManager.getServerUrl() + "/editor?fileName=" + encodeURIComponent(fileName) + docManager.getCustomParams();
            res.redirect(redirectPath);
            return;
        }

        var userAddress = docManager.curUserHostAddress();
        var fileName = fileUtility.getFileName(req.query.fileName);
        var key = docManager.getKey(fileName);
        var url = docManager.getFileUri(fileName);
        var mode = req.query.mode || "edit"; //mode: view/edit 
        var type = req.query.type || ""; //type: embedded/mobile/desktop
        if (type == "") {
                type = new RegExp(configServer.get("mobileRegEx"), "i").test(req.get('User-Agent')) ? "mobile" : "desktop";
            }

        var canEdit = configServer.get('editedDocs').indexOf(fileUtility.getFileExtension(fileName)) != -1;

        var countVersion = 1;

        var historyPath = docManager.historyPath(fileName, userAddress);
        var changes = null;
        var keyVersion = key;

        if (historyPath != '') {

            countVersion = docManager.countVersion(historyPath) + 1;
            for (var i = 1; i <= countVersion; i++) {
                if (i < countVersion) {
                    var keyPath = docManager.keyPath(fileName, userAddress, i);
                    keyVersion = "" + fileSystem.readFileSync(keyPath);
                } else {
                    keyVersion = key;
                }
                history.push(docManager.getHistory(fileName, changes, keyVersion, i));

                var historyD = {
                    version: i,
                    key: keyVersion,
                    url: i == countVersion ? url : (docManager.getlocalFileUri(fileName, i, true) + "/prev" + fileUtility.getFileExtension(fileName)),
                };
                if (i > 1) {
                    historyD.previous = {
                        key: historyData[i-2].key,
                        url: historyData[i-2].url,
                        
                    };
                    historyD.changesUrl = docManager.getlocalFileUri(fileName, i-1) + "/diff.zip";
                }
                historyData.push(historyD);
                
                if (i < countVersion) {
                    var changesFile = docManager.changesPath(fileName, userAddress, i);
                    changes = docManager.getChanges(changesFile);
                }
            }
        } else {
            history.push(docManager.getHistory(fileName, changes, keyVersion, countVersion));
            historyData.push({
                version: countVersion,
                key: key,
                url: url
            });
        }

        if (cfgSignatureEnable) {
            for (var i = 0; i < historyData.length; i++) {
                historyData[i].token = jwt.sign(historyData[i], cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});
            }
        }

        var argss = {
            apiUrl: siteUrl + configServer.get('apiUrl'),
            file: {
                name: fileName,
                ext: fileUtility.getFileExtension(fileName, true),
                uri: url,
                version: countVersion,
                created: new Date().toDateString()
            },
            editor: {
                type: type,
                documentType: fileUtility.getFileType(fileName),
                key: key,
                token: "",
                callbackUrl: docManager.getCallback(fileName),
                isEdit: canEdit && mode != "review",
                mode: canEdit && mode != "view" ? "edit" : "view",
                canBackToFolder: type != "embedded",
                backUrl: docManager.getServerUrl(),
                curUserHostAddress: docManager.curUserHostAddress(),
                lang: lang,
                userid: userid,
                name: name,
                fileChoiceUrl: fileChoiceUrl,
                plugins: JSON.stringify(plugins)
            },
            history: history,
            historyData: historyData
        };

        if (cfgSignatureEnable) {
            app.render('config', argss, function(err, html){
                if (err) {
                    console.log(err);
                } else {
                    argss.editor.token = jwt.sign(JSON.parse("{"+html+"}"), cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});
                }
                res.render("editor", argss);
              });
        } else {
              res.render("editor", argss);
        }
    }
    catch (ex) {
        console.log(ex);
        res.status(500);
        res.render("error", { message: "Server error" });
    }
});

app.use(function (req, res, next) {
    const err = new Error("Not Found");
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
