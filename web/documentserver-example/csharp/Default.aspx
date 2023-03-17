<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="Default.aspx.cs" Inherits="OnlineEditorsExample._Default" Title="ONLYOFFICE" %>

<%@ Import Namespace="System.IO" %>
<%@ Import Namespace="System.Web.WebPages" %>
<%@ Import Namespace="System.Linq" %>
<%@ Import Namespace="System.Web.Configuration" %>
<%@ Import Namespace="OnlineEditorsExample" %>
<%@ Import Namespace="System.Collections.Generic" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head runat="server">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <title>ONLYOFFICE</title>
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
    <link rel="icon" href="~/favicon.ico" type="image/x-icon" />

    <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:900,800,700,600,500,400,300&subset=latin,cyrillic-ext,cyrillic,latin-ext" />

    <link rel="stylesheet" type="text/css" href="app_themes/stylesheet.css" />
    <link rel="stylesheet" type="text/css" href="app_themes/media.css" />

    <link rel="stylesheet" type="text/css" href="app_themes/jquery-ui.css" />

</head>
<body>
    <form id="form1" runat="server">
        <header>
            <div class="center">
                <a href="">
                    <img src ="app_themes/images/logo.svg" alt="ONLYOFFICE" />
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
                                                <a class="try-editor word" data-type="word">Document</a>
                                            </li>
                                            <li>
                                                <a class="try-editor cell" data-type="cell">Spreadsheet</a>
                                            </li>
                                            <li>
                                                <a class="try-editor slide" data-type="slide">Presentation</a>
                                            </li>
                                            <li>
                                                <a class="try-editor form" data-type="docxf">Form template</a>
                                            </li>
                                        </ul>
                                        <label class="side-option">
                                            <input id="createSample" class="checkbox" type="checkbox" />With sample content
                                        </label>
                                    </div>
                                    <div class="upload-panel clearFix">
                                        <a class="file-upload">Upload file
                                            <input type="file" id="fileupload" name="files[]" data-url="webeditor.ashx?type=upload" />
                                        </a>
                                    </div>
                                </div>
                                <table class="user-block-table" cellspacing="0" cellpadding="0">
                                    <tbody>
                                        <tr>
                                            <td valign="middle">
                                                <span class="select-user">Username</span>
                                                <img id="info" class="info" src="app_themes/images/info.svg" />
                                                <select class="select-user" id="user">
                                                    <% foreach (User user in Users.getAllUsers())
                                                       { %>
                                                           <option value="<%= user.id %>"><%= user.name.IsEmpty() ? "Anonymous" : user.name %></option>
                                                    <% } %>
                                                </select>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td valign="middle">
                                                <span class="select-user">Language</span>
                                            <img class="info info-tooltip" data-id="language"
                                                 data-tooltip="Choose the language for ONLYOFFICE editors interface"
                                                 src="app_themes/images/info.svg" />
                                                <select class="select-user" id="language">
                                                    <% Dictionary<string, string> languages = GetLanguages(); 
                                                    foreach (var lang in languages)
                                                        { %>
                                                            <option value="<%= lang.Key %>"><%= lang.Value %></option>
                                                        <% } %>
                                                </select>
                                            </td>
                                        </tr>
                                        <tr>
                                        <td valign="middle">
                                            <label class="side-option">
                                                <input id="directUrl" type="checkbox" class="checkbox" />Try opening on client
                                                <img id="directUrlInfo" class="info info-tooltip" data-id="directUrlInfo" data-tooltip="Some files can be opened in the user's browser without connecting to the document server." src="app_themes/images/info.svg" />
                                            </label>
                                        </td>
                                    </tr>
                                    </tbody>
                                </table>
                            </div>
                        </td>
                        <td class="section">
                        <% var storedFiles = GetStoredFiles(); %>
                            <div class="main-panel">
                                <div id="portal-info" style="display: <%= storedFiles.Any() ? "none" : "table-cell" %>">
                                    <span class="portal-name">ONLYOFFICE Document Editors – Welcome!</span>
                                    <span class="portal-descr">
                                        Get started with a demo-sample of ONLYOFFICE Document Editors, the first html5-based editors.
                                        <br /> You may upload your own documents for testing using the "<b>Upload file</b>" button and <b>selecting</b> the necessary files on your PC.
                                    </span>
                                    <span class="portal-descr">Please do NOT use this integration example on your own server without proper code modifications, it is intended for testing purposes only. In case you enabled this test example, disable it before going for production.</span>
                                    <span class="portal-descr">You can open the same document using different users in different Web browser sessions, so you can check out multi-user editing functions.</span>
                                    <% foreach (User user in Users.getAllUsers())
                                      { %>
                                      <div class="user-descr">
                                       <b><%= user.name.IsEmpty() ? "Anonymous" : user.name %></b>
                                           <ul>
                                           <% foreach (string description in user.descriptions)
                                                   { %>
                                                       <li><%= description %></li>
                                                <% } %>
                                           </ul>
                                       </div>
                                       <% } %>
                                </div>
                            <%
                                if (storedFiles.Any())
                                { %>
                                    <div class="stored-list">
                                        <span class="header-list">Your documents</span>
                                        <table class="tableHeader" cellspacing="0" cellpadding="0" width="100%">
                                            <thead>
                                                <tr >
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
                                                <%  foreach (var storedFile in storedFiles)
                                                    {
                                                        var directUrlParam = GetDirectUrlParam();
                                                        var editUrl = "doceditor.aspx?fileID=" + HttpUtility.UrlEncode(storedFile.Name) + directUrlParam;
                                                        var ext = Path.GetExtension(storedFile.Name).ToLower();
                                                        var docType = DocumentType(storedFile.Name);
                                                        var canEdit = EditedExts.Contains(ext);
                                                        var isFillFormDoc = FillFormsExts.Contains(ext);
                                                        %>

                                                        <tr class="tableRow" title="<%= storedFile.Name %> [<%= GetFileVersion(storedFile.Name, HttpContext.Current.Request.UserHostAddress.Replace(':','_')) %>]">
                                                            <td class="contentCells">
                                                                <a class="stored-edit <%= docType %>" href="<%= editUrl %>" target="_blank">
                                                                    <span><%= storedFile.Name %></span>
                                                                </a>
                                                            </td>
                                                            <% if (canEdit) { %>
                                                                <td class="contentCells contentCells-icon">
                                                                    <a href="<%= editUrl + "&editorsType=desktop&editorsMode=edit" %>" target="_blank">
                                                                        <img src="app_themes/images/desktop.svg" alt="Open in editor for full size screens" title="Open in editor for full size screens"/>
                                                                    </a>
                                                                </td>
                                                                <td class="contentCells contentCells-icon">
                                                                    <a href="<%= editUrl + "&editorsType=mobile&editorsMode=edit" %>" target="_blank">
                                                                        <img src="app_themes/images/mobile.svg" alt="Open in editor for mobile devices" title="Open in editor for mobile devices"/>
                                                                    </a>
                                                                </td>
                                                                <td class="contentCells contentCells-icon">
                                                                    <a href="<%= editUrl + "&editorsType=desktop&editorsMode=comment" %>" target="_blank">
                                                                         <img src="app_themes/images/comment.svg" alt="Open in editor for comment" title="Open in editor for comment"/>
                                                                    </a>
                                                                </td>
                                                                <% if (docType == "word") { %>
                                                                    <td class="contentCells contentCells-icon">
                                                                        <a href="<%= editUrl + "&editorsType=desktop&editorsMode=review" %>" target="_blank">
                                                                            <img src="app_themes/images/review.svg" alt="Open in editor for review" title="Open in editor for review"/>
                                                                        </a>
                                                                    </td>
                                                                <% } else if (docType == "cell") { %>
                                                                    <td class="contentCells contentCells-icon">
                                                                        <a href="<%= editUrl + "&editorsType=desktop&editorsMode=filter" %>" target="_blank">
                                                                            <img src="app_themes/images/filter.svg" alt="Open in editor without access to change the filter" title="Open in editor without access to change the filter" />
                                                                        </a>
                                                                    </td>
                                                                <% } %>
                                                                <% if (docType == "word") { %>
                                                                    <td class="contentCells contentCells-icon">
                                                                        <a href="<%= editUrl + "&editorsType=desktop&editorsMode=blockcontent" %>" target="_blank">
                                                                            <img src="app_themes/images/block-content.svg" alt="Open in editor without content control modification" title="Open in editor without content control modification"/>
                                                                        </a>
                                                                    </td>
                                                                <% } else{%>
                                                                    <td class="contentCells contentCells-icon"></td>
                                                                <%} %>
                                                                <%if (docType != "word" && docType != "cell"){%>
                                                                    <td class="contentCells contentCells-icon "></td>
                                                                <% } %>
                                                                    <% if (isFillFormDoc) { %>
                                                                        <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">
                                                                            <a href="<%= editUrl + "&editorsType=desktop&editorsMode=fillForms" %>" target="_blank">
                                                                                <img src="app_themes/images/fill-forms.svg" alt="Open in editor for filling in forms" title="Open in editor for filling in forms"/>
                                                                            </a>
                                                                        </td>
                                                                <% } else { %>
                                                                    <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift"></td>
                                                                <% } %>
                                                                <% } else if (isFillFormDoc) { %>
                                                                    <td class="contentCells contentCells-icon "></td>
                                                                    <td class="contentCells contentCells-icon">
                                                                        <a href="<%= editUrl + "&editorsType=mobile&editorsMode=fillForms" %>" target="_blank">
                                                                           <img src="app_themes/images/mobile-fill-forms.svg" alt="Open in editor for filling in forms for mobile devices" title="Open in editor for filling in forms for mobile devices"/>
                                                                        </a>
                                                                    </td>
                                                                    <td class="contentCells contentCells-icon "></td>
                                                                    <td class="contentCells contentCells-icon "></td>
                                                                    <td class="contentCells contentCells-icon "></td>
                                                                    <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">
                                                                        <a href="<%= editUrl + "&editorsType=desktop&editorsMode=fillForms" %>" target="_blank">
                                                                            <img src="app_themes/images/fill-forms.svg" alt="Open in editor for filling in forms" title="Open in editor for filling in forms"/>
                                                                        </a>
                                                                    </td>
                                                            <% } else { %>
                                                                <td class="contentCells contentCells-shift contentCells-icon contentCellsEmpty" colspan="6"></td>
                                                            <% } %>
                                                            <td class="contentCells contentCells-icon firstContentCellViewers">
                                                                <a href="<%= editUrl + "&editorsType=desktop&editorsMode=view" %>" target="_blank">
                                                                    <img src="app_themes/images/desktop.svg" alt="Open in viewer for full size screens" title="Open in viewer for full size screens"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <a href="<%= editUrl + "&editorsType=mobile&editorsMode=view" %>" target="_blank">
                                                                    <img src="app_themes/images/mobile.svg" alt="Open in viewer for mobile devices" title="Open in viewer for mobile devices"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift">
                                                                <a href="<%= editUrl + "&editorsType=embedded&editorsMode=embedded" %>" target="_blank">
                                                                    <img src="app_themes/images/embeded.svg" alt="Open in embedded mode" title="Open in embedded mode"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift downloadContentCellShift">
                                                                <a href="webeditor.ashx?type=download&fileName=<%= HttpUtility.UrlEncode(storedFile.Name) %>">
                                                                    <img class="icon-download" src="app_themes/images/download.svg" alt="Download" title="Download" />
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift">
                                                                <a class="delete-file" data-filename="<%= storedFile.Name %>">
                                                                    <img class="icon-delete" src="app_themes/images/delete.svg" alt="Delete" title="Delete" />
                                                                </a>
                                                            </td>
                                                        </tr>
                                                <%  } %>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                            <%  } %>
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

        <span id="loadScripts" data-docs="<%= UrlPreloadScripts %>"></span>

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
                                &copy; Ascensio System SIA <%= DateTime.Now.Year.ToString() %>. All rights reserved.
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </footer>
    </form>

    <script language="javascript" type="text/javascript" src="script/jquery-3.6.4.min.js"></script>
    <script language="javascript" type="text/javascript" src="script/jquery-migrate-3.4.1.min.js"></script>
    <script language="javascript" type="text/javascript" src="script/jquery-ui.min.js"></script>
    <script language="javascript" type="text/javascript" src="script/jquery.blockUI.js"></script>
    <script language="javascript" type="text/javascript" src="script/jquery.iframe-transport.js"></script>
    <script language="javascript" type="text/javascript" src="script/jquery.fileupload.js"></script>
    <script language="javascript" type="text/javascript" src="script/jquery.dropdownToggle.js"></script>
    <script language="javascript" type="text/javascript" src="script/jscript.js"></script>
    <script language="javascript" type="text/javascript">
        var FillFormExtList = '<%= string.Join(",", FillFormsExts.ToArray()) %>';
        var ConverExtList = '<%= string.Join(",", ConvertExts.ToArray()) %>';
        var EditedExtList = '<%= string.Join(",", EditedExts.ToArray()) %>';
    </script>

</body>
</html>
