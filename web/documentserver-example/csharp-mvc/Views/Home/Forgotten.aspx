<%@ Page Title="ONLYOFFICE" Language="C#" Inherits="System.Web.Mvc.ViewPage<OnlineEditorsExampleMVC.Models.ForgottenFilesModel>" %>

<%@ Import Namespace="System.IO" %>
<%@ Import Namespace="System.Web.Configuration" %>
<%@ Import Namespace="OnlineEditorsExampleMVC.Helpers" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html lang="en">
<head runat="server">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta name="viewport" content="width=device-width" />
    <meta name="server-version" content=<%= DocManagerHelper.GetVersion() %> />
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
    <title>ONLYOFFICE</title>

    <link href="<%: Url.Content("~/favicon.ico") %>" rel="shortcut icon" type="image/x-icon" />

    <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:900,800,700,600,500,400,300&subset=latin,cyrillic-ext,cyrillic,latin-ext" />

    <%: Styles.Render("~/Content/css") %>
    <%: Styles.Render("~/Content/forgotten") %>
</head>
<body>
    <header>
        <div class="center main-nav">
            <a href="./">
                <img src ="content/images/logo.svg" alt="ONLYOFFICE" />
            </a>
        </div>
        <menu class="responsive-nav">
            <li>
              <a href="#" onclick="toggleSidePanel(event)">
                <img src="content/images/mobile-menu.svg" alt="ONLYOFFICE" />
              </a>
            </li>
            <li>
              <a href="./">
                <img src ="content/images/mobile-logo.svg" alt="ONLYOFFICE" />
              </a>
            </li>
        </menu>
    </header>

    <div class="center main">
        <table class="table-main">
            <tbody>
                <tr>
                    <td class="left-panel section"></td>
                    <td class="section">
                        <div class="main-panel">
                            <menu class="links">
                                <li class="home-link" >
                                  <a href="./">
                                    <img src="content/images/home.svg" alt="Home"/>
                                  </a>
                                </li>
                                <li class="active">
                                  <a href="/Forgotten">Forgotten files</a>
                                </li>
                            </menu>
                            <div class="stored-list">
                                <div class="storedHeader">
                                    <div class="storedHeaderText">
                                        <span class="header-list">Forgotten files</span>
                                    </div>
                                </div>
                                <table class="tableHeader" cellspacing="0" cellpadding="0" width="100%">
                                    <thead>
                                        <tr>
                                            <td class="tableHeaderCell">Filename</td>
                                            <td class="tableHeaderCell">Action</td>
                                        </tr>
                                    </thead>
                                </table>
                                <div class="scroll-table-body">
                                    <table cellspacing="0" cellpadding="0" width="100%">
                                        <tbody>
                                            <%  foreach (var file in Model.files) { %>
                                                <tr class="tableRow" title="<%= file["key"] %>">
                                                    <td>
                                                        <a class="stored-edit action-link <%= file["type"] %>" href="<%= file["url"] %>" target="_blank">
                                                            <span><%= file["key"] %></span>
                                                        </a>
                                                    </td>
                                                    <td>
                                                        <a href="<%= file["url"] %>">
                                                            <img class="icon-download" src="content/images/download.svg" alt="Download" title="Download" /></a>
                                                        <a class="delete-file" data="<%= file["key"] %>">
                                                            <img class="icon-action" src="content/images/delete.svg" alt="Delete" title="Delete" /></a>
                                                    </td>
                                                </tr>
                                            <%  } %>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <footer>
        <div class="center">
            <table>
                <tbody>
                    <tr>
                        <td>
                            <a href="https://api.onlyoffice.com/docs/docs-api/get-started/how-it-works/" target="_blank">API Documentation</a>
                        </td>
                        <td>
                            <a href="mailto:sales@onlyoffice.com">Submit your request</a>
                        </td>
                        <td class="copy">
                            &copy; Ascensio System SIA <%= DateTime.Now.Year.ToString() %>. All rights reserved.
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </footer>

    <%: Scripts.Render("~/bundles/jquery", "~/bundles/forgotten") %>
</body>
</html>
