<!--*
 *
 * (c) Copyright Ascensio System Limited 2010-2017
 *
 * This program is freeware. You can redistribute it and/or modify it under the terms of the GNU 
 * General Public License (GPL) version 3 as published by the Free Software Foundation (https://www.gnu.org/copyleft/gpl.html). 
 * In accordance with Section 7(a) of the GNU GPL its Section 15 shall be amended to the effect that 
 * Ascensio System SIA expressly excludes the warranty of non-infringement of any third-party rights.
 *
 * THIS PROGRAM IS DISTRIBUTED WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. For more details, see GNU GPL at https://www.gnu.org/copyleft/gpl.html
 *
 * You can contact Ascensio System SIA by email at sales@onlyoffice.com
 *
 * The interactive user interfaces in modified source and object code versions of ONLYOFFICE must display 
 * Appropriate Legal Notices, as required under Section 5 of the GNU GPL version 3.
 *
 * Pursuant to Section 7 § 3(b) of the GNU GPL you must retain the original ONLYOFFICE logo which contains 
 * relevant author attributions when distributing the software. If the display of the logo in its graphic 
 * form is not reasonably feasible for technical reasons, you must include the words "Powered by ONLYOFFICE" 
 * in every copy of the program you distribute. 
 * Pursuant to Section 7 § 3(e) we decline to grant you any rights under trademark law for use of our trademarks.
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

        function getXmlHttp() {
            var xmlhttp;
            try {
                xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
            } catch (e) {
                try {
                    xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
                } catch (ex) {
                    xmlhttp = false;
                }
            }
            if (!xmlhttp && typeof XMLHttpRequest !== "undefined") {
                xmlhttp = new XMLHttpRequest();
            }
            return xmlhttp;
        }

    </script>
        
    </head>
    <body>
        <div class="form">
            <div id="iframeEditor"></div>
        </div>
    </body>
</html>
