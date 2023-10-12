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

const path = require('path');
const fileSystem = require('fs');
const configServer = require('config').get('server');
const fileUtility = require('./fileUtility');
const documentService = require('./documentService');

const storageConfigFolder = configServer.get('storageFolder');

const DocManager = function DocManager(req, res) {
  this.req = req;
  this.res = res;
};

// check if the path exists or not
DocManager.prototype.existsSync = function existsSync(directory) {
  let res = true;
  try {
    // synchronously test the user's permissions for the directory specified by path;
    // the directory is visible to the calling process
    fileSystem.accessSync(directory, fileSystem.F_OK);
  } catch (e) { // the response is set to false, if an error occurs
    res = false;
  }
  return res;
};

// create a new directory if it doesn't exist
DocManager.prototype.createDirectory = function createDirectory(directory) {
  if (!this.existsSync(directory)) {
    fileSystem.mkdirSync(directory);
  }
};

// get the language from the request
DocManager.prototype.getLang = function getLang() {
  if (/^[a-z]{2}(-[A-Z]{2})?$/i.test(this.req.query.lang)) {
    return this.req.query.lang;
  } // the default language value is English
  return 'en';
};

// get customization parameters
DocManager.prototype.getCustomParams = function getCustomParams() {
  let params = '';

  const { userid } = this.req.query; // user id
  params += (userid ? `&userid=${userid}` : '');

  const { lang } = this.req.query; // language
  params += (lang ? `&lang=${this.getLang()}` : '');

  const { directUrl } = this.req.query; // directUrl
  params += (directUrl ? `&directUrl=${directUrl === 'true'}` : '');

  const { fileName } = this.req.query; // file name
  params += (fileName ? `&fileName=${fileName}` : '');

  const { mode } = this.req.query; // mode: view/edit/review/comment/fillForms/embedded
  params += (mode ? `&mode=${mode}` : '');

  const { type } = this.req.query; // type: embedded/mobile/desktop
  params += (type ? `&type=${type}` : '');

  return params;
};

// get the correct file name if such a name already exists
DocManager.prototype.getCorrectName = function getCorrectName(fileName, userAddress) {
  const baseName = fileUtility.getFileName(fileName, true); // get file name from the url without extension
  const ext = fileUtility.getFileExtension(fileName); // get file extension from the url
  let name = baseName + ext; // get full file name
  let index = 1;

  // if the file with such a name already exists in this directory
  while (this.existsSync(this.storagePath(name, userAddress))) {
    name = `${baseName} (${index})${ext}`; // add an index after its base name
    index += 1;
  }

  return name;
};

// processes a request editnew
DocManager.prototype.requestEditnew = function requestEditnew(req, fileName, user) {
  let correctName = fileName;
  if (req.params.id !== fileName) { // processes a repeated request editnew
    this.fileRemove(req.params.id);
    correctName = this.getCorrectName(req.params.id);
  }
  this.fileSizeZero(correctName);
  this.saveFileData(correctName, user.id, user.name);

  return correctName;
};

// delete a file with its history
DocManager.prototype.fileRemove = function fileRemove(fileName) {
  const filePath = this.storagePath(fileName); // get the path to this file
  fileSystem.unlinkSync(filePath); // and delete it

  const userAddress = this.curUserHostAddress();
  const historyPath = this.historyPath(fileName, userAddress, true);
  this.cleanFolderRecursive(historyPath, true); // clean all the files from the history folder
};

// create a zero-size file
DocManager.prototype.fileSizeZero = function fileSizeZero(fileName) {
  const storagePath = this.storagePath(fileName);
  const fh = fileSystem.openSync(storagePath, 'w');
  fileSystem.closeSync(fh);
};

// create demo document
// eslint-disable-next-line no-unused-vars
DocManager.prototype.createDemo = function createDemo(isSample, fileExt, userid, username, wopi) {
  const demoName = `${isSample ? 'sample' : 'new'}.${fileExt}`;
  const fileName = this.getCorrectName(demoName); // get the correct file name if such a name already exists

  // copy sample document of a necessary extension to the storage path
  this.copyFile(path.join(__dirname, '..', 'public', 'assets', 'document-templates', isSample
    ? 'sample' : 'new', demoName), this.storagePath(fileName));

  this.saveFileData(fileName, userid, username); // save file data to the file

  return fileName;
};

// save file data to the file
DocManager.prototype.saveFileData = function saveFileData(fileName, userid, username, userAddress) {
  let address = userAddress;
  if (!address) {
    address = this.curUserHostAddress(); // get current user host address
  }
  // get full creation date of the document
  const dateCreate = fileSystem.statSync(this.storagePath(fileName, address)).mtime;
  const minutes = (dateCreate.getMinutes() < 10 ? '0' : '') + dateCreate.getMinutes().toString();
  const month = (dateCreate.getMonth() < 10 ? '0' : '') + (parseInt(dateCreate.getMonth().toString(), 10) + 1);
  const sec = (dateCreate.getSeconds() < 10 ? '0' : '') + dateCreate.getSeconds().toString();
  const dateFormat = `${dateCreate.getFullYear()}-${month}-${dateCreate.getDate()} `
    + `${dateCreate.getHours()}:${minutes}:${sec}`;

  const fileInfo = this.historyPath(fileName, address, true); // get file history information
  this.createDirectory(fileInfo); // create a new history directory if it doesn't exist

  // write all the file information to a new txt file
  fileSystem.writeFileSync(path.join(fileInfo, `${fileName}.txt`), `${dateFormat},${userid},${username}`);
};

// get file data
DocManager.prototype.getFileData = function getFileData(fileName, userAddress) {
  // get the path to the file with file information
  const history = path.join(this.historyPath(fileName, userAddress, true), `${fileName}.txt`);
  if (!this.existsSync(history)) { // if such a file doesn't exist
    return ['2017-01-01', 'uid-1', 'John Smith']; // return default information
  }

  return ((fileSystem.readFileSync(history)).toString())
    .split(',');
};

// get server url
DocManager.prototype.getServerUrl = function getServerUrl(forDocumentServer) {
  return (forDocumentServer && !!configServer.get('exampleUrl'))
    ? configServer.get('exampleUrl') : this.getServerPath();
};

// get server address from the request
DocManager.prototype.getServerPath = function getServerPath() {
  return this.getServerHost() + (this.req.headers['x-forwarded-path'] || this.req.baseUrl);
};

// get host address from the request
DocManager.prototype.getServerHost = function getServerHost() {
  return `${this.getProtocol()}://${this.req.headers['x-forwarded-host'] || this.req.headers.host}`
    + `${this.req.headers['x-forwarded-prefix'] || ''}`;
};

// get protocol from the request
DocManager.prototype.getProtocol = function getProtocol() {
  return this.req.headers['x-forwarded-proto'] || this.req.protocol;
};

// get callback url
DocManager.prototype.getCallback = function getCallback(fileName) {
  const server = this.getServerUrl(true);
  const hostAddress = this.curUserHostAddress();
  // get callback handler
  const handler = `/track?filename=${encodeURIComponent(fileName)}&useraddress=${encodeURIComponent(hostAddress)}`;

  return server + handler;
};

// get url to the created file
DocManager.prototype.getCreateUrl = function getCreateUrl(docType, userid, type, lang) {
  const server = this.getServerUrl();
  const ext = this.getInternalExtension(docType).replace('.', '');
  const handler = `/editor?fileExt=${ext}&userid=${userid}&type=${type}&lang=${lang}`;

  return server + handler;
};

// get url to download a file
DocManager.prototype.getDownloadUrl = function getDownloadUrl(fileName, forDocumentServer) {
  const server = this.getServerUrl(forDocumentServer);
  let handler = `/download?fileName=${encodeURIComponent(fileName)}`;
  if (forDocumentServer) {
    const hostAddress = this.curUserHostAddress();
    handler += `&useraddress=${encodeURIComponent(hostAddress)}`;
  }

  return server + handler;
};

DocManager.prototype.storageRootPath = function storageRootPath(userAddress) {
  // get the path to the directory for the host address
  return path.join(storageConfigFolder, this.curUserHostAddress(userAddress));
};

// get the storage path of the given file
DocManager.prototype.storagePath = function storagePath(fileName, userAddress) {
  const fileNameExt = fileUtility.getFileName(fileName); // get the file name with extension
  const directory = this.storageRootPath(userAddress);
  this.createDirectory(directory); // create a new directory if it doesn't exist
  return path.join(directory, fileNameExt); // put the given file to this directory
};

// get the path to the forcesaved file version
DocManager.prototype.forcesavePath = function forcesavePath(fileName, userAddress, create) {
  let directory = this.storageRootPath(userAddress);
  if (!this.existsSync(directory)) { // the directory with host address doesn't exist
    return '';
  }
  directory = path.join(directory, `${fileName}-history`); // get the path to the history of the given file
  // the history directory doesn't exist and we are not supposed to create it
  if (!create && !this.existsSync(directory)) {
    return '';
  }
  this.createDirectory(directory); // create history directory if it doesn't exist
  directory = path.join(directory, fileName); // and get the path to the given file
  if (!create && !this.existsSync(directory)) {
    return '';
  }
  return directory;
};

// create the path to the file history
DocManager.prototype.historyPath = function historyPath(fileName, userAddress, create) {
  let directory = this.storageRootPath(userAddress);
  if (!this.existsSync(directory)) {
    return '';
  }
  directory = path.join(directory, `${fileName}-history`);
  if (!create && !this.existsSync(path.join(directory, '1'))) {
    return '';
  }
  return directory;
};

// get the path to the specified file version
DocManager.prototype.versionPath = function versionPath(fileName, userAddress, version) {
  // get the path to the history of a given file or create it if it doesn't exist
  const historyPath = this.historyPath(fileName, userAddress, true);
  return path.join(historyPath, `${version}`);
};

// get the path to the previous file version
DocManager.prototype.prevFilePath = function prevFilePath(fileName, userAddress, version) {
  return path.join(this.versionPath(fileName, userAddress, version), `prev${fileUtility.getFileExtension(fileName)}`);
};

// get the path to the file with document versions differences
DocManager.prototype.diffPath = function diffPath(fileName, userAddress, version) {
  return path.join(this.versionPath(fileName, userAddress, version), 'diff.zip');
};

// get the path to the file with document changes
DocManager.prototype.changesPath = function changesPath(fileName, userAddress, version) {
  return path.join(this.versionPath(fileName, userAddress, version), 'changes.txt');
};

// get the path to the file with key value in it
DocManager.prototype.keyPath = function keyPath(fileName, userAddress, version) {
  return path.join(this.versionPath(fileName, userAddress, version), 'key.txt');
};

// get the path to the file with the user information
DocManager.prototype.changesUser = function changesUser(fileName, userAddress, version) {
  return path.join(this.versionPath(fileName, userAddress, version), 'user.txt');
};

// get all the stored files
DocManager.prototype.getStoredFiles = function getStoredFiles() {
  const userAddress = this.curUserHostAddress();
  const directory = this.storageRootPath(userAddress);
  this.createDirectory(directory);
  const result = [];
  const storedFiles = fileSystem.readdirSync(directory); // read the user host directory contents
  for (let i = 0; i < storedFiles.length; i++) { // run through all the elements from the folder
    const stats = fileSystem.lstatSync(path.join(directory, storedFiles[i])); // save element parameters

    if (!stats.isDirectory()) { // if the element isn't a directory
      const historyPath = this.historyPath(storedFiles[i], userAddress); // get the path to the file history
      let version = 0;
      if (historyPath !== '') { // if the history path exists
        version = this.countVersion(historyPath); // get the last file version
      }

      const time = stats.mtime.getTime(); // get the time of element modification
      const item = { // create an object with element data
        time,
        name: storedFiles[i],
        documentType: fileUtility.getFileType(storedFiles[i]),
        canEdit: fileUtility.getEditExtensions().indexOf(fileUtility.getFileExtension(storedFiles[i], true)) !== -1,
        version: version + 1,
      };

      if (!result.length) { // if the result array is empty
        result.push(item); // push the item object to it
      } else {
        let j = 0;
        for (; j < result.length; j++) {
          if (time > result[j].time) { // otherwise, run through all the objects from the result array
            break;
          }
        }
        result.splice(j, 0, item); // and add new object in ascending order of time
      }
    }
  }
  return result;
};

// get current user host address
DocManager.prototype.curUserHostAddress = function curUserHostAddress(userAddress) {
  let address = userAddress;
  if (!address) { // if user address isn't passed to the function
    // take it from the header or use the remote address
    address = this.req.headers['x-forwarded-for'] || this.req.connection.remoteAddress;
  }

  return address.replace(/[^0-9a-zA-Z.=]/g, '_');
};

// copy file
DocManager.prototype.copyFile = function copyFile(exist, target) {
  fileSystem.writeFileSync(target, fileSystem.readFileSync(exist));
};

// get an internal extension
DocManager.prototype.getInternalExtension = function getInternalExtension(fileType) {
  if (fileType === fileUtility.fileType.word) { // .docx for word type
    return '.docx';
  }

  if (fileType === fileUtility.fileType.cell) { // .xlsx for cell type
    return '.xlsx';
  }

  if (fileType === fileUtility.fileType.slide) { // .pptx for slide type
    return '.pptx';
  }

  return '.docx'; // the default value is .docx
};

// get the template image url
DocManager.prototype.getTemplateImageUrl = function getTemplateImageUrl(fileType) {
  const serverUrl = this.getServerUrl(true);
  if (fileType === fileUtility.fileType.word) { // for word type
    return `${serverUrl}/images/file_docx.svg`;
  }

  if (fileType === fileUtility.fileType.cell) { // for cell type
    return `${serverUrl}/images/file_xlsx.svg`;
  }

  if (fileType === fileUtility.fileType.slide) { // for slide type
    return `${serverUrl}/images/file_pptx.svg`;
  }

  return `${serverUrl}/images/file_docx.svg`; // the default value
};

// get document key
DocManager.prototype.getKey = function getKey(fileName, userAddress) {
  const address = userAddress || this.curUserHostAddress();
  let key = address + fileName; // get document key by adding local file url to the current user host address

  const historyPath = this.historyPath(fileName, address); // get the path to the file history
  if (historyPath !== '') { // if the path to the file history exists
    key += this.countVersion(historyPath); // add file version number to the document key
  }

  const storagePath = this.storagePath(fileName, address); // get the storage path to the given file
  const stat = fileSystem.statSync(storagePath); // get file information
  key += stat.mtime.getTime(); // and add creation time to the document key

  return documentService.generateRevisionId(key); // generate the document key value
};

// get current date
DocManager.prototype.getDate = function getDate(date) {
  const minutes = (date.getMinutes() < 10 ? '0' : '') + date.getMinutes().toString();
  return `${date.getMonth()}/${date.getDate()}/${date.getFullYear()} ${date.getHours()}:${minutes}`;
};

// get changes made in the file
DocManager.prototype.getChanges = function getChanges(fileName) {
  if (this.existsSync(fileName)) { // if the directory with such a file exists
    return JSON.parse(fileSystem.readFileSync(fileName)); // read this file and parse it
  }
  return null;
};

// get the last file version
DocManager.prototype.countVersion = function countVersion(directory) {
  let i = 0;
  while (this.existsSync(path.join(directory, `${i + 1}`))) { // run through all the file versions
    i += 1; // and count them
  }
  return i;
};

DocManager.prototype.getHistoryObject = function getHistoryObject(fileName, userAddr = null, userDirectUrl = null) {
  const userAddress = userAddr || this.curUserHostAddress();
  const historyPath = this.historyPath(fileName, userAddress);
  const key = this.getKey(fileName);
  const directUrl = this.getDownloadUrl(fileName);
  const fileExt = fileUtility.getFileExtension(fileName);
  const url = this.getDownloadUrl(fileName, true);
  const history = [];
  const historyData = [];
  let countVersion = 1;
  let changes = null;
  let keyVersion = key;

  if (historyPath !== '') {
    countVersion = this.countVersion(historyPath) + 1; // get the number of file versions
    for (let i = 1; i <= countVersion; i++) { // get keys to all the file versions
      if (i < countVersion) {
        const keyPath = this.keyPath(fileName, userAddress, i);
        if (!fileSystem.existsSync(keyPath)) continue;
        keyVersion = `${fileSystem.readFileSync(keyPath)}`;
      } else {
        keyVersion = key;
      }
      // write all the file history information
      history.push(this.getHistory(fileName, changes, keyVersion, i));

      const userUrl = i === countVersion ? directUrl : (`${this.getServerUrl(false)}/history?fileName=`
        + `${encodeURIComponent(fileName)}&file=prev${fileExt}&ver=${i}`);
      const historyD = {
        fileType: fileExt.slice(1),
        version: i,
        key: keyVersion,
        url: i === countVersion ? url : (`${this.getServerUrl(true)}/history?fileName=`
          + `${encodeURIComponent(fileName)}&file=prev${fileExt}&ver=${i}&useraddress=${userAddress}`),
        directUrl: !userDirectUrl ? null : userUrl,
      };

      // check if the path to the file with document versions differences exists
      if (i > 1 && this.existsSync(this.diffPath(fileName, userAddress, i - 1))) {
        historyD.previous = { // write information about previous file version
          fileType: historyData[i - 2].fileType,
          key: historyData[i - 2].key,
          url: historyData[i - 2].url,
          directUrl: !userDirectUrl ? null : historyData[i - 2].directUrl,
        };
        const changesUrl = `${this.getServerUrl(true)}/history?fileName=`
          + `${encodeURIComponent(fileName)}&file=diff.zip&ver=${i - 1}&useraddress=${userAddress}`;
        historyD.changesUrl = changesUrl; // get the path to the diff.zip file and write it to the history object
      }

      historyData.push(historyD);

      if (i < countVersion) {
        // get the path to the file with document changes
        const changesFile = this.changesPath(fileName, userAddress, i);
        changes = this.getChanges(changesFile); // get changes made in the file
      }
    }
  } else { // if history path is empty
    // write the history information about the last file version
    history.push(this.getHistory(fileName, changes, keyVersion, countVersion));
    historyData.push({
      fileType: fileExt.slice(1),
      version: countVersion,
      key,
      url,
      directUrl: !userDirectUrl ? null : directUrl,
    });
  }

  return { history, historyData, countVersion };
};
// get file history information
DocManager.prototype.getHistory = function getHistory(fileName, content, keyVersion, version) {
  let oldVersion = false;
  let contentJson = null;
  let fileContent = content;
  let userNameFromJson = null;
  let userIdFromJson = null;
  let createdFromJson = null;
  if (fileContent) { // if content is defined
    if (fileContent.changes && fileContent.changes.length) { // and there are some modifications in the content
      [contentJson] = fileContent.changes; // write these modifications to the json content
    } else if (fileContent.length) {
      [contentJson] = fileContent; // otherwise, write original content to the json content
      oldVersion = true; // and note that this is an old version
    } else {
      fileContent = false;
    }
  }

  const userAddress = this.curUserHostAddress();

  if (content && contentJson) {
    userNameFromJson = oldVersion ? contentJson.username : contentJson.user.name;
    userIdFromJson = oldVersion ? contentJson.userid : contentJson.user.id;
    createdFromJson = oldVersion ? contentJson.date : contentJson.created;
  }

  const username = userNameFromJson || (this.getFileData(fileName, userAddress))[2];
  const userid = userIdFromJson || (this.getFileData(fileName, userAddress))[1];
  const created = createdFromJson || (this.getFileData(fileName, userAddress))[0];
  const res = (fileContent && !oldVersion) ? fileContent : { changes: fileContent };
  res.key = keyVersion; // write the information about the user, creation time, key and version to the result object
  res.version = version;
  res.created = created;
  res.user = {
    id: userid,
    name: username !== 'null' ? username : null,
  };

  return res;
};

// clean folder
DocManager.prototype.cleanFolderRecursive = function cleanFolderRecursive(folder, me) {
  if (fileSystem.existsSync(folder)) { // if the given folder exists
    const files = fileSystem.readdirSync(folder);
    files.forEach((file) => { // for each file from the folder
      const curPath = path.join(folder, file); // get its current path
      if (fileSystem.lstatSync(curPath).isDirectory()) {
        this.cleanFolderRecursive(curPath, true); // for each folder included in this one repeat the same procedure
      } else {
        fileSystem.unlinkSync(curPath); // remove the file
      }
    });
    if (me) {
      fileSystem.rmdirSync(folder);
    }
  }
};

// get files information
DocManager.prototype.getFilesInfo = function getFilesInfo(fileId) {
  const userAddress = this.curUserHostAddress();
  const directory = this.storageRootPath(userAddress);
  const filesInDirectory = this.getStoredFiles(); // get all the stored files from the folder
  const responseArray = [];
  let responseObject;
  // run through all the files from the directory
  for (let currentFile = 0; currentFile < filesInDirectory.length; currentFile++) {
    const file = filesInDirectory[currentFile];
    const stats = fileSystem.lstatSync(path.join(directory, file.name)); // get file information
    const fileObject = { // write file parameters to the file object
      version: file.version,
      id: this.getKey(file.name),
      contentLength: `${(stats.size / 1024).toFixed(2)} KB`,
      pureContentLength: stats.size,
      title: file.name,
      updated: stats.mtime,
    };
    if (fileId !== undefined) { // if file id is defined
      if (this.getKey(file.name) === fileId) { // and it is equal to the document key value
        responseObject = fileObject; // response object will be equal to the file object
        break;
      }
    } else responseArray.push(fileObject); // otherwise, push file object to the response array
  }
  if (fileId !== undefined) {
    if (responseObject !== undefined) return responseObject;
    return 'File not found';
  } return responseArray;
};

DocManager.prototype.getInstanceId = function getInstanceId() {
  return this.getServerUrl();
};

// save all the functions to the DocManager module to export it later in other files
module.exports = DocManager;
