<?php
/*
 *
 * (c) Copyright Ascensio System Limited 2010-2017
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
*/
?>

<?php
    require_once( dirname(__FILE__) . '/config.php' );
    require_once( dirname(__FILE__) . '/common.php' );
    require_once( dirname(__FILE__) . '/functions.php' );

    $user = $_GET["user"];
?>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html lang="en">

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <title>ONLYOFFICE Integration Edition</title>

        <link rel="icon" href="./favicon.ico" type="image/x-icon" />

        <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:900,800,700,600,500,400,300&subset=latin,cyrillic-ext,cyrillic,latin-ext" />

        <link rel="stylesheet" type="text/css" href="css/stylesheet.css" />

        <link rel="stylesheet" type="text/css" href="css/jquery-ui.css" />

        <script type="text/javascript" src="js/jquery-1.9.0.min.js"></script>

        <script type="text/javascript" src="js/jquery-ui.min.js"></script>

        <script type="text/javascript" src="js/jquery.blockUI.js"></script>

        <script type="text/javascript" src="js/jquery.iframe-transport.js"></script>

        <script type="text/javascript" src="js/jquery.fileupload.js"></script>

        <script type="text/javascript" src="js/jquery.dropdownToggle.js"></script>

        <script type="text/javascript">
            var ConverExtList = '<?php echo implode(",", $GLOBALS["DOC_SERV_CONVERT"]) ?>';
            var EditedExtList = '<?php echo implode(",", $GLOBALS["DOC_SERV_EDITED"]) ?>';
        </script>
    </head>
    <body>
        <form id="form1">
        <header>
            <a href="/">
                <img src ="css/images/logo.png" alt="ONLYOFFICE" />
            </a>
        </header>
        <div class="main-panel">
            <span class="portal-name">ONLYOFFICE Integration Edition – Welcome!</span>
            <br />
            <br />
            <span class="portal-descr">Get started with a demo-sample of ONLYOFFICE Integration Edition, the first html5-based editors. You may upload your own documents for testing using the "Upload file" button and selecting the necessary files on your PC.</span>

            <table class="user-block-table" cellspacing="0" cellpadding="0">
                <tr>
                    <td valign="middle" width="30%">
                        <span class="select-user">Username:</span>
                        <select class="select-user" id="user">
                            <option value="0">Jonn Smith</option>
                            <option value="1">Mark Pottato</option>
                            <option value="2">Hamish Mitchell</option>
                        </select>
                    </td>
                    <td valign="middle" width="70%">Select user name before opening the document; you can open the same document using different users in different Web browser sessions, so you can check out multi-user editing functions.</td>
                </tr>
                <!--<tr>
                    <td valign="middle" width="30%">
                        <select class="select-user" id="language">
                            <option value="en" selected>English</option>
                            <option value="de">Deutsch</option>
                            <option value="es">Espanol</option>
                            <option value="fr">Francais</option>
                            <option value="it">Italiano</option>
                            <option value="pt">Portuguese</option>
                            <option value="ru">Русский</option>
                            <option value="sl">Slovenian</option>
                        </select>
                    </td>
                    <td valign="middle" width="70%">Choose the language for ONLYOFFICE&trade; editors interface.</td>
                </tr>-->
            </table>
            <br />
        <br />

            <div class="help-block">
                <span>Upload your file or create new file</span>
                <br />
                <br />
                <div class="clearFix">
                    <div class="upload-panel clearFix">
                        <a class="file-upload">Upload
                            <br />
                            File
                            <input type="file" id="fileupload" name="files" data-url="webeditor-ajax.php?type=upload" />
                        </a>
                        <br />
                        <label class="save-original">
                            <input type="checkbox" id="checkOriginalFormat" class="checkbox" />Keep file format
                        </label>
                    </div>
                    <div class="create-panel clearFix">
                        <ul class="try-editor-list clearFix">
                            <li>
                                <a class="try-editor document reload-page" target="_blank" href="doceditor.php?fileExt=docx&user=<?php echo $user; ?>">Create
                                    <br />
                                    Document</a>
                            </li>
                            <li>
                                <a class="try-editor spreadsheet reload-page" target="_blank" href="doceditor.php?fileExt=xlsx&user=<?php echo $user; ?>">Create
                                    <br />
                                    Spreadsheet</a>
                            </li>
                            <li>
                                <a class="try-editor presentation reload-page" target="_blank" href="doceditor.php?fileExt=pptx&user=<?php echo $user; ?>">Create
                                    <br />
                                    Presentation</a>
                            </li>
                        </ul>
                        <label class="create-sample">
                            <input type="checkbox" id="createSample" class="checkbox" />Create a file filled with sample content
                        </label>
                    </div>
                </div>
            </div>
            <br />
            <br />

            <div class="help-block">
                <span>Your documents</span>
                <br />
                <br />

                <div class="stored-list">
                    <div id="UserFiles">  
                        <table cellspacing="0" cellpadding="0" width="100%">
                            <thead>
                                <tr class="tableHeader">
                                    <td class="tableHeaderCell tableHeaderCellFileName">Filename</td>
                                    <td colspan="3" class="tableHeaderCell contentCells-shift">Editors</td>
                                    <td colspan="3" class="tableHeaderCell">Viewers</td>
                                </tr>
                            </thead>
                            <tbody>
                            <?php $storedFiles = getStoredFiles();
                                foreach ($storedFiles as &$storeFile) 
                                {
                                    echo '<tr class="tableRow" title="'.$storeFile->name.'">';
                                    echo ' <td class="contentCells">';
                                    echo '  <a class="stored-edit '.$storeFile->documentType.'" href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.$user.'" target="_blank">';
                                    echo '   <span title="'.$storeFile->name.'">'.$storeFile->name.'</span>';
                                    echo '  </a>';
                                    echo '  <a href="webeditor-ajax.php?type=download&filename='.$storeFile->name.'">';
                                    echo '   <img class="icon-download" src="css/images/download-24.png" alt="Download" title="Download" /></a>';
                                    echo '  </a>';
                                    echo '  <a class="delete-file" data="'.$storeFile->name.'">';
                                    echo '   <img class="icon-delete" src="css/images/delete-24.png" alt="Delete" title="Delete" /></a>';
                                    echo '  </a>';
                                    echo ' </td>';
                                    echo ' <td class="contentCells contentCells-icon">';
                                    echo '  <a href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.$user.'&type=desktop" target="_blank">';
                                    echo '   <img src="css/images/desktop-24.png" alt="Open in editor for full size screens" title="Open in editor for full size screens" /></a>';
                                    echo '  </a>';
                                    echo ' </td>';
                                    echo ' <td class="contentCells contentCells-icon">';
                                    echo '  <a href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.$user.'&type=mobile" target="_blank">';
                                    echo '   <img src="css/images/mobile-24.png" alt="Open in editor for mobile devices" title="Open in editor for mobile devices" /></a>';
                                    echo '  </a>';
                                    echo ' <td class="contentCells contentCells-shift contentCells-icon">';
                                    if ($storeFile->documentType == "text") {
                                    echo '  <a href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.$user.'&action=review&type=desktop" target="_blank">';
                                    echo '   <img src="css/images/review-24.png" alt="Open in editor for review" title="Open in editor for review" /></a>';
                                    echo '  </a>';
                                    }
                                    echo ' </td>';
                                    echo ' <td class="contentCells contentCells-icon">';
                                    echo '  <a href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.$user.'&action=view&type=desktop" target="_blank">';
                                    echo '   <img src="css/images/desktop-24.png" alt="Open in viewer for full size screens" title="Open in viewer for full size screens" /></a>';
                                    echo '  </a>';
                                    echo ' </td>';
                                    echo ' <td class="contentCells contentCells-icon">';
                                    echo '  <a href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.$user.'&action=view&type=mobile" target="_blank">';
                                    echo '   <img src="css/images/mobile-24.png" alt="Open in viewer for mobile devices" title="Open in viewer for mobile devices" /></a>';
                                    echo '  </a>';
                                    echo ' </td>';
                                    echo ' <td class="contentCells contentCells-icon">';
                                    echo '  <a href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.$user.'&type=embedded" target="_blank">';
                                    echo '   <img src="css/images/embeded-24.png" alt="Open in embedded mode" title="Open in embedded mode" /></a>';
                                    echo '  </a>';
                                    echo ' </td>';
                                    echo '</tr>';
                                }
                            ?>
                            </tbody>
                        </table>
                    </div>
                </div>

            </div>

            <br />
            <br />
            <br />
            <div class="help-block">
                <span>Want to learn the magic?</span>
                <br />
                Explore ONLYOFFICE Integration Edition <a href="http://api.onlyoffice.com/editors/howitworks" target="_blank">API Documentation.</a>
            </div>
            <br />
            <br />
            <br />
            <div class="help-block">
                <span>Any questions?</span>
                <br />
                Please, <a href="mailto:sales@onlyoffice.com">submit your request here</a>.
            </div>
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
            
            <?php if (($GLOBALS['MODE']) != "view") { ?>
            <div id="beginEdit" class="button disable">Edit</div>
            <?php } ?>
            <div id="cancelEdit" class="button gray">Cancel</div>
        </div>

        <span id="loadScripts" data-docs="<?php echo $GLOBALS['DOC_SERV_PRELOADER_URL'] ?>"></span>
        <footer>&copy; Ascensio Systems Inc <?php echo date("Y") ?>. All rights reserved.</footer>

        <script type="text/javascript" src="js/jscript.js"></script>
    </form>
    </body>
</html>
