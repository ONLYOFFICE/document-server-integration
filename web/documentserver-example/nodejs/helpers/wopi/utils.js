const config = require('config');
const syncRequest = require("sync-request");
const xmlParser = require("fast-xml-parser");
const he = require("he");

var cache = null;

function getDiscoveryInfo(retry) {
    let actions = [];

    if (cache) return cache;

    try {
        let response = syncRequest("GET", config.get("wopi.discovery"));
        let discovery = xmlParser.parse(response.getBody().toString(), {
            attributeNamePrefix: "",
            ignoreAttributes: false,
            parseAttributeValue: true,
            attrValueProcessor: (val, attrName) => he.decode(val, { isAttributeValue: true })
        });
        for (let app of discovery["wopi-discovery"]["net-zone"].app) {
            if (!Array.isArray(app.action)) { app.action = [app.action]; }
            for (let action of app.action) {
                actions.push({
                    app: app.name,
                    favIconUrl: app.favIconUrl,
                    checkLicense: app.checkLicense == 'true',
                    name: action.name,
                    ext: action.ext || "",
                    progid: action.progid || "",
                    isDefault: action.default ? true : false,
                    urlsrc: action.urlsrc,
                    requires: action.requires || ""
                });
            }
        }
    } catch (e) {
        if (retry) {
            setTimeout(getDiscoveryInfo, 1000, true);
        }
        return actions;
    }

    cache = actions;
    setTimeout(() => cache = null, 1000 * 60 * 60); // 1 hour

    return actions;
}

function getActions(ext) {
    let actions = getDiscoveryInfo();
    let filtered = [];

    for (let action of actions) {
        if (action.ext == ext) {
            filtered.push(action);
        }
    }

    return filtered;
}

function getAction(ext, name) {
    let actions = getDiscoveryInfo();

    for (let action of actions) {
        if (action.ext == ext && action.name == name) {
            return action;
        }
    }

    return null;
}

function getDefaultAction(ext) {
    let actions = getDiscoveryInfo();

    for (let action of actions) {
        if (action.ext == ext && action.isDefault) {
            return action;
        }
    }

    return null;
}

function getActionUrl(host, userAddress, action, filename) {
    return action.urlsrc.replace(/<.*&>/g, "") + "WOPISrc=" + host + "/wopi/files/" + filename + "@" + userAddress;
}

exports.getDiscoveryInfo = getDiscoveryInfo;
exports.getAction = getAction;
exports.getActions = getActions;
exports.getActionUrl = getActionUrl;
exports.getDefaultAction = getDefaultAction;