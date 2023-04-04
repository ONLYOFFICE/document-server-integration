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

const fileSystem = require('fs');
const mime = require('mime');
const path = require('path');
const reqConsts = require('./request');
const fileUtility = require('../fileUtility');
const lockManager = require('./lockManager');
const users = require('../users');
const DocManager = require('../docManager');

// return lock mismatch
const returnLockMismatch = function returnLockMismatch(res, lock, reason) {
  res.setHeader(reqConsts.requestHeaders.Lock, lock || ''); // set the X-WOPI-Lock header
  if (reason) { // if there is a reason for lock mismatch
    res.setHeader(reqConsts.requestHeaders.LockFailureReason, reason); // set it as the X-WOPI-LockFailureReason header
  }
  res.sendStatus(409); // conflict
};

// lock file editing
const lock = function lock(wopi, req, res, userHost) {
  const requestLock = req.headers[reqConsts.requestHeaders.Lock.toLowerCase()];

  const userAddress = req.DocManager.curUserHostAddress(userHost); // get current user host address
  const filePath = req.DocManager.storagePath(wopi.id, userAddress); // get the storage path of the given file

  if (!lockManager.hasLock(filePath)) {
    // file isn't locked => lock
    lockManager.lock(filePath, requestLock);
    res.sendStatus(200);
  } else if (lockManager.getLock(filePath) === requestLock) {
    // lock matches current lock => extend duration
    lockManager.lock(filePath, requestLock);
    res.sendStatus(200);
  } else {
    // file locked by someone else => return lock mismatch
    const locked = lockManager.getLock(filePath);
    returnLockMismatch(res, lock, `File already locked by ${locked}`);
  }
};

const saveFileFromBody = function saveFileFromBody(req, filename, userAddress, isNewVersion, callback) {
  if (req.body) {
    const storagePath = req.DocManager.storagePath(filename, userAddress);
    let historyPath = req.DocManager.historyPath(filename, userAddress); // get the path to the file history
    if (historyPath === '') { // if it is empty
      historyPath = req.DocManager.historyPath(filename, userAddress, true); // create it
      req.DocManager.createDirectory(historyPath); // and create a new directory for the history
    }

    let version = 0;
    if (isNewVersion) {
      const countVersion = req.DocManager.countVersion(historyPath); // get the last file version
      version = countVersion + 1; // get a number of a new file version
      // get the path to the specified file version
      const versionPath = req.DocManager.versionPath(filename, userAddress, version);
      req.DocManager.createDirectory(versionPath); // and create a new directory for the specified version

      // get the path to the previous file version
      const pathPrev = path.join(versionPath, `prev${fileUtility.getFileExtension(filename)}`);
      fileSystem.renameSync(storagePath, pathPrev); // synchronously rename the given file as the previous file version
    }

    const filestream = fileSystem.createWriteStream(storagePath);
    req.pipe(filestream);
    req.on('end', () => {
      filestream.close();
      callback(null, version);
    });
  } else {
    callback('empty body');
  }
};

// return name that wopi-client can use as the value of X-WOPI-RelativeTarget in a future PutRelativeFile operation
const returnValidRelativeTarget = function returnValidRelativeTarget(res, filename) {
  res.setHeader(reqConsts.requestHeaders.ValidRelativeTarget, filename); // set the X-WOPI-ValidRelativeTarget header
  res.sendStatus(409); // file with that name already exists
};

// retrieve a lock on a file
const getLock = function getLock(wopi, req, res, userHost) {
  const userAddress = req.DocManager.curUserHostAddress(userHost);
  const filePath = req.DocManager.storagePath(wopi.id, userAddress);

  // get the lock of the specified file and set it as the X-WOPI-Lock header
  res.setHeader(reqConsts.requestHeaders.lock, lockManager.getLock(filePath));
  res.sendStatus(200);
};

// refresh the lock on a file by resetting its automatic expiration timer to 30 minutes
const refreshLock = function refreshLock(wopi, req, res, userHost) {
  const requestLock = req.headers[reqConsts.requestHeaders.Lock.toLowerCase()];

  const userAddress = req.DocManager.curUserHostAddress(userHost);
  const filePath = req.DocManager.storagePath(wopi.id, userAddress);

  if (!lockManager.hasLock(filePath)) {
    // file isn't locked => mismatch
    returnLockMismatch(res, '', 'File isn\'t locked');
  } else if (lockManager.getLock(filePath) === requestLock) {
    // lock matches current lock => extend duration
    lockManager.lock(filePath, requestLock);
    res.sendStatus(200);
  } else {
    // lock mismatch
    returnLockMismatch(res, lockManager.getLock(filePath), 'Lock mismatch');
  }
};

// allow for file editing
const unlock = function unlock(wopi, req, res, userHost) {
  const requestLock = req.headers[reqConsts.requestHeaders.Lock.toLowerCase()];

  const userAddress = req.DocManager.curUserHostAddress(userHost);
  const filePath = req.DocManager.storagePath(wopi.id, userAddress);

  if (!lockManager.hasLock(filePath)) {
    // file isn't locked => mismatch
    returnLockMismatch(res, '', 'File isn\'t locked');
  } else if (lockManager.getLock(filePath) === requestLock) {
    // lock matches current lock => unlock
    lockManager.unlock(filePath);
    res.sendStatus(200);
  } else {
    // lock mismatch
    returnLockMismatch(res, lockManager.getLock(filePath), 'Lock mismatch');
  }
};

// allow for file editing, and then immediately take a new lock on the file
const unlockAndRelock = function unlockAndRelock(wopi, req, res, userHost) {
  const requestLock = req.headers[reqConsts.requestHeaders.Lock.toLowerCase()];
  const oldLock = req.headers[reqConsts.requestHeaders.oldLock.toLowerCase()]; // get the X-WOPI-OldLock header

  const userAddress = req.DocManager.curUserHostAddress(userHost);
  const filePath = req.DocManager.storagePath(wopi.id, userAddress);

  if (!lockManager.hasLock(filePath)) {
    // file isn't locked => mismatch
    returnLockMismatch(res, '', 'File isn\'t locked');
  } else if (lockManager.getLock(filePath) === oldLock) {
    // lock matches current lock => lock with new key
    lockManager.lock(filePath, requestLock);
    res.sendStatus(200);
  } else {
    // lock mismatch
    returnLockMismatch(res, lockManager.getLock(filePath), 'Lock mismatch');
  }
};

// request a message to retrieve a file
const getFile = function getFile(wopi, req, res, userHost) {
  const userAddress = req.DocManager.curUserHostAddress(userHost);

  const storagePath = req.DocManager.storagePath(wopi.id, userAddress);

  res.setHeader('Content-Length', fileSystem.statSync(storagePath).size);
  res.setHeader('Content-Type', mime.getType(storagePath));

  res.setHeader('Content-Disposition', `attachment; filename*=UTF-8''${encodeURIComponent(wopi.id)}`);

  const filestream = fileSystem.createReadStream(storagePath); // open a file as a readable stream
  filestream.pipe(res); // retrieve data from file stream and output it to the response object
};

// request a message to update a file
const putFile = function putFile(wopi, req, res, userHost) {
  const requestLock = req.headers[reqConsts.requestHeaders.Lock.toLowerCase()];

  const userAddress = req.DocManager.curUserHostAddress(userHost);
  const storagePath = req.DocManager.storagePath(wopi.id, userAddress);

  if (!lockManager.hasLock(storagePath)) {
    // ToDo: if body length is 0 bytes => handle document creation

    // file isn't locked => mismatch
    returnLockMismatch(res, '', 'File isn\'t locked');
  } else if (lockManager.getLock(storagePath) === requestLock) {
    // lock matches current lock => put file
    saveFileFromBody(req, wopi.id, userAddress, true, (err, version) => {
      if (!err) {
        res.setHeader(reqConsts.requestHeaders.ItemVersion, version); // set the X-WOPI-ItemVersion header
      }
      res.sendStatus(err ? 404 : 200);
    });
  } else {
    // lock mismatch
    returnLockMismatch(res, lockManager.getLock(storagePath), 'Lock mismatch');
  }
};

const putRelativeFile = function putRelativeFile(wopi, req, res, userHost) {
  const userAddress = req.DocManager.curUserHostAddress(userHost);
  const storagePath = req.DocManager.storagePath(wopi.id, userAddress);

  let filename = req.headers[reqConsts.requestHeaders.RelativeTarget.toLowerCase()]; // we cannot modify this filename
  if (filename) {
    if (req.DocManager.existsSync(storagePath)) { // check if already exists
      const overwrite = req.headers[reqConsts.requestHeaders.OverwriteRelativeTarget.toLowerCase()]; // overwrite header
      if (overwrite && overwrite === 'true') { // check if we can overwrite
        if (lockManager.hasLock(storagePath)) { // check if file locked
          // file is locked, cannot overwrite
          returnValidRelativeTarget(res, req.DocManager.getCorrectName(wopi.id, userAddress));
          return;
        }
      } else {
        // file exists and overwrite header is false
        returnValidRelativeTarget(res, req.DocManager.getCorrectName(wopi.id, userAddress));
        return;
      }
    }
  } else {
    filename = req.headers[reqConsts.requestHeaders.SuggestedTarget.toLowerCase()]; // we can modify this filename

    if (filename.startsWith('.')) { // check if extension
      filename = fileUtility.getFileName(wopi.id, true) + filename; // get original filename with new extension
    }

    filename = req.DocManager.getCorrectName(filename, userAddress); // get correct filename if already exists
  }

  const isConverted = req.headers[reqConsts.requestHeaders.FileConversion.toLowerCase()];
  console.log(`putRelativeFile after conversation: ${isConverted}`);

  // if we got here, then we can save a file
  saveFileFromBody(req, filename, userAddress, false, (err) => {
    if (err) {
      res.sendStatus(404);
      return;
    }

    const serverUrl = req.DocManager.getServerUrl(true);
    const fileActionUrl = `${serverUrl}/wopi-action/${filename}?action=`;

    const fileInfo = {
      Name: filename,
      Url: `${serverUrl}/wopi/files/${filename}`,
      HostViewUrl: `${fileActionUrl}view`,
      HostEditNewUrl: `${fileActionUrl}editnew`,
      HostEditUrl: `${fileActionUrl}edit`,
    };
    res.status(200).send(fileInfo);
  });
};

// return information about the file properties, access rights and editor settings
const checkFileInfo = function checkFileInfo(wopi, req, res, userHost) {
  const userAddress = req.DocManager.curUserHostAddress(userHost);
  const version = req.DocManager.getKey(wopi.id, userAddress);

  const storagePath = req.DocManager.storagePath(wopi.id, userAddress);
  // add wopi query
  const query = new URLSearchParams(wopi.accessToken);
  const user = users.getUser(query.get('userid'));

  // create the file information object
  const fileInfo = {
    BaseFileName: wopi.id,
    OwnerId: req.DocManager.getFileData(wopi.id, userAddress)[1],
    Size: fileSystem.statSync(storagePath).size,
    UserId: user.id,
    UserFriendlyName: user.name,
    Version: version,
    UserCanWrite: true,
    SupportsGetLock: true,
    SupportsLocks: true,
    SupportsUpdate: true,
  };
  res.status(200).send(fileInfo);
};

// parse wopi request
const parseWopiRequest = function parseWopiRequest(req) {
  const wopiData = {
    requestType: reqConsts.requestType.None,
    accessToken: req.query.access_token,
    id: req.params.id,
  };

  // get the request path
  const reqPath = req.path.substring('/wopi/'.length);

  if (reqPath.startsWith('files')) { // if it starts with "files"
    if (reqPath.endsWith('/contents')) { // ends with "/contents"
      if (req.method === 'GET') { // and the request method is GET
        wopiData.requestType = reqConsts.requestType.GetFile; // then the request type is GetFile
      } else if (req.method === 'POST') { // if the request method is POST
        wopiData.requestType = reqConsts.requestType.PutFile; // then the request type is PutFile
      }
    } else if (req.method === 'GET') { // otherwise, if the request method is GET
      wopiData.requestType = reqConsts.requestType.CheckFileInfo; // the request type is CheckFileInfo
    } else if (req.method === 'POST') { // if the request method is POST
      // get the X-WOPI-Override header which determines the request type
      const wopiOverride = req.headers[reqConsts.requestHeaders.RequestType.toLowerCase()];
      switch (wopiOverride) {
        case 'LOCK': // if it is equal to LOCK
          // check if the request sends the X-WOPI-OldLock header
          if (req.headers[reqConsts.requestHeaders.OldLock.toLowerCase()]) {
            // if yes, then the request type is UnlockAndRelock
            wopiData.requestType = reqConsts.requestType.UnlockAndRelock;
          } else {
            wopiData.requestType = reqConsts.requestType.Lock; // otherwise, it is Lock
          }
          break;

        case 'GET_LOCK': // if it is equal to GET_LOCK
          wopiData.requestType = reqConsts.requestType.GetLock; // the request type is GetLock
          break;

        case 'REFRESH_LOCK': // if it is equal to REFRESH_LOCK
          wopiData.requestType = reqConsts.requestType.RefreshLock; // the request type is RefreshLock
          break;

        case 'UNLOCK': // if it is equal to UNLOCK
          wopiData.requestType = reqConsts.requestType.Unlock; // the request type is Unlock
          break;

        case 'PUT_RELATIVE': // if it is equal to PUT_RELATIVE
          // the request type is PutRelativeFile (creates a new file on the host based on the current file)
          wopiData.requestType = reqConsts.requestType.PutRelativeFile;
          break;

        case 'RENAME_FILE': // if it is equal to RENAME_FILE
          wopiData.requestType = reqConsts.requestType.RenameFile; // the request type is RenameFile (renames a file)
          break;

        case 'PUT_USER_INFO': // if it is equal to PUT_USER_INFO
          // the request type is PutUserInfo (stores some basic user information on the host)
          wopiData.requestType = reqConsts.requestType.PutUserInfo;
          break;
        default:
      }
    }
  }

  return wopiData;
};

const actionMapping = {};
actionMapping[reqConsts.requestType.GetFile] = getFile;
actionMapping[reqConsts.requestType.PutFile] = putFile;
actionMapping[reqConsts.requestType.PutRelativeFile] = putRelativeFile;
actionMapping[reqConsts.requestType.CheckFileInfo] = checkFileInfo;
actionMapping[reqConsts.requestType.UnlockAndRelock] = unlockAndRelock;
actionMapping[reqConsts.requestType.Lock] = lock;
actionMapping[reqConsts.requestType.GetLock] = getLock;
actionMapping[reqConsts.requestType.RefreshLock] = refreshLock;
actionMapping[reqConsts.requestType.Unlock] = unlock;

exports.fileRequestHandler = (req, res) => {
  let userAddress = null;
  req.DocManager = new DocManager(req, res);
  if (req.params.id.includes('@')) { // if there is the "@" sign in the id parameter
    const split = req.params.id.split('@'); // split this parameter by "@"
    [req.params.id] = split; // rewrite id with the first part of the split parameter
    [, userAddress] = split; // save the second part as the user address
  }

  const wopiData = parseWopiRequest(req); // get the wopi data

  // an error of the unknown request type
  if (wopiData.requestType === reqConsts.requestType.None) {
    res.status(500).send({
      title: 'fileHandler', method: req.method, id: req.params.id, error: 'unknown',
    });
    return;
  }

  // an error of the unsupported request type
  const action = actionMapping[wopiData.requestType];
  if (!action) {
    res.status(501).send({
      title: 'fileHandler', method: req.method, id: req.params.id, error: 'unsupported',
    });
    return;
  }

  action(wopiData, req, res, userAddress);
};
