﻿<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="DocEditor.aspx.cs" Inherits="OnlineEditorsExample.DocEditor" Title="ONLYOFFICE" %>

<%@ Import Namespace="System.IO" %>
<%@ Import Namespace="OnlineEditorsExample" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head runat="server">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=no, minimal-ui" />
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="mobile-web-app-capable" content="yes" />
    <link rel="icon" href="<%= "app_themes/images/" + DocumentType + ".ico" %>" type="image/x-icon" />
    <title><%= FileName + " - ONLYOFFICE" %></title>
    <!--
    *
    * (c) Copyright Ascensio System SIA 2025
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
    -->
    <style>
        html {
            height: 100%;
            width: 100%;
        }

        body {
            background: #fff;
            color: #333;
            font-family: Arial, Tahoma,sans-serif;
            font-size: 12px;
            font-weight: normal;
            height: 100%;
            margin: 0;
            overflow-y: hidden;
            padding: 0;
            text-decoration: none;
        }

        form {
            height: 100%;
        }

        div {
            margin: 0;
            padding: 0;
        }
    </style>

    <script language="javascript" type="text/javascript" src="<%= DocServiceApiUri %>"></script>

    <script type="text/javascript" language="javascript">

        var docEditor;
        var config;

        var innerAlert = function (message, inEditor) {
            if (console && console.log)
                console.log(message);
            if (inEditor && docEditor)
                docEditor.showMessage(message);
        };

        // the application is loaded into the browser
        var onAppReady = function () {
            innerAlert("Document editor ready");
        };

        // the document is modified
        var onDocumentStateChange = function (event) {
            var title = document.title.replace(/^\*/g, "");
            document.title = (event.data ? "*" : "") + title;
        };

        // the user is trying to switch the document from the viewing into the editing mode
        var onRequestEditRights = function () {
            location.href = location.href.replace(RegExp("editorsMode=\\w+\&?", "i"), "") + "&editorsMode=edit";
        };

        // an error or some other specific event occurs
        var onError = function (event) {
            if (event)
                innerAlert(event.data);
        };

        // the document is opened for editing with the old document.key value
        var onOutdatedVersion = function (event) {
            location.reload(true);
        };

        // replace the link to the document which contains a bookmark
        var replaceActionLink = function(href, linkParam) {
            var link;
            var actionIndex = href.indexOf("&actionLink=");
            if (actionIndex != -1) {
                var endIndex = href.indexOf("&", actionIndex + "&actionLink=".length);
                if (endIndex != -1) {
                    link = href.substring(0, actionIndex) + href.substring(endIndex) + "&actionLink=" + encodeURIComponent(linkParam);
                } else {
                    link = href.substring(0, actionIndex) + "&actionLink=" + encodeURIComponent(linkParam);
                }
            } else {
                link = href + "&actionLink=" + encodeURIComponent(linkParam);
            }
            return link;
        }

        // the user is trying to get link for opening the document which contains a bookmark, scrolling to the bookmark position
        var onMakeActionLink = function (event) {
            var actionData = event.data;
            var linkParam = JSON.stringify(actionData);
            docEditor.setActionLink(replaceActionLink(location.href, linkParam));  // set the link to the document which contains a bookmark
        };

        var onRequestClose = function () {  // close editor
            docEditor.destroyEditor();
            innerAlert("Document editor closed successfully");
        };

        var onUserActionRequired = function () {
            console.log("User action required");
        };

        // the meta information of the document is changed via the meta command
        var onMetaChange = function (event) {
            if (event.data.favorite !== undefined) {
                var favorite = !!event.data.favorite;
                var title = document.title.replace(/^\☆/g, "");
                document.title = (favorite ? "☆" : "") + title;
                docEditor.setFavorite(favorite);  // change the Favorite icon state
            }

            innerAlert("onMetaChange: " + JSON.stringify(event.data));
        };

        // the user is trying to insert an image by clicking the Image from Storage button
        var onRequestInsertImage = function (event) {
            docEditor.insertImage({  // insert an image into the file
                "c": event.data.c,
                <%= InsertImageConfig %>
            })
        };

        // the user is trying to select document for comparing by clicking the Document from Storage button
        var onRequestSelectDocument = function (event) {
            var data = <%= DocumentData %>;
            data.c = event.data.c;
            docEditor.setRequestedDocument(data);  // select a document for comparing
        };

        // the user is trying to select recipients data by clicking the Mail merge button
        var onRequestSelectSpreadsheet = function (event) {
            var data = <%= DataSpreadsheet %>;
            data.c = event.data.c;
            docEditor.setRequestedSpreadsheet(data);  // insert recipient data for spreadsheet into the file
        };

        var onRequestSaveAs = function (event) {  //  the user is trying to save file by clicking Save Copy as... button
            var title = event.data.title;
            var url = event.data.url;
            var data = {
                title: title,
                url: url
            };
            let xhr = new XMLHttpRequest();
            xhr.open("POST", "webeditor.ashx?type=saveas");
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.send(JSON.stringify(data));
            xhr.onload = function () {
                innerAlert(xhr.responseText);
                innerAlert(JSON.parse(xhr.responseText).file, true);
            }
        };

        var onRequestRename = function(event) { //  the user is trying to rename file by clicking Rename... button
            innerAlert("onRequestRename: " + JSON.stringify(event.data));

            var newfilename = event.data;
            var data = {
                newfilename: newfilename,
                dockey: config.document.key,
                ext: config.document.fileType
            };

            let xhr = new XMLHttpRequest();
            xhr.open("POST", "webeditor.ashx?type=rename");
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.send(JSON.stringify(data));
            xhr.onload = function () {
                innerAlert(xhr.responseText);
            }
        };

        var onRequestRefreshFile = function(event) {
            let xhr = new XMLHttpRequest();
            xhr.open("GET", "webeditor.ashx?type=config&fileName=" + encodeURIComponent(config.document.title) +
                "&directUrl=" + !!config.document.directUrl +
                "&permissions=" + encodeURIComponent(JSON.stringify(config.document.permissions)));
            xhr.send();
            xhr.onload = function () {
                innerAlert(xhr.responseText);
                docEditor.refreshFile(JSON.parse(xhr.responseText));
            };
        };

        var onRequestOpen = function (event) {  // user open external data source
            innerAlert("onRequestOpen");
            var windowName = event.data.windowName;

            requestReference(event.data, function (data) {
                if (data.error) {
                    var winEditor = window.open("", windowName);
                    winEditor.close();
                    innerAlert(data.error, true);
                    return;
                }

                var link = data.link;
                window.open(link, windowName);
            });
        };

        var onRequestReferenceData = function (event) {  // user refresh external data source
            innerAlert("onRequestReferenceData");

            requestReference(event.data, function (data) {
                docEditor.setReferenceData(data);
            });
        };

        var requestReference = function (data, callback) {
            innerAlert(data);

            data.directUrl = !!config.document.directUrl;
            let xhr = new XMLHttpRequest();
            xhr.open("POST", "webeditor.ashx?type=reference");
            xhr.setRequestHeader("Content-Type", "application/json");
            xhr.send(JSON.stringify(data));
            xhr.onload = function () {
                console.log(xhr.responseText);
                callback(JSON.parse(xhr.responseText));
            }
        };

        var onRequestReferenceSource = function (event) {
          innerAlert("onRequestReferenceSource");
          let xhr = new XMLHttpRequest();
          xhr.open("GET", "webeditor.ashx?type=files");
          xhr.setRequestHeader("Content-Type", "application/json");
          xhr.send();
          xhr.onload = function () {
            if (xhr.status === 200) {
              innerAlert(JSON.parse(xhr.responseText));
              let fileList = JSON.parse(xhr.responseText);
              let firstXlsxName;
              let file;
              for (file of fileList) {
                if (file["title"]) {
                  if (getFileExt(file["title"]) === "xlsx")
                  {
                    firstXlsxName = file["title"];
                    break;
                  }
                }
              }
              if (firstXlsxName) {
                let data = {
                  directUrl : !!config.document.directUrl,
                  path : firstXlsxName
                };
                let xhr = new XMLHttpRequest();
                xhr.open("POST", "webeditor.ashx?type=reference");
                xhr.setRequestHeader("Content-Type", "application/json");
                xhr.send(JSON.stringify(data));
                xhr.onload = function () {
                  if (xhr.status === 200) {
                    docEditor.setReferenceSource(JSON.parse(xhr.responseText));
                  } else {
                    innerAlert("/reference - bad status");
                  }
                }
              } else {
                innerAlert("No *.xlsx files");
              }
            } else {
              innerAlert("/files - bad status");
            }
          }
        };

        var onRequestUsers = function (event) {
            if (event && event.data){
                var c = event.data.c;
            }
            switch (c) {
                case "info":
                    users = [];
                    var allUsers = <%= UsersInfo %>;
                    for (var i = 0; i < event.data.id.length; i++) {
                        for (var j = 0; j < allUsers.length; j++) {
                            if (allUsers[j].id == event.data.id[i]) {
                                users.push(allUsers[j]);
                                break;
                            }
                        }
                    }
                    break;
                case "protect":
                    var users = <%= UsersForProtect %>;
                    break;
                default:
                    users = <%= UsersForMentions %>;
            }
            docEditor.setUsers({
                "c": c,
                "users": users,
            });
        };

        var onRequestSendNotify = function (event) {
            event.data.actionLink = replaceActionLink(location.href, JSON.stringify(event.data.actionLink));
            var data = JSON.stringify(event.data);
            innerAlert("onRequestSendNotify: " + data);
        };

        config = <%= DocConfig %>;

        config.width = "100%";
        config.height = "100%";

        config.events = {
            'onAppReady': onAppReady,
            'onDocumentStateChange': onDocumentStateChange,
            'onUserActionRequired': onUserActionRequired,
            'onError': onError,
            'onOutdatedVersion': onOutdatedVersion,
            'onMakeActionLink': onMakeActionLink,
            'onMetaChange': onMetaChange,
            'onRequestInsertImage': onRequestInsertImage,
            'onRequestSelectDocument': onRequestSelectDocument,
            "onRequestSelectSpreadsheet": onRequestSelectSpreadsheet,
        };

        if (config.editorConfig.user.id) {
            config.events['onRequestRefreshFile'] = onRequestRefreshFile;
            config.events['onRequestClose'] = onRequestClose;
            config.events['onRequestHistory'] = function (event) {  // the user is trying to show the document version history

                let xhr = new XMLHttpRequest();
                xhr.open("GET", "webeditor.ashx?type=gethistory&filename=<%= FileName %>");
                xhr.setRequestHeader("Content-Type", "application/json");
                xhr.send();
                xhr.onload = function () {
                    console.log(xhr.responseText);
                    docEditor.refreshHistory(JSON.parse(xhr.responseText));
                }
            };
            config.events['onRequestHistoryData'] = function (event) {  // the user is trying to click the specific document version in the document version history
                var ver = event.data;

                let xhr = new XMLHttpRequest();
                xhr.open("GET", "webeditor.ashx?type=getversiondata&filename=<%= FileName %>&version=" + ver + "&directUrl=" + !!config.document.directUrl);
                xhr.setRequestHeader("Content-Type", "application/json");
                xhr.send();
                xhr.onload = function () {
                    console.log(xhr.responseText);
                    docEditor.setHistoryData(JSON.parse(xhr.responseText));  // send the link to the document for viewing the version history
                }
            };
            if (config.editorConfig.user.id !== "uid-3") {
                config.events['onRequestHistoryClose'] = function () {  // the user is trying to go back to the document from viewing the document version history
                    document.location.reload();
                };
                config.events['onRequestRestore'] = function (event) {
                    var fileName = "<%= FileName %>";
                    var version = event.data.version;
                    var url = event.data.url;
                    var data = {
                        fileName: fileName,
                        version: version,
                        url: url
                    };

                    let xhr = new XMLHttpRequest();
                    xhr.open("POST", "webeditor.ashx?type=restore&directUrl=" + !!config.document.directUrl);
                    xhr.setRequestHeader('Content-Type', 'application/json');
                    xhr.send(JSON.stringify(data));
                    xhr.onload = function () {
                        docEditor.refreshHistory(JSON.parse(xhr.responseText));
                    }
                };
            }

            // add mentions for not anonymous users
            <% if (!string.IsNullOrEmpty(UsersForMentions))
            { %>
                config.events['onRequestUsers'] = onRequestUsers;
            <% } %>

            config.events['onRequestSaveAs'] = onRequestSaveAs;
            // the user is mentioned in a comment
            config.events['onRequestSendNotify'] = onRequestSendNotify;
            // prevent file renaming for anonymous users
            config.events['onRequestRename'] = onRequestRename;
            config.events['onRequestReferenceData'] = onRequestReferenceData;
            // prevent switch the document from the viewing into the editing mode for anonymous users
            config.events['onRequestEditRights'] = onRequestEditRights;
            config.events['onRequestOpen'] = onRequestOpen;
            config.events['onRequestReferenceSource'] = onRequestReferenceSource;
        }

        var сonnectEditor = function () {
            docEditor = new DocsAPI.DocEditor("iframeEditor", config);
        };

        const getFileExt = function (fileName) {
          if (fileName.indexOf(".")) {
            return fileName.split('.').reverse()[0];
          }
          return false;
        };

        if (window.addEventListener) {
            window.addEventListener("load", сonnectEditor);
        } else if (window.attachEvent) {
            window.attachEvent("load", сonnectEditor);
        }

    </script>
</head>
<body>
    <form id="form1" runat="server">
        <div id="iframeEditor">
        </div>
    </form>
</body>
</html>
