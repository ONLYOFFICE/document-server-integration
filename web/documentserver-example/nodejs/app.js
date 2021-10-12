"use strict";
/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

// connect the necessary packages and modules
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
const storageFolder = configServer.get("storageFolder");
const mime = require("mime");
const docManager = require("./helpers/docManager");
const documentService = require("./helpers/documentService");
const fileUtility = require("./helpers/fileUtility");
const wopiApp = require("./helpers/wopi/wopiRouting");
const users = require("./helpers/users");
const siteUrl = configServer.get('siteUrl');
const fileChoiceUrl = configServer.has('fileChoiceUrl') ? configServer.get('fileChoiceUrl') : "";
const plugins = config.get('plugins');
const cfgSignatureEnable = configServer.get('token.enable');
const cfgSignatureUseForRequest = configServer.get('token.useforrequest');
const cfgSignatureAuthorizationHeader = configServer.get('token.authorizationHeader');
const cfgSignatureAuthorizationHeaderPrefix = configServer.get('token.authorizationHeaderPrefix');
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


const app = express();  // create an application object
app.set("views", path.join(__dirname, "views"));  // specify the path to the main template
app.set("view engine", "ejs");  // specify which template engine is used


app.use(function (req, res, next) {
    res.setHeader('Access-Control-Allow-Origin', '*');  // allow any Internet domain to access the resources of this site
    next();
});

app.use(express.static(path.join(__dirname, "public")));  // public directory
if (config.has('server.static')) {  // check if there are static files such as .js, .css files, images, samples and process them
  const staticContent = config.get('server.static');
  for (let i = 0; i < staticContent.length; ++i) {
    const staticContentElem = staticContent[i];
    app.use(staticContentElem['name'], express.static(staticContentElem['path'], staticContentElem['options']));
  }
}
app.use(favicon(__dirname + "/public/images/favicon.ico"));  // use favicon


app.use(bodyParser.json());  // connect middleware that parses json
app.use(bodyParser.urlencoded({ extended: false }));  // connect middleware that parses urlencoded bodies


app.get("/", function (req, res) {  // define a handler for default page
    try {

        docManager.init(storageFolder, req, res);

        res.render("index", {  // render index template with the parameters specified
            preloaderUrl: siteUrl + configServer.get('preloaderUrl'),
            convertExts: configServer.get('convertedDocs').join(","),
            editedExts: configServer.get('editedDocs').join(","),
            storedFiles: docManager.getStoredFiles(),
            params: docManager.getCustomParams(),
            users: users,
            serverUrl: docManager.getServerUrl(),
        });

    }
    catch (ex) {
        console.log(ex);  // display error message in the console
        res.status(500);  // write status parameter to the response
        res.render("error", { message: "Server error" });  // render error template with the message parameter specified
        return;
    }
});

app.get("/download", function(req, res) {  // define a handler for downloading files
    docManager.init(storageFolder, req, res);

    var fileName = fileUtility.getFileName(req.query.fileName);
    var userAddress = req.query.useraddress;

    if (cfgSignatureEnable && cfgSignatureUseForRequest) { 
        var authorization = req.get(cfgSignatureAuthorizationHeader);
        if (authorization && authorization.startsWith(cfgSignatureAuthorizationHeaderPrefix)) {
            var token = authorization.substring(cfgSignatureAuthorizationHeaderPrefix.length);
            try {
                var decoded = jwt.verify(token, cfgSignatureSecret);
            } catch (err) {
                console.log('checkJwtHeader error: name = ' + err.name + ' message = ' + err.message + ' token = ' + token)
                res.sendStatus(403);
                return;
            }
        }
    }

    var path = docManager.forcesavePath(fileName, userAddress, false);  // get the path to the force saved document version
    if (path == "") {
        path = docManager.storagePath(fileName, userAddress);  // or to the original document
    }

    res.setHeader("Content-Length", fileSystem.statSync(path).size);  // add headers to the response to specify the page parameters
    res.setHeader("Content-Type", mime.getType(path));

    res.setHeader("Content-Disposition", "attachment; filename*=UTF-8\'\'" + encodeURIComponent(fileName));

    var filestream = fileSystem.createReadStream(path);
    filestream.pipe(res);  // send file information to the response by streams
});

app.post("/upload", function (req, res) {  // define a handler for uploading files

    docManager.init(storageFolder, req, res);
    docManager.storagePath(""); // mkdir if not exist

    const userIp = docManager.curUserHostAddress();  // get the path to the user host
    const uploadDir = path.join(storageFolder, userIp);
    const uploadDirTmp = path.join(uploadDir, 'tmp');  // and create directory for temporary files if it doesn't exist
    docManager.createDirectory(uploadDirTmp);

    const form = new formidable.IncomingForm();  // create a new incoming form
    form.uploadDir = uploadDirTmp;  // and write there all the necessary parameters
    form.keepExtensions = true;
    form.maxFileSize = configServer.get("maxFileSize");

    form.parse(req, function (err, fields, files) {  // parse this form 
    	if (err) {  // if an error occurs
			docManager.cleanFolderRecursive(uploadDirTmp, true);  // clean the folder with temporary files
			res.writeHead(200, { "Content-Type": "text/plain" });  // and write the error status and message to the response
			res.write("{ \"error\": \"" + err.message + "\"}");
			res.end();
			return;
		}

        const file = files.uploadedFile;

        if (file == undefined) {  // if file parameter is undefined
            res.writeHead(200, { "Content-Type": "text/plain" });  // write the error status and message to the response
            res.write("{ \"error\": \"Uploaded file not found\"}");
            res.end();
            return;
        }

        file.name = docManager.getCorrectName(file.name);

        if (configServer.get('maxFileSize') < file.size || file.size <= 0) {  // check if the file size exceeds the maximum file size
			docManager.cleanFolderRecursive(uploadDirTmp, true);  // clean the folder with temporary files
            res.writeHead(200, { "Content-Type": "text/plain" });
            res.write("{ \"error\": \"File size is incorrect\"}");
            res.end();
            return;
        }

        const exts = [].concat(configServer.get('viewedDocs'), configServer.get('editedDocs'), configServer.get('convertedDocs'));  // all the supported file extensions
        const curExt = fileUtility.getFileExtension(file.name);
        const documentType = fileUtility.getFileType(file.name);

        if (exts.indexOf(curExt) == -1) {  // check if the file extension is supported
			docManager.cleanFolderRecursive(uploadDirTmp, true);  // if not, clean the folder with temporary files
            res.writeHead(200, { "Content-Type": "text/plain" });  // and write the error status and message to the response
            res.write("{ \"error\": \"File type is not supported\"}");
            res.end();
            return;
        }

        fileSystem.rename(file.path, uploadDir + "/" + file.name, function (err) {  // rename a file
			docManager.cleanFolderRecursive(uploadDirTmp, true);  // clean the folder with temporary files
            res.writeHead(200, { "Content-Type": "text/plain" });
            if (err) {  // if an error occurs
                res.write("{ \"error\": \"" + err + "\"}");  // write an error message to the response
            } else {
                res.write("{ \"filename\": \"" + file.name + "\", \"documentType\": \"" + documentType + "\" }");  // otherwise, write a new file name to the response

                var user = users.getUser(req.query.userid); // get user id and name parameters or set them to the default values

                docManager.saveFileData(file.name, user.id, user.name);
            }
            res.end();
        });
    });
});

app.post("/create", function (req, res) {
    var title = req.body.title;
    var fileUrl = req.body.url;

    try {
        docManager.init(storageFolder, req, res);
        docManager.storagePath(""); // mkdir if not exist

        var fileName = docManager.getCorrectName(title);
        var userAddress = docManager.curUserHostAddress();
        docManager.historyPath(fileName, userAddress, true);

        var file = syncRequest("GET", fileUrl);
        var fileBody = file.getBody();

        if (configServer.get("maxFileSize") < fileBody.length || fileBody.length <= 0) {  // check if the file size exceeds the maximum file size
            res.writeHead(200, { "Content-Type": "application/json" });
            res.write(JSON.stringify({ "error": "File size is incorrect" }));
            res.end();
            return;
        }

        const exts = [].concat(configServer.get("viewedDocs"), configServer.get("editedDocs"), configServer.get("convertedDocs"));  // all the supported file extensions
        const curExt = fileUtility.getFileExtension(fileName);

        if (exts.indexOf(curExt) == -1) {  // check if the file extension is supported
            res.writeHead(200, { "Content-Type": "application/json" });  // and write the error status and message to the response
            res.write(JSON.stringify({ "error": "File type is not supported" }));
            res.end();
            return;
        }

        fileSystem.writeFileSync(docManager.storagePath(fileName), fileBody);

        res.writeHead(200, { "Content-Type": "application/json" });
        res.write(JSON.stringify({ "file" : fileName }));
        res.end();

    } catch (e) {
        res.status(500);
        res.write(JSON.stringify({
            error: 1,
            message: e.message
        }));
        res.end();
    }
});

app.post("/convert", function (req, res) {  // define a handler for converting files

    var fileName = fileUtility.getFileName(req.body.filename);
    var filePass = req.body.filePass ? req.body.filePass : null;
    var lang = req.body.lang ? req.body.lang : null;
    var fileUri = docManager.getFileUri(fileName);
    var fileExt = fileUtility.getFileExtension(fileName);
    var fileType = fileUtility.getFileType(fileName);
    var internalFileExt = docManager.getInternalExtension(fileType);
    var response = res;

    var writeResult = function (filename, step, error) {
        var result = {};

        // write file name, step and error values to the result object if they are defined
        if (filename != null)
            result["filename"] = filename;

        if (step != null)
            result["step"] = step;

        if (error != null)
            result["error"] = error;

        response.setHeader("Content-Type", "application/json");
        response.write(JSON.stringify(result));
        response.end();
    };

    var callback = function (err, data) {
        if (err) {  // if an error occurs
            if (err.name === "ConnectionTimeoutError" || err.name === "ResponseTimeoutError") {  // check what type of error it is
                writeResult(fileName, 0, null);  // despite the timeout errors, write the file to the result object
            } else {
                writeResult(null, null, JSON.stringify(err));  // other errors trigger an error message
            }
            return;
        }

        try {
            var responseUri = documentService.getResponseUri(data.toString());
            var result = responseUri.key;
            var newFileUri = responseUri.value;  // get the callback url

            if (result != 100) {  // if the status isn't 100
                writeResult(fileName, result, null);  // write the origin file to the result object
                return;
            }

            var correctName = docManager.getCorrectName(fileUtility.getFileName(fileName, true) + internalFileExt);  // get the file name with a new extension

            var file = syncRequest("GET", newFileUri);
            fileSystem.writeFileSync(docManager.storagePath(correctName), file.getBody());  // write a file with a new extension, but with the content from the origin file

            fileSystem.unlinkSync(docManager.storagePath(fileName));  // remove file with the origin extension

            var userAddress = docManager.curUserHostAddress();
            var historyPath = docManager.historyPath(fileName, userAddress, true);
            var correctHistoryPath = docManager.historyPath(correctName, userAddress, true);  // get the history path to the file with a new extension

            fileSystem.renameSync(historyPath, correctHistoryPath);  // change the previous history path 

            fileSystem.renameSync(path.join(correctHistoryPath, fileName + ".txt"), path.join(correctHistoryPath, correctName + ".txt"));  // change the name of the .txt file with document information

            writeResult(correctName, result, null);  // write a file with a new name to the result object
        } catch (e) {
            console.log(e);  // display error message in the console
            writeResult(null, null, e.message);
        }
    };

    try {
        if (configServer.get('convertedDocs').indexOf(fileExt) != -1) {  // check if the file with such an extension can be converted
            let storagePath = docManager.storagePath(fileName);
            const stat = fileSystem.statSync(storagePath);
            let key = fileUri + stat.mtime.getTime();

            key = documentService.generateRevisionId(key);  // get document key
            documentService.getConvertedUri(fileUri, fileExt, internalFileExt, key, true, callback, filePass, lang);  // get the url to the converted file
        } else {
            writeResult(fileName, null, null);  // if the file with such an extension can't be converted, write the origin file to the result object
        }
    } catch (ex) {
        console.log(ex);
        writeResult(null, null, "Server error");
    }
});

app.get("/files", function(req, res) {  // define a handler for getting files information
    try {
        docManager.init(storageFolder, req, res); 
        const filesInDirectoryInfo = docManager.getFilesInfo();  // get the information about the files from the storage path
        res.setHeader("Content-Type", "application/json");
        res.write(JSON.stringify(filesInDirectoryInfo));  // transform files information into the json string
    } catch (ex) {
        console.log(ex);
        res.write("Server error");
    }
    res.end();
});

app.get("/files/file/:fileId", function(req, res) {  // define a handler for getting file information by its id
    try {
        docManager.init(storageFolder, req, res);
        const fileId = req.params.fileId;
        const fileInfoById = docManager.getFilesInfo(fileId);  // get the information about the file specified by a file id
        res.setHeader("Content-Type", "application/json");
        res.write(JSON.stringify(fileInfoById));
    } catch (ex) {
        console.log(ex);
        res.write("Server error");
    }
    res.end();
});

app.delete("/file", function (req, res) {  // define a handler for removing file
    try {
    	docManager.init(storageFolder, req, res);
        let fileName = req.query.filename;
        if (fileName) {  // if the file name is defined
			fileName = fileUtility.getFileName(fileName);  // get its part without an extension

			const filePath = docManager.storagePath(fileName);  // get the path to this file
			fileSystem.unlinkSync(filePath);  // and delete it

			const userAddress = docManager.curUserHostAddress();
			const historyPath = docManager.historyPath(fileName, userAddress, true);
			docManager.cleanFolderRecursive(historyPath, true);  // clean all the files from the history folder
		} else {
			docManager.cleanFolderRecursive(docManager.storagePath(''), false);  // if the file name is undefined, clean the storage folder
		}

        res.write("{\"success\":true}");
    } catch (ex) {
        console.log(ex);
        res.write("Server error");
    }
    res.end();
});

app.get("/csv", function (req, res) {  // define a handler for downloading csv files
    var fileName = "csv.csv";
    var csvPath = path.join(__dirname, "public", "assets",  "sample", fileName);

    res.setHeader("Content-Length", fileSystem.statSync(csvPath).size);  // add headers to the response to specify the page parameters
    res.setHeader("Content-Type", mime.getType(csvPath));

    res.setHeader("Content-Disposition", "attachment; filename*=UTF-8\'\'" + encodeURIComponent(fileName));

    var filestream = fileSystem.createReadStream(csvPath);
    filestream.pipe(res);  // send file information to the response by streams
})

app.post("/track", function (req, res) {  // define a handler for tracking file changes

    docManager.init(storageFolder, req, res);

    var userAddress = req.query.useraddress;
    var fileName = fileUtility.getFileName(req.query.filename);
    var version = 0;

    // track file changes
    var processTrack = function (response, body, fileName, userAddress) {

        // callback file saving process
        var callbackProcessSave = function (downloadUri, body, fileName, userAddress, newFileName) {
            try {
                var storagePath = docManager.storagePath(newFileName, userAddress);

                var historyPath = docManager.historyPath(newFileName, userAddress);  // get the path to the history data
                if (historyPath == "") {  // if the history path doesn't exist
                    historyPath = docManager.historyPath(newFileName, userAddress, true);  // create it
                    docManager.createDirectory(historyPath);  // and create a directory for the history data
                }

                var count_version = docManager.countVersion(historyPath);  // get the next file version number
                version = count_version + 1;
                var versionPath = docManager.versionPath(newFileName, userAddress, version);  // get the path to the specified file version
                docManager.createDirectory(versionPath);  // create a directory to the specified file version

                var downloadZip = body.changesurl;
                if (downloadZip) {
                    var path_changes = docManager.diffPath(newFileName, userAddress, version);  // get the path to the file with document versions differences
                    var diffZip = syncRequest("GET", downloadZip);
                    fileSystem.writeFileSync(path_changes, diffZip.getBody());  // write the document version differences to the archive
                }

                var changeshistory = body.changeshistory || JSON.stringify(body.history);
                if (changeshistory) {
                    var path_changes_json = docManager.changesPath(newFileName, userAddress, version);  // get the path to the file with document changes
                    fileSystem.writeFileSync(path_changes_json, changeshistory);  // and write this data to the path in json format
                }

                var path_key = docManager.keyPath(newFileName, userAddress, version);  // get the path to the key.txt file
                fileSystem.writeFileSync(path_key, body.key);  // write the key value to the key.txt file

                var path_prev = path.join(versionPath, "prev" + fileUtility.getFileExtension(fileName));  // get the path to the previous file version
                fileSystem.renameSync(docManager.storagePath(fileName, userAddress), path_prev);  // and write it to the current path

                var file = syncRequest("GET", downloadUri);
                fileSystem.writeFileSync(storagePath, file.getBody());

                var forcesavePath = docManager.forcesavePath(newFileName, userAddress, false);  // get the path to the forcesaved file
                if (forcesavePath != "") {  // if this path is empty
                    fileSystem.unlinkSync(forcesavePath);  // remove it
                }

            } catch (ex) {
                response.write("{\"error\":1}");
                response.end();
                return;
            }

            response.write("{\"error\":0}");
            response.end();
        }

        // file saving process
        var processSave = function (downloadUri, body, fileName, userAddress) {

            if (!downloadUri) {
                response.write("{\"error\":1}");
                response.end();
                return;
            }

            var curExt = fileUtility.getFileExtension(fileName);  // get current file extension
            var downloadExt = fileUtility.getFileExtension(downloadUri);  // get the extension of the downloaded file
            var newFileName = fileName;

            // convert downloaded file to the file with the current extension if these extensions aren't equal
            if (downloadExt != curExt) {
                var key = documentService.generateRevisionId(downloadUri);
                newFileName = docManager.getCorrectName(fileUtility.getFileName(fileName, true) + downloadExt, userAddress);  // get the correct file name if it already exists
                try {
                    documentService.getConvertedUriSync(downloadUri, downloadExt, curExt, key, function (err, data) {
                        if (err) {
                            callbackProcessSave(downloadUri, body, fileName, userAddress, newFileName);
                            return;
                        }
                        try {
                            var res = documentService.getResponseUri(data);
                            callbackProcessSave(res.value, body, fileName, userAddress, fileName);
                            return;
                        } catch (ex) {
                            console.log(ex);
                            callbackProcessSave(downloadUri, body, fileName, userAddress, newFileName);
                            return;
                        }
                    });
                    return;
                } catch (ex) {
                    console.log(ex);
                }
            }
            callbackProcessSave(downloadUri, body, fileName, userAddress, newFileName);
        };

        // callback file force saving process
        var callbackProcessForceSave = function (downloadUri, body, fileName, userAddress, newFileName = false){
            try {
                var downloadExt = fileUtility.getFileExtension(downloadUri);
                var isSubmitForm = body.forcesavetype === 3; // SubmitForm

                if (isSubmitForm) {
                    // new file
                    if (newFileName){
                        fileName = docManager.getCorrectName(fileUtility.getFileName(fileName, true) + "-form" + downloadExt, userAddress);
                    } else {
                        var ext = fileUtility.getFileExtension(fileName);
                        fileName = docManager.getCorrectName(fileUtility.getFileName(fileName, true) + "-form" + ext, userAddress);
                    }
                    var forcesavePath = docManager.storagePath(fileName, userAddress);
                } else {
                    if (newFileName){
                        fileName = docManager.getCorrectName(fileUtility.getFileName(fileName, true) + downloadExt, userAddress);
                    }
                    // create forcesave path if it doesn't exist
                    forcesavePath = docManager.forcesavePath(fileName, userAddress, false);
                    if (forcesavePath == "") {
                        forcesavePath = docManager.forcesavePath(fileName, userAddress, true);
                    }
                }

                var file = syncRequest("GET", downloadUri);
                fileSystem.writeFileSync(forcesavePath, file.getBody());

                if (isSubmitForm) {
                    var uid =body.actions[0].userid
                    docManager.saveFileData(fileName, uid, "Filling Form", userAddress);
                }
            } catch (ex) {
                response.write("{\"error\":1}");
                response.end();
                return;
            }

            response.write("{\"error\":0}");
            response.end();
        }

        // file force saving process
        var processForceSave = function (downloadUri, body, fileName, userAddress) {

            if (!downloadUri) {
                response.write("{\"error\":1}");
                response.end();
                return;
            }

            var curExt = fileUtility.getFileExtension(fileName);
            var downloadExt = fileUtility.getFileExtension(downloadUri);

            // convert downloaded file to the file with the current extension if these extensions aren't equal
            if (downloadExt != curExt) {
                var key = documentService.generateRevisionId(downloadUri);
                try {
                    documentService.getConvertedUriSync(downloadUri, downloadExt, curExt, key, function (err, data) {
                        if (err) {
                            callbackProcessForceSave(downloadUri, body, fileName, userAddress, true);
                            return;
                        }
                        try {
                            var res = documentService.getResponseUri(data);
                            callbackProcessForceSave(res.value, body, fileName, userAddress, false);
                            return;
                        } catch (ex) {
                            console.log(ex);
                            callbackProcessForceSave(downloadUri, body, fileName, userAddress, true);
                            return;
                        }
                    });
                    return;
                } catch (ex) {
                    console.log(ex);
                }
            }
            callbackProcessForceSave (downloadUri, body, fileName, userAddress, false);
        };

        if (body.status == 1) { // editing
            if (body.actions && body.actions[0].type == 0) { // finished edit
                var user = body.actions[0].userid;
                if (body.users.indexOf(user) == -1) {
                    var key = body.key;
                    try {
                        documentService.commandRequest("forcesave", key);  // call the forcesave command
                    } catch (ex) {
                        console.log(ex);
                    }
                }
            }
        } else if (body.status == 2 || body.status == 3) { // MustSave, Corrupted
            processSave(body.url, body, fileName, userAddress);  // save file
            return;
        } else if (body.status == 6 || body.status == 7) { // MustForceSave, CorruptedForceSave
            processForceSave(body.url, body, fileName, userAddress);  // force save file
            return;
        }

        response.write("{\"error\":0}");
        response.end();
    };

    // read request body
    var readbody = function (request, response, fileName, userAddress) {
        var content = "";
        request.on('data', function (data) {  // get data from the request
            content += data;
        });
        request.on('end', function () {
            var body = JSON.parse(content);
            processTrack(response, body, fileName, userAddress);  // and track file changes
        });
    };

    // check jwt token
    if (cfgSignatureEnable && cfgSignatureUseForRequest) {
        var body = null;
        if (req.body.hasOwnProperty("token")) {  // if request body has its own token
            body = documentService.readToken(req.body.token);  // read and verify it
        } else {
            var checkJwtHeaderRes = documentService.checkJwtHeader(req);  // otherwise, check jwt token headers
            if (checkJwtHeaderRes) {  // if they exist
                var body;
                if (checkJwtHeaderRes.payload) {
                    body = checkJwtHeaderRes.payload;  // get the payload object
                }
                // get user address and file name from the query
                if (checkJwtHeaderRes.query) {
                    if (checkJwtHeaderRes.query.useraddress) {
                        userAddress = checkJwtHeaderRes.query.useraddress;
                    }
                    if (checkJwtHeaderRes.query.filename) {
                        fileName = fileUtility.getFileName(checkJwtHeaderRes.query.filename);
                    }
                }
            }
        }
        if (body == null) {
            res.write("{\"error\":1}");
            res.end();
            return;
        }
        processTrack(res, body, fileName, userAddress);
        return;
    }

    if (req.body.hasOwnProperty("status")) {  // if the request body has status parameter
        processTrack(res, req.body, fileName, userAddress);  // track file changes
    } else {
        readbody(req, res, fileName, userAddress);  // otherwise, read request body first
    }
});

app.get("/editor", function (req, res) {  // define a handler for editing document
    try {

        docManager.init(storageFolder, req, res);

        var fileName = fileUtility.getFileName(req.query.fileName);
        var fileExt = req.query.fileExt;
        var history = [];
        var historyData = [];
        var lang = docManager.getLang();
        var user = users.getUser(req.query.userid);

        var userid = user.id;
        var name = user.name;
        var actionData = req.query.action ? req.query.action : "null";

        var templatesImageUrl = docManager.getTemplateImageUrl(fileUtility.getFileType(fileName));
        var createUrl = docManager.getCreateUrl(fileUtility.getFileType(fileName), userid, type, lang);
        var templates = [
            {
                "image": "",
                "title": "Blank",
                "url": createUrl
            },
            {
                "image": templatesImageUrl,
                "title": "With sample content",
                "url": createUrl + "&sample=true"
            }
        ];

        var userGroup = user.group;
        var reviewGroups = user.reviewGroups;
        var commentGroups = user.commentGroups;

        if (fileExt != null) {
            var fileName = docManager.createDemo(!!req.query.sample, fileExt, userid, name);  // create demo document of a given extension

            // get the redirect path
            var redirectPath = docManager.getServerUrl() + "/editor?fileName=" + encodeURIComponent(fileName) + docManager.getCustomParams();
            res.redirect(redirectPath);
            return;
        }

        var userAddress = docManager.curUserHostAddress();
        if (!docManager.existsSync(docManager.storagePath(fileName, userAddress))) {  // if the file with a given name doesn't exist
            throw { 
                "message": "File not found: " + fileName  // display error message
            };
        }
        var key = docManager.getKey(fileName);
        var url = docManager.getDownloadUrl(fileName);
        var urlUser = docManager.getlocalFileUri(fileName, 0, false)
        var mode = req.query.mode || "edit"; // mode: view/edit/review/comment/fillForms/embedded
        var type = req.query.type || ""; // type: embedded/mobile/desktop
        if (type == "") {
                type = new RegExp(configServer.get("mobileRegEx"), "i").test(req.get('User-Agent')) ? "mobile" : "desktop";
            }

        var canEdit = configServer.get('editedDocs').indexOf(fileUtility.getFileExtension(fileName)) != -1;  // check if this file can be edited
        var submitForm = canEdit && (mode == "edit" || mode == "fillForms");

        var countVersion = 1;

        var historyPath = docManager.historyPath(fileName, userAddress);
        var changes = null;
        var keyVersion = key;

        if (historyPath != '') {

            countVersion = docManager.countVersion(historyPath) + 1;  // get the number of file versions
            for (var i = 1; i <= countVersion; i++) {  // get keys to all the file versions
                if (i < countVersion) {
                    var keyPath = docManager.keyPath(fileName, userAddress, i);
                    if (!fileSystem.existsSync(keyPath)) continue;
                    keyVersion = "" + fileSystem.readFileSync(keyPath);
                } else {
                    keyVersion = key;
                }
                history.push(docManager.getHistory(fileName, changes, keyVersion, i));  // write all the file history information

                var historyD = {
                    version: i,
                    key: keyVersion,
                    url: i == countVersion ? url : (docManager.getlocalFileUri(fileName, i, true) + "/prev" + fileUtility.getFileExtension(fileName)),
                };

                if (i > 1 && docManager.existsSync(docManager.diffPath(fileName, userAddress, i-1))) {  // check if the path to the file with document versions differences exists
                    historyD.previous = {  // write information about previous file version
                        key: historyData[i-2].key,
                        url: historyData[i-2].url,
                    };
                    historyD.changesUrl = docManager.getlocalFileUri(fileName, i-1) + "/diff.zip";  // get the path to the diff.zip file and write it to the history object
                }

                historyData.push(historyD);
                
                if (i < countVersion) {
                    var changesFile = docManager.changesPath(fileName, userAddress, i);  // get the path to the file with document changes
                    changes = docManager.getChanges(changesFile);  // get changes made in the file
                }
            }
        } else {  // if history path is empty
            history.push(docManager.getHistory(fileName, changes, keyVersion, countVersion));  // write the history information about the last file version
            historyData.push({
                version: countVersion,
                key: key,
                url: url
            });
        }

        if (cfgSignatureEnable) {
            for (var i = 0; i < historyData.length; i++) {
                historyData[i].token = jwt.sign(historyData[i], cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});  // sign token with given data using signature secret
            }
        }

        // file config data
        var argss = {
            apiUrl: siteUrl + configServer.get('apiUrl'),
            file: {
                name: fileName,
                ext: fileUtility.getFileExtension(fileName, true),
                uri: url,
                uriUser: urlUser,
                version: countVersion,
                created: new Date().toDateString(),
                favorite: user.favorite != null ? user.favorite : "null"
            },
            editor: {
                type: type,
                documentType: fileUtility.getFileType(fileName),
                key: key,
                token: "",
                callbackUrl: docManager.getCallback(fileName),
                createUrl: userid != "uid-0" ? createUrl : null,
                templates: user.templates ? templates : null,
                isEdit: canEdit && (mode == "edit" || mode == "view" || mode == "filter" || mode == "blockcontent"),
                review: canEdit && (mode == "edit" || mode == "review"),
                comment: mode != "view" && mode != "fillForms" && mode != "embedded" && mode != "blockcontent",
                fillForms: mode != "view" && mode != "comment" && mode != "embedded" && mode != "blockcontent",
                modifyFilter: mode != "filter",
                modifyContentControl: mode != "blockcontent",
                copy: !user.deniedPermissions.includes("copy"),
                download: !user.deniedPermissions.includes("download"),
                print: !user.deniedPermissions.includes("print"),
                mode: canEdit && mode != "view" ? "edit" : "view",
                canBackToFolder: type != "embedded",
                backUrl: docManager.getServerUrl() + "/",
                curUserHostAddress: docManager.curUserHostAddress(),
                lang: lang,
                userid: userid,
                name: name,
                userGroup: userGroup,
                reviewGroups: JSON.stringify(reviewGroups),
                commentGroups: JSON.stringify(commentGroups),
                fileChoiceUrl: fileChoiceUrl,
                submitForm: submitForm,
                plugins: JSON.stringify(plugins),
                actionData: actionData
            },
            history: history,
            historyData: historyData,
            dataInsertImage: {
                fileType: "png",
                url: docManager.getServerUrl(true) + "/images/logo.png"
            },
            dataCompareFile: {
                fileType: "docx",
                url: docManager.getServerUrl(true) + "/assets/sample/sample.docx"
            },
            dataMailMergeRecipients: {
                fileType: "csv",
                url: docManager.getServerUrl(true) + "/csv"
            },
            usersForMentions: user.id != "uid-0" ? users.getUsersForMentions(user.id) : null,
        };

        if (cfgSignatureEnable) {
            app.render('config', argss, function(err, html){  // render a config template with the parameters specified
                if (err) {
                    console.log(err);
                } else {
                    // sign token with given data using signature secret
                    argss.editor.token = jwt.sign(JSON.parse("{"+html+"}"), cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});
                    argss.dataInsertImage.token = jwt.sign(argss.dataInsertImage, cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});
                    argss.dataCompareFile.token = jwt.sign(argss.dataCompareFile, cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});
                    argss.dataMailMergeRecipients.token = jwt.sign(argss.dataMailMergeRecipients, cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});
                }
                res.render("editor", argss);  // render the editor template with the parameters specified
              });
        } else {
              res.render("editor", argss);
        }
    }
    catch (ex) {
        console.log(ex);
        res.status(500);
        res.render("error", { message: "Server error: " + ex.message });
    }
});

wopiApp.registerRoutes(app);

// "Not found" error with 404 status
app.use(function (req, res, next) {
    const err = new Error("Not Found");
    err.status = 404;
    next(err);
});

// render the error template with the parameters specified
app.use(function (err, req, res, next) {
    res.status(err.status || 500);
    res.render("error", {
        message: err.message
    });
});

// save all the functions to the app module to export it later in other files
module.exports = app;