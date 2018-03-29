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

        var onOutdatedVersion = function (event) {
            location.reload(true);
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
                        'onOutdatedVersion': onOutdatedVersion,
                    }
                });
        };

        if (window.addEventListener) {
            window.addEventListener("load", сonnectEditor);
        } else if (window.attachEvent) {
            window.attachEvent("load", сonnectEditor);
        }

    </script>
</body>
</html>
