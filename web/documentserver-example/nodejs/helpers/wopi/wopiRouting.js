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

const tokenValidator = require("./tokenValidator");
const filesController = require("./filesController");
const utils = require("./utils");
const docManager = require("../docManager");
const fileUtility = require("../fileUtility");
const config = require('config');
const configServer = config.get('server');
const storageFolder = configServer.get("storageFolder");
const users = require("../users");

exports.registerRoutes = function(app) {

    // define a handler for the default wopi page
    app.get("/wopi", function(req, res) {

        // get the wopi discovery information
        let actions = utils.getDiscoveryInfo(3);
        let wopiEnable = actions.length != 0 ? true : false;

        try {
            docManager.init(storageFolder, req, res);

            // get all the stored files
            let files = docManager.getStoredFiles();

            // run through all the files and write the corresponding information to each file
            for (var file of files) {
                let ext = fileUtility.getFileExtension(file.name, true);  // get an extension of each file
                file.actions = utils.getActions(ext);  // get actions of the specified extension
                file.defaultAction = utils.getDefaultAction(ext);  // get the default action of the specified extension
            }

            // render wopiIndex template with the parameters specified
            res.render("wopiIndex", {
                wopiEnable : wopiEnable,
                storedFiles: wopiEnable ? files : [],
                params: docManager.getCustomParams(),
                users: users,
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
        var user = users.getUser(req.query.userid);  // get a user by the id

        if (fileExt != null) {  // if the file extension exists
            var fileName = docManager.createDemo(!!req.query.sample, fileExt, user.id, user.name);  // create demo document of the given extension
            var redirectPath = docManager.getServerUrl() + req._parsedOriginalUrl.pathname.replace("/wopi-new", "") + "/wopi-action/" + encodeURIComponent(fileName) + "?action=edit" + docManager.getCustomParams();  // get the redirect path
            res.redirect(redirectPath);
            return;
        }
    });
    // define a handler for getting wopi action information by its id
    app.get("/wopi-action/:id", function(req, res) {
        try {
            docManager.init(storageFolder, req, res);

            // get an action for the specified extension and name
            let action = utils.getAction(fileUtility.getFileExtension(req.params['id'], true), req.query["action"]);

            // render wopiAction template with the parameters specified
            res.render("wopiAction", {
                actionUrl: utils.getActionUrl(docManager.getServerUrl() + req._parsedOriginalUrl.pathname.split("/wopi-action")[0], docManager.curUserHostAddress(), action, req.params['id']),
                token: "test",
                tokenTtl: Date.now() + 1000 * 60 * 60 * 10,
                params: docManager.getCustomParams(),
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
};
