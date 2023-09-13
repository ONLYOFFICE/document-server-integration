<%@page import="helpers.DocumentManager"%>
<%@page import="helpers.FileUtility"%>
<%@page import="helpers.ConfigManager"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="java.util.Map"%>
<%@page import="helpers.Users"%>
<%@page import="entities.User"%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width" />
        <!--
        *
        * (c) Copyright Ascensio System SIA 2023
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
                                            <li>
                                                <a class="try-editor form" data-type="docxf">Form template</a>
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
                                                    <img id="info" class="info" src="css/img/info.svg" />
                                                    <select class="select-user" id="user">
                                                        <% for (User user : Users.getAllUsers()) { %>
                                                            <option value="<%= user.getId() %>"><%= user.getName() == null ? "Anonymous" : user.getName() %></option>
                                                        <% } %>
                                                    </select>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td valign="middle">
                                                    <span class="select-user">Language</span>
                                                    <img class="info info-tooltip" data-id="language"
                                                         data-tooltip="Choose the language for ONLYOFFICE editors interface"
                                                         src="css/img/info.svg" />
                                                    <select class="select-user" id="language">
                                                        <% Map<String, String> languages = DocumentManager.getLanguages(); %>
                                                        <% for (Map.Entry<String, String> language : languages.entrySet()) { %>
                                                            <option value="<%=language.getKey()%>"><%=language.getValue()%></option>
                                                        <% } %>
                                                    </select>
                                                </td>
                                            </tr>
                                            <td valign="middle">
                                                <label class="side-option">
                                                    <input id="directUrl" type="checkbox" class="checkbox" />Try opening on client
                                                    <img id="directUrlInfo" class="info info-tooltip" data-id="directUrlInfo" data-tooltip="Some files can be opened in the user's browser without connecting to the document server." src="css/img/info.svg" />
                                                </label>
                                            </td>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </td>
                        <td class="section">
                            <% DocumentManager.init(request, response); %>
                            <% File[] files = DocumentManager.getStoredFiles(null); %>
                            <div class="main-panel">
                                <div id="portal-info" style="display: <%= files.length > 0 ? "none" : "table-cell" %>">
                                    <span class="portal-name">ONLYOFFICE Document Editors – Welcome!</span>
                                    <span class="portal-descr">
                                        Get started with a demo-sample of ONLYOFFICE Document Editors, the first html5-based editors.
                                        <br /> You may upload your own documents for testing using the "<b>Upload file</b>" button and <b>selecting</b> the necessary files on your PC.
                                    </span>
                                    <span class="portal-descr">Please do NOT use this integration example on your own server without proper code modifications, it is intended for testing purposes only. In case you enabled this test example, disable it before going for production.</span>
                                    <span class="portal-descr">You can open the same document using different users in different Web browser sessions, so you can check out multi-user editing functions.</span>
                                    <% for (User user : Users.getAllUsers()) { %>
                                        <div class="user-descr">
                                            <b><%= user.getName() == null ? "Anonymous" : user.getName() %></b>
                                            <ul>
                                                <% for (String description : user.getDescriptions()) { %>
                                                <li><%= description %></li>
                                                <% } %>
                                            </ul>
                                        </div>
                                    <% } %>
                                </div>
                                <% if (files.length > 0)  { %>
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
                                                    <% for (Integer i = 0; i < files.length; i++) {
                                                        Boolean isFillFormDoc = DocumentManager.getFillExts().contains(FileUtility.getFileExtension(files[i].getName()).toLowerCase());
                                                        String docType = FileUtility.getFileType(files[i].getName()).toString().toLowerCase();
                                                        Boolean canEdit = DocumentManager.getEditedExts().contains(FileUtility.getFileExtension(files[i].getName()));
                                                        String version=" ["+DocumentManager.getFileVersion(DocumentManager.historyDir(DocumentManager.storagePath(files[i].getName(), null)))+"]";
                                                    %>
                                                        <tr class="tableRow" title="<%= files[i].getName() %><%= version %>">
                                                            <td class="contentCells">
                                                                <a class="stored-edit <%= docType %>" href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8").concat(request.getParameter("directUrl") != null ? "&directUrl=".concat(request.getParameter("directUrl")) : "") %>" target="_blank">
                                                                    <span><%= files[i].getName() %></span>
                                                                </a>
                                                            </td>
                                                            <% if (canEdit) { %>
                                                                <td class="contentCells contentCells-icon">
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8").concat(request.getParameter("directUrl") != null ? "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=desktop&mode=edit" target="_blank">
                                                                        <img src="css/img/desktop.svg" alt="Open in editor for full size screens" title="Open in editor for full size screens"/>
                                                                    </a>
                                                                </td>
                                                                <td class="contentCells contentCells-icon">
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8").concat(request.getParameter("directUrl") != null ? "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=mobile&mode=edit" target="_blank">
                                                                        <img src="css/img/mobile.svg" alt="Open in editor for mobile devices" title="Open in editor for mobile devices"/>
                                                                    </a>
                                                                </td>
                                                                <td class="contentCells contentCells-icon">
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8").concat(request.getParameter("directUrl") != null ? "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=desktop&mode=comment" target="_blank">
                                                                        <img src="css/img/comment.svg" alt="Open in editor for comment" title="Open in editor for comment"/>
                                                                    </a>
                                                                </td>
                                                                <% if (docType.equals("word")) { %>
                                                                <td class="contentCells contentCells-icon">
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8").concat(request.getParameter("directUrl") != null ? "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=desktop&mode=review" target="_blank">
                                                                        <img src="css/img/review.svg" alt="Open in editor for review" title="Open in editor for review"/>
                                                                    </a>
                                                                </td>
                                                                <% } else if (docType.equals("cell")) { %>
                                                                <td class="contentCells contentCells-icon">
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8").concat(request.getParameter("directUrl") != null ? "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=desktop&mode=filter" target="_blank">
                                                                        <img src="css/img/filter.svg" alt="Open in editor without access to change the filter" title="Open in editor without access to change the filter"/>
                                                                    </a>
                                                                </td>
                                                                <% } %>
                                                                <% if (docType.equals("word")) { %>
                                                                <td class="contentCells contentCells-icon">
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8")
                                                                    .concat(request.getParameter("directUrl") != null ?
                                                                     "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=desktop&mode=blockcontent" target="_blank">
                                                                        <img src="css/img/block-content.svg" alt="Open in editor without content control modification" title="Open in editor without content control modification"/>
                                                                    </a>
                                                                </td>
                                                                <% } else { %>
                                                                <td class="contentCells contentCells-icon"></td>
                                                                <% } %>
                                                                <% if (!docType.equals("cell") && !docType.equals("word")) { %>
                                                                <td class="contentCells contentCells-icon "></td>
                                                                <% } %>
                                                                <% if (isFillFormDoc) { %>
                                                                <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8")
                                                                    .concat(request.getParameter("directUrl") != null ?
                                                                     "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=desktop&mode=fillForms" target="_blank">
                                                                        <img src="css/img/fill-forms.svg" alt="Open in editor for filling in forms" title="Open in editor for filling in forms"/>
                                                                    </a>
                                                                </td>
                                                                <% } else { %>
                                                                <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift"></td>
                                                                <% }%>
                                                                <% } else if (isFillFormDoc) {%>
                                                                <td class="contentCells contentCells-icon "></td>
                                                                <td class="contentCells contentCells-icon">
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8")
                                                                    .concat(request.getParameter("directUrl") != null ?
                                                                     "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=mobile&mode=fillForms" target="_blank">
                                                                        <img src="css/img/mobile-fill-forms.svg" alt="Open in editor for filling in forms for mobile devices" title="Open in editor for filling in forms for mobile devices" />
                                                                    </a>
                                                                </td>
                                                                <td class="contentCells contentCells-icon "></td>
                                                                <td class="contentCells contentCells-icon "></td>
                                                                <td class="contentCells contentCells-icon "></td>
                                                                <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">
                                                                    <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8")
                                                                    .concat(request.getParameter("directUrl") != null ?
                                                                     "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=desktop&mode=fillForms" target="_blank">
                                                                        <img src="css/img/fill-forms.svg" alt="Open in editor for filling in forms" title="Open in editor for filling in forms"/>
                                                                    </a>
                                                            <% } else { %>
                                                            <td class="contentCells contentCells-shift contentCells-icon contentCellsEmpty" colspan="6"></td>
                                                            <% } %>
                                                            <td class="contentCells contentCells-icon firstContentCellViewers">
                                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8")
                                                                .concat(request.getParameter("directUrl") != null ?
                                                                 "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=desktop&mode=view" target="_blank">
                                                                    <img src="css/img/desktop.svg" alt="Open in viewer for full size screens" title="Open in viewer for full size screens"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8")
                                                                .concat(request.getParameter("directUrl") != null ?
                                                                 "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=mobile&mode=view" target="_blank">
                                                                    <img src="css/img/mobile.svg" alt="Open in viewer for mobile devices" title="Open in viewer for mobile devices"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift">
                                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8")
                                                                .concat(request.getParameter("directUrl") != null ?
                                                                 "&directUrl=".concat(request.getParameter("directUrl")) : "") %>&type=embedded&mode=embedded" target="_blank">
                                                                    <img src="css/img/embeded.svg" alt="Open in embedded mode" title="Open in embedded mode"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift downloadContentCellShift">
                                                                <a href="IndexServlet?type=download&fileName=<%=URLEncoder.encode(files[i].getName(), "UTF-8")%>">
                                                                    <img class="icon-download" src="css/img/download.svg" alt="Download" title="Download" />
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift">
                                                                <a class="delete-file" data-filename="<%= files[i].getName() %>">
                                                                    <img class="icon-delete" src="css/img/delete.svg" alt="Delete" title="Delete" />
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
                <span class="progress-descr">Note the speed of all operations depends on your connection quality and server location.</span>
                <br />
                <div class="error-message">
                    <b>Upload error: </b><span></span>
                    <br />
                    Please select another file and try again.
                </div>
            </div>
            <iframe id="embeddedView" src="" height="345px" width="432px" frameborder="0" scrolling="no" allowtransparency></iframe>
            <br />
            <div class="buttonsMobile">
                <div id="beginEdit" class="button orange disable">Edit</div>
                <div id="beginView" class="button gray disable">View</div>
                <div id="beginEmbedded" class="button gray disable">Embedded view</div>
                <div id="cancelEdit" class="button gray">Cancel</div>
            </div>
        </div>

        <span id="loadScripts" data-docs="<%= ConfigManager.getProperty("files.docservice.url.site") + ConfigManager.getProperty("files.docservice.url.preloader") %>"></span>

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
                                &copy; Ascensio Systems SIA 2023. All rights reserved.
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </footer>

        <script type="text/javascript" src="scripts/jquery-3.6.4.min.js"></script>
        <script type="text/javascript" src="scripts/jquery-migrate-3.4.1.min.js"></script>
        <script type="text/javascript" src="scripts/jquery-ui.js"></script>
        <script type="text/javascript" src="scripts/jquery.blockUI.js"></script>
        <script type="text/javascript" src="scripts/jquery.iframe-transport.js"></script>
        <script type="text/javascript" src="scripts/jquery.fileupload.js"></script>
        <script type="text/javascript" src="scripts/jquery.dropdownToggle.js"></script>
        <script type="text/javascript" src="scripts/jscript.js"></script>

        <script language="javascript" type="text/javascript">
            var FillExtList = "<%= String.join(",", DocumentManager.getFillExts()) %>".split(",");
            var ConverExtList = "<%= String.join(",", DocumentManager.getConvertExts()) %>".split(",");
            var EditedExtList = "<%= String.join(",", DocumentManager.getEditedExts()) %>".split(",");
            var UrlConverter = "IndexServlet?type=convert";
            var UrlEditor = "EditorServlet";

            document.addEventListener('DOMContentLoaded', function() {
                var lang = document.cookie
                        .split('; ')
                        .find((row) => row.startsWith('ulang='))
                        ?.split('=')[1];

                var languages = Array.from(document.getElementById("language").options).map(e => e.value)

                if (!languages.includes(lang)) {
                    lang = "en";
                }

                document.getElementById("language").value=lang;
            });
        </script>
    </body>
</html>
