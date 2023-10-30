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
const urlModule = require('url');
const urllib = require('urllib');
const xmlParser = require('fast-xml-parser');
const he = require('he');

const configServer = config.get('server');
const siteUrl = configServer.get('siteUrl'); // the path to the editors installation

let cache = null;

const requestDiscovery = async function requestDiscovery(url) {
  // eslint-disable-next-line no-unused-vars
  return new Promise((resolve, reject) => {
    const actions = [];
    urllib.request(urlModule.parse(url + configServer.get('wopi.discovery')), { method: 'GET' }, (err, data) => {
      if (data) {
        // create the discovery XML file with the parameters from the response
        const xmlParseOptions = {
          attributeNamePrefix: '',
          ignoreAttributes: false,
          parseAttributeValue: true,
          attrValueProcessor: (val) => he.decode(val, { isAttributeValue: true }),
        };
        const parser = new xmlParser.XMLParser(xmlParseOptions);
        // create the discovery XML file with the parameters from the response
        const discovery = parser.parse(data.toString());
        if (discovery['wopi-discovery']) {
          discovery['wopi-discovery']['net-zone'].app.forEach((app) => {
            let appAction = app.action;
            if (!Array.isArray(appAction)) {
              appAction = [appAction];
            }
            appAction.forEach((action) => {
              actions.push({ // write all the parameters to the actions element
                app: app.name,
                favIconUrl: app.favIconUrl,
                checkLicense: app.checkLicense === 'true',
                name: action.name,
                ext: action.ext || '',
                progid: action.progid || '',
                isDefault: !!action.default,
                urlsrc: action.urlsrc,
                requires: action.requires || '',
              });
            });
          });
        }
      }
      resolve(actions);
    });
  });
};

// get the wopi discovery information
const getDiscoveryInfo = async function getDiscoveryInfo(url) {
  let actions = [];

  if (cache) return cache;

  try {
    actions = await requestDiscovery(url);
  } catch (e) {
    return actions;
  }

  cache = actions;
  setTimeout(() => {
    cache = null;
    return cache;
  }, 1000 * 60 * 60); // 1 hour

  return actions;
};

const initWopi = async function initWopi(DocManager) {
  let absSiteUrl = siteUrl;
  if (absSiteUrl.indexOf('/') === 0) {
    absSiteUrl = DocManager.getServerHost() + siteUrl;
  }

  // get the wopi discovery information
  await getDiscoveryInfo(absSiteUrl);
};

// get actions of the specified extension
const getActions = async function getActions(ext) {
  const actions = await getDiscoveryInfo(); // get the wopi discovery information
  const filtered = [];

  actions.forEach((action) => { // and filter it by the specified extention
    if (action.ext === ext) {
      filtered.push(action);
    }
  });

  return filtered;
};

// get an action for the specified extension and name
const getAction = async function getAction(ext, name) {
  const actions = await getDiscoveryInfo();
  let act = null;

  actions.forEach((action) => {
    if (action.ext === ext && action.name === name) {
      act = action;
    }
  });

  return act;
};

// get the default action for the specified extension
const getDefaultAction = async function getDefaultAction(ext) {
  const actions = await getDiscoveryInfo();
  let act = null;

  actions.forEach((action) => {
    if (action.ext === ext && action.isDefault) {
      act = action;
    }
  });

  return act;
};

// get the action url
const getActionUrl = function getActionUrl(host, userAddress, action, filename) {
  return `${action.urlsrc.replace(/<.*&>/g, '')}WOPISrc=${host}/wopi/files/${filename}@${userAddress}`;
};

exports.initWopi = initWopi;
exports.getDiscoveryInfo = getDiscoveryInfo;
exports.getAction = getAction;
exports.getActions = getActions;
exports.getActionUrl = getActionUrl;
exports.getDefaultAction = getDefaultAction;
