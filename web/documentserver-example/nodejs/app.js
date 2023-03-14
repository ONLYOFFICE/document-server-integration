"use strict";
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

// connect the necessary packages and modules
const express = require("express");
const path = require("path");
const favicon = require("serve-favicon");
const bodyParser = require("body-parser");
const config = require('config');
const configServer = config.get('server');
const docManager = require("./helpers/docManager");
const wopiApp = require("./helpers/wopi/wopiRouting");
const verifyPeerOff = configServer.get('verify_peer_off');

if(verifyPeerOff) {
    process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
}

String.prototype.hashCode = function () {
	const len = this.length;
	let ret = 0;
    for (let i = 0; i < len; i++) {
        ret = (31 * ret + this.charCodeAt(i)) << 0;
    }
    return ret;
};
String.prototype.format = function () {
    let text = this.toString();

    if (!arguments.length) return text;

    for (let i = 0; i < arguments.length; i++) {
        text = text.replace(new RegExp("\\{" + i + "\\}", "gi"), arguments[i]);
    }

    return text;
};


const app = express();  // create an application object
app.disable("x-powered-by");
app.set("views", path.join(__dirname, "views"));  // specify the path to the main template
app.set("view engine", "ejs");  // specify which template engine is used


app.use(function (req, res, next) {
    res.setHeader('Access-Control-Allow-Origin', '*');  // allow any Internet domain to access the resources of this site
    next();
});

app.use(express.static(path.join(__dirname, "public")));  // public directory
if (config.has('server.static')) {  // check if there are static files such as .js, .css files, images, samples and process them
  const staticContent = config.get('server.static');
  for (let i = 0; i < staticContent.length; ++i) {
    const staticContentElem = staticContent[i];
    app.use(staticContentElem['name'], express.static(staticContentElem['path'], staticContentElem['options']));
  }
}
app.use(favicon(__dirname + "/public/images/favicon.ico"));  // use favicon


app.use(bodyParser.json());  // connect middleware that parses json
app.use(bodyParser.urlencoded({ extended: false }));  // connect middleware that parses urlencoded bodies


app.get("/", function (req, res) {  // define a handler for default page
    req.docManager = new docManager(req, res);
    var redirectPath = req.docManager.getServerUrl() + "/wopi";
    res.redirect(redirectPath);
});


wopiApp.registerRoutes(app);

// "Not found" error with 404 status
app.use(function (req, res, next) {
    const err = new Error("Not Found");
    err.status = 404;
    next(err);
});

// render the error template with the parameters specified
app.use(function (err, req, res, next) {
    res.status(err.status || 500);
    res.render("error", {
        message: err.message
    });
});

// save all the functions to the app module to export it later in other files
module.exports = app;