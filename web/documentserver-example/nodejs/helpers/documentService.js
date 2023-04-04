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

// get all the necessary values and modules
const urlModule = require('url');
const urllib = require('urllib');
const jwt = require('jsonwebtoken');
const configServer = require('config').get('server');
const fileUtility = require('./fileUtility');
const guidManager = require('./guidManager');

const siteUrl = configServer.get('siteUrl'); // the path to the editors installation
const cfgSignatureEnable = configServer.get('token.enable');
const cfgSignatureUseForRequest = configServer.get('token.useforrequest');
const cfgSignatureAuthorizationHeader = configServer.get('token.authorizationHeader');
const cfgSignatureAuthorizationHeaderPrefix = configServer.get('token.authorizationHeaderPrefix');
const cfgSignatureSecretExpiresIn = configServer.get('token.expiresIn');
const cfgSignatureSecret = configServer.get('token.secret');
const cfgSignatureSecretAlgorithmRequest = configServer.get('token.algorithmRequest');

const documentService = {};

documentService.userIp = null;

// get the url of the converted file (synchronous)
documentService.getConvertedUriSync = function getConvertedUriSync(
  documentUri,
  fromExtension,
  toExtension,
  documentRevisionId,
  callback,
) {
  documentService.getConvertedUri(documentUri, fromExtension, toExtension, documentRevisionId, false, (err, data) => {
    callback(err, data);
  });
};

// get the url of the converted file
documentService.getConvertedUri = function getConvertedUri(
  documentUri,
  fromExtension,
  toExtension,
  documentRevisionId,
  async,
  callback,
  filePass = null,
  lang = null,
) {
  const fromExt = fromExtension || fileUtility.getFileExtension(documentUri); // get the current document extension

  const title = fileUtility.getFileName(documentUri) || guidManager.newGuid(); // get the current document name or uuid

  // generate the document key value
  const revisionId = documentService.generateRevisionId(documentRevisionId || documentUri);

  const params = { // write all the conversion parameters to the params dictionary
    async,
    url: documentUri,
    outputtype: toExtension.replace('.', ''),
    filetype: fromExt.replace('.', ''),
    title,
    key: revisionId,
    password: filePass,
    region: lang,
  };

  const uri = siteUrl + configServer.get('converterUrl'); // get the absolute converter url
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };

  if (cfgSignatureEnable && cfgSignatureUseForRequest) { // if the signature is enabled and it can be used for request
    // write signature authorization header
    headers[cfgSignatureAuthorizationHeader] = cfgSignatureAuthorizationHeaderPrefix + this.fillJwtByUrl(uri, params);
    params.token = documentService.getToken(params); // get token and save it to the parameters
  }

  // parse url to allow request by relative url after
  // https://github.com/node-modules/urllib/pull/321/commits/514de1924bf17a38a6c2db2a22a6bc3494c0a959
  urllib.request(
    urlModule.parse(uri),
    {
      method: 'POST',
      headers,
      data: params,
    },
    callback,
  );
};

// generate the document key value
documentService.generateRevisionId = function generateRevisionId(expectedKey) {
  const maxKeyLength = 128; // the max key length is 128
  let expKey = expectedKey;
  if (expKey.length > maxKeyLength) { // if the expected key length is greater than the max key length
    // the expected key is hashed and a fixed length value is stored in the string format
    expKey = expKey.hashCode().toString();
  }

  const key = expKey.replace(/[^0-9-.a-zA-Z_=]/g, '_');

  return key.substring(0, Math.min(key.length, maxKeyLength)); // the resulting key is of the max key length or less
};

// create an error message for the error code
documentService.processConvertServiceResponceError = function processConvertServiceResponceError(errorCode) {
  let errorMessage = '';
  const errorMessageTemplate = 'Error occurred in the ConvertService: ';

  // add the error message to the error message template depending on the error code
  switch (errorCode) {
    case -20:
      errorMessage = `${errorMessageTemplate}Error encrypt signature`;
      break;
    case -8:
      errorMessage = `${errorMessageTemplate}Error document signature`;
      break;
    case -7:
      errorMessage = `${errorMessageTemplate}Error document request`;
      break;
    case -6:
      errorMessage = `${errorMessageTemplate}Error database`;
      break;
    case -5:
      errorMessage = `${errorMessageTemplate}Incorrect password`;
      break;
    case -4:
      errorMessage = `${errorMessageTemplate}Error download error`;
      break;
    case -3:
      errorMessage = `${errorMessageTemplate}Error convertation error`;
      break;
    case -2:
      errorMessage = `${errorMessageTemplate}Error convertation timeout`;
      break;
    case -1:
      errorMessage = `${errorMessageTemplate}Error convertation unknown`;
      break;
    case 0: // if the error code is equal to 0, the error message is empty
      break;
    default:
      errorMessage = `ErrorCode = ${errorCode}`; // default value for the error message
      break;
  }

  throw new Error(errorMessage);
};

// get the response url
documentService.getResponseUri = function getResponseUri(json) {
  const fileResult = JSON.parse(json);

  if (fileResult.error) { // if an error occurs
    documentService.processConvertServiceResponceError(parseInt(fileResult.error, 10)); // get an error message
  }

  const isEndConvert = fileResult.endConvert; // check if the conversion is completed

  let percent = parseInt(fileResult.percent, 10); // get the conversion percentage
  let uri = null;
  let fileType = null;

  if (isEndConvert) { // if the conversion is completed
    if (!fileResult.fileUrl) { // and the file url doesn't exist
      throw new Error('FileUrl is null'); // the file url is null
    }

    uri = fileResult.fileUrl; // otherwise, get the file url
    ({ fileType } = fileResult); // get the file type
    percent = 100;
  } else { // if the conversion isn't completed
    percent = percent >= 100 ? 99 : percent; // get the percentage value
  }

  return {
    percent,
    uri,
    fileType,
  };
};

// create a command request
documentService.commandRequest = function commandRequest(method, documentRevisionId, callback, meta = null) {
  const revisionId = documentService.generateRevisionId(documentRevisionId); // generate the document key value
  const params = { // create a parameter object with command method and the document key value in it
    c: method,
    key: revisionId,
  };

  if (meta) {
    params.meta = meta;
  }

  const uri = siteUrl + configServer.get('commandUrl'); // get the absolute command url
  const headers = { // create a headers object
    'Content-Type': 'application/json',
  };
  if (cfgSignatureEnable && cfgSignatureUseForRequest) {
    headers[cfgSignatureAuthorizationHeader] = cfgSignatureAuthorizationHeaderPrefix + this.fillJwtByUrl(uri, params);
    params.token = documentService.getToken(params);
  }

  // parse url to allow request by relative url after
  // https://github.com/node-modules/urllib/pull/321/commits/514de1924bf17a38a6c2db2a22a6bc3494c0a959
  urllib.request(
    urlModule.parse(uri),
    {
      method: 'POST',
      headers,
      data: params,
    },
    callback,
  );
};

// check jwt token headers
documentService.checkJwtHeader = function checkJwtHeader(req) {
  let decoded = null;
  const authorization = req.get(cfgSignatureAuthorizationHeader); // get signature authorization header from the request
  // if authorization header exists and it starts with the authorization header prefix
  if (authorization && authorization.startsWith(cfgSignatureAuthorizationHeaderPrefix)) {
    // the resulting token starts after the authorization header prefix
    const token = authorization.substring(cfgSignatureAuthorizationHeaderPrefix.length);
    try {
      decoded = jwt.verify(token, cfgSignatureSecret); // verify signature on jwt token using signature secret
    } catch (err) {
      // print debug information to the console
      console.log(`checkJwtHeader error: name = ${err.name} message = ${err.message} token = ${token}`);
    }
  }
  return decoded;
};

// get jwt token using url information
documentService.fillJwtByUrl = function fillJwtByUrl(uri, optDataObject) {
  const parseObject = urlModule.parse(uri, true); // get parse object from the url
  const payload = { query: parseObject.query, payload: optDataObject }; // create payload object

  const options = { algorithm: cfgSignatureSecretAlgorithmRequest, expiresIn: cfgSignatureSecretExpiresIn };
  // sign token with given data using signature secret and options parameters
  return jwt.sign(payload, cfgSignatureSecret, options);
};

// get token
documentService.getToken = function getToken(data) {
  const options = { algorithm: cfgSignatureSecretAlgorithmRequest, expiresIn: cfgSignatureSecretExpiresIn };
  // sign token with given data using signature secret and options parameters
  return jwt.sign(data, cfgSignatureSecret, options);
};

// read and verify token
documentService.readToken = function readToken(token) {
  try {
    return jwt.verify(token, cfgSignatureSecret); // verify signature on jwt token using signature secret
  } catch (err) {
    console.log(`checkJwtHeader error: name = ${err.name} message = ${err.message} token = ${token}`);
  }
  return null;
};

// save all the functions to the documentService module to export it later in other files
module.exports = documentService;
