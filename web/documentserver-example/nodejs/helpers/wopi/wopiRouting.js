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

    app.get("/wopi", function(req, res) {
        try {
            docManager.init(storageFolder, req, res);

            let files = docManager.getStoredFiles();

            for (var file of files) {
                let ext = fileUtility.getFileExtension(file.name, true);
                file.actions = utils.getActions(ext);
                file.defaultAction = utils.getDefaultAction(ext);
            }

            res.render("wopiIndex", {
                storedFiles: files,
                params: docManager.getCustomParams(),
                users: users,
            });

        } catch (ex) {
            console.log(ex);
            res.status(500);
            res.render("error", { message: "Server error" });
            return;
        }
    });
    app.get("/wopi-new", function(req, res) {
        var fileExt = req.query.fileExt;
        var user = users.getUser(req.query.userid);

        if (fileExt != null) {
            var fileName = docManager.createDemo(!!req.query.sample, fileExt, user.id, user.name);
            var redirectPath = docManager.getServerUrl() + "/wopi-action/" + encodeURIComponent(fileName) + "?action=edit" + docManager.getCustomParams();
            res.redirect(redirectPath);
            return;
        }
    });
    app.get("/wopi-action/:id", function(req, res) {
        try {
            docManager.init(storageFolder, req, res);

            let action = utils.getAction(fileUtility.getFileExtension(req.params['id'], true), req.query["action"]);

            res.render("wopiAction", {
                actionUrl: utils.getActionUrl(docManager.getServerUrl(), docManager.curUserHostAddress(), action, req.params['id']),
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

    app.route('/wopi/files/:id')
        .all(tokenValidator.isValidToken)
        .get(filesController.fileRequestHandler)
        .post(filesController.fileRequestHandler);

    app.route('/wopi/files/:id/contents')
        .all(tokenValidator.isValidToken)
        .get(filesController.fileRequestHandler)
        .post(filesController.fileRequestHandler);

    app.route('/wopi/folders/:id')
        .all(tokenValidator.isValidToken)
        .get(filesController.fileRequestHandler)
        .post(filesController.fileRequestHandler);

    app.route('/wopi/folders/:id/contents')
        .all(tokenValidator.isValidToken)
        .get(filesController.fileRequestHandler)
        .post(filesController.fileRequestHandler);
};