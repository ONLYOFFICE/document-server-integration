<!DOCTYPE html>
<html lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <meta name="server-version" content="{serverVersion}">
    <title>ONLYOFFICE Document Editors</title>

    <link rel="icon" href="assets/images/favicon.ico" type="image/x-icon" />

    <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:900,
            800,700,600,500,400,300&subset=latin,cyrillic-ext,cyrillic,latin-ext" />

    <link rel="stylesheet" type="text/css" href="assets/css/stylesheet.css" />
    <link rel="stylesheet" type="text/css" href="assets/css/media.css">
    <link rel="stylesheet" type="text/css" href="assets/css/jquery-ui.css" />
</head>
<body>
<form id="form1">
    <header>
        <div class="center main-nav">
            <a href="./">
                <img src ="assets/images/logo.svg" alt="ONLYOFFICE" />
            </a>
        </div>
        <menu class="responsive-nav">
            <li>
                <a href="#" onclick="toggleSidePanel(event)">
                    <img src ="assets/images/mobile-menu.svg" alt="ONLYOFFICE" />
                </a>
            </li>
            <li>
                <a href="./">
                    <img src ="assets/images/mobile-logo.svg" alt="ONLYOFFICE" />
                </a>
            </li>
        </menu>
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
                                        <a class="try-editor word reload-page" target="_blank"
                                           href="editor?fileExt=docx&user={user}">Document</a>
                                    </li>
                                    <li>
                                        <a class="try-editor cell reload-page" target="_blank"
                                           href="editor?fileExt=xlsx&user={user}">Spreadsheet</a>
                                    </li>
                                    <li>
                                        <a class="try-editor slide reload-page" target="_blank"
                                           href="editor?fileExt=pptx&user={user}">Presentation</a>
                                    </li>
                                    <li>
                                        <a class="try-editor form reload-page" target="_blank"
                                           href="editor?fileExt=pdf&user={user}">PDF form</a>
                                    </li>
                                </ul>
                                <label class="side-option">
                                    <input type="checkbox" id="createSample" class="checkbox" />
                                    With sample content
                                </label>
                            </div>
                            <div class="upload-panel clearFix">
                                <a class="file-upload">Upload file
                                    <input type="file" id="fileupload" name="files"
                                           data-url="upload?user={user}" />
                                </a>
                            </div>

                            <table class="user-block-table" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td valign="middle">
                                        <span class="select-user">Username</span>
                                        <img id="info" class="info" src="assets/images/info.svg" />
                                        <select class="select-user" id="user">
                                            {userOpts}
                                        </select>
                                    </td>
                                </tr>
                                <tr>
                                    <td valign="middle">
                                        <span class="select-user">Language</span>
                                        <img class="info info-tooltip" data-id="language"
                                             data-tooltip="Choose the language for ONLYOFFICE editors interface"
                                             src="assets/images/info.svg" />
                                        <select class="select-user" id="language">
                                            {langs}
                                        </select>
                                    </td>
                                </tr>
                                <tr>
                                    <td valign="middle">
                                        <label class="side-option">
                                            <input id="directUrl" type="checkbox" class="checkbox" />
                                            Try opening on client
                                            <img id="directUrlInfo" class="info info-tooltip"
                                                 data-id="directUrlInfo" data-tooltip=
                                                 "Some files can be opened in the user's
                                                             browser without connecting to the document server."
                                                 src="assets/images/info.svg" />
                                        </label>
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </div>
                    <button class="mobile-close-btn" onclick="toggleSidePanel(event)">
                        <img src="assets/images/close.svg" alt="">
                    </button>
                </td>
                <td class="section">
                    <div class="main-panel">
                        <menu class="links">
                            <li class="home-link active" >
                                <a href="./">
                                    <img src="assets/images/home.svg" alt="Home"/>
                            </a>
                            </li>
                            {forgottenLink}
                        </menu>
                        <div id="portal-info" style="display: {portalInfoDisplay}">
                            <span class="portal-name">ONLYOFFICE Document Editors – Welcome!</span>
                            <span class="portal-descr">
                                        Get started with a demo-sample of ONLYOFFICE Document Editors,
                                        the first html5-based editors.
                                        <br /> You may upload your own documents for testing using the
                                        "<b>Upload file</b>" button and <b>selecting</b>
                                        the necessary files on your PC.
                                    </span>
                            <span class="portal-descr">
                                        Please do NOT use this integration example on your own server without
                                        proper code modifications, it is intended for testing purposes only.
                                        In case you enabled this test example, disable it before going for
                                        production.
                                    </span>
                            <span class="portal-descr">
                                        You can open the same document using different
                                        users in different Web browser sessions, so you can check out multi-user
                                        editing functions.
                                    </span>
                            {userDescr}
                            </div>
                            {storedList}
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
            <span class="step-descr">The loading speed depends on file size
                        and additional elements it contains.</span>
            <div id="select-file-type" class="invisible">
                <br />
                <span class="step">Please select the current document type</span>
                <div class="buttonsMobile indent">
                    <div class="button file-type document" data="docx">Document</div>
                    <div class="button file-type spreadsheet" data="xlsx">Spreadsheet</div>
                    <div class="button file-type presentation" data="pptx">Presentation</div>
                </div>
            </div>
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
            <span class="progress-descr">Note the speed of all operations depends
                        on your connection quality and server location.</span>
            <br />
            <div class="error-message">
                <b>Upload error: </b><span></span>
                <br />
                Please select another file and try again.
            </div>
        </div>
        <iframe id="embeddedView" src="" height="345px" width="432px"
                frameborder="0" scrolling="no" allowtransparency></iframe>
        <br />
        <div class="buttonsMobile">
            {editButton}
            <div id="beginView" class="button gray disable">View</div>
            <div id="beginEmbedded" class="button gray disable">Embedded view</div>
            <div id="cancelEdit" class="button gray">Cancel</div>
        </div>
    </div>

    <div id="convertingProgress">
        <div id="convertingSteps">
            <span id="convertFileName" class="convertFileName"></span>
            <span id="convertStep1" class="step">1. Select a format file to convert</span>
            <span class="step-descr">The converting speed depends on file size and additional elements it contains.</span>
            <table cellspacing="0" cellpadding="0" width="100%" class="convertTable">
                <tbody>
                    <tr class="typeButtonsRow" id="convTypes"></tr>
                </tbody>
            </table>
            <br />
            <span id="convertStep2" class="step">2. File conversion</span>
            <span class="step-descr disable" id="convert-descr">The file is converted <div class="convertPercent" id="convertPercent">0 %</div></span>
            <span class="step-error hidden" id="convert-error"></span>
            <div class="describeUpload">Note the speed of all operations depends on your connection quality and server location.</div>
            <input type="hidden" name="hiddenFileName" id="hiddenFileName" />
        </div>
        <br />
        <div class="buttonsMobile">
            <div id="downloadConverted" class="button converting orange disable">DOWNLOAD</div>
            <div id="beginViewConverted" class="button converting wide gray disable">VIEW</div>
            <div id="beginEditConverted" class="button converting wide gray disable">EDIT</div>
            <div id="cancelEdit" class="button converting gray">CANCEL</div>
        </div>
    </div>

    <span id="loadScripts" data-docs="{dataDocs}"></span>

    <footer>
        <div class="center">
            <table>
                <tbody>
                <tr>
                    <td>
                        <a href="http://api.onlyoffice.com/editors/howitworks" target="_blank">
                            API Documentation
                        </a>
                    </td>
                    <td>
                        <a href="mailto:sales@onlyoffice.com">Submit your request</a>
                    </td>
                    <td class="copy">
                        &copy; Ascensio Systems SIA {date}. All rights reserved.
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    </footer>
</form>

<script type="text/javascript" src="assets/js/jquery-3.6.4.min.js"></script>
<script type="text/javascript" src="assets/js/jquery-migrate-3.4.1.min.js"></script>
<script type="text/javascript" src="assets/js/jquery-ui.min.js"></script>
<script type="text/javascript" src="assets/js/jquery.blockUI.js"></script>
<script type="text/javascript" src="assets/js/jquery.iframe-transport.js"></script>
<script type="text/javascript" src="assets/js/jquery.fileupload.js"></script>
<script type="text/javascript" src="assets/js/jquery.dropdownToggle.js"></script>
<script type="text/javascript" src="assets/js/formats.js"></script>
<script type="text/javascript" src="assets/js/jscript.js"></script>
</body>
</html>