<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="DocEditor.aspx.cs" Inherits="OnlineEditorsExample.DocEditor" Title="ONLYOFFICE" %>

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
    <title>ONLYOFFICE</title>
    <!--
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
            var title = document.title.replace(/\*$/g, "");
            document.title = title + (event.data ? "*" : "");
        };

        // the user is trying to switch the document from the viewing into the editing mode
        var onRequestEditRights = function () {
            location.href = location.href.replace(RegExp("editorsMode=view\&?", "i"), "");
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

        // the meta information of the document is changed via the meta command
        var onMetaChange = function (event) {
            if (event.data.favorite) {
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

        var onRequestReferenceData = function (event) {  // user refresh external data source

            event.data.directUrl = !!config.document.directUrl;
            let xhr = new XMLHttpRequest();
            xhr.open("POST", "webeditor.ashx?type=reference");
            xhr.setRequestHeader("Content-Type", "application/json");
            xhr.send(JSON.stringify(event.data));
            xhr.onload = function () {
                console.log(xhr.responseText);
                docEditor.setReferenceData(JSON.parse(xhr.responseText));
            }
        };

        config = <%= DocConfig %>;

        config.width = "100%";
        config.height = "100%";

        config.events = {
            'onAppReady': onAppReady,
            'onDocumentStateChange': onDocumentStateChange,
            'onError': onError,
            'onOutdatedVersion': onOutdatedVersion,
            'onMakeActionLink': onMakeActionLink,
            'onMetaChange': onMetaChange,
            'onRequestInsertImage': onRequestInsertImage,
            'onRequestSelectDocument': onRequestSelectDocument,
            "onRequestSelectSpreadsheet": onRequestSelectSpreadsheet,
        };

        if (config.editorConfig.user.id) {

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
            config.events['onRequestHistoryClose'] = function () {  // the user is trying to go back to the document from viewing the document version history
                document.location.reload();
            };
            config.events['onRequestRestore'] = function (event) {
                var fileName = "<%= FileName %>";
                var version = event.data.version;
                var data = {
                    fileName: fileName,
                    version: version
                };

                let xhr = new XMLHttpRequest();
                xhr.open("POST", "webeditor.ashx?type=restore&directUrl=" + !!config.document.directUrl);
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.send(JSON.stringify(data));
                xhr.onload = function () {
                    docEditor.refreshHistory(JSON.parse(xhr.responseText));
                }
            };

            // add mentions for not anonymous users
            <% if (!string.IsNullOrEmpty(UsersForMentions))
            { %>
                config.events['onRequestUsers'] = function () {
                    docEditor.setUsers({  // set a list of users to mention in the comments
                        "users": <%= UsersForMentions %>
                    });
                };
            <% } %>

            // the user is mentioned in a comment
            config.events['onRequestSendNotify'] = function (event) {
                event.data.actionLink = replaceActionLink(location.href, JSON.stringify(event.data.actionLink));
                var data = JSON.stringify(event.data);
                innerAlert("onRequestSendNotify: " + data);
            };
            // prevent file renaming for anonymous users
            config.events['onRequestRename'] = onRequestRename;
            config.events['onRequestReferenceData'] = onRequestReferenceData;
            // prevent switch the document from the viewing into the editing mode for anonymous users
            config.events['onRequestEditRights'] = onRequestEditRights;
        }

        if (config.editorConfig.createUrl) {
            config.events.onRequestSaveAs = onRequestSaveAs;
        };

        var сonnectEditor = function () {
            if ((config.document.fileType === "docxf" || config.document.fileType === "oform")
                && DocsAPI.DocEditor.version().split(".")[0] < 7) {
                innerAlert("Please update ONLYOFFICE Docs to version 7.0 to work on fillable forms online.");
                return;
            }

            docEditor = new DocsAPI.DocEditor("iframeEditor", config);
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
