<?php
/**
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
 */
 ?>

<!DOCTYPE html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1,
            maximum-scale=1, minimum-scale=1, user-scalable=no, minimal-ui" />
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="mobile-web-app-capable" content="yes" />
    <link rel="icon" href="/images/favicon.ico" type="image/x-icon" />
    <title>{!! $fileName !!} - ONLYOFFICE</title>

    <style>
        html {
            height: 100%;
            width: 100%;
        }

        body {
            background: #fff;
            color: #333;
            font-family: Arial, Tahoma, sans-serif;
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

    <script type="text/javascript" src="{!! $apiUrl !!}"></script>

    <script type="text/javascript">
        var docEditor;
        var config;
        let history;

        var innerAlert = function(message, inEditor) {
            if (console && console.log)
                console.log(message);
            if (inEditor && docEditor)
                docEditor.showMessage(message);
        };

        // the application is loaded into the browser
        var onAppReady = function() {
            innerAlert("Document editor ready");
        };

        // the document is modified
        var onDocumentStateChange = function(event) {
            var title = document.title.replace(/^\*/g, "");
            document.title = (event.data ? "*" : "") + title;
        };

        // the user is trying to switch the document from the viewing into the editing mode
        var onRequestEditRights = function() {
            location.href = location.href.replace(RegExp("action=\\w+\&?", "i"), "") + "&action=edit";
        };

        // an error or some other specific event occurs
        var onError = function(event) {
            if (event)
                innerAlert(event.data);
        };

        // the document is opened for editing with the old document.key value
        var onOutdatedVersion = function(event) {
            location.reload(true);
        };

        // replace the link to the document which contains a bookmark
        var replaceActionLink = function(href, linkParam) {
            var link;
            var actionIndex = href.indexOf("&actionLink=");
            if (actionIndex != -1) {
                var endIndex = href.indexOf("&", actionIndex + "&actionLink=".length);
                if (endIndex != -1) {
                    link = href.substring(0, actionIndex) + href.substring(endIndex) +
                        "&actionLink=" + encodeURIComponent(linkParam);
                } else {
                    link = href.substring(0, actionIndex) + "&actionLink=" + encodeURIComponent(linkParam);
                }
            } else {
                link = href + "&actionLink=" + encodeURIComponent(linkParam);
            }
            return link;
        };

        var onRequestOpen = function(event) { // user open external data source
            innerAlert("onRequestOpen");
            var windowName = event.data.windowName;

            requestReference(event.data, function(data) {
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

        var onRequestRefreshFile = function(event) {
            let xhr = new XMLHttpRequest();
            xhr.open("GET", "files/config?fileName=" + encodeURIComponent(config.document.title) +
                "&directUrl=" + !!config.document.directUrl +
                "&permissions=" + encodeURIComponent(JSON.stringify(config.document.permissions)));
            xhr.send();
            xhr.onload = function () {
                innerAlert(xhr.responseText);
                docEditor.refreshFile(JSON.parse(xhr.responseText));
            };
        };

        var onRequestReferenceData = function(event) { // user refresh external data source
            innerAlert("onRequestReferenceData");

            requestReference(event.data, function(data) {
                docEditor.setReferenceData(data);
            });
        };

        var requestReference = function(data, callback) {
            innerAlert(data);

            data.directUrl = !!config.document.directUrl;
            let xhr = new XMLHttpRequest();
            xhr.open("POST", "/api/files/reference");
            xhr.setRequestHeader('X-CSRF-TOKEN', "{{ csrf_token() }}")
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.send(JSON.stringify(data));
            xhr.onload = function() {
                innerAlert(xhr.responseText);
                callback(JSON.parse(xhr.responseText));
            }
        };

        var onRequestReferenceSource = function (event) {
          innerAlert("onRequestReferenceSource");
          let xhr = new XMLHttpRequest();
          xhr.open("GET", "/files");
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
                xhr.open("POST", "/api/files/reference");
                xhr.setRequestHeader("Content-Type", "application/json");
                xhr.setRequestHeader('X-CSRF-TOKEN', "{{ csrf_token() }}")
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

        // the user is trying to get link for opening the document which contains a bookmark,
        // scrolling to the bookmark position
        var onMakeActionLink = function(event) {
            var actionData = event.data;
            var linkParam = JSON.stringify(actionData);
            // set the link to the document which contains a bookmark
            docEditor.setActionLink(replaceActionLink(location.href, linkParam));
        };

        var onRequestClose = function () {  // close editor
            docEditor.destroyEditor();
            innerAlert("Document editor closed successfully");
        };

        var onUserActionRequired = function () {
            console.log("User action required");
        };

        // the meta information of the document is changed via the meta command
        var onMetaChange = function(event) {
            if (event.data.title !== undefined) {
                document.title = event.data.title + " - ONLYOFFICE";
            }

            if (event.data.favorite !== undefined) {
                var favorite = !!event.data.favorite;
                var title = document.title.replace(/^\☆/g, "");
                document.title = (favorite ? "☆" : "") + title;
                docEditor.setFavorite(favorite); // change the Favorite icon state
            }

            innerAlert("onMetaChange: " + JSON.stringify(event.data));
        };

        // the user is trying to insert an image by clicking the Image from Storage button
        var onRequestInsertImage = function(event) {
            docEditor.insertImage({ // insert an image into the file
                "c": event.data.c,
                {!! $dataInsertImage !!},
            })
        };

        // the user is trying to select document for comparing by clicking the Document from Storage button
        var onRequestSelectDocument = function(event) {
            var data = {!! $dataDocument !!};
            data.c = event.data.c;
            docEditor.setRequestedDocument(data); // select a document for comparing
        };

        // the user is trying to select recipients data by clicking the Mail merge button
        var onRequestSelectSpreadsheet = function(event) {
            // insert recipient data for mail merge into the file
            var data = {!! $dataSpreadsheet !!};
            data.c = event.data.c;
            docEditor.setRequestedSpreadsheet(data);
        };

        var onRequestSaveAs = function(event) { //  the user is trying to save file by clicking Save Copy as... button
            var title = event.data.title;
            var url = event.data.url;
            var data = {
                title: title,
                url: url
            };
            let xhr = new XMLHttpRequest();
            xhr.open("POST", "/files/saveas");
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.send(JSON.stringify(data));
            xhr.onload = function() {
                innerAlert(xhr.responseText);
                innerAlert(JSON.parse(xhr.responseText.substring(xhr.responseText.indexOf("{"))).file, true);
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
            xhr.open("POST", "/files/rename");
            xhr.setRequestHeader('X-CSRF-TOKEN', "{{ csrf_token() }}")
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.send(JSON.stringify(data));
            xhr.onload = function() {
                innerAlert(xhr.responseText);
            }
        };

        function onRequestHistory() {
            const query = new URLSearchParams(window.location.search);
            const filename = encodeURIComponent(query.get('fileID'));

            const req = new XMLHttpRequest();
            req.open("GET", `/files/history?filename=${filename}`);
            req.setRequestHeader('Content-Type', 'application/json');
            req.send();
            req.onload = function() {
                if (req.status != 200) {
                    response = JSON.parse(req.response)
                    innerAlert(response.error)
                    return
                }
                history = JSON.parse(req.response);
                docEditor.refreshHistory({
                    currentVersion: history.currentVersion,
                    history: history.history
                })
            }
        }

        function onRequestHistoryData(event) {
            var ver = event.data;
            var histData = history.history;
            docEditor.setHistoryData(histData[ver - 1]) 
        }

        function onRequestHistoryClose() {
            document.location.reload()
        }

        function onRequestRestore(event) {
            const query = new URLSearchParams(window.location.search)
            const config = {!! $config !!}
            const data = {
                filename: query.get('fileID'),
                fileType: event.data.fileType,
                version: event.data.version,
                url: event.data.url,
                userId: query.get('user') || config.editorConfig.user.id
            }
            const request = new XMLHttpRequest()
            request.open("PUT", '/api/files/versions/restore')
            request.setRequestHeader('X-CSRF-TOKEN', "{{ csrf_token() }}")
            request.setRequestHeader('Content-Type', 'application/json');
            request.setRequestHeader('Accept', 'application/json');
            request.send(JSON.stringify(data));
            request.onload = function() {
                if (request.status != 200) {
                    response = JSON.parse(request.response)
                    innerAlert(response.error)
                    return
                }
                onRequestHistory()
            }
        }

        // add mentions for not anonymous users
        var onRequestUsers = function(event) {
            if (event && event.data) {
                var c = event.data.c;
            }

            switch (c) {
                case "info":
                    users = [];
                    var allUsers = {!! $usersInfo !!};
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
                    var users = {!! $usersForProtect !!};
                    break;
                default:
                    users = {!! $usersForMentions !!};
            }

            if ((c === "protect" || c === "mention") && users && event.data.count) {
                let from = event.data.from;
                let count = event.data.count;
                let search = event.data.search;
                if (from != 0) users = [];
                var resultCount = 234;
                for (var i = Math.max(users.length, from); i < Math.min(from + count, resultCount); i++){
                    users.push({
                        email: "test@test.test" + (i + 1),
                        id: "id" + (i + 1),
                        name: "test_" + search + (i + 1)
                    });
                }
            }

            var result = {
                "c": c,
                "users": users,
            };
            if (resultCount) {
                // support v9.0
                result.total = 1 + (!event.data.count || users.length < event.data.count ? 0 : (event.data.from + event.data.count));
                // since v9.0.1
                result.isPaginated = true;
            }

            docEditor.setUsers(result);
        };

        var onRequestSendNotify = function(event) {
            event.data.actionLink = replaceActionLink(location.href, JSON.stringify(event.data.actionLink));
            var data = JSON.stringify(event.data);
            innerAlert("onRequestSendNotify: " + data);
        };

        var onRequestStartFilling = function(event) {
            var data = event.data;
            var submit = confirm("Start filling?\n" + JSON.stringify(data));
            if (submit) {
                docEditor.startFilling(true);
            }
        };

        var onStartFilling = function(event) {
            innerAlert("The form is ready to fill out.");
        };

        var connectEditor = function() {
            config = {!! $config !!}

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
                'onRequestSelectSpreadsheet': onRequestSelectSpreadsheet,
                'onRequestReferenceData': onRequestReferenceData,
                "onRequestOpen": onRequestOpen,
            };

            {!! $history !!}

            docEditor = new DocsAPI.DocEditor("iframeEditor", config);
        };

        const getFileExt = function (fileName) {
          if (fileName.indexOf(".")) {
            return fileName.split('.').reverse()[0];
          }
          return false;
        };

        if (window.addEventListener) {
            window.addEventListener("load", connectEditor);
        } else if (window.attachEvent) {
            window.attachEvent("load", connectEditor);
        }
    </script>
</head>

<body>
    <form id="form1">
        <div id="iframeEditor">
        </div>
    </form>
</body>

</html>