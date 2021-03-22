<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="Default.aspx.cs" Inherits="OnlineEditorsExample._Default" Title="ONLYOFFICE" %>

<%@ Import Namespace="System.IO" %>
<%@ Import Namespace="System.Linq" %>
<%@ Import Namespace="System.Web.Configuration" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head runat="server">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
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
    <link rel="icon" href="~/favicon.ico" type="image/x-icon" />

    <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:900,800,700,600,500,400,300&subset=latin,cyrillic-ext,cyrillic,latin-ext" />

    <link rel="stylesheet" type="text/css" href="app_themes/stylesheet.css" />

    <link rel="stylesheet" type="text/css" href="app_themes/jquery-ui.css" />

    <script language="javascript" type="text/javascript" src="script/jquery-1.9.0.min.js"></script>

    <script language="javascript" type="text/javascript" src="script/jquery-ui.min.js"></script>

    <script language="javascript" type="text/javascript" src="script/jquery.blockUI.js"></script>

    <script language="javascript" type="text/javascript" src="script/jquery.iframe-transport.js"></script>

    <script language="javascript" type="text/javascript" src="script/jquery.fileupload.js"></script>

    <script language="javascript" type="text/javascript" src="script/jquery.dropdownToggle.js"></script>

    <script language="javascript" type="text/javascript" src="script/jscript.js"></script>

    <script language="javascript" type="text/javascript">
        var ConverExtList = '<%= string.Join(",", ConvertExts.ToArray()) %>';
        var EditedExtList = '<%= string.Join(",", EditedExts.ToArray()) %>';
    </script>
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
                                        </ul>
                                        <label class="create-sample">
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
                        </td>
                        <td class="section">
                            <div class="main-panel">
                            <%  var storedFiles = GetStoredFiles();
                                if (!storedFiles.Any())
                                { %>
                                    <span class="portal-name">ONLYOFFICE Document Editors – Welcome!</span>
                                    <span class="portal-descr">
                                        Get started with a demo-sample of ONLYOFFICE Document Editors, the first html5-based editors.
                                        <br /> You may upload your own documents for testing using the "<b>Upload file</b>" button and <b>selecting</b> the necessary files on your PC.
                                    </span>
                            <%  }
                                else
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
                                                        var editUrl = "doceditor.aspx?fileID=" + HttpUtility.UrlEncode(storedFile.Name);
                                                        var docType = DocumentType(storedFile.Name); %>

                                                        <tr class="tableRow" title="<%= storedFile.Name %>">
                                                            <td class="contentCells">
                                                                <a class="stored-edit <%= docType %>" href="<%= editUrl %>" target="_blank">
                                                                    <span title="<%= storedFile.Name %>"><%= storedFile.Name %></span>
                                                                </a>
                                                            </td>

                                                            <td class="contentCells contentCells-icon">
                                                                <a href="<%= editUrl + "&editorsType=desktop&editorsMode=edit" %>" target="_blank">
                                                                    <img src="app_themes/images/desktop-24.png" alt="Open in editor for full size screens" title="Open in editor for full size screens"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <a href="<%= editUrl + "&editorsType=mobile&editorsMode=edit" %>" target="_blank">
                                                                    <img src="app_themes/images/mobile-24.png" alt="Open in editor for mobile devices" title="Open in editor for mobile devices"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <% if (docType == "word") { %>
                                                                    <a href="<%= editUrl + "&editorsType=desktop&editorsMode=review" %>" target="_blank">
                                                                        <img src="app_themes/images/review-24.png" alt="Open in editor for review" title="Open in editor for review"/>
                                                                    </a>
                                                                <% } else if (docType == "cell") { %>
                                                                    <a href="<%= editUrl + "&editorsType=desktop&editorsMode=filter" %>" target="_blank">
                                                                        <img src="app_themes/images/filter-24.png" alt="Open in editor without access to change the filter" title="Open in editor without access to change the filter" />
                                                                    </a>
                                                                <% } %>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <a href="<%= editUrl + "&editorsType=desktop&editorsMode=comment" %>" target="_blank">
                                                                    <img src="app_themes/images/comment-24.png" alt="Open in editor for comment" title="Open in editor for comment"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <% if (docType == "word") { %>
                                                                    <a href="<%= editUrl + "&editorsType=desktop&editorsMode=fillForms" %>" target="_blank">
                                                                        <img src="app_themes/images/fill-forms-24.png" alt="Open in editor for filling in forms" title="Open in editor for filling in forms"/>
                                                                    </a>
                                                                <% } %>
                                                            </td>
                                                            <td class="contentCells contentCells-shift contentCells-icon">
                                                                <% if (docType == "word") { %>
                                                                    <a href="<%= editUrl + "&editorsType=desktop&editorsMode=blockcontent" %>" target="_blank">
                                                                        <img src="app_themes/images/block-content-24.png" alt="Open in editor without content control modification" title="Open in editor without content control modification"/>
                                                                    </a>
                                                                <% } %>
                                                            </td>

                                                            <td class="contentCells contentCells-icon">
                                                                <a href="<%= editUrl + "&editorsType=desktop&editorsMode=view" %>" target="_blank">
                                                                    <img src="app_themes/images/desktop-24.png" alt="Open in viewer for full size screens" title="Open in viewer for full size screens"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon">
                                                                <a href="<%= editUrl + "&editorsType=mobile&editorsMode=view" %>" target="_blank">
                                                                    <img src="app_themes/images/mobile-24.png" alt="Open in viewer for mobile devices" title="Open in viewer for mobile devices"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift">
                                                                <a href="<%= editUrl + "&editorsType=embedded&editorsMode=embedded" %>" target="_blank">
                                                                    <img src="app_themes/images/embeded-24.png" alt="Open in embedded mode" title="Open in embedded mode"/>
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift">
                                                                <a href="webeditor.ashx?type=download&filename=<%= HttpUtility.UrlEncode(storedFile.Name) %>">
                                                                    <img class="icon-download" src="app_themes/images/download-24.png" alt="Download" title="Download" />
                                                                </a>
                                                            </td>
                                                            <td class="contentCells contentCells-icon contentCells-shift">
                                                                <a class="delete-file" data-filename="<%= storedFile.Name %>">
                                                                    <img class="icon-delete" src="app_themes/images/delete-24.png" alt="Delete" title="Delete" />
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
                <span class="progress-descr">Please note, that the speed of all operations greatly depends on the server and the client locations. In case they differ or are located in differernt countries/continents, there might be lack of speed and greater wait time. The best results are achieved when the server and client computers are located in one and the same place (city).</span>
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
</body>
</html>
