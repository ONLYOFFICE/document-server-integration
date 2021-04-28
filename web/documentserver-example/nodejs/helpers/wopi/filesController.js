const reqConsts = require('./request');
const docManager = require("../docManager");
const fileUtility = require("../fileUtility");
const utils = require("./utils");
const fileSystem = require("fs");
const mime = require("mime");

const actionMapping = {};
actionMapping[reqConsts.requestType.GetFile] = getFile;
actionMapping[reqConsts.requestType.PutFile] = putFile;
actionMapping[reqConsts.requestType.CheckFileInfo] = checkFileInfo;

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
                let wopiOverride = req.headers[reqConsts.requestHeaders.requestType]
                switch (wopiOverride) {
                    case "LOCK":
                        if (req.headers[reqConsts.requestHeaders.OldLock]) {
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
    let userAddress = docManager.curUserHostAddress(userHost);

    let path = docManager.storagePath(wopi.id, userAddress);

    if (req.body) {
        let filestream = fileSystem.createWriteStream(path);
        req.pipe(filestream);
        req.on('end', () => {
            filestream.close();
            res.sendStatus(200);
        })
    } else {
        res.sendStatus(404);
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
        "UserCanWrite": true
    };
    res.status(200).send(fileInfo);
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
    }

    let action = actionMapping[wopiData.requestType];
    if (!action) {
        res.status(501).send({ 'title': 'fileHandler', 'method': req.method, 'id': req.params['id'], 'error': "unsupported" });
    }

    action(wopiData, req, res);
}