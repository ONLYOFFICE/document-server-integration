'use strict';
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

// connect the necessary packages and modules
const express = require('express');
const path = require('path');
const favicon = require('serve-favicon');
const bodyParser = require('body-parser');
const fileSystem = require('fs');
const formidable = require('formidable');
const jwt = require('jsonwebtoken');
const config = require('config');
const configServer = config.get('server');
const storageFolder = configServer.get('storageFolder');
const mime = require('mime');
const docManager = require('./helpers/docManager');
const documentService = require('./helpers/documentService');
const fileUtility = require('./helpers/fileUtility');
const wopiApp = require('./helpers/wopi/wopiRouting');
const users = require('./helpers/users');
const siteUrl = configServer.get('siteUrl');
const fileChoiceUrl = configServer.has('fileChoiceUrl') ? configServer.get('fileChoiceUrl') : '';
const plugins = config.get('plugins');
const cfgSignatureEnable = configServer.get('token.enable');
const cfgSignatureUseForRequest = configServer.get('token.useforrequest');
const cfgSignatureAuthorizationHeader = configServer.get('token.authorizationHeader');
const cfgSignatureAuthorizationHeaderPrefix = configServer.get('token.authorizationHeaderPrefix');
const cfgSignatureSecretExpiresIn = configServer.get('token.expiresIn');
const cfgSignatureSecret = configServer.get('token.secret');
const urllib = require('urllib');
const { emitWarning } = require('process');
const verifyPeerOff = configServer.get('verify_peer_off');

if(verifyPeerOff) {
    process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
}

String.prototype.hashCode = function () {
	const len = this.length;
	let ret = 0;
    for (let i = 0; i < len; i += 1) {
        ret = Math.trunc(31 * ret + this.charCodeAt(i));
    }
    return ret;
};
String.prototype.format = function (...args) {
    let text = this.toString();

    if (!args.length) return text;

    for (let i = 0; i < args.length; i += 1) {
        text = text.replace(new RegExp(`\\{${  i  }\\}`, 'gi'), args[i]);
    }

    return text;
};


const app = express();  // create an application object
app.disable('x-powered-by');
app.set('views', path.join(__dirname, 'views'));  // specify the path to the main template
app.set('view engine', 'ejs');  // specify which template engine is used


app.use((req, res, next) => {
    res.setHeader('Access-Control-Allow-Origin', '*');  // allow any Internet domain to access the resources of this site
    next();
});

app.use(express.static(path.join(__dirname, 'public')));  // public directory
if (config.has('server.static')) {  // check if there are static files such as .js, .css files, images, samples and process them
  const staticContent = config.get('server.static');
  for (let i = 0; i < staticContent.length; i += 1) {
    const staticContentElem = staticContent[i];
    app.use(staticContentElem.name, express.static(staticContentElem.path, staticContentElem.options));
  }
}
app.use(favicon(`${__dirname  }/public/images/favicon.ico`));  // use favicon


app.use(bodyParser.json());  // connect middleware that parses json
app.use(bodyParser.urlencoded({ extended: false }));  // connect middleware that parses urlencoded bodies


app.get('/', (req, res) => {  // define a handler for default page
    try {

        req.docManager = new docManager(req, res);

        res.render('index', {  // render index template with the parameters specified
            preloaderUrl: siteUrl + configServer.get('preloaderUrl'),
            convertExts: configServer.get('convertedDocs'),
            editedExts: configServer.get('editedDocs'),
            fillExts: configServer.get('fillDocs'),
            storedFiles: req.docManager.getStoredFiles(),
            params: req.docManager.getCustomParams(),
            users,
            serverUrl: req.docManager.getServerUrl(),
            languages: configServer.get('languages'),
        });

    } catch (ex) {
        console.log(ex);  // display error message in the console
        res.status(500);  // write status parameter to the response
        res.render('error', { message: 'Server error' });  // render error template with the message parameter specified
        return;
    }
});

app.get('/download', (req, res) => {  // define a handler for downloading files
    req.docManager = new docManager(req, res);

    let fileName = fileUtility.getFileName(req.query.fileName);
    let userAddress = req.query.useraddress;

    if (!!userAddress
        && cfgSignatureEnable && cfgSignatureUseForRequest) {
        let authorization = req.get(cfgSignatureAuthorizationHeader);
        if (authorization && authorization.startsWith(cfgSignatureAuthorizationHeaderPrefix)) {
            const token = authorization.substring(cfgSignatureAuthorizationHeaderPrefix.length);
        }

        try {
            let decoded = jwt.verify(token, cfgSignatureSecret);
        } catch (err) {
            console.log(`checkJwtHeader error: name = ${  err.name  } message = ${  err.message  } token = ${  token}`)
            res.sendStatus(403);
            return;
        }
    }

    let path = req.docManager.forcesavePath(fileName, userAddress, false);  // get the path to the force saved document version
    if (path == '') {
        path = req.docManager.storagePath(fileName, userAddress);  // or to the original document
    }

    res.setHeader('Content-Length', fileSystem.statSync(path).size);  // add headers to the response to specify the page parameters
    res.setHeader('Content-Type', mime.getType(path));

    res.setHeader('Content-Disposition', `attachment; filename*=UTF-8''${  encodeURIComponent(fileName)}`);

    let filestream = fileSystem.createReadStream(path);
    filestream.pipe(res);  // send file information to the response by streams
});

app.get('/history', (req, res) => {
    req.docManager = new docManager(req, res);
    if (cfgSignatureEnable && cfgSignatureUseForRequest) {
        let authorization = req.get(cfgSignatureAuthorizationHeader);
        if (authorization && authorization.startsWith(cfgSignatureAuthorizationHeaderPrefix)) {
            let token = authorization.substring(cfgSignatureAuthorizationHeaderPrefix.length);
            try {
                let decoded = jwt.verify(token, cfgSignatureSecret);
            } catch (err) {
                console.log(`checkJwtHeader error: name = ${  err.name  } message = ${  err.message  } token = ${  token}`);
                res.sendStatus(403);
                return;
            }
        } else {
            res.sendStatus(403);
            return;
        }
    }

    let {fileName} = req.query;
    let userAddress = req.query.useraddress;
    let {ver} = req.query;
    let {file} = req.query;

    if (file.includes('diff')) {
        const Path = req.docManager.diffPath(fileName, userAddress, ver);
    } else if (file.includes('prev')) {
        const Path = req.docManager.prevFilePath(fileName, userAddress, ver);
    } else {
        res.sendStatus(403);
        return;
    }

    res.setHeader('Content-Length', fileSystem.statSync(Path).size);  // add headers to the response to specify the page parameters
    res.setHeader('Content-Type', mime.getType(Path));
    res.setHeader('Content-Disposition', `attachment; filename*=UTF-8''${  encodeURIComponent(file)}`);

    let filestream = fileSystem.createReadStream(Path);
    filestream.pipe(res);  // send file information to the response by streams
})

app.post('/upload', (req, res) => {  // define a handler for uploading files

    req.docManager = new docManager(req, res);
    req.docManager.storagePath(''); // mkdir if not exist

    const userIp = req.docManager.curUserHostAddress();  // get the path to the user host
    const uploadDir = req.docManager.storageRootPath(userIp);
    const uploadDirTmp = path.join(uploadDir, 'tmp');  // and create directory for temporary files if it doesn't exist
    req.docManager.createDirectory(uploadDirTmp);

    const form = new formidable.IncomingForm();  // create a new incoming form
    form.uploadDir = uploadDirTmp;  // and write there all the necessary parameters
    form.keepExtensions = true;

    form.parse(req, (err, fields, files) => {  // parse this form
    	if (err) {  // if an error occurs
			// docManager.cleanFolderRecursive(uploadDirTmp, true);  // clean the folder with temporary files
			res.writeHead(200, { 'Content-Type': 'text/plain' });  // and write the error status and message to the response
			res.write(`{ "error": "${  err.message  }"}`);
			res.end();
			return;
		}

        const file = files.uploadedFile;

        if (file == undefined) {  // if file parameter is undefined
            res.writeHead(200, { 'Content-Type': 'text/plain' });  // write the error status and message to the response
            res.write('{ "error": "Uploaded file not found"}');
            res.end();
            return;
        }

        file.name = req.docManager.getCorrectName(file.name);

        if (configServer.get('maxFileSize') < file.size || file.size <= 0) {  // check if the file size exceeds the maximum file size
			// docManager.cleanFolderRecursive(uploadDirTmp, true);  // clean the folder with temporary files
            res.writeHead(200, { 'Content-Type': 'text/plain' });
            res.write('{ "error": "File size is incorrect"}');
            res.end();
            return;
        }

        const exts = [].concat(configServer.get('viewedDocs'), configServer.get('editedDocs'), configServer.get('convertedDocs'), configServer.get('fillDocs'));  // all the supported file extensions
        const curExt = fileUtility.getFileExtension(file.name);
        const documentType = fileUtility.getFileType(file.name);

        if (exts.indexOf(curExt) == -1) {  // check if the file extension is supported
			// docManager.cleanFolderRecursive(uploadDirTmp, true);  // if not, clean the folder with temporary files
            res.writeHead(200, { 'Content-Type': 'text/plain' });  // and write the error status and message to the response
            res.write('{ "error": "File type is not supported"}');
            res.end();
            return;
        }

        fileSystem.rename(file.path, `${uploadDir  }/${  file.name}`, (err) => {  // rename a file
			// docManager.cleanFolderRecursive(uploadDirTmp, true);  // clean the folder with temporary files
            res.writeHead(200, { 'Content-Type': 'text/plain' });
            if (err) {  // if an error occurs
                res.write(`{ "error": "${  err  }"}`);  // write an error message to the response
            } else {
                res.write(`{ "filename": "${  file.name  }", "documentType": "${  documentType  }" }`);  // otherwise, write a new file name to the response

                let user = users.getUser(req.query.userid); // get user id and name parameters or set them to the default values

                req.docManager.saveFileData(file.name, user.id, user.name);
            }
            res.end();
        });
    });
});

app.post('/create', (req, res) => {
    let {title} = req.body;
    let fileUrl = req.body.url;

    try {
        req.docManager = new docManager(req, res);
        req.docManager.storagePath(''); // mkdir if not exist

        let fileName = req.docManager.getCorrectName(title);
        let userAddress = req.docManager.curUserHostAddress();
        req.docManager.historyPath(fileName, userAddress, true);

        urllib.request(fileUrl, {method: 'GET'},(err, data) => {
            if (configServer.get('maxFileSize') < data.length || data.length <= 0) {  // check if the file size exceeds the maximum file size
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.write(JSON.stringify({ error: 'File size is incorrect' }));
                res.end();
                return;
            }

            const exts = [].concat(configServer.get('viewedDocs'), configServer.get('editedDocs'), configServer.get('convertedDocs'), configServer.get('fillDocs'));  // all the supported file extensions
            const curExt = fileUtility.getFileExtension(fileName);

            if (exts.indexOf(curExt) == -1) {  // check if the file extension is supported
                res.writeHead(200, { 'Content-Type': 'application/json' });  // and write the error status and message to the response
                res.write(JSON.stringify({ error: 'File type is not supported' }));
                res.end();
                return;
            }

            fileSystem.writeFileSync(req.docManager.storagePath(fileName), data);

            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.write(JSON.stringify({ file : fileName }));
            res.end();

        });

    } catch (e) {
        res.status(500);
        res.write(JSON.stringify({
            error: 1,
            message: e.message
        }));
        res.end();
    }
});

app.post('/convert', (req, res) => {  // define a handler for converting files
    req.docManager = new docManager(req, res);

    let fileName = fileUtility.getFileName(req.body.filename);
    let filePass = req.body.filePass ? req.body.filePass : null;
    let lang = req.body.lang ? req.body.lang : null;
    let fileUri = req.docManager.getDownloadUrl(fileName, true);
    let fileExt = fileUtility.getFileExtension(fileName);
    let fileType = fileUtility.getFileType(fileName);
    let internalFileExt = 'ooxml';
    let response = res;

    let writeResult = function (filename, step, error) {
        let result = {};

        // write file name, step and error values to the result object if they are defined
        if (filename != null) result.filename = filename;

        if (step != null) result.step = step;

        if (error != null) result.error = error;

        response.setHeader('Content-Type', 'application/json');
        response.write(JSON.stringify(result));
        response.end();
    };

    let callback = async function (err, res) {
        if (err) {  // if an error occurs
            if (err.name === 'ConnectionTimeoutError' || err.name === 'ResponseTimeoutError') {  // check what type of error it is
                writeResult(fileName, 0, null);  // despite the timeout errors, write the file to the result object
            } else {
                writeResult(null, null, JSON.stringify(err));  // other errors trigger an error message
            }
            return;
        }

        try {
            let responseData = documentService.getResponseUri(res.toString());
            let result = responseData.percent;
            let newFileUri = responseData.uri;  // get the callback url
            let newFileType = `.${  responseData.fileType}`;  // get the file type

            if (result != 100) {  // if the status isn't 100
                writeResult(fileName, result, null);  // write the origin file to the result object
                return;
            }

            let correctName = req.docManager.getCorrectName(fileUtility.getFileName(fileName, true) + newFileType);  // get the file name with a new extension

            const {status, data} = await urllib.request(newFileUri, {method: 'GET'});

            if (status != 200) throw new Error(`Conversion service returned status: ${  status}`);

            fileSystem.writeFileSync(req.docManager.storagePath(correctName), data);  // write a file with a new extension, but with the content from the origin file
            fileSystem.unlinkSync(req.docManager.storagePath(fileName));  // remove file with the origin extension

            let userAddress = req.docManager.curUserHostAddress();
            let historyPath = req.docManager.historyPath(fileName, userAddress, true);
            let correctHistoryPath = req.docManager.historyPath(correctName, userAddress, true);  // get the history path to the file with a new extension

            fileSystem.renameSync(historyPath, correctHistoryPath);  // change the previous history path

            fileSystem.renameSync(path.join(correctHistoryPath, `${fileName  }.txt`), path.join(correctHistoryPath, `${correctName  }.txt`));  // change the name of the .txt file with document information

            writeResult(correctName, result, null);  // write a file with a new name to the result object
        } catch (e) {
            console.log(e);  // display error message in the console
            writeResult(null, null, e.message);
        }
    };

    try {
        if (configServer.get('convertedDocs').indexOf(fileExt) != -1) {  // check if the file with such an extension can be converted
            const storagePath = req.docManager.storagePath(fileName);
            const stat = fileSystem.statSync(storagePath);
            let key = fileUri + stat.mtime.getTime();

            key = documentService.generateRevisionId(key);  // get document key
            documentService.getConvertedUri(fileUri, fileExt, internalFileExt, key, true, callback, filePass, lang);  // get the url to the converted file
        } else {
            writeResult(fileName, null, null);  // if the file with such an extension can't be converted, write the origin file to the result object
        }
    } catch (ex) {
        console.log(ex);
        writeResult(null, null, 'Server error');
    }
});

app.get('/files', (req, res) => {  // define a handler for getting files information
    try {
        req.docManager = new docManager(req, res);
        const filesInDirectoryInfo = req.docManager.getFilesInfo();  // get the information about the files from the storage path
        res.setHeader('Content-Type', 'application/json');
        res.write(JSON.stringify(filesInDirectoryInfo));  // transform files information into the json string
    } catch (ex) {
        console.log(ex);
        res.write('Server error');
    }
    res.end();
});

app.get('/files/file/:fileId', (req, res) => {  // define a handler for getting file information by its id
    try {
        req.docManager = new docManager(req, res);
        const {fileId} = req.params;
        const fileInfoById = req.docManager.getFilesInfo(fileId);  // get the information about the file specified by a file id
        res.setHeader('Content-Type', 'application/json');
        res.write(JSON.stringify(fileInfoById));
    } catch (ex) {
        console.log(ex);
        res.write('Server error');
    }
    res.end();
});

app.delete('/file', (req, res) => {  // define a handler for removing file
    try {
    	req.docManager = new docManager(req, res);
        let fileName = req.query.filename;
        if (fileName) {  // if the file name is defined
			fileName = fileUtility.getFileName(fileName);  // get its part without an extension

			req.docManager.fileRemove(fileName); // delete file and his history
		} else {
			req.docManager.cleanFolderRecursive(req.docManager.storagePath(''), false);  // if the file name is undefined, clean the storage folder
		}

        res.write('{"success":true}');
    } catch (ex) {
        console.log(ex);
        res.write('Server error');
    }
    res.end();
});

app.get('/csv', (req, res) => {  // define a handler for downloading csv files
    let fileName = 'csv.csv';
    let csvPath = path.join(__dirname, 'public', 'assets',  'sample', fileName);

    res.setHeader('Content-Length', fileSystem.statSync(csvPath).size);  // add headers to the response to specify the page parameters
    res.setHeader('Content-Type', mime.getType(csvPath));

    res.setHeader('Content-Disposition', `attachment; filename*=UTF-8''${  encodeURIComponent(fileName)}`);

    let filestream = fileSystem.createReadStream(csvPath);
    filestream.pipe(res);  // send file information to the response by streams
})

app.post('/reference', (req, res) => { // define a handler for renaming file

    req.docManager = new docManager(req, res);

    let result = function (data) {
        res.writeHead(200, {'Content-Type': 'application/json' });
        res.write(JSON.stringify(data));
        res.end();
    };

    let {referenceData} = req.body;
    if (!!referenceData) {
        let {instanceId} = referenceData;

        if (instanceId === req.docManager.getInstanceId()) {
            let fileKey = JSON.parse(referenceData.fileKey);
            const {userAddress} = fileKey;

            if (userAddress === req.docManager.curUserHostAddress()
                && req.docManager.existsSync(req.docManager.storagePath(fileKey.fileName, userAddress))) {
                let {fileName} = fileKey;
            }
        }
    }

    if (!fileName && !!req.body.path) {
        let path = fileUtility.getFileName(req.body.path);

        if (req.docManager.existsSync(req.docManager.storagePath(path, userAddress))) {
        let fileName = path;
        }
    }

    if (!fileName) {
        result({ error: 'File is not found' });
        return;
    }

    let data = {
        fileType: fileUtility.getFileExtension(fileName).slice(1),
        url: req.docManager.getDownloadUrl(fileName, true),
        directUrl: req.body.directUrl ? req.docManager.getDownloadUrl(fileName) : null,
        referenceData: {
            fileKey: JSON.stringify({ fileName, userAddress: req.docManager.curUserHostAddress()}),
            instanceId: req.docManager.getServerUrl()
        },
        path: fileName,
    };

    if (cfgSignatureEnable) {
        data.token = jwt.sign(data, cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});  // sign token with given data using signature secret
    }

    result(data);
});

app.post('/track', async (req, res) => {  // define a handler for tracking file changes

    req.docManager = new docManager(req, res);

    let userAddress = req.query.useraddress;
    let fileName = fileUtility.getFileName(req.query.filename);
    let version = 0;

    // track file changes
    let processTrack = async function (response, body, fileName, userAddress) {

        // callback file saving process
        let callbackProcessSave = async function (downloadUri, body, fileName, userAddress, newFileName) {
            try {
                const {status, data} = await urllib.request(downloadUri, {method: 'GET'});

                if (status != 200) throw new Error(`Document editing service returned status: ${  status}`);

                let storagePath = req.docManager.storagePath(newFileName, userAddress);

                let historyPath = req.docManager.historyPath(newFileName, userAddress);  // get the path to the history data
                if (historyPath == '') {  // if the history path doesn't exist
                    historyPath = req.docManager.historyPath(newFileName, userAddress, true);  // create it
                    req.docManager.createDirectory(historyPath);  // and create a directory for the history data
                }

                let count_version = req.docManager.countVersion(historyPath);  // get the next file version number
                version = count_version + 1;
                let versionPath = req.docManager.versionPath(newFileName, userAddress, version);  // get the path to the specified file version
                req.docManager.createDirectory(versionPath);  // create a directory to the specified file version

                let downloadZip = body.changesurl;
                if (downloadZip) {
                    let path_changes = req.docManager.diffPath(newFileName, userAddress, version);  // get the path to the file with document versions differences
                    const {status, data} = await urllib.request(downloadZip, {method: 'GET'});
                    if (status == 200) {
                        fileSystem.writeFileSync(path_changes, data);  // write the document version differences to the archive
                    } else {
                        emitWarning(`Document editing service returned status: ${  status}`);
                    }
                }

                let changeshistory = body.changeshistory || JSON.stringify(body.history);
                if (changeshistory) {
                    let path_changes_json = req.docManager.changesPath(newFileName, userAddress, version);  // get the path to the file with document changes
                    fileSystem.writeFileSync(path_changes_json, changeshistory);  // and write this data to the path in json format
                }

                let path_key = req.docManager.keyPath(newFileName, userAddress, version);  // get the path to the key.txt file
                fileSystem.writeFileSync(path_key, body.key);  // write the key value to the key.txt file

                let path_prev = path.join(versionPath, `prev${  fileUtility.getFileExtension(fileName)}`);  // get the path to the previous file version
                fileSystem.renameSync(req.docManager.storagePath(fileName, userAddress), path_prev);  // and write it to the current path

                fileSystem.writeFileSync(storagePath, data);

                let forcesavePath = req.docManager.forcesavePath(newFileName, userAddress, false);  // get the path to the forcesaved file
                if (forcesavePath != '') {  // if this path is empty
                    fileSystem.unlinkSync(forcesavePath);  // remove it
                }

            } catch (ex) {
                console.log(ex);
                response.write('{"error":1}');
                response.end();
                return;
            }

            response.write('{"error":0}');
            response.end();
        }

        // file saving process
        let processSave = async function (downloadUri, body, fileName, userAddress) {

            if (!downloadUri) {
                response.write('{"error":1}');
                response.end();
                return;
            }

            let curExt = fileUtility.getFileExtension(fileName);  // get current file extension
            let downloadExt = `.${  body.filetype}`; // get the extension of the downloaded file

            let newFileName = fileName;

            // convert downloaded file to the file with the current extension if these extensions aren't equal
            if (downloadExt != curExt) {
                let key = documentService.generateRevisionId(downloadUri);
                newFileName = req.docManager.getCorrectName(fileUtility.getFileName(fileName, true) + downloadExt, userAddress);  // get the correct file name if it already exists
                try {
                    documentService.getConvertedUriSync(downloadUri, downloadExt, curExt, key, async (err, data) => {
                        if (err) {
                            await callbackProcessSave(downloadUri, body, fileName, userAddress, newFileName);
                            return;
                        }
                        try {
                            let res = documentService.getResponseUri(data);
                            await callbackProcessSave(res.uri, body, fileName, userAddress, fileName);
                            return;
                        } catch (ex) {
                            console.log(ex);
                            await callbackProcessSave(downloadUri, body, fileName, userAddress, newFileName);
                            return;
                        }
                    });
                    return;
                } catch (ex) {
                    console.log(ex);
                }
            }
            await callbackProcessSave(downloadUri, body, fileName, userAddress, newFileName);
        };

        // callback file force saving process
        let callbackProcessForceSave = async function (downloadUri, body, fileName, userAddress, newFileName = false) {
            try {
                const {status, data} = await urllib.request(downloadUri, {method: 'GET'});

                if (status != 200) throw new Error(`Document editing service returned status: ${  status}`);

                let downloadExt = `.${  body.fileType}`;
                let isSubmitForm = body.forcesavetype === 3; // SubmitForm
                let correctName = '';

                if (isSubmitForm) {
                    // new file
                    if (newFileName) {
                        correctName = req.docManager.getCorrectName(`${fileUtility.getFileName(fileName, true)  }-form${  downloadExt}`, userAddress);
                    } else {
                        let ext = fileUtility.getFileExtension(fileName);
                        correctName = req.docManager.getCorrectName(`${fileUtility.getFileName(fileName, true)  }-form${  ext}`, userAddress);
                    }
                    let forcesavePath = req.docManager.storagePath(correctName, userAddress);
                } else {
                    if (newFileName) {
                        correctName = req.docManager.getCorrectName(fileUtility.getFileName(fileName, true) + downloadExt, userAddress);
                    }
                    // create forcesave path if it doesn't exist
                    let forcesavePath = req.docManager.forcesavePath(correctName, userAddress, false);
                    if (forcesavePath == '') {
                        forcesavePath = req.docManager.forcesavePath(correctName, userAddress, true);
                    }
                }

                fileSystem.writeFileSync(forcesavePath, data);

                if (isSubmitForm) {
                    let uid =body.actions[0].userid
                    req.docManager.saveFileData(correctName, uid, 'Filling Form', userAddress);
                }
            } catch (ex) {
                response.write('{"error":1}');
                response.end();
                return;
            }

            response.write('{"error":0}');
            response.end();
        }

        // file force saving process
        let processForceSave = async function (downloadUri, body, fileName, userAddress) {

            if (!downloadUri) {
                response.write('{"error":1}');
                response.end();
                return;
            }

            let curExt = fileUtility.getFileExtension(fileName);
            let downloadExt = `.${  body.filetype}`;

            // convert downloaded file to the file with the current extension if these extensions aren't equal
            if (downloadExt != curExt) {
                let key = documentService.generateRevisionId(downloadUri);
                try {
                    documentService.getConvertedUriSync(downloadUri, downloadExt, curExt, key, async (err, data) => {
                        if (err) {
                            await callbackProcessForceSave(downloadUri, body, fileName, userAddress, true);
                            return;
                        }
                        try {
                            let res = documentService.getResponseUri(data);
                            await callbackProcessForceSave(res.uri, body, fileName, userAddress, false);
                            return;
                        } catch (ex) {
                            console.log(ex);
                            await callbackProcessForceSave(downloadUri, body, fileName, userAddress, true);
                            return;
                        }
                    });
                    return;
                } catch (ex) {
                    console.log(ex);
                }
            }
            await callbackProcessForceSave (downloadUri, body, fileName, userAddress, false);
        };

        if (body.status == 1) { // editing
            if (body.actions && body.actions[0].type == 0) { // finished edit
                let user = body.actions[0].userid;
                if (body.users.indexOf(user) == -1) {
                    let {key} = body;
                    try {
                        documentService.commandRequest('forcesave', key);  // call the forcesave command
                    } catch (ex) {
                        console.log(ex);
                    }
                }
            }
        } else if (body.status == 2 || body.status == 3) { // MustSave, Corrupted
            await processSave(body.url, body, fileName, userAddress);  // save file
            return;
        } else if (body.status == 6 || body.status == 7) { // MustForceSave, CorruptedForceSave
            await processForceSave(body.url, body, fileName, userAddress);  // force save file
            return;
        }

        response.write('{"error":0}');
        response.end();
    };

    // read request body
    let readbody = async function (request, response, fileName, userAddress) {
        let content = '';
        request.on('data', async (data) => {  // get data from the request
            content += data;
        });
        request.on('end', async () => {
            let body = JSON.parse(content);
            await processTrack(response, body, fileName, userAddress);  // and track file changes
        });
    };

    // check jwt token
    if (cfgSignatureEnable && cfgSignatureUseForRequest) {
        let body = null;
        if (req.body.hasOwnProperty('token')) {  // if request body has its own token
            body = documentService.readToken(req.body.token);  // read and verify it
        } else {
            let checkJwtHeaderRes = documentService.checkJwtHeader(req);  // otherwise, check jwt token headers
            if (checkJwtHeaderRes) {  // if they exist
                let body;
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
            res.write('{"error":1}');
            res.end();
            return;
        }
        await processTrack(res, body, fileName, userAddress);
        return;
    }

    if (req.body.hasOwnProperty('status')) {  // if the request body has status parameter
        await processTrack(res, req.body, fileName, userAddress);  // track file changes
    } else {
        await readbody(req, res, fileName, userAddress);  // otherwise, read request body first
    }
});

app.get('/editor', (req, res) => {  // define a handler for editing document
    try {

        req.docManager = new docManager(req, res);

        let fileName = fileUtility.getFileName(req.query.fileName);
        let {fileExt} = req.query;
        let history = [];
        let historyData = [];
        let lang = req.docManager.getLang();
        let user = users.getUser(req.query.userid);
        let userDirectUrl = req.query.directUrl == 'true';

        let userid = user.id;
        let {name} = user;

        let actionData = 'null';
        if (req.query.action) {
            try {
                actionData = JSON.stringify(JSON.parse(req.query.action));
            } catch (ex) {
                console.log(ex);
            }
        }

        let type = req.query.type || ''; // type: embedded/mobile/desktop
        if (type == '') {
            type = new RegExp(configServer.get('mobileRegEx'), 'i').test(req.get('User-Agent')) ? 'mobile' : 'desktop';
        } else if (type != 'mobile'
            && type != 'embedded') {
                type = 'desktop';
        }

        let templatesImageUrl = req.docManager.getTemplateImageUrl(fileUtility.getFileType(fileName));
        let createUrl = req.docManager.getCreateUrl(fileUtility.getFileType(fileName), userid, type, lang);
        let templates = [
            {
                image: '',
                title: 'Blank',
                url: createUrl
            },
            {
                image: templatesImageUrl,
                title: 'With sample content',
                url: `${createUrl  }&sample=true`
            }
        ];

        let userGroup = user.group;
        let {reviewGroups} = user;
        let {commentGroups} = user;
        let {userInfoGroups} = user;

        if (fileExt != null) {
            let fileName = req.docManager.createDemo(!!req.query.sample, fileExt, userid, name, false);  // create demo document of a given extension

            // get the redirect path
            let redirectPath = `${req.docManager.getServerUrl()  }/editor?fileName=${  encodeURIComponent(fileName)  }${req.docManager.getCustomParams()}`;
            res.redirect(redirectPath);
            return;
        }
        fileExt = fileUtility.getFileExtension(fileName);

        let userAddress = req.docManager.curUserHostAddress();
        if (!req.docManager.existsSync(req.docManager.storagePath(fileName, userAddress))) {  // if the file with a given name doesn't exist
            throw {
                message: `File not found: ${  fileName}`  // display error message
            };
        }
        let key = req.docManager.getKey(fileName);
        let url = req.docManager.getDownloadUrl(fileName, true);
        let directUrl = req.docManager.getDownloadUrl(fileName);
        let mode = req.query.mode || 'edit'; // mode: view/edit/review/comment/fillForms/embedded

        let canEdit = configServer.get('editedDocs').indexOf(fileExt) != -1;  // check if this file can be edited
        if ((!canEdit && mode == 'edit' || mode == 'fillForms') && configServer.get('fillDocs').indexOf(fileExt) != -1) {
            mode = 'fillForms';
            canEdit = true;
        }
        if (!canEdit && mode == 'edit') {
            mode = 'view';
        }
        let submitForm = mode == 'fillForms' && userid == 'uid-1' && !1;

        let countVersion = 1;

        let historyPath = req.docManager.historyPath(fileName, userAddress);
        let changes = null;
        let keyVersion = key;

        if (historyPath != '') {

            countVersion = req.docManager.countVersion(historyPath) + 1;  // get the number of file versions
            for (let i = 1; i <= countVersion; i += 1) {  // get keys to all the file versions
                if (i < countVersion) {
                    let keyPath = req.docManager.keyPath(fileName, userAddress, i);
                    if (!fileSystem.existsSync(keyPath)) continue;
                    keyVersion = `${  fileSystem.readFileSync(keyPath)}`;
                } else {
                    keyVersion = key;
                }
                history.push(req.docManager.getHistory(fileName, changes, keyVersion, i));  // write all the file history information

                let userUrl = i == countVersion ? directUrl : (`${req.docManager.getServerUrl(false)}/history?fileName=${encodeURIComponent(fileName)}&file=prev${fileExt}&ver=${i}`);
                let historyD = {
                    fileType: fileExt.slice(1),
                    version: i,
                    key: keyVersion,
                    url: i == countVersion ? url : (`${req.docManager.getServerUrl(true)}/history?fileName=${encodeURIComponent(fileName)}&file=prev${fileExt}&ver=${i}&useraddress=${userAddress}`),
                    directUrl: !userDirectUrl ? null : userUrl,
                };

                if (i > 1 && req.docManager.existsSync(req.docManager.diffPath(fileName, userAddress, i-1))) {  // check if the path to the file with document versions differences exists
                    historyD.previous = {  // write information about previous file version
                        fileType: historyData[i-2].fileType,
                        key: historyData[i-2].key,
                        url: historyData[i-2].url,
                        directUrl: !userDirectUrl ? null :  historyData[i-2].directUrl,
                    };
                    const changesUrl = `${req.docManager.getServerUrl(true)}/history?fileName=${encodeURIComponent(fileName)}&file=diff.zip&ver=${i-1}&useraddress=${userAddress}`;
                    historyD.changesUrl = changesUrl;  // get the path to the diff.zip file and write it to the history object
                }

                historyData.push(historyD);

                if (i < countVersion) {
                    let changesFile = req.docManager.changesPath(fileName, userAddress, i);  // get the path to the file with document changes
                    changes = req.docManager.getChanges(changesFile);  // get changes made in the file
                }
            }
        } else {  // if history path is empty
            history.push(req.docManager.getHistory(fileName, changes, keyVersion, countVersion));  // write the history information about the last file version
            historyData.push({
                fileType: fileExt.slice(1),
                version: countVersion,
                key,
                url,
                directUrl: !userDirectUrl ? null : directUrl,
            });
        }

        if (cfgSignatureEnable) {
            for (let i = 0; i < historyData.length; i += 1) {
                historyData[i].token = jwt.sign(historyData[i], cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});  // sign token with given data using signature secret
            }
        }

        // file config data
        let argss = {
            apiUrl: siteUrl + configServer.get('apiUrl'),
            file: {
                name: fileName,
                ext: fileUtility.getFileExtension(fileName, true),
                uri: url,
                directUrl: !userDirectUrl ? null : directUrl,
                uriUser: directUrl,
                version: countVersion,
                created: new Date().toDateString(),
                favorite: user.favorite != null ? user.favorite : 'null'
            },
            editor: {
                type,
                documentType: fileUtility.getFileType(fileName),
                key,
                token: '',
                callbackUrl: req.docManager.getCallback(fileName),
                createUrl: userid != 'uid-0' ? createUrl : null,
                templates: user.templates ? templates : null,
                isEdit: canEdit && (mode == 'edit' || mode == 'view' || mode == 'filter' || mode == 'blockcontent'),
                review: canEdit && (mode == 'edit' || mode == 'review'),
                chat: userid != 'uid-0',
                coEditing: mode == 'view' && userid == 'uid-0' ? {mode: 'strict', change: false} : null,
                comment: mode != 'view' && mode != 'fillForms' && mode != 'embedded' && mode != 'blockcontent',
                fillForms: mode != 'view' && mode != 'comment' && mode != 'embedded' && mode != 'blockcontent',
                modifyFilter: mode != 'filter',
                modifyContentControl: mode != 'blockcontent',
                copy: !user.deniedPermissions.includes('copy'),
                download: !user.deniedPermissions.includes('download'),
                print: !user.deniedPermissions.includes('print'),
                mode: mode != 'view' ? 'edit' : 'view',
                canBackToFolder: type != 'embedded',
                backUrl: `${req.docManager.getServerUrl()  }/`,
                curUserHostAddress: req.docManager.curUserHostAddress(),
                lang,
                userid: userid != 'uid-0' ? userid : null,
                name,
                userGroup,
                reviewGroups: JSON.stringify(reviewGroups),
                commentGroups: JSON.stringify(commentGroups),
                userInfoGroups: JSON.stringify(userInfoGroups),
                fileChoiceUrl,
                submitForm,
                plugins: JSON.stringify(plugins),
                actionData,
                fileKey: userid != 'uid-0' ? JSON.stringify({ fileName, userAddress: req.docManager.curUserHostAddress()}) : null,
                instanceId: userid != 'uid-0' ? req.docManager.getInstanceId() : null,
                protect: !user.deniedPermissions.includes('protect')
            },
            history,
            historyData,
            dataInsertImage: {
                fileType: 'png',
                url: `${req.docManager.getServerUrl(true)  }/images/logo.png`,
                directUrl: !userDirectUrl ? null : `${req.docManager.getServerUrl()  }/images/logo.png`,
            },
            dataCompareFile: {
                fileType: 'docx',
                url: `${req.docManager.getServerUrl(true)  }/assets/sample/sample.docx`,
                directUrl: !userDirectUrl ? null : `${req.docManager.getServerUrl()  }/assets/sample/sample.docx`,
            },
            dataMailMergeRecipients: {
                fileType: 'csv',
                url: `${req.docManager.getServerUrl(true)  }/csv`,
                directUrl: !userDirectUrl ? null : `${req.docManager.getServerUrl()  }/csv`,
            },
            usersForMentions: user.id != 'uid-0' ? users.getUsersForMentions(user.id) : null,
        };

        if (cfgSignatureEnable) {
            app.render('config', argss, (err, html) => {  // render a config template with the parameters specified
                if (err) {
                    console.log(err);
                } else {
                    // sign token with given data using signature secret
                    argss.editor.token = jwt.sign(JSON.parse(`{${html}}`), cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});
                    argss.dataInsertImage.token = jwt.sign(argss.dataInsertImage, cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});
                    argss.dataCompareFile.token = jwt.sign(argss.dataCompareFile, cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});
                    argss.dataMailMergeRecipients.token = jwt.sign(argss.dataMailMergeRecipients, cfgSignatureSecret, {expiresIn: cfgSignatureSecretExpiresIn});
                }
                res.render('editor', argss);  // render the editor template with the parameters specified
              });
        } else {
              res.render('editor', argss);
        }
    } catch (ex) {
        console.log(ex);
        res.status(500);
        res.render('error', { message: `Server error: ${  ex.message}` });
    }
});

app.post('/rename', (req, res) => { // define a handler for renaming file

    let {newfilename} = req.body;
    let origExt = req.body.ext;
    let curExt = fileUtility.getFileExtension(newfilename, true);
    if (curExt !== origExt) {
        newfilename += `.${  origExt}`;
    }

    let {dockey} = req.body;
    let meta = {title: newfilename};

    let result = function (err, data, ress) {
        res.writeHead(200, {'Content-Type': 'application/json' });
        res.write(JSON.stringify({ result: ress }));
        res.end();
    };

    documentService.commandRequest('meta', dockey, meta, result);
});

wopiApp.registerRoutes(app);

// "Not found" error with 404 status
app.use((req, res, next) => {
    const err = new Error('Not Found');
    err.status = 404;
    next(err);
});

// render the error template with the parameters specified
app.use((err, req, res, next) => {
    res.status(err.status || 500);
    res.render('error', {
        message: err.message
    });
});

// save all the functions to the app module to export it later in other files
module.exports = app;
