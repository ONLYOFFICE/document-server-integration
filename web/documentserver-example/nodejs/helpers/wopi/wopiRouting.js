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

const config = require('config');
const tokenValidator = require('./tokenValidator');
const filesController = require('./filesController');
const utils = require('./utils');
const DocManager = require('../docManager');
const fileUtility = require('../fileUtility');
const users = require('../users');

const configServer = config.get('server');
const siteUrl = configServer.get('siteUrl'); // the path to the editors installation

const getCustomWopiParams = function getCustomWopiParams(query) {
  let tokenParams = '';
  let actionParams = '';

  const { userid } = query; // user id
  tokenParams += (userid ? `&userid=${userid}` : '');

  const { lang } = query; // language
  actionParams += (lang ? `&ui=${lang}` : '');

  return { tokenParams, actionParams };
};

exports.registerRoutes = function registerRoutes(app) {
  // define a handler for the default wopi page
  app.get('/wopi', async (req, res) => {
    req.DocManager = new DocManager(req, res);

    await utils.initWopi(req.DocManager);

    // get the wopi discovery information
    const actions = await utils.getDiscoveryInfo();
    const wopiEnable = actions.length !== 0;
    const docsExtEdit = []; // Supported extensions for WOPI

    actions.forEach((el) => {
      if (el.name === 'edit') docsExtEdit.push(`.${el.ext}`);
    });

    // Checking supported extensions
    const editedExts = fileUtility.getEditExtensions().filter((i) => docsExtEdit.includes(i));
    const fillExts = fileUtility.getFillExtensions().filter((i) => docsExtEdit.includes(i));

    try {
      // get all the stored files
      const files = req.DocManager.getStoredFiles();

      // run through all the files and write the corresponding information to each file
      // eslint-disable-next-line no-restricted-syntax
      for (const file of files) {
        const ext = fileUtility.getFileExtension(file.name, true); // get an extension of each file
        // eslint-disable-next-line no-await-in-loop
        file.actions = await utils.getActions(ext); // get actions of the specified extension
        // eslint-disable-next-line no-await-in-loop
        file.defaultAction = await utils.getDefaultAction(ext);// get the default action of the specified extension
      }

      // render wopiIndex template with the parameters specified
      res.render('wopiIndex', {
        wopiEnable,
        storedFiles: wopiEnable ? files : [],
        params: req.DocManager.getCustomParams(),
        users,
        preloaderUrl: siteUrl + configServer.get('preloaderUrl'),
        convertExts: fileUtility.getConvertExtensions(),
        editedExts,
        fillExts,
        languages: configServer.get('languages'),
      });
    } catch (ex) {
      console.log(ex); // display error message in the console
      res.status(500); // write status parameter to the response
      // render error template with the message parameter specified
      res.render('error', { message: 'Server error' });
    }
  });
  // define a handler for creating a new wopi editing session
  app.get('/wopi-new', (req, res) => {
    const { fileExt } = req.query; // get the file extension from the request

    req.DocManager = new DocManager(req, res);

    if (fileExt) { // if the file extension exists
      const fileName = req.DocManager.getCorrectName(`new.${fileExt}`);
      const redirectPath = `${req.DocManager.getServerUrl(true)}/wopi-action/`
      + `${encodeURIComponent(fileName)}?action=editnew${req.DocManager.getCustomParams()}`; // get the redirect path
      res.redirect(redirectPath);
    }
  });
  // define a handler for getting wopi action information by its id
  app.get('/wopi-action/:id', async (req, res) => {
    try {
      req.DocManager = new DocManager(req, res);

      await utils.initWopi(req.DocManager);

      let fileName = req.DocManager.getCorrectName(req.params.id);
      const fileExt = fileUtility.getFileExtension(fileName, true); // get the file extension from the request
      const user = users.getUser(req.query.userid); // get a user by the id

      // get an action for the specified extension and name
      const action = await utils.getAction(fileExt, req.query.action);

      if (action && req.query.action === 'editnew') {
        fileName = req.DocManager.requestEditnew(req, fileName, user);
      }

      // render wopiAction template with the parameters specified
      res.render('wopiAction', {
        actionUrl: utils.getActionUrl(
          req.DocManager.getServerUrl(true),
          req.DocManager.curUserHostAddress(),
          action,
          req.params.id,
        ),
        token: 'test',
        tokenTtl: Date.now() + 1000 * 60 * 60 * 10,
        params: getCustomWopiParams(req.query),
      });
    } catch (ex) {
      console.log(ex);
      res.status(500);
      res.render('error', { message: 'Server error' });
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
