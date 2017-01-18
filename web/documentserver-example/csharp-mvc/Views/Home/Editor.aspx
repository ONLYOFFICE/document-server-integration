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

<%@ Page Title="ONLYOFFICE" Language="C#" Inherits="System.Web.Mvc.ViewPage<OnlineEditorsExampleMVC.Models.FileModel>" %>
<%@ Import Namespace="System.IO" %>
<%@ Import Namespace="System.Web.Configuration" %>
<%@ Import Namespace="OnlineEditorsExampleMVC.Helpers" %>

<!DOCTYPE html>

<html>
<head runat="server">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta name="viewport" content="width=device-width" />
    <link rel="icon" href="~/favicon.ico" type="image/x-icon" />
    <title><%= Model.FileName + " - ONLYOFFICE" %></title>
    
    <%: Styles.Render("~/Content/editor") %>

</head>
<body>
    <div class="form">
        <div id="iframeEditor">
        </div>
    </div>
    
    <%: Scripts.Render(new []{ WebConfigurationManager.AppSettings["files.docservice.url.api"] }) %>

    <script type="text/javascript" language="javascript">

        var docEditor;
        var fileName = "<%= Model.FileName %>";
        var fileType = "<%= Path.GetExtension(Model.FileName).Trim('.') %>";

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

        var сonnectEditor = function () {

            docEditor = new DocsAPI.DocEditor("iframeEditor",
                {
                    width: "100%",
                    height: "100%",

                    type: '<%= Request["mode"] != "embedded" ? "desktop" : "embedded" %>',
                    documentType: "<%= Model.DocumentType %>",
                    document: {
                        title: fileName,
                        url: "<%= Model.FileUri %>",
                        fileType: fileType,
                        key: "<%= Model.Key %>",

                        info: {
                            author: "Me",
                            created: "<%= DateTime.Now.ToShortDateString() %>",
                        },

                        permissions: {
                            edit: "<%= DocManagerHelper.EditedExts.Contains(Path.GetExtension(Model.FileName)) %>" == "True",
                            download: true,
                        }
                    },
                    editorConfig: {
                        mode: '<%= DocManagerHelper.EditedExts.Contains(Path.GetExtension(Model.FileName)) && Request["mode"] != "view" ? "edit" : "view" %>',

                        lang: "en",

                        callbackUrl: "<%= Model.CallbackUrl %>",

                        user: {
                            id: "<%= DocManagerHelper.CurUserHostAddress() %>",
                            name: "John Smith",
                        },

                        embedded: {
                            saveUrl: "<%= Model.FileUri %>",
                            embedUrl: "<%= Model.FileUri %>",
                            shareUrl: "<%= Model.FileUri %>",
                            toolbarDocked: "top",
                        },

                        customization: {
                            about: true,
                            feedback: true,
                            goback: {
                                url: "<%= Url.Action("Index", "Home") %>",
                            },
                        },
                    },
                    events: {
                        'onReady': onReady,
                        'onDocumentStateChange': onDocumentStateChange,
                        'onRequestEditRights': onRequestEditRights,
                        'onError': onError,
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
                if (!xmlhttp && typeof XMLHttpRequest != 'undefined') {
                    xmlhttp = new XMLHttpRequest();
                }
                return xmlhttp;
            }

    </script>
</body>
</html>
