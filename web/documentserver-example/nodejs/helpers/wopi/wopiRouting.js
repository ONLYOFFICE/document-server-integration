const tokenValidator = require("./tokenValidator");
const filesController = require("./filesController");
const utils = require("./utils");
const docManager = require("../docManager");
const fileUtility = require("../fileUtility");
const config = require('config');
const configServer = config.get('server');
const storageFolder = configServer.get("storageFolder");

exports.registerRoutes = function(app) {

    app.get("/wopi", function(req, res) {
        try {
            docManager.init(storageFolder, req, res);

            let files = docManager.getStoredFiles();

            for (var file of files) {
                file.actions = utils.getActions(fileUtility.getFileExtension(file.name, true));
            }

            res.render("wopiIndex", {
                storedFiles: files
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
        var userid = req.query.userid ? req.query.userid : "uid-1";
        var name = (userid == "uid-0" ? null : (req.query.name ? req.query.name : "John Smith"));

        if (fileExt != null) {
            var fileName = docManager.createDemo(!!req.query.sample, fileExt, userid, name);
            var redirectPath = docManager.getServerUrl() + "/wopi-action/" + encodeURIComponent(fileName) + "?action=edit";
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