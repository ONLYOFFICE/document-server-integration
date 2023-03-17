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

const tokenValidator = require("./tokenValidator");
const filesController = require("./filesController");
const utils = require("./utils");
const docManager = require("../docManager");
const fileUtility = require("../fileUtility");
const config = require('config');
const configServer = config.get('server');
const siteUrl = configServer.get("siteUrl");  // the path to the editors installation
const users = require("../users");

getCustomWopiParams = function (query) {
    let tokenParams = "";
    let actionParams = "";

    const userid = query.userid;  // user id
    tokenParams += (userid ? "&userid=" + userid : "");

    const lang = query.lang;  // language
    actionParams += (lang ? "&ui=" + lang : "");

    return { "tokenParams": tokenParams, "actionParams": actionParams };
};

exports.registerRoutes = function(app) {

    // define a handler for the default wopi page
    app.get("/wopi", async function(req, res) {

        req.docManager = new docManager(req, res);

        await utils.initWopi(req.docManager);

        // get the wopi discovery information
        let actions = await utils.getDiscoveryInfo();
        let wopiEnable = actions.length != 0 ? true : false;
        let docsExtEdit = [];    // Supported extensions for WOPI

        actions.forEach(el => {
            if (el.name == "edit") docsExtEdit.push("."+el.ext);
        });

        let editedExts = configServer.get('editedDocs').filter(i => docsExtEdit.includes(i));   // Checking supported extensions
        let fillExts = configServer.get("fillDocs").filter(i => docsExtEdit.includes(i));

        try {
            // get all the stored files
            let files = req.docManager.getStoredFiles();

            // run through all the files and write the corresponding information to each file
            for (var file of files) {
                let ext = fileUtility.getFileExtension(file.name, true);  // get an extension of each file
                file.actions = await utils.getActions(ext);  // get actions of the specified extension
                file.defaultAction = await utils.getDefaultAction(ext);  // get the default action of the specified extension
            }

            // render wopiIndex template with the parameters specified
            res.render("wopiIndex", {
                wopiEnable : wopiEnable,
                storedFiles: wopiEnable ? files : [],
                params: req.docManager.getCustomParams(),
                users: users,
                serverUrl: req.docManager.getServerUrl(),
                preloaderUrl: siteUrl + configServer.get('preloaderUrl'),
                convertExts: configServer.get('convertedDocs'),
                editedExts: editedExts,
                fillExts: fillExts,
                languages: configServer.get('languages'),
            });

        } catch (ex) {
            console.log(ex);  // display error message in the console
            res.status(500);  // write status parameter to the response
            res.render("error", { message: "Server error" });  // render error template with the message parameter specified
            return;
        }
    });
    // define a handler for creating a new wopi editing session
    app.get("/wopi-new", function(req, res) {
        var fileExt = req.query.fileExt;  // get the file extension from the request

        req.docManager = new docManager(req, res);

        if (fileExt != null) {  // if the file extension exists
            var fileName = req.docManager.getCorrectName("new." + fileExt)
            var redirectPath = req.docManager.getServerUrl(true) + "/wopi-action/" + encodeURIComponent(fileName) + "?action=editnew" + req.docManager.getCustomParams();  // get the redirect path
            res.redirect(redirectPath);
            return;
        }
    });
    // define a handler for getting wopi action information by its id
    app.get("/wopi-action/:id", async function(req, res) {
        try {
            req.docManager = new docManager(req, res);

            await utils.initWopi(req.docManager);

            var fileName = req.docManager.getCorrectName(req.params['id'])
            var fileExt = fileUtility.getFileExtension(fileName, true);  // get the file extension from the request
            var user = users.getUser(req.query.userid);  // get a user by the id

            // get an action for the specified extension and name
            let action = await utils.getAction(fileExt, req.query["action"]);

            if (action != null && req.query["action"] == "editnew") {
                fileName = req.docManager.RequestEditnew(req, fileName, user);
            }

            // render wopiAction template with the parameters specified
            res.render("wopiAction", {
                actionUrl: utils.getActionUrl(req.docManager.getServerUrl(true), req.docManager.curUserHostAddress(), action, req.params['id']),
                token: "test",
                tokenTtl: Date.now() + 1000 * 60 * 60 * 10,
                params: getCustomWopiParams(req.query),
            });

        } catch (ex) {
            console.log(ex);
            res.status(500);
            res.render("error", { message: "Server error" });
            return;
        }
    });

    // define a handler for getting file information by its id
    app.route('/wopi/files/:id')
        .all(tokenValidator.isValidToken)
        .get(filesController.fileRequestHandler)
        .post(filesController.fileRequestHandler);

    // define a handler for reading/writing the file contents
    app.route('/wopi/files/:id/contents')
        .all(tokenValidator.isValidToken)
        .get(filesController.fileRequestHandler)
        .post(filesController.fileRequestHandler);

    // define a handler for getting folder information by its id
    app.route('/wopi/folders/:id')
        .all(tokenValidator.isValidToken)
        .get(filesController.fileRequestHandler)
        .post(filesController.fileRequestHandler);

    // define a handler for reading/writing the folder contents
    app.route('/wopi/folders/:id/contents')
        .all(tokenValidator.isValidToken)
        .get(filesController.fileRequestHandler)
        .post(filesController.fileRequestHandler);

    // define a handler for upload files
    app.route('/wopi/upload')
        .all(tokenValidator.isValidToken)
        .get(filesController.fileRequestHandler)
        .post(filesController.fileRequestHandler);

};
