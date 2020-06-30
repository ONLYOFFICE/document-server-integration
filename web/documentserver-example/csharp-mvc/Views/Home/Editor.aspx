<!--*
 *
 * (c) Copyright Ascensio System SIA 2020
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
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=no, minimal-ui" />
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="mobile-web-app-capable" content="yes" />
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

        var innerAlert = function (message) {
            if (console && console.log)
                console.log(message);
        };

        var onAppReady = function () {
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

        var onMakeActionLink = function (event) {
            var actionData = event.data;
            var linkParam = JSON.stringify(actionData);
            docEditor.setActionLink(replaceActionLink(location.href, linkParam));
        };

        var config = <%= Model.GetDocConfig(Request, Url) %>;

        config.width = "100%";
        config.height = "100%";

        config.events = {
            'onAppReady': onAppReady,
            'onDocumentStateChange': onDocumentStateChange,
            'onRequestEditRights': onRequestEditRights,
            'onError': onError,
            'onOutdatedVersion': onOutdatedVersion,
            "onMakeActionLink": onMakeActionLink,
        };

        <% string hist, histData; %>
        <% Model.GetHistory(out hist, out histData); %>
        <% if (!string.IsNullOrEmpty(hist) && !string.IsNullOrEmpty(histData))
        { %>
        config.events['onRequestHistory'] = function () {
            docEditor.refreshHistory(<%= hist %>);
        };
        config.events['onRequestHistoryData'] = function (event) {
            var ver = event.data;
            var histData = <%= histData %>;
            docEditor.setHistoryData(histData[ver]);
        };
        config.events['onRequestHistoryClose '] = function () {
            document.location.reload();
        };
        <% } %>

        var сonnectEditor = function () {
            docEditor = new DocsAPI.DocEditor("iframeEditor", config);
        };

        if (window.addEventListener) {
            window.addEventListener("load", сonnectEditor);
        } else if (window.attachEvent) {
            window.attachEvent("load", сonnectEditor);
        }

    </script>
</body>
</html>
