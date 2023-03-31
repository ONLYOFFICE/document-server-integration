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

const initWopi = async function (DocManager) {
  let absSiteUrl = siteUrl;
  if (absSiteUrl.indexOf('/') === 0) {
    absSiteUrl = DocManager.getServerHost() + siteUrl;
  }

  // get the wopi discovery information
  await getDiscoveryInfo(absSiteUrl);
}

// get the wopi discovery information
const getDiscoveryInfo = async function (siteUrl) {
  let actions = [];

  if (cache) return cache;

  try {
    actions = await requestDiscovery(siteUrl);
  } catch (e) {
    return actions;
  }

  cache = actions;
  setTimeout(() => {
    return cache = null
  }, 1000 * 60 * 60); // 1 hour

  return actions;
}

const requestDiscovery = async function (siteUrl) {
  // eslint-disable-next-line no-unused-vars
  return new Promise((resolve, reject) => {
    const actions = [];
    urllib.request(urlModule.parse(siteUrl + configServer.get('wopi.discovery')), { method: 'GET' }, (err, data) => {
      if (data) {
        // create the discovery XML file with the parameters from the response
        const discovery = xmlParser.parse(data.toString(), {
          attributeNamePrefix: '',
          ignoreAttributes: false,
          parseAttributeValue: true,
          // eslint-disable-next-line no-unused-vars
          attrValueProcessor: (val, attrName) => {
            return he.decode(val, { isAttributeValue: true })
          }
        });
        if (discovery['wopi-discovery']) {
          for (const app of discovery['wopi-discovery']['net-zone'].app) {
            if (!Array.isArray(app.action)) {
              app.action = [app.action];
            }
            for (const action of app.action) {
              actions.push({ // write all the parameters to the actions element
                app: app.name,
                favIconUrl: app.favIconUrl,
                checkLicense: app.checkLicense == 'true',
                name: action.name,
                ext: action.ext || '',
                progid: action.progid || '',
                isDefault: !!action.default,
                urlsrc: action.urlsrc,
                requires: action.requires || ''
              });
            }
          }
        }
      }
      resolve(actions);
    });
  })
}

// get actions of the specified extension
const getActions = async function (ext) {
  const actions = await getDiscoveryInfo(); // get the wopi discovery information
  const filtered = [];

  for (const action of actions) { // and filter it by the specified extention
    if (action.ext == ext) {
      filtered.push(action);
    }
  }

  return filtered;
}

// get an action for the specified extension and name
const getAction = async function (ext, name) {
  const actions = await getDiscoveryInfo();

  for (const action of actions) {
    if (action.ext == ext && action.name == name) {
      return action;
    }
  }

  return null;
}

// get the default action for the specified extension
const getDefaultAction = async function (ext) {
  const actions = await getDiscoveryInfo();

  for (const action of actions) {
    if (action.ext == ext && action.isDefault) {
      return action;
    }
  }

  return null;
}

// get the action url
const getActionUrl = function (host, userAddress, action, filename) {
  return `${action.urlsrc.replace(/<.*&>/g, '')}WOPISrc=${host}/wopi/files/${filename}@${userAddress}`;
}

exports.initWopi = initWopi;
exports.getDiscoveryInfo = getDiscoveryInfo;
exports.getAction = getAction;
exports.getActions = getActions;
exports.getActionUrl = getActionUrl;
exports.getDefaultAction = getDefaultAction;
