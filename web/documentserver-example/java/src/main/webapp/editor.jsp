<!--*
 *
 * (c) Copyright Ascensio System Limited 2010-2018
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
*-->

<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Arrays"%>
<%@page import="entities.FileModel"%>
<%@page import="helpers.DocumentManager"%>
<%@page import="helpers.FileUtility"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>ONLYOFFICE</title>
        <link rel="icon" href="favicon.ico" type="image/x-icon" />
        <link rel="stylesheet" type="text/css" href="css/editor.css" />

        <% DocumentManager.Init(request, response); %>
        <% FileModel Model = (FileModel)request.getAttribute("file"); %>

        <script type="text/javascript" src="${docserviceApiUrl}"></script>
        
        <script type="text/javascript" language="javascript">

        var docEditor;
        var fileName = "<%= Model.GetFileName() %>";
        var fileType = "<%= FileUtility.GetFileExtension(Model.GetFileName()).replace(".", "") %>";

        var innerAlert = function (message) {
            if (console && console.log)
                console.log(message);
        };

        var onReady = function () {
            innerAlert("Document editor ready");
        };

        var onDocumentStateChange = function (event) {
            var title = document.title.replace(/\*$/g, "");
            document.title = title + (event.data ? "*" : "");
        };

        var onRequestEditRights = function () {
		    location.href = location.href.replace(RegExp("action=view\&?", "i"), "");
        };

        var onError = function (event) {
            if (event)
                innerAlert(event.data);
        };

        var onOutdatedVersion = function (event) {
            location.reload(true);
        };

        var сonnectEditor = function () {

            docEditor = new DocsAPI.DocEditor("iframeEditor",
                {
                    width: "100%",
                    height: "100%",
                    type: "${type}",
                    documentType: "<%= Model.GetDocumentType() %>",
                    
                    document: {
                        title: fileName,
                        url: "<%= Model.GetFileUri() %>",
                        fileType: fileType,
                        key: "<%= Model.GetKey() %>",

                        info: {
                            author: "Me",
                            created: "<%= new SimpleDateFormat("MM/dd/yyyy").format(new Date()) %>",
                        },

                        permissions: {
                            edit: <%= Boolean.toString(DocumentManager.GetEditedExts().contains(FileUtility.GetFileExtension(Model.GetFileName()))).toLowerCase() %>,
                            download: true,
                        }
                    },
                    editorConfig: {
                        mode: "<%= DocumentManager.GetEditedExts().contains(FileUtility.GetFileExtension(Model.GetFileName())) && !"view".equals(request.getAttribute("mode")) ? "edit" : "view" %>",

                        lang: "en",

                        callbackUrl: "<%= Model.GetCallbackUrl() %>",

                        user: {
                            id: "<%= Model.CurUserHostAddress() %>",
                            name: "John Smith",
                        },

                        embedded: {
                            saveUrl: "<%= Model.GetFileUri() %>",
                            embedUrl: "<%= Model.GetFileUri() %>",
                            shareUrl: "<%= Model.GetFileUri() %>",
                            toolbarDocked: "top",
                        },

                        customization: {
                            about: true,
                            feedback: true,
                            goback: {
                                url: "<%= Model.GetServerUrl() %>/IndexServlet",
                            },
                        },
                    },
                    events: {
                        "onReady": onReady,
                        "onDocumentStateChange": onDocumentStateChange,
                        'onRequestEditRights': onRequestEditRights,
                        "onError": onError,
                        "onOutdatedVersion": onOutdatedVersion,
                    }
                });
        };

        if (window.addEventListener) {
            window.addEventListener("load", сonnectEditor);
        } else if (window.attachEvent) {
            window.attachEvent("load", сonnectEditor);
        }

    </script>
        
    </head>
    <body>
        <div class="form">
            <div id="iframeEditor"></div>
        </div>
    </body>
</html>
