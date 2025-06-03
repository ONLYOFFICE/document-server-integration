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
    <link rel="stylesheet" type="text/css" href="assets/css/forgotten.css" />
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
                <a href="#">
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
                <td class="left-panel section"></td>
                <td class="section">
                    <div class="main-panel">
                        <menu class="links">
                            <li class="home-link" >
                                <a href="./">
                                    <img src="assets/images/home.svg" alt="Home"/>
                            </a>
                            </li>
                            <li class="active">
                                <a href="forgotten">Forgotten files</a>
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
                                        {files}
                                    </tbody>
                                </table>
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
                        <a href="https://api.onlyoffice.com/docs/docs-api/get-started/how-it-works/" target="_blank">
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

<script type="text/javascript" src="assets/js/forgotten.js"></script>
</body>
</html>