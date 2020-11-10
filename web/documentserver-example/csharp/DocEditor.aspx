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
    <link rel="icon" href="~/favicon.ico" type="image/x-icon" />
    <title>ONLYOFFICE</title>
    <!--
    *
    * (c) Copyright Ascensio System SIA 2020
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
            location.href = location.href.replace(RegExp("editorsMode=view\&?", "i"), "");
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

        var config = <%= DocConfig %>;

        config.width = "100%";
        config.height = "100%";

        config.events = {
            'onAppReady': onAppReady,
            'onDocumentStateChange': onDocumentStateChange,
            'onRequestEditRights': onRequestEditRights,
            'onError': onError,
            'onOutdatedVersion': onOutdatedVersion,
            'onMakeActionLink': onMakeActionLink,
        };

        <% if (!string.IsNullOrEmpty(History) && !string.IsNullOrEmpty(HistoryData))
        { %>
        config.events['onRequestHistory'] = function () {
            docEditor.refreshHistory(<%= History %>);
        };
        config.events['onRequestHistoryData'] = function (event) {
            var ver = event.data;
            var histData = <%= HistoryData %>;
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
</head>
<body>
    <form id="form1" runat="server">
        <div id="iframeEditor">
        </div>
    </form>
</body>
</html>
