const reqConsts = require('./request');
const docManager = require("../docManager");
const fileUtility = require("../fileUtility");
const lockManager = require("./lockManager");
const utils = require("./utils");
const fileSystem = require("fs");
const mime = require("mime");
const path = require("path");

const actionMapping = {};
actionMapping[reqConsts.requestType.GetFile] = getFile;
actionMapping[reqConsts.requestType.PutFile] = putFile;
actionMapping[reqConsts.requestType.CheckFileInfo] = checkFileInfo;
actionMapping[reqConsts.requestType.UnlockAndRelock] = unlockAndRelock;
actionMapping[reqConsts.requestType.Lock] = lock;
actionMapping[reqConsts.requestType.GetLock] = getLock;
actionMapping[reqConsts.requestType.RefreshLock] = refreshLock;
actionMapping[reqConsts.requestType.Unlock] = unlock;

function parseWopiRequest(req) {
    let wopiData = {
        requestType: reqConsts.requestType.None,
        accessToken: req.query["access_token"],
        id: req.params['id']
    }

    let reqPath = req.path.substring("/wopi/".length)

    if (reqPath.startsWith("files")) {
        if (reqPath.endsWith("/contents")) {
            if (req.method == "GET") {
                wopiData.requestType = reqConsts.requestType.GetFile;
            } else if (req.method == "POST") {
                wopiData.requestType = reqConsts.requestType.PutFile;
            }
        } else {
            if (req.method == "GET") {
                wopiData.requestType = reqConsts.requestType.CheckFileInfo;
            } else if (req.method == "POST") {
                let wopiOverride = req.headers[reqConsts.requestHeaders.RequestType.toLowerCase()];
                switch (wopiOverride) {
                    case "LOCK":
                        if (req.headers[reqConsts.requestHeaders.OldLock.toLowerCase()]) {
                            wopiData.requestType = reqConsts.requestType.UnlockAndRelock;
                        } else {
                            wopiData.requestType = reqConsts.requestType.Lock;
                        }
                        break;

                    case "GET_LOCK":
                        wopiData.requestType = reqConsts.requestType.GetLock;
                        break;

                    case "REFRESH_LOCK":
                        wopiData.requestType = reqConsts.requestType.RefreshLock;
                        break;

                    case "UNLOCK":
                        wopiData.requestType = reqConsts.requestType.Unlock;
                        break;

                    case "PUT_RELATIVE":
                        wopiData.requestType = reqConsts.requestType.PutRelativeFile;
                        break;

                    case "RENAME_FILE":
                        wopiData.requestType = reqConsts.requestType.RenameFile;
                        break;

                    case "PUT_USER_INFO":
                        wopiData.requestType = reqConsts.requestType.PutUserInfo;
                        break;
                }
            }
        }
    } else if (reqPath.startsWith("folders")) {

    }

    return wopiData;
}

function lock(wopi, req, res, userHost) {
    let requestLock = req.headers[reqConsts.requestHeaders.Lock.toLowerCase()];

    let userAddress = docManager.curUserHostAddress(userHost);
    let filePath = docManager.storagePath(wopi.id, userAddress);

    if (!lockManager.hasLock(filePath)) {
        // file isn't locked => lock
        lockManager.lock(filePath, requestLock);
        res.sendStatus(200);
    } else if (lockManager.getLock(filePath) == requestLock) {
        // lock matches current lock => extend duration
        lockManager.lock(filePath, requestLock);
        res.sendStatus(200);
    } else {
        // file locked by someone else => return lock mismatch
        let lock = lockManager.getLock(filePath);
        returnLockMismatch(res, lock, "File already locked by " + lock)
    }
}

function getLock(wopi, req, res, userHost) {
    let userAddress = docManager.curUserHostAddress(userHost);
    let filePath = docManager.storagePath(wopi.id, userAddress);

    res.headers[reqConsts.requestHeaders.lock] == lockManager.getLock(filePath);
    res.sendStatus(200);
}

function refreshLock(wopi, req, res, userHost) {
    let requestLock = req.headers[reqConsts.requestHeaders.Lock.toLowerCase()];

    let userAddress = docManager.curUserHostAddress(userHost);
    let filePath = docManager.storagePath(wopi.id, userAddress);

    if (!lockManager.hasLock(filePath)) {
        // file isn't locked => mismatch
        returnLockMismatch(res, "", "File isn't locked");
    } else if (lockManager.getLock(filePath) == requestLock) {
        // lock matches current lock => extend duration
        lockManager.lock(filePath, requestLock);
        res.sendStatus(200);
    } else {
        // lock mismatch
        returnLockMismatch(res, lockManager.getLock(filePath), "Lock mismatch");
    }
}

function unlock(wopi, req, res, userHost) {
    let requestLock = req.headers[reqConsts.requestHeaders.Lock.toLowerCase()];

    let userAddress = docManager.curUserHostAddress(userHost);
    let filePath = docManager.storagePath(wopi.id, userAddress);

    if (!lockManager.hasLock(filePath)) {
        // file isn't locked => mismatch
        returnLockMismatch(res, "", "File isn't locked");
    } else if (lockManager.getLock(filePath) == requestLock) {
        // lock matches current lock => unlock
        lockManager.unlock(filePath);
        res.sendStatus(200);
    } else {
        // lock mismatch
        returnLockMismatch(res, lockManager.getLock(filePath), "Lock mismatch");
    }
}

function unlockAndRelock(wopi, req, res, userHost) {
    let requestLock = req.headers[reqConsts.requestHeaders.Lock.toLowerCase()];
    let oldLock = req.headers[reqConsts.requestHeaders.oldLock.toLowerCase()];

    let userAddress = docManager.curUserHostAddress(userHost);
    let filePath = docManager.storagePath(wopi.id, userAddress);

    if (!lockManager.hasLock(filePath)) {
        // file isn't locked => mismatch
        returnLockMismatch(res, "", "File isn't locked");
    } else if (lockManager.getLock(filePath) == oldLock) {
        // lock matches current lock => lock with new key
        lockManager.lock(filePath, requestLock);
        res.sendStatus(200);
    } else {
        // lock mismatch
        returnLockMismatch(res, lockManager.getLock(filePath), "Lock mismatch");
    }
}

function getFile(wopi, req, res, userHost) {
    let userAddress = docManager.curUserHostAddress(userHost);

    let path = docManager.storagePath(wopi.id, userAddress);

    res.setHeader("Content-Length", fileSystem.statSync(path).size);
    res.setHeader("Content-Type", mime.getType(path));

    res.setHeader("Content-Disposition", "attachment; filename*=UTF-8\'\'" + encodeURIComponent(wopi.id));

    let filestream = fileSystem.createReadStream(path);
    filestream.pipe(res);
}

function putFile(wopi, req, res, userHost) {
    let requestLock = req.headers[reqConsts.requestHeaders.Lock.toLowerCase()];

    let userAddress = docManager.curUserHostAddress(userHost);
    let storagePath = docManager.storagePath(wopi.id, userAddress);

    if (!lockManager.hasLock(storagePath)) {
        // ToDo: if body length is 0 bytes => handle document creation

        // file isn't locked => mismatch
        returnLockMismatch(res, "", "File isn't locked");
    } else if (lockManager.getLock(storagePath) == requestLock) {
        // lock matches current lock => put file
        if (req.body) {
            var historyPath = docManager.historyPath(wopi.id, userAddress);
            if (historyPath == "") {
                historyPath = docManager.historyPath(wopi.id, userAddress, true);
                docManager.createDirectory(historyPath);
            }

            var count_version = docManager.countVersion(historyPath);
            version = count_version + 1;
            var versionPath = docManager.versionPath(wopi.id, userAddress, version);
            docManager.createDirectory(versionPath);

            var path_prev = path.join(versionPath, "prev" + fileUtility.getFileExtension(wopi.id));
            fileSystem.renameSync(docManager.storagePath(wopi.id, userAddress), path_prev);

            let filestream = fileSystem.createWriteStream(storagePath);
            req.pipe(filestream);
            req.on('end', () => {
                filestream.close();
                res.sendStatus(200);
            })
        } else {
            res.sendStatus(404);
        }
    } else {
        // lock mismatch
        returnLockMismatch(res, lockManager.getLock(storagePath), "Lock mismatch");
    }
}

function checkFileInfo(wopi, req, res, userHost) {
    let userAddress = docManager.curUserHostAddress(userHost);
    let historyPath = docManager.historyPath(wopi.id, userAddress);
    let version = 1;
    if (historyPath != "") {
        version = docManager.countVersion(historyPath);
    }
    let path = docManager.storagePath(wopi.id, userAddress);

    let fileInfo = {
        "BaseFileName": wopi.id,
        "OwnerId": docManager.getFileData(wopi.id, userAddress)[1],
        "Size": fileSystem.statSync(path).size,
        "UserId": req.query.userid ? req.query.userid : "uid-1",
        "Version": version,
        "UserCanWrite": true,
        "SupportsGetLock": true,
        "SupportsLocks": true,
        "SupportsUpdate": true,
    };
    res.status(200).send(fileInfo);
}

function returnLockMismatch(res, lock, reason) {
    res.headers[reqConsts.requestHeaders.Lock] = lock || "";
    if (reason) {
        res.headers[reqConsts.requestHeaders.LockFailureReason] = reason;
    }
    res.sendStatus(409); // conflict
}

exports.fileRequestHandler = (req, res) => {
    let userAddress = null;
    if (req.params['id'].includes("@")) {
        let split = req.params['id'].split("@");
        req.params['id'] = split[0];
        userAddress = split[1];
    }

    let wopiData = parseWopiRequest(req);

    if (wopiData.requestType == reqConsts.requestType.None) {
        res.status(500).send({ 'title': 'fileHandler', 'method': req.method, 'id': req.params['id'], 'error': "unknown" });
        return;
    }

    let action = actionMapping[wopiData.requestType];
    if (!action) {
        res.status(501).send({ 'title': 'fileHandler', 'method': req.method, 'id': req.params['id'], 'error': "unsupported" });
        return;
    }

    action(wopiData, req, res);
}