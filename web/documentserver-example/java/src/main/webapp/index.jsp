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

<%@page import="helpers.DocumentManager"%>
<%@page import="helpers.FileUtility"%>
<%@page import="helpers.ConfigManager"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>ONLYOFFICE</title>
        <link rel="icon" href="favicon.ico" type="image/x-icon" />
        <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:900,800,700,600,500,400,300&subset=latin,cyrillic-ext,cyrillic,latin-ext" />
        <link rel="stylesheet" type="text/css" href="css/stylesheet.css" />
        <link rel="stylesheet" type="text/css" href="css/jquery-ui.css" />
    </head>
    <body>

        <div class="top-panel"></div>

        <div class="main-panel">
            <span class="portal-name">ONLYOFFICE Document Editors</span>
            <br />
            <br />
            <span class="portal-descr">Get started with a demo-sample of ONLYOFFICE Document Editors, the first html5-based editors. You may upload your own documents for testing using the "Upload file" button and selecting the necessary files on your PC.</span>

            <table class="user-block-table" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td width="30%" valign="middle">
                            <span class="select-user">Username:</span>
                            <select class="select-user" id="user">
                                <option value="uid-1">John Smith</option>
                                <option value="uid-2">Mark Pottato</option>
                                <option value="uid-3">Hamish Mitchell</option>
                            </select>
                        </td>
                        <td width="70%" valign="middle">Select user name before opening the document; you can open the same document using different users in different Web browser sessions, so you can check out multi-user editing functions.</td>
                    </tr>
                    <tr>
                        <td width="30%" valign="middle">
                            <select class="select-user" id="language">
                                <option value="en">English</option>
                                <option value="bg">Bulgarian</option>
                                <option value="zh">Chinese</option>
                                <option value="cs">Czech</option>
                                <option value="nl">Dutch</option>
                                <option value="fr">French</option>
                                <option value="de">German</option>
                                <option value="hu">Hungarian</option>
                                <option value="it">Italian</option>
                                <option value="ja">Japanese</option>
                                <option value="ko">Korean</option>
                                <option value="lv">Latvian</option>
                                <option value="pl">Polish</option>
                                <option value="pt">Portuguese</option>
                                <option value="ru">Russian</option>
                                <option value="sk">Slovak</option>
                                <option value="sl">Slovenian</option>
                                <option value="es">Spanish</option>
                                <option value="tr">Turkish</option>
                                <option value="uk">Ukrainian</option>
                                <option value="vi">Vietnamese</option>
                            </select>
                        </td>
                        <td width="70%" valign="middle">Choose the language for ONLYOFFICE™ editors interface.</td>
                    </tr>
                </tbody>
            </table>
            <br />
            <br />

            <div class="help-block">
                <span class="try-descr">Upload your file or create new file</span>
                <br />
                <br />
                <div class="clearFix">
                    <div class="upload-panel clearFix">
                        <a class="file-upload">
                            Upload
                            <br />
                            File
                            <input type="file" id="fileupload" name="file" data-url="IndexServlet?type=upload" />
                        </a>
                    </div>
                    <div class="create-panel">
                        <ul class="try-editor-list clearFix">
                            <li><a class="try-editor document" data-type="docx">Create<br />Document</a></li>
                            <li><a class="try-editor spreadsheet" data-type="xlsx">Create<br />Spreadsheet</a></li>
                            <li><a class="try-editor presentation" data-type="pptx">Create<br />Presentation</a></li>
                        </ul>
                        <label class="create-sample">
                            <input id="createSample" class="checkbox" type="checkbox" />
                            Create a file filled with sample content
                        </label>
                    </div>
                </div>
            </div>

            <% DocumentManager.Init(request, response); %>
            <% File[] files = DocumentManager.GetStoredFiles(null); %>
            <% if (files.length > 0) { %>

                <div class="help-block">
                    <span>Your documents</span>
                    <br />
                    <br />

                    <div class="stored-list">
                        <table width="100%" cellspacing="0" cellpadding="0">
                            <thead>
                                <tr class="tableHeader">
                                    <td class="tableHeaderCell tableHeaderCellFileName">Filename</td>
                                    <td colspan="6" class="tableHeaderCell contentCells-shift">Editors</td>
                                    <td colspan="3" class="tableHeaderCell">Viewers</td>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (Integer i = 0; i < files.length; i++) { %>
                                    <% String docType = FileUtility.GetFileType(files[i].getName()).toString().toLowerCase(); %>
                                    <tr class="tableRow" title="<%= files[i].getName() %>">
                                        <td class="contentCells">
                                            <a class="stored-edit <%= docType %>" href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>" target="_blank">
                                                <span title="<%= files[i].getName() %>"><%= files[i].getName() %></span>
                                            </a>
                                            <a href="<%= DocumentManager.GetFileUri(files[i].getName()) %>">
                                                <img class="icon-download" src="css/img/download-24.png" alt="Download" title="Download" />
                                            </a>
                                            <a class="delete-file" data-filename="<%= files[i].getName() %>">
                                                <img class="icon-delete" src="css/img/delete-24.png" alt="Delete" title="Delete" />
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
                                            <% if (docType.equals("text")) { %>
                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=desktop&mode=review" target="_blank">
                                                    <img src="css/img/review-24.png" alt="Open in editor for review" title="Open in editor for review"/>
                                                </a>
                                            <% } else if (docType.equals("spreadsheet")) { %>
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
                                            <% if (docType.equals("text")) { %>
                                                <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=desktop&mode=fillForms" target="_blank">
                                                    <img src="css/img/fill-forms-24.png" alt="Open in editor for filling in forms" title="Open in editor for filling in forms"/>
                                                </a>
                                            <% } %>
                                        </td>
                                        <td class="contentCells contentCells-shift contentCells-icon">
                                            <% if (docType.equals("text")) { %>
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
                                        <td class="contentCells contentCells-icon">
                                            <a href="EditorServlet?fileName=<%= URLEncoder.encode(files[i].getName(), "UTF-8") %>&type=embedded&mode=embedded" target="_blank">
                                                <img src="css/img/embeded-24.png" alt="Open in embedded mode" title="Open in embedded mode"/>
                                            </a>
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>

            <% } %>

            <br />
            <br />
            <br />
            <div class="help-block">
                <span>Want to learn how it works?</span>
                <br />
                Read the editor <a href="https://api.onlyoffice.com/">API Documentation</a>
            </div>
            <br />
            <br />
            <br />
            <div class="help-block">
                <span>Any questions?</span>
                <br />
                Please, <a href="mailto:sales@onlyoffice.com">submit your request</a> and we'll help you shortly.
            </div>
        </div>

        <div id="hint">
            <div class="corner"></div>
            If you check this option the file will be saved both in the original and converted into Office Open XML format for faster viewing and editing. In other case the document will be overwritten by its copy in Office Open XML format.
        </div>

        <div id="mainProgress">
            <div id="uploadSteps">
                <span id="step1" class="step">1. Loading the file</span>
                <span class="step-descr">The file loading process will take some time depending on the file size, presence or absence of additional elements in it (macros, etc.) and the connection speed.</span>
                <br />
                <span id="step2" class="step">2. File conversion</span>
                <span class="step-descr">The file is being converted into Office Open XML format for the document faster viewing and editing.</span>
                <br />
                <span id="step3" class="step">3. Loading editor scripts</span>
                <span class="step-descr">The scripts for the editor are loaded only once and are will be cached on your computer in future. It might take some time depending on the connection speed.</span>
                <input type="hidden" name="hiddenFileName" id="hiddenFileName" />
                <br />
                <br />
                <span class="progress-descr">Please note, that the speed of all operations greatly depends on the server and the client locations. In case they differ or are located in different countries/continents, there might be lack of speed and greater wait time. The best results are achieved when the server and client computers are located in one and the same place (city).</span>
                <br />
                <br />
                <div class="error-message">
                    <span></span>
                    <br />
                    Please select another file and try again. If you have questions please <a href="mailto:sales@onlyoffice.com">contact us.</a>
                </div>
            </div>
            <iframe id="embeddedView" src="" height="345px" width="600px" frameborder="0" scrolling="no" allowtransparency></iframe>
            <br />
            <div id="beginEmbedded" class="button disable">Embedded view</div>
            <div id="beginView" class="button disable">View</div>
            <div id="beginEdit" class="button disable">Edit</div>
            <div id="cancelEdit" class="button gray">Cancel</div>
        </div>

        <span id="loadScripts" data-docs="<%= ConfigManager.GetProperty("files.docservice.url.preloader") %>"></span>

        <div class="bottom-panel">
            &copy; Ascensio System SIA <%= Calendar.getInstance().get(Calendar.YEAR) %>. All rights reserved.
        </div>

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
