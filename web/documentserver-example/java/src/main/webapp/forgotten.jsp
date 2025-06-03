<%@page import="helpers.ConfigManager"%>
<%@page import="java.util.ArrayList"%>
<%@page import="entities.ForgottenFile"%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<% ArrayList<ForgottenFile> files = (ArrayList) request.getAttribute("files"); %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width" />
        <meta name="server-version" content="<%= ConfigManager.getProperty("version") %>" />
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
        <link rel="icon" href="favicon.ico" type="image/x-icon" />
        <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:900,800,700,600,500,400,300&subset=latin,cyrillic-ext,cyrillic,latin-ext" />
        <link rel="stylesheet" type="text/css" href="css/stylesheet.css" />
        <link rel="stylesheet" type="text/css" href="css/jquery-ui.css" />
        <link rel="stylesheet" type="text/css" href="css/media.css">
        <link rel="stylesheet" type="text/css" href="css/forgotten.css">
    </head>
    <body>
        <header>
            <div class="center main-nav">
                <a href="./">
                    <img src ="css/img/logo.svg" alt="ONLYOFFICE" />
                </a>
            </div>
            <menu class="responsive-nav">
                <li>
                  <a href="#">
                    <img src="css/img/mobile-menu.svg" alt="ONLYOFFICE" />
                  </a>
                </li>
                <li>
                  <a href="./">
                    <img src ="css/img/mobile-logo.svg" alt="ONLYOFFICE" />
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
                                        <img src="css/img/home.svg" alt="Home"/>
                                      </a>
                                    </li>
                                    <li class="active">
                                      <a href="/ForgottenServlet">Forgotten files</a>
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
                                                <% for (Integer i = 0; i < files.size(); i++) { %>
                                                    <tr class="tableRow" title='<%= (files.get(i)).getKey() %>'>
                                                        <td>
                                                            <a class="stored-edit action-link <%= (files.get(i)).getType() %>" href='<%= (files.get(i)).getUrl() %>' target="_blank">
                                                                <span><%= (files.get(i)).getKey() %></span>
                                                            </a>
                                                        </td>
                                                        <td>
                                                            <a href='<%= (files.get(i)).getUrl() %>'>
                                                                <img class="icon-download" src="css/img/download.svg" alt="Download" title="Download" /></a>
                                                            <a class="delete-file" data='<%= (files.get(i)).getKey() %>'>
                                                                <img class="icon-action" src="css/img/delete.svg" alt="Delete" title="Delete" /></a>
                                                        </td>
                                                    </tr>
                                                <% } %>
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
                                &copy; Ascensio Systems SIA 2025. All rights reserved.
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </footer>

        <script type="text/javascript" src="scripts/forgotten.js"></script>
    </body>
</html>
