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
const mime = require('mime');
const urllib = require('urllib');
const urlModule = require('url');
const { emitWarning } = require('process');
const DocManager = require('./helpers/docManager');
const documentService = require('./helpers/documentService');
const fileUtility = require('./helpers/fileUtility');
const wopiApp = require('./helpers/wopi/wopiRouting');
const users = require('./helpers/users');

const configServer = config.get('server');
const siteUrl = configServer.get('siteUrl');
const fileChoiceUrl = configServer.has('fileChoiceUrl') ? configServer.get('fileChoiceUrl') : '';
const cfgSignatureEnable = configServer.get('token.enable');
const cfgSignatureUseForRequest = configServer.get('token.useforrequest');
const cfgSignatureAuthorizationHeader = configServer.get('token.authorizationHeader');
const cfgSignatureAuthorizationHeaderPrefix = configServer.get('token.authorizationHeaderPrefix');
const cfgSignatureSecretExpiresIn = configServer.get('token.expiresIn');
const cfgSignatureSecret = configServer.get('token.secret');
const verifyPeerOff = configServer.get('verify_peer_off');
const plugins = config.get('plugins');

if (verifyPeerOff) {
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
}

String.prototype.hashCode = function hashCode() {
  const len = this.length;
  let ret = 0;
  for (let i = 0; i < len; i++) {
    ret = Math.trunc(31 * ret + this.charCodeAt(i));
  }
  return ret;
};
String.prototype.format = function format(...args) {
  let text = this.toString();

  if (!args.length) return text;

  for (let i = 0; i < args.length; i++) {
    text = text.replace(new RegExp(`\\{${i}\\}`, 'gi'), args[i]);
  }

  return text;
};

const app = express(); // create an application object
app.disable('x-powered-by');
app.set('views', path.join(__dirname, 'views')); // specify the path to the main template
app.set('view engine', 'ejs'); // specify which template engine is used

app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*'); // allow any Internet domain to access the resources of this site
  next();
});

app.use(express.static(path.join(__dirname, 'public'))); // public directory
app.use(favicon(`${__dirname}/public/images/favicon.ico`)); // use favicon

app.use(bodyParser.json()); // connect middleware that parses json
app.use(bodyParser.urlencoded({ extended: false })); // connect middleware that parses urlencoded bodies

app.get('/', (req, res) => { // define a handler for default page
  try {
    req.DocManager = new DocManager(req, res);

    res.render('index', { // render index template with the parameters specified
      preloaderUrl: siteUrl + configServer.get('preloaderUrl'),
      convertExts: fileUtility.getConvertExtensions(),
      editedExts: fileUtility.getEditExtensions(),
      fillExts: fileUtility.getFillExtensions(),
      storedFiles: req.DocManager.getStoredFiles(),
      params: req.DocManager.getCustomParams(),
      users,
      languages: configServer.get('languages'),
    });
  } catch (ex) {
    console.log(ex); // display error message in the console
    res.status(500); // write status parameter to the response
    res.render('error', { message: 'Server error' }); // render error template with the message parameter specified
  }
});

app.get('/download', (req, res) => { // define a handler for downloading files
  req.DocManager = new DocManager(req, res);

  const fileName = fileUtility.getFileName(req.query.fileName);
  const userAddress = req.query.useraddress;
  let token = '';

  if (!!userAddress
        && cfgSignatureEnable && cfgSignatureUseForRequest) {
    const authorization = req.get(cfgSignatureAuthorizationHeader);
    if (authorization && authorization.startsWith(cfgSignatureAuthorizationHeaderPrefix)) {
      token = authorization.substring(cfgSignatureAuthorizationHeaderPrefix.length);
    }

    try {
      jwt.verify(token, cfgSignatureSecret);
    } catch (err) {
      console.log(`checkJwtHeader error: name = ${err.name} message = ${err.message} token = ${token}`);
      res.sendStatus(403);
      return;
    }
  }

  // get the path to the force saved document version
  let filePath = req.DocManager.forcesavePath(fileName, userAddress, false);
  if (filePath === '') {
    filePath = req.DocManager.storagePath(fileName, userAddress); // or to the original document
  }

  // add headers to the response to specify the page parameters
  res.setHeader('Content-Length', fileSystem.statSync(filePath).size);
  res.setHeader('Content-Type', mime.getType(filePath));

  res.setHeader('Content-Disposition', `attachment; filename*=UTF-8''${encodeURIComponent(fileName)}`);

  const filestream = fileSystem.createReadStream(filePath);
  filestream.pipe(res); // send file information to the response by streams
});

app.get('/history', (req, res) => {
  req.DocManager = new DocManager(req, res);
  if (cfgSignatureEnable && cfgSignatureUseForRequest) {
    const authorization = req.get(cfgSignatureAuthorizationHeader);
    if (authorization && authorization.startsWith(cfgSignatureAuthorizationHeaderPrefix)) {
      const token = authorization.substring(cfgSignatureAuthorizationHeaderPrefix.length);
      try {
        jwt.verify(token, cfgSignatureSecret);
      } catch (err) {
        console.log(`checkJwtHeader error: name = ${err.name} message = ${err.message} token = ${token}`);
        res.sendStatus(403);
        return;
      }
    } else {
      res.sendStatus(403);
      return;
    }
  }

  const { fileName } = req.query;
  const userAddress = req.query.useraddress;
  const { ver } = req.query;
  const { file } = req.query;
  let Path = '';

  if (file.includes('diff')) {
    Path = req.DocManager.diffPath(fileName, userAddress, ver);
  } else if (file.includes('prev')) {
    Path = req.DocManager.prevFilePath(fileName, userAddress, ver);
  } else {
    res.sendStatus(403);
    return;
  }

  // add headers to the response to specify the page parameters
  res.setHeader('Content-Length', fileSystem.statSync(Path).size);
  res.setHeader('Content-Type', mime.getType(Path));
  res.setHeader('Content-Disposition', `attachment; filename*=UTF-8''${encodeURIComponent(file)}`);

  const filestream = fileSystem.createReadStream(Path);
  filestream.pipe(res); // send file information to the response by streams
});

app.post('/upload', (req, res) => { // define a handler for uploading files
  req.DocManager = new DocManager(req, res);
  req.DocManager.storagePath(''); // mkdir if not exist

  const userIp = req.DocManager.curUserHostAddress(); // get the path to the user host
  const uploadDir = req.DocManager.storageRootPath(userIp);
  const uploadDirTmp = path.join(uploadDir, 'tmp'); // and create directory for temporary files if it doesn't exist
  req.DocManager.createDirectory(uploadDirTmp);

  const form = new formidable.IncomingForm(); // create a new incoming form
  form.uploadDir = uploadDirTmp; // and write there all the necessary parameters
  form.keepExtensions = true;

  form.parse(req, (err, fields, files) => { // parse this form
    if (err) { // if an error occurs
      // DocManager.cleanFolderRecursive(uploadDirTmp, true);  // clean the folder with temporary files
      res.writeHead(200, { 'Content-Type': 'text/plain' }); // and write the error status and message to the response
      res.write(`{ "error": "${err.message}"}`);
      res.end();
      return;
    }

    const file = files.uploadedFile;

    if (file === undefined) { // if file parameter is undefined
      res.writeHead(200, { 'Content-Type': 'text/plain' }); // write the error status and message to the response
      res.write('{ "error": "Uploaded file not found"}');
      res.end();
      return;
    }

    file.name = req.DocManager.getCorrectName(file.name);

    // check if the file size exceeds the maximum file size
    if (configServer.get('maxFileSize') < file.size || file.size <= 0) {
      // DocManager.cleanFolderRecursive(uploadDirTmp, true);  // clean the folder with temporary files
      res.writeHead(200, { 'Content-Type': 'text/plain' });
      res.write('{ "error": "File size is incorrect"}');
      res.end();
      return;
    }

    const exts = fileUtility.getSuppotredExtensions(); // all the supported file extensions
    const curExt = fileUtility.getFileExtension(file.name, true);
    const documentType = fileUtility.getFileType(file.name);

    if (exts.indexOf(curExt) === -1) { // check if the file extension is supported
      // DocManager.cleanFolderRecursive(uploadDirTmp, true);  // if not, clean the folder with temporary files
      res.writeHead(200, { 'Content-Type': 'text/plain' }); // and write the error status and message to the response
      res.write('{ "error": "File type is not supported"}');
      res.end();
      return;
    }

    fileSystem.rename(file.path, `${uploadDir}/${file.name}`, (error) => { // rename a file
      // DocManager.cleanFolderRecursive(uploadDirTmp, true);  // clean the folder with temporary files
      res.writeHead(200, { 'Content-Type': 'text/plain' });
      if (error) { // if an error occurs
        res.write(`{ "error": "${error}"}`); // write an error message to the response
      } else {
        // otherwise, write a new file name to the response
        res.write(`{ "filename": "${file.name}", "documentType": "${documentType}" }`);

        // get user id and name parameters or set them to the default values
        const user = users.getUser(req.query.userid);

        req.DocManager.saveFileData(file.name, user.id, user.name);
      }
      res.end();
    });
  });
});

app.post('/create', (req, res) => {
  const { title } = req.body;
  const fileUrl = req.body.url;

  try {
    req.DocManager = new DocManager(req, res);
    req.DocManager.storagePath(''); // mkdir if not exist

    const fileName = req.DocManager.getCorrectName(title);
    const userAddress = req.DocManager.curUserHostAddress();
    req.DocManager.historyPath(fileName, userAddress, true);

    urllib.request(fileUrl, { method: 'GET' }, (err, data) => {
      // check if the file size exceeds the maximum file size
      if (configServer.get('maxFileSize') < data.length || data.length <= 0) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.write(JSON.stringify({ error: 'File size is incorrect' }));
        res.end();
        return;
      }

      const exts = fileUtility.getSuppotredExtensions(); // all the supported file extensions
      const curExt = fileUtility.getFileExtension(fileName, true);

      if (exts.indexOf(curExt) === -1) { // check if the file extension is supported
        // and write the error status and message to the response
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.write(JSON.stringify({ error: 'File type is not supported' }));
        res.end();
        return;
      }

      fileSystem.writeFileSync(req.DocManager.storagePath(fileName), data);

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.write(JSON.stringify({ file: fileName }));
      res.end();
    });
  } catch (e) {
    res.status(500);
    res.write(JSON.stringify({
      error: 1,
      message: e.message,
    }));
    res.end();
  }
});

app.post('/convert', (req, res) => { // define a handler for converting files
  req.DocManager = new DocManager(req, res);

  const fileName = fileUtility.getFileName(req.body.filename);
  const filePass = req.body.filePass ? req.body.filePass : null;
  const lang = req.body.lang ? req.body.lang : null;
  const fileUri = req.DocManager.getDownloadUrl(fileName, true);
  const fileExt = fileUtility.getFileExtension(fileName, true);
  const internalFileExt = 'ooxml';
  const response = res;

  const writeResult = function writeResult(filename, step, error) {
    const result = {};

    // write file name, step and error values to the result object if they are defined
    if (filename !== null) result.filename = filename;

    if (step !== null) result.step = step;

    if (error !== null) result.error = error;

    response.setHeader('Content-Type', 'application/json');
    response.write(JSON.stringify(result));
    response.end();
  };

  const callback = async function callback(err, resp) {
    if (err) { // if an error occurs
      // check what type of error it is
      if (err.name === 'ConnectionTimeoutError' || err.name === 'ResponseTimeoutError') {
        writeResult(fileName, 0, null); // despite the timeout errors, write the file to the result object
      } else {
        writeResult(null, null, JSON.stringify(err)); // other errors trigger an error message
      }
      return;
    }

    try {
      const responseData = documentService.getResponseUri(resp.toString());
      const result = responseData.percent;
      const newFileUri = responseData.uri; // get the callback url
      const newFileType = `.${responseData.fileType}`; // get the file type

      if (result !== 100) { // if the status isn't 100
        writeResult(fileName, result, null); // write the origin file to the result object
        return;
      }

      // get the file name with a new extension
      const correctName = req.DocManager.getCorrectName(fileUtility.getFileName(fileName, true) + newFileType);

      const { status, data } = await urllib.request(newFileUri, { method: 'GET' });

      if (status !== 200) throw new Error(`Conversion service returned status: ${status}`);

      // write a file with a new extension, but with the content from the origin file
      fileSystem.writeFileSync(req.DocManager.storagePath(correctName), data);
      fileSystem.unlinkSync(req.DocManager.storagePath(fileName)); // remove file with the origin extension

      const userAddress = req.DocManager.curUserHostAddress();
      const historyPath = req.DocManager.historyPath(fileName, userAddress, true);
      // get the history path to the file with a new extension
      const correctHistoryPath = req.DocManager.historyPath(correctName, userAddress, true);

      fileSystem.renameSync(historyPath, correctHistoryPath); // change the previous history path

      fileSystem.renameSync(
        path.join(correctHistoryPath, `${fileName}.txt`),
        path.join(correctHistoryPath, `${correctName}.txt`),
      ); // change the name of the .txt file with document information

      writeResult(correctName, result, null); // write a file with a new name to the result object
    } catch (e) {
      console.log(e); // display error message in the console
      writeResult(null, null, e.message);
    }
  };

  try {
    // check if the file with such an extension can be converted
    if (fileUtility.getConvertExtensions().indexOf(fileExt) !== -1) {
      const storagePath = req.DocManager.storagePath(fileName);
      const stat = fileSystem.statSync(storagePath);
      let key = fileUri + stat.mtime.getTime();

      key = documentService.generateRevisionId(key); // get document key
      // get the url to the converted file
      documentService.getConvertedUri(fileUri, fileExt, internalFileExt, key, true, callback, filePass, lang);
    } else {
      // if the file with such an extension can't be converted, write the origin file to the result object
      writeResult(fileName, null, null);
    }
  } catch (ex) {
    console.log(ex);
    writeResult(null, null, 'Server error');
  }
});

app.get('/files', (req, res) => { // define a handler for getting files information
  try {
    req.DocManager = new DocManager(req, res);
    // get the information about the files from the storage path
    const filesInDirectoryInfo = req.DocManager.getFilesInfo();
    res.setHeader('Content-Type', 'application/json');
    res.write(JSON.stringify(filesInDirectoryInfo)); // transform files information into the json string
  } catch (ex) {
    console.log(ex);
    res.write('Server error');
  }
  res.end();
});

app.get('/files/file/:fileId', (req, res) => { // define a handler for getting file information by its id
  try {
    req.DocManager = new DocManager(req, res);
    const { fileId } = req.params;
    // get the information about the file specified by a file id
    const fileInfoById = req.DocManager.getFilesInfo(fileId);
    res.setHeader('Content-Type', 'application/json');
    res.write(JSON.stringify(fileInfoById));
  } catch (ex) {
    console.log(ex);
    res.write('Server error');
  }
  res.end();
});

app.delete('/file', (req, res) => { // define a handler for removing file
  try {
    req.DocManager = new DocManager(req, res);
    let fileName = req.query.filename;
    if (fileName) { // if the file name is defined
      fileName = fileUtility.getFileName(fileName); // get its part without an extension

      req.DocManager.fileRemove(fileName); // delete file and his history
    } else {
      // if the file name is undefined, clean the storage folder
      req.DocManager.cleanFolderRecursive(req.DocManager.storagePath(''), false);
    }

    res.write('{"success":true}');
  } catch (ex) {
    console.log(ex);
    res.write('Server error');
  }
  res.end();
});

app.get('/csv', (req, res) => { // define a handler for downloading csv files
  const fileName = 'csv.csv';
  const csvPath = path.join(__dirname, 'public', 'assets', 'document-templates', 'sample', fileName);

  // add headers to the response to specify the page parameters
  res.setHeader('Content-Length', fileSystem.statSync(csvPath).size);
  res.setHeader('Content-Type', mime.getType(csvPath));

  res.setHeader('Content-Disposition', `attachment; filename*=UTF-8''${encodeURIComponent(fileName)}`);

  const filestream = fileSystem.createReadStream(csvPath);
  filestream.pipe(res); // send file information to the response by streams
});

app.post('/reference', (req, res) => { // define a handler for renaming file
  req.DocManager = new DocManager(req, res);

  const result = function result(data) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.write(JSON.stringify(data));
    res.end();
  };

  const { referenceData } = req.body;
  let fileName = '';
  let userAddress = '';
  if (referenceData) {
    const { instanceId } = referenceData;

    if (instanceId === req.DocManager.getInstanceId()) {
      const fileKey = JSON.parse(referenceData.fileKey);
      ({ userAddress } = fileKey);

      if (userAddress === req.DocManager.curUserHostAddress()
                && req.DocManager.existsSync(req.DocManager.storagePath(fileKey.fileName, userAddress))) {
        ({ fileName } = fileKey);
      }
    }
  }

  if (!fileName && !!req.body.link) {
    if (req.body.link.indexOf(req.DocManager.curUserHostAddress()) !== -1) {
      result({ error: 'You do not have access to this site' });
      return;
    }

    const urlObj = urlModule.parse(req.body.link, true);
    fileName = urlObj.query.fileName;
    if (!req.DocManager.existsSync(req.DocManager.storagePath(fileName, userAddress))) {
      result({ error: 'File is not exist' });
      return;
    }
  }

  if (!fileName && !!req.body.path) {
    const filePath = fileUtility.getFileName(req.body.path);

    if (req.DocManager.existsSync(req.DocManager.storagePath(filePath, userAddress))) {
      fileName = filePath;
    }
  }

  if (!fileName) {
    result({ error: 'File is not found' });
    return;
  }

  const data = {
    fileType: fileUtility.getFileExtension(fileName).slice(1),
    key: req.DocManager.getKey(fileName),
    url: req.DocManager.getDownloadUrl(fileName, true),
    directUrl: req.body.directUrl ? req.DocManager.getDownloadUrl(fileName) : null,
    referenceData: {
      fileKey: JSON.stringify({ fileName, userAddress: req.DocManager.curUserHostAddress() }),
      instanceId: req.DocManager.getServerUrl(),
    },
    link: `${req.DocManager.getServerUrl()}/editor?fileName=${encodeURIComponent(fileName)}`,
    path: fileName,
  };

  if (cfgSignatureEnable) {
    // sign token with given data using signature secret
    data.token = jwt.sign(data, cfgSignatureSecret, { expiresIn: cfgSignatureSecretExpiresIn });
  }

  result(data);
});

app.put('/restore', (req, res) => { // define a handler for restore file version
  const { fileName } = req.body;
  const result = {};
  if (fileName) {
    req.DocManager = new DocManager(req, res);
    const userAddress = req.DocManager.curUserHostAddress();
    const key = req.DocManager.getKey(fileName);
    const { version } = req.body;
    const filePath = req.DocManager.storagePath(fileName, userAddress);
    const historyPath = req.DocManager.historyPath(fileName, userAddress);
    const newVersion = req.DocManager.countVersion(historyPath) + 1;
    const versionPath = path.join(`${historyPath}`, `${version}`, `prev${fileUtility.getFileExtension(fileName)}`);
    const newVersionPath = path.join(`${historyPath}`, `${newVersion}`);

    if (fileSystem.existsSync(versionPath)) {
      req.DocManager.createDirectory(newVersionPath);
      req.DocManager.copyFile(
        filePath,
        path.join(`${newVersionPath}`, `prev${fileUtility.getFileExtension(fileName)}`),
      );
      fileSystem.writeFileSync(path.join(`${newVersionPath}`, 'key.txt'), key);
      req.DocManager.copyFile(versionPath, filePath);
      result.success = true;
    } else {
      result.success = false;
      result.error = 'Version path does not exists';
    }
  } else {
    result.success = false;
    result.error = 'Filename is empty';
  }

  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.write(JSON.stringify(result));
  res.end();
});

app.post('/track', async (req, res) => { // define a handler for tracking file changes
  req.DocManager = new DocManager(req, res);

  let uAddress = req.query.useraddress;
  let fName = fileUtility.getFileName(req.query.filename);
  let version = 0;

  // track file changes
  const processTrack = async function processTrack(response, bodyTrack, fileNameTrack, userAddressTrack) {
    // callback file saving process
    const callbackProcessSave = async function callbackProcessSave(
      downloadUri,
      body,
      fileName,
      userAddress,
      newFileName,
    ) {
      try {
        const { status, data } = await urllib.request(downloadUri, { method: 'GET' });

        if (status !== 200) throw new Error(`Document editing service returned status: ${status}`);

        const storagePath = req.DocManager.storagePath(newFileName, userAddress);

        let historyPath = req.DocManager.historyPath(newFileName, userAddress); // get the path to the history data
        if (historyPath === '') { // if the history path doesn't exist
          historyPath = req.DocManager.historyPath(newFileName, userAddress, true); // create it
          req.DocManager.createDirectory(historyPath); // and create a directory for the history data
        }

        const countVersion = req.DocManager.countVersion(historyPath); // get the next file version number
        version = countVersion + 1;
        // get the path to the specified file version
        const versionPath = req.DocManager.versionPath(newFileName, userAddress, version);
        req.DocManager.createDirectory(versionPath); // create a directory to the specified file version

        const downloadZip = body.changesurl;
        if (downloadZip) {
          // get the path to the file with document versions differences
          const pathChanges = req.DocManager.diffPath(newFileName, userAddress, version);
          const zip = await urllib.request(downloadZip, { method: 'GET' });
          const statusZip = zip.status;
          const dataZip = zip.data;
          if (status === 200) {
            fileSystem.writeFileSync(pathChanges, dataZip); // write the document version differences to the archive
          } else {
            emitWarning(`Document editing service returned status: ${statusZip}`);
          }
        }

        const changeshistory = body.changeshistory || JSON.stringify(body.history);
        if (changeshistory) {
          // get the path to the file with document changes
          const pathChangesJson = req.DocManager.changesPath(newFileName, userAddress, version);
          fileSystem.writeFileSync(pathChangesJson, changeshistory); // and write this data to the path in json format
        }

        const pathKey = req.DocManager.keyPath(newFileName, userAddress, version); // get the path to the key.txt file
        fileSystem.writeFileSync(pathKey, body.key); // write the key value to the key.txt file

        // get the path to the previous file version
        const pathPrev = path.join(versionPath, `prev${fileUtility.getFileExtension(fileName)}`);
        // and write it to the current path
        fileSystem.renameSync(req.DocManager.storagePath(fileName, userAddress), pathPrev);

        fileSystem.writeFileSync(storagePath, data);

        // get the path to the forcesaved file
        const forcesavePath = req.DocManager.forcesavePath(newFileName, userAddress, false);
        if (forcesavePath !== '') { // if this path is empty
          fileSystem.unlinkSync(forcesavePath); // remove it
        }
      } catch (ex) {
        console.log(ex);
        response.write('{"error":1}');
        response.end();
        return;
      }

      response.write('{"error":0}');
      response.end();
    };

    // file saving process
    const processSave = async function processSave(downloadUri, body, fileName, userAddress) {
      if (!downloadUri) {
        response.write('{"error":1}');
        response.end();
        return;
      }

      const curExt = fileUtility.getFileExtension(fileName); // get current file extension
      const downloadExt = `.${body.filetype}`; // get the extension of the downloaded file

      let newFileName = fileName;

      // convert downloaded file to the file with the current extension if these extensions aren't equal
      if (downloadExt !== curExt) {
        const key = documentService.generateRevisionId(downloadUri);
        // get the correct file name if it already exists
        newFileName = req.DocManager.getCorrectName(fileUtility.getFileName(fileName, true) + downloadExt, userAddress);
        try {
          documentService.getConvertedUriSync(downloadUri, downloadExt, curExt, key, async (err, data) => {
            if (err) {
              await callbackProcessSave(downloadUri, body, fileName, userAddress, newFileName);
              return;
            }
            try {
              const resp = documentService.getResponseUri(data);
              await callbackProcessSave(resp.uri, body, fileName, userAddress, fileName);
            } catch (ex) {
              console.log(ex);
              await callbackProcessSave(downloadUri, body, fileName, userAddress, newFileName);
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
    const callbackProcessForceSave = async function callbackProcessForceSave(
      downloadUri,
      body,
      fileName,
      userAddress,
      newFileName = false,
    ) {
      try {
        const { status, data } = await urllib.request(downloadUri, { method: 'GET' });

        if (status !== 200) throw new Error(`Document editing service returned status: ${status}`);

        const downloadExt = `.${body.fileType}`;
        const isSubmitForm = body.forcesavetype === 3; // SubmitForm
        let correctName = fileName;
        let forcesavePath = '';

        if (isSubmitForm) {
          // new file
          if (newFileName) {
            correctName = req.DocManager.getCorrectName(
              `${fileUtility.getFileName(fileName, true)}-form${downloadExt}`,
              userAddress,
            );
          } else {
            const ext = fileUtility.getFileExtension(fileName);
            correctName = req.DocManager.getCorrectName(
              `${fileUtility.getFileName(fileName, true)}-form${ext}`,
              userAddress,
            );
          }
          forcesavePath = req.DocManager.storagePath(correctName, userAddress);
        } else {
          if (newFileName) {
            correctName = req.DocManager.getCorrectName(fileUtility.getFileName(
              fileName,
              true,
            ) + downloadExt, userAddress);
          }
          // create forcesave path if it doesn't exist
          forcesavePath = req.DocManager.forcesavePath(correctName, userAddress, false);
          if (forcesavePath === '') {
            forcesavePath = req.DocManager.forcesavePath(correctName, userAddress, true);
          }
        }

        fileSystem.writeFileSync(forcesavePath, data);

        if (isSubmitForm) {
          const uid = body.actions[0].userid;
          req.DocManager.saveFileData(correctName, uid, 'Filling Form', userAddress);
        }
      } catch (ex) {
        response.write('{"error":1}');
        response.end();
        return;
      }

      response.write('{"error":0}');
      response.end();
    };

    // file force saving process
    const processForceSave = async function processForceSave(downloadUri, body, fileName, userAddress) {
      if (!downloadUri) {
        response.write('{"error":1}');
        response.end();
        return;
      }

      const curExt = fileUtility.getFileExtension(fileName);
      const downloadExt = `.${body.filetype}`;

      // convert downloaded file to the file with the current extension if these extensions aren't equal
      if (downloadExt !== curExt) {
        const key = documentService.generateRevisionId(downloadUri);
        try {
          documentService.getConvertedUriSync(downloadUri, downloadExt, curExt, key, async (err, data) => {
            if (err) {
              await callbackProcessForceSave(downloadUri, body, fileName, userAddress, true);
              return;
            }
            try {
              const resp = documentService.getResponseUri(data);
              await callbackProcessForceSave(resp.uri, body, fileName, userAddress, false);
            } catch (ex) {
              console.log(ex);
              await callbackProcessForceSave(downloadUri, body, fileName, userAddress, true);
            }
          });
          return;
        } catch (ex) {
          console.log(ex);
        }
      }
      await callbackProcessForceSave(downloadUri, body, fileName, userAddress, false);
    };

    if (bodyTrack.status === 1) { // editing
      if (bodyTrack.actions && bodyTrack.actions[0].type === 0) { // finished edit
        const user = bodyTrack.actions[0].userid;
        if (bodyTrack.users.indexOf(user) === -1) {
          const { key } = bodyTrack;
          try {
            documentService.commandRequest('forcesave', key); // call the forcesave command
          } catch (ex) {
            console.log(ex);
          }
        }
      }
    } else if (bodyTrack.status === 2 || bodyTrack.status === 3) { // MustSave, Corrupted
      await processSave(bodyTrack.url, bodyTrack, fileNameTrack, userAddressTrack); // save file
      return;
    } else if (bodyTrack.status === 6 || bodyTrack.status === 7) { // MustForceSave, CorruptedForceSave
      await processForceSave(bodyTrack.url, bodyTrack, fileNameTrack, userAddressTrack); // force save file
      return;
    }

    response.write('{"error":0}');
    response.end();
  };

  // read request body
  const readbody = async function readbody(request, response, fileName, userAddress) {
    let content = '';
    request.on('data', async (data) => { // get data from the request
      content += data;
    });
    request.on('end', async () => {
      const body = JSON.parse(content);
      await processTrack(response, body, fileName, userAddress); // and track file changes
    });
  };

  // check jwt token
  if (cfgSignatureEnable && cfgSignatureUseForRequest) {
    let body = null;
    if (req.body.hasOwnProperty('token')) { // if request body has its own token
      body = documentService.readToken(req.body.token); // read and verify it
    } else {
      const checkJwtHeaderRes = documentService.checkJwtHeader(req); // otherwise, check jwt token headers
      if (checkJwtHeaderRes) { // if they exist
        if (checkJwtHeaderRes.payload) {
          body = checkJwtHeaderRes.payload; // get the payload object
        }
        // get user address and file name from the query
        if (checkJwtHeaderRes.query) {
          if (checkJwtHeaderRes.query.useraddress) {
            uAddress = checkJwtHeaderRes.query.useraddress;
          }
          if (checkJwtHeaderRes.query.filename) {
            fName = fileUtility.getFileName(checkJwtHeaderRes.query.filename);
          }
        }
      }
    }
    if (!body) {
      res.write('{"error":1}');
      res.end();
      return;
    }
    await processTrack(res, body, fName, uAddress);
    return;
  }

  if (req.body.hasOwnProperty('status')) { // if the request body has status parameter
    await processTrack(res, req.body, fName, uAddress); // track file changes
  } else {
    await readbody(req, res, fName, uAddress); // otherwise, read request body first
  }
});

app.get('/editor', (req, res) => { // define a handler for editing document
  try {
    req.DocManager = new DocManager(req, res);

    const fileName = fileUtility.getFileName(req.query.fileName);
    let { fileExt } = req.query;
    const lang = req.DocManager.getLang();
    const user = users.getUser(req.query.userid);
    const userDirectUrl = req.query.directUrl === 'true';

    const userid = user.id;
    const { name } = user;

    let actionData = 'null';
    if (req.query.action) {
      try {
        actionData = JSON.stringify(JSON.parse(req.query.action));
      } catch (ex) {
        console.log(ex);
      }
    }

    let type = req.query.type || ''; // type: embedded/mobile/desktop
    if (type === '') {
      type = new RegExp(configServer.get('mobileRegEx'), 'i').test(req.get('User-Agent')) ? 'mobile' : 'desktop';
    } else if (type !== 'mobile'
            && type !== 'embedded') {
      type = 'desktop';
    }

    const templatesImageUrl = req.DocManager.getTemplateImageUrl(fileUtility.getFileType(fileName));
    const createUrl = req.DocManager.getCreateUrl(fileUtility.getFileType(fileName), userid, type, lang);
    const templates = [
      {
        image: '',
        title: 'Blank',
        url: createUrl,
      },
      {
        image: templatesImageUrl,
        title: 'With sample content',
        url: `${createUrl}&sample=true`,
      },
    ];

    const userGroup = user.group;
    const { reviewGroups } = user;
    const { commentGroups } = user;
    const { userInfoGroups } = user;

    if (fileExt) {
      // create demo document of a given extension
      const fName = req.DocManager.createDemo(!!req.query.sample, fileExt, userid, name, false);

      // get the redirect path
      const redirectPath = `${req.DocManager.getServerUrl()}/editor?fileName=`
      + `${encodeURIComponent(fName)}${req.DocManager.getCustomParams()}`;
      res.redirect(redirectPath);
      return;
    }
    fileExt = fileUtility.getFileExtension(fileName);

    const userAddress = req.DocManager.curUserHostAddress();
    // if the file with a given name doesn't exist
    if (!req.DocManager.existsSync(req.DocManager.storagePath(fileName, userAddress))) {
      throw new Error(`File not found: ${fileName}`); // display error message
    }
    const key = req.DocManager.getKey(fileName);
    const url = req.DocManager.getDownloadUrl(fileName, true);
    const directUrl = req.DocManager.getDownloadUrl(fileName);
    let mode = req.query.mode || 'edit'; // mode: view/edit/review/comment/fillForms/embedded

    let canEdit = fileUtility.getEditExtensions().indexOf(fileExt.slice(1)) !== -1; // check if this file can be edited
    if (((!canEdit && mode === 'edit') || mode === 'fillForms')
      && fileUtility.getFillExtensions().indexOf(fileExt.slice(1)) !== -1) {
      mode = 'fillForms';
      canEdit = true;
    }
    if (!canEdit && mode === 'edit') {
      mode = 'view';
    }
    const submitForm = mode === 'fillForms' && userid === 'uid-1';

    // file config data
    const argss = {
      apiUrl: siteUrl + configServer.get('apiUrl'),
      file: {
        name: fileName,
        ext: fileUtility.getFileExtension(fileName, true),
        uri: url,
        directUrl: !userDirectUrl ? null : directUrl,
        uriUser: directUrl,
        created: new Date().toDateString(),
        favorite: user.favorite != null ? user.favorite : 'null',
      },
      editor: {
        type,
        documentType: fileUtility.getFileType(fileName),
        key,
        token: '',
        callbackUrl: req.DocManager.getCallback(fileName),
        createUrl: userid !== 'uid-0' ? createUrl : null,
        templates: user.templates ? templates : null,
        isEdit: canEdit && (mode === 'edit' || mode === 'view' || mode === 'filter' || mode === 'blockcontent'),
        review: canEdit && (mode === 'edit' || mode === 'review'),
        chat: userid !== 'uid-0',
        coEditing: mode === 'view' && userid === 'uid-0' ? { mode: 'strict', change: false } : null,
        comment: mode !== 'view' && mode !== 'fillForms' && mode !== 'embedded' && mode !== 'blockcontent',
        fillForms: mode !== 'view' && mode !== 'comment' && mode !== 'embedded' && mode !== 'blockcontent',
        modifyFilter: mode !== 'filter',
        modifyContentControl: mode !== 'blockcontent',
        copy: !user.deniedPermissions.includes('copy'),
        download: !user.deniedPermissions.includes('download'),
        print: !user.deniedPermissions.includes('print'),
        mode: mode !== 'view' ? 'edit' : 'view',
        canBackToFolder: type !== 'embedded',
        backUrl: `${req.DocManager.getServerUrl()}/`,
        curUserHostAddress: req.DocManager.curUserHostAddress(),
        lang,
        userid: userid !== 'uid-0' ? userid : null,
        name,
        userGroup,
        reviewGroups: JSON.stringify(reviewGroups),
        commentGroups: JSON.stringify(commentGroups),
        userInfoGroups: JSON.stringify(userInfoGroups),
        fileChoiceUrl,
        submitForm,
        plugins: JSON.stringify(plugins),
        actionData,
        fileKey: userid !== 'uid-0'
          ? JSON.stringify({ fileName, userAddress: req.DocManager.curUserHostAddress() }) : null,
        instanceId: userid !== 'uid-0' ? req.DocManager.getInstanceId() : null,
        protect: !user.deniedPermissions.includes('protect'),
      },
      dataInsertImage: {
        fileType: 'png',
        url: `${req.DocManager.getServerUrl(true)}/images/logo.png`,
        directUrl: !userDirectUrl ? null : `${req.DocManager.getServerUrl()}/images/logo.png`,
      },
      dataDocument: {
        fileType: 'docx',
        url: `${req.DocManager.getServerUrl(true)}/assets/document-templates/sample/sample.docx`,
        directUrl: !userDirectUrl
          ? null
          : `${req.DocManager.getServerUrl()}/assets/document-templates/sample/sample.docx`,
      },
      dataSpreadsheet: {
        fileType: 'csv',
        url: `${req.DocManager.getServerUrl(true)}/csv`,
        directUrl: !userDirectUrl ? null : `${req.DocManager.getServerUrl()}/csv`,
      },
      usersForMentions: user.id !== 'uid-0' ? users.getUsersForMentions(user.id) : null,
      usersForProtect: user.id !== 'uid-0' ? users.getUsersForProtect(user.id) : null,
    };

    if (cfgSignatureEnable) {
      app.render('config', argss, (err, html) => { // render a config template with the parameters specified
        if (err) {
          console.log(err);
        } else {
          // sign token with given data using signature secret
          argss.editor.token = jwt.sign(
            JSON.parse(`{${html}}`),
            cfgSignatureSecret,
            { expiresIn: cfgSignatureSecretExpiresIn },
          );
          argss.dataInsertImage.token = jwt.sign(
            argss.dataInsertImage,
            cfgSignatureSecret,
            { expiresIn: cfgSignatureSecretExpiresIn },
          );
          argss.dataDocument.token = jwt.sign(
            argss.dataDocument,
            cfgSignatureSecret,
            { expiresIn: cfgSignatureSecretExpiresIn },
          );
          argss.dataSpreadsheet.token = jwt.sign(
            argss.dataSpreadsheet,
            cfgSignatureSecret,
            { expiresIn: cfgSignatureSecretExpiresIn },
          );
        }
        res.render('editor', argss); // render the editor template with the parameters specified
      });
    } else {
      res.render('editor', argss);
    }
  } catch (ex) {
    console.log(ex);
    res.status(500);
    res.render('error', { message: `Server error: ${ex.message}` });
  }
});

app.post('/rename', (req, res) => { // define a handler for renaming file
  let { newfilename } = req.body;
  const origExt = req.body.ext;
  const curExt = fileUtility.getFileExtension(newfilename, true);
  if (curExt !== origExt) {
    newfilename += `.${origExt}`;
  }

  const { dockey } = req.body;
  const meta = { title: newfilename };

  const result = function result(err, data, ress) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.write(JSON.stringify({ result: ress }));
    res.end();
  };

  documentService.commandRequest('meta', dockey, result, meta);
});

app.post('/historyObj', (req, res) => {
  req.DocManager = new DocManager(req, res);
  const { fileName } = req.body;
  const { directUrl } = req.body || null;
  const historyObj = req.DocManager.getHistoryObject(fileName, null, directUrl);

  if (cfgSignatureEnable) {
    for (let i = 0; i < historyObj.historyData.length; i++) {
      // sign token with given data using signature secret
      historyObj.historyData[i].token = jwt.sign(
        historyObj.historyData[i],
        cfgSignatureSecret,
        { expiresIn: cfgSignatureSecretExpiresIn },
      );
    }
  }
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.write(JSON.stringify(historyObj));
  res.end();
});

wopiApp.registerRoutes(app);

// "Not found" error with 404 status
app.use((req, res, next) => {
  const err = new Error('Not Found');
  err.status = 404;
  next(err);
});

// render the error template with the parameters specified
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  res.status(err.status || 500);
  res.render('error', {
    message: err.message,
  });
});

// save all the functions to the app module to export it later in other files
module.exports = app;
