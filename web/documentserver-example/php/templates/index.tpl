<!DOCTYPE html>
<html lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
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
        <div class="center">
            <a href="">
                <img src ="assets/images/logo.svg" alt="ONLYOFFICE" />
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
                                           href="editor?fileExt=docxf&user={user}">Form template</a>
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
                </td>
                <td class="section">
                    <div class="main-panel">
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
<script type="text/javascript" src="assets/js/jscript.js"></script>
<script type="text/javascript">
    var FillFormsExtList = '{fillFormsExtList}';
    var ConverExtList = '{converExtList}';
    var EditedExtList = '{editedExtList}';
</script>
</body>
</html>