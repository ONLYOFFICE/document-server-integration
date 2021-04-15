<%@page import="helpers.DocumentManager"%>
<%@page import="helpers.FileUtility"%>
<%@page import="helpers.ConfigManager"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <!--
        *
        * (c) Copyright Ascensio System SIA 2021
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
    </head>
    <body>
        <header>
            <div class="center">
                <a href="">
                    <img src ="css/img/logo.svg" alt="ONLYOFFICE" />
                </a>
            </div>
        </header>
        <div class="center main">
            <table class="table-main">
                <tbody>
                    <tr>
                        <td class="left-panel section">
                            <div class="help-block">
                                <span>Create new</span>
                                <div class="clearFix">
                                    <div class="create-panel clearFix">
                                        <ul class="try-editor-list clearFix">
                                            <li>
                                                <a class="try-editor word" data-type="docx">Document</a>
                                            </li>
                                            <li>
                                                <a class="try-editor cell" data-type="xlsx">Spreadsheet</a>
                                            </li>
                                            <li>
                                                <a class="try-editor slide" data-type="pptx">Presentation</a>
                                            </li>
                                        </ul>
                                        <label class="create-sample">
                                            <input id="createSample" class="checkbox" type="checkbox" />With sample content
                                        </label>
                                    </div>

                                    <div class="upload-panel clearFix">
                                        <a class="file-upload">Upload file
                                            <input type="file" id="fileupload" name="file" data-url="IndexServlet?type=upload" />
                                        </a>
                                    </div>

                                    <table class="user-block-table" cellspacing="0" cellpadding="0">
                                        <tbody>
                                            <tr>
                                                <td valign="middle">
                                                    <span class="select-user">Username</span>
                                                    <img class="info" data-id="user" data-tooltip="You can open the same document using different users in different Web browser sessions, so you can check out multi-user editing functions" src="css/img/info.svg" />
                                                    <select class="select-user" id="user">
                                                        <option value="uid-1">John Smith</option>
                                                        <option value="uid-2">Mark Pottato</option>
                                                        <option value="uid-3">Hamish Mitchell</option>
                                                        <option value="uid-0">anonymous</option>
                                                    </select>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td valign="middle">
                                                    <span class="select-user">Language</span>
                                                    <img class="info" data-id="language" data-tooltip="Choose the language for ONLYOFFICE editors interface" src="css/img/info.svg" />
                                                    <select class="select-user" id="language">
                                                        <option value="en">English</option>
                                                        <option value="be">Belarusian</option>
                                                        <option value="bg">Bulgarian</option>
                                                        <option value="ca">Catalan</option>
                                                        <option value="zh">Chinese</option>
                                                        <option value="cs">Czech</option>
                                                        <option value="da">Danish</option>
                                                        <option value="nl">Dutch</option>
                                                        <option value="fi">Finnish</option>
                                                        <option value="fr">French</option>
                                                        <option value="de">German</option>
                                                        <option value="el">Greek</option>
                                                        <option value="hu">Hungarian</option>
                                                        <option value="id">Indonesian</option>
                                                        <option value="it">Italian</option>
                                                        <option value="ja">Japanese</option>
                                                        <option value="ko">Korean</option>
                                                        <option value="lv">Latvian</option>
                                                        <option value="lo">Lao</option>
                                                        <option value="nb">Norwegian</option>
                                                        <option value="pl">Polish</option>
                                                        <option value="pt">Portuguese</option>
                                                        <option value="ro">Romanian</option>
                                                        <option value="ru">Russian</option>
                                                        <option value="sk">Slovak</option>
                                                        <option value="sl">Slovenian</option>
                                                        <option value="sv">Swedish</option>
                                                        <option value="es">Spanish</option>
                                                        <option value="tr">Turkish</option>
                                                        <option value="uk">Ukrainian</option>
                                                        <option value="vi">Vietnamese</option>
                                                    </select>
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </td>
                        <td class="section">
                            <div class="main-panel">
                                <% DocumentManager.Init(request, response); %>
                                <% File[] files = DocumentManager.GetStoredFiles(null); %>
                                <% if (files.length <= 0) { %>
                                    <span class="portal-name">ONLYOFFICE Document Editors – Welcome!</span>
                                    <span class="portal-descr">
                                        Get started with a demo-sample of ONLYOFFICE Document Editors, the first html5-based editors.
                                        <br /> You may upload your own documents for testing using the "<b>Upload file</b>" button and <b>selecting</b> the necessary files on your PC.
                                    </span>
                                <% } else { %>
                                    <div class="stored-list">
                                       <span class="header-list">Your documents</span>
                                       <table class="tableHeader" cellspacing="0" cellpadding="0" width="100%">
                                           <thead>
                                               <tr>
                                                   <td class="tableHeaderCell tableHeaderCellFileName">Filename</td>
                                                   <td class="tableHeaderCell tableHeaderCellEditors contentCells-shift">Editors</td>
                                                   <td class="tableHeaderCell tableHeaderCellViewers">Viewers</td>
                                                   <td class="tableHeaderCell tableHeaderCellDownload">Download</td>
                                                   <td class="tableHeaderCell tableHeaderCellRemove">Remove</td>
                                               </tr>
                                           </thead>
                                       </table>
                                        <div class="scroll-table-body">
                                            <table cellspacing="0" cellpadding="0" width="100%">
                                                <tbody>
                                                    <% for (Integer i = 0; i < files.length; i++) { %>
                                                        <% String docType = FileUtility.GetFileType(files[i].getName()).toString().toLowerCase(); %>
                                                        <tr class="tableRow" title="<%= files[i].getName() %>">
                                                            <td class="contentCells">
                                                                <a class="stored-edit <%= docType %>" href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>" target="_blank">
                                                                    <span title="<%= files[i].getName() %>"><%= files[i].getName() %></span>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=desktop&mode=edit" target="_blank">
                                                                    <img src="css/img/desktop-24.png" alt="Open in editor for full size screens" title="Open in editor for full size screens"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=mobile&mode=edit" target="_blank">
                                                                    <img src="css/img/mobile-24.png" alt="Open in editor for mobile devices" title="Open in editor for mobile devices"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <% if (docType.equals("word")) { %>
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=desktop&mode=review" target="_blank">
                                                                        <img src="css/img/review-24.png" alt="Open in editor for review" title="Open in editor for review"/>
                                                                    </a>
                                                                <% } else if (docType.equals("cell")) { %>
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=desktop&mode=filter" target="_blank">
                                                                        <img src="css/img/filter-24.png" alt="Open in editor without access to change the filter" title="Open in editor without access to change the filter" />
                                                                    </a>
                                                                <% } %>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=desktop&mode=comment" target="_blank">
                                                                    <img src="css/img/comment-24.png" alt="Open in editor for comment" title="Open in editor for comment"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <% if (docType.equals("word")) { %>
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=desktop&mode=fillForms" target="_blank">
                                                                        <img src="css/img/fill-forms-24.png" alt="Open in editor for filling in forms" title="Open in editor for filling in forms"/>
                                                                    </a>
                                                                <% } %>
                                                            </td>
                                                            <td class="contentCells contentCells-shift contentCells-icon">
                                                                <% if (docType.equals("word")) { %>
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=desktop&mode=blockcontent" target="_blank">
                                                                        <img src="css/img/block-content-24.png" alt="Open in editor without content control modification" title="Open in editor without content control modification"/>
                                                                    </a>
                                                                <% } %>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=desktop&mode=view" target="_blank">
                                                                    <img src="css/img/desktop-24.png" alt="Open in viewer for full size screens" title="Open in viewer for full size screens"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=mobile&mode=view" target="_blank">
                                                                    <img src="css/img/mobile-24.png" alt="Open in viewer for mobile devices" title="Open in viewer for mobile devices"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift">
                                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=embedded&mode=embedded" target="_blank">
                                                                    <img src="css/img/embeded-24.png" alt="Open in embedded mode" title="Open in embedded mode"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift">
                                                                <a href="IndexServlet?type=download&name=<%=URLEncoder.encode(files[i].getName(), "UTF-8")%>">
                                                                    <img class="icon-download" src="css/img/download-24.png" alt="Download" title="Download" />
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift">
                                                                <a class="delete-file" data-filename="<%= files[i].getName() %>">
                                                                    <img class="icon-delete" src="css/img/delete-24.png" alt="Delete" title="Delete" />
                                                                </a>
                                                            </td>
                                                        </tr>
                                                    <% } %>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                <% } %>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div id="mainProgress">
            <div id="uploadSteps">
                <span id="uploadFileName" class="uploadFileName"></span>
                <div class="describeUpload">After these steps are completed, you can work with your document.</div>
                <span id="step1" class="step">1. Loading the file.</span>
                <span class="step-descr">The loading speed depends on file size and additional elements it contains.</span>
                <br />
                <span id="step2" class="step">2. Conversion.</span>
                <span class="step-descr">The file is converted to OOXML so that you can edit it.</span>
                <br />
                <div id="blockPassword">
                    <span class="descrFilePass">The file is password protected.</span>
                    <br />
                    <div>
                        <input id="filePass" type="password"/>
                        <div id="enterPass" class="button orange">Enter</div>
                        <div id="skipPass" class="button gray">Skip</div>
                    </div>
                    <span class="errorPass"></span>
                    <br />
                </div>
                <span id="step3" class="step">3. Loading editor scripts.</span>
                <span class="step-descr">They are loaded only once, they will be cached on your computer.</span>
                <input type="hidden" name="hiddenFileName" id="hiddenFileName" />
                <br />
                <br />
                <span class="progress-descr">Note the speed of all operations depends on your connection quality and server location.</span>
                <br />
                <br />
                <div class="error-message">
                    <b>Upload error: </b><span></span>
                    <br />
                    Please select another file and try again.
                </div>
            </div>
            <iframe id="embeddedView" src="" height="345px" width="432px" frameborder="0" scrolling="no" allowtransparency></iframe>
            <br />
            <div id="beginEdit" class="button orange disable">Edit</div>
            <div id="beginView" class="button gray disable">View</div>
            <div id="beginEmbedded" class="button gray disable">Embedded view</div>
            <div id="cancelEdit" class="button gray">Cancel</div>
        </div>

        <span id="loadScripts" data-docs="<%= ConfigManager.GetProperty("files.docservice.url.site") + ConfigManager.GetProperty("files.docservice.url.preloader") %>"></span>

        <footer>
            <div class="center">
                <table>
                    <tbody>
                        <tr>
                            <td>
                                <a href="http://api.onlyoffice.com/editors/howitworks" target="_blank">API Documentation</a>
                            </td>
                            <td>
                                <a href="mailto:sales@onlyoffice.com">Submit your request</a>
                            </td>
                            <td class="copy">
                                &copy; Ascensio Systems SIA 2020. All rights reserved.
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </footer>

        <script type="text/javascript" src="scripts/jquery-1.8.2.js"></script>
        <script type="text/javascript" src="scripts/jquery-ui.js"></script>
        <script type="text/javascript" src="scripts/jquery.blockUI.js"></script>
        <script type="text/javascript" src="scripts/jquery.iframe-transport.js"></script>
        <script type="text/javascript" src="scripts/jquery.fileupload.js"></script>
        <script type="text/javascript" src="scripts/jquery.dropdownToggle.js"></script>
        <script type="text/javascript" src="scripts/jscript.js"></script>

        <script language="javascript" type="text/javascript">
            var ConverExtList = "<%= String.join(",", DocumentManager.GetConvertExts()) %>";
            var EditedExtList = "<%= String.join(",", DocumentManager.GetEditedExts()) %>";
            var UrlConverter = "IndexServlet?type=convert";
            var UrlEditor = "EditorServlet";
        </script>

    </body>
</html>
