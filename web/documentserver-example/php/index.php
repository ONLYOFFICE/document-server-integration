<?php
    /**
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
     */

    require_once( dirname(__FILE__) . '/config.php' );
    require_once( dirname(__FILE__) . '/common.php' );
    require_once( dirname(__FILE__) . '/functions.php' );
    require_once( dirname(__FILE__) . '/users.php' );

    $user = $_GET["user"];
?>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html lang="en">

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <title>ONLYOFFICE Document Editors</title>

        <link rel="icon" href="./favicon.ico" type="image/x-icon" />

        <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:900,800,700,600,500,400,300&subset=latin,cyrillic-ext,cyrillic,latin-ext" />

        <link rel="stylesheet" type="text/css" href="css/stylesheet.css" />
        <link rel="stylesheet" type="text/css" href="css/media.css">
        <link rel="stylesheet" type="text/css" href="css/jquery-ui.css" />
    </head>
    <body>
        <form id="form1">
            <header>
                <div class="center">
                    <a href="">
                        <img src ="css/images/logo.svg" alt="ONLYOFFICE" />
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
                                                    <a class="try-editor word reload-page" target="_blank" href="doceditor.php?fileExt=docx&user=<?php echo htmlentities($user); ?>">Document</a>
                                                </li>
                                                <li>
                                                    <a class="try-editor cell reload-page" target="_blank" href="doceditor.php?fileExt=xlsx&user=<?php echo htmlentities($user); ?>">Spreadsheet</a>
                                                </li>
                                                <li>
                                                    <a class="try-editor slide reload-page" target="_blank" href="doceditor.php?fileExt=pptx&user=<?php echo htmlentities($user); ?>">Presentation</a>
                                                </li>
                                            </ul>
                                            <label class="create-sample">
                                                <input type="checkbox" id="createSample" class="checkbox" />With sample content
                                            </label>
                                        </div>
                                        <div class="upload-panel clearFix">
                                            <a class="file-upload">Upload file
                                                <input type="file" id="fileupload" name="files" data-url="webeditor-ajax.php?type=upload&user=<?php echo htmlentities($user); ?>" />
                                            </a>
                                        </div>

                                        <table class="user-block-table" cellspacing="0" cellpadding="0">
                                            <tr>
                                                <td valign="middle">
                                                    <span class="select-user">Username</span>
                                                    <img class="info" src="css/images/info.svg" />
                                                    <select class="select-user" id="user">
                                                        <?php foreach(getAllUsers() as $user_l) {
                                                            $name = $user_l->name ? $user_l->name : "Anonymous";
                                                            echo '<option value="'.$user_l->id.'">'.$name.'</option>';
                                                        } ?>
                                                    </select>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td valign="middle">
                                                    <span class="select-user">Language editors interface</span>
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
                                        </table>
                                    </div>
                                </div>
                            </td>
                            <td class="section">
                                <div class="main-panel">
                                    <div id="portal-info">
                                        <span class="portal-name">ONLYOFFICE Document Editors – Welcome!</span>
                                        <span class="portal-descr">
                                            Get started with a demo-sample of ONLYOFFICE Document Editors, the first html5-based editors.
                                            <br /> You may upload your own documents for testing using the "<b>Upload file</b>" button and <b>selecting</b> the necessary files on your PC.
                                        </span>
                                        <span class="portal-descr">You can open the same document using different users in different Web browser sessions, so you can check out multi-user editing functions.</span>
                                        <?php foreach(getAllUsers() as $user_l) {
                                            $name = $user_l->name ? $user_l->name : "Anonymous";
                                            echo '<div class="user-descr">';
                                            echo '<b>'.$name.'</b>';
                                            echo '<ul>';
                                            foreach ($user_l->descriptions as $description) {
                                                echo '<li>'.$description.'</li>';
                                            }
                                            echo '</ul>';
                                            echo '</div>';
                                        } ?>"
                                    </div>
                                    <?php
                                        $storedFiles = getStoredFiles();
                                        if (!empty($storedFiles)) { ?>
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
                                                            <?php foreach ($storedFiles as &$storeFile) {
                                                                echo '<tr class="tableRow" title="'.$storeFile->name.' ['.getFileVersion(getHistoryDir(getStoragePath($storeFile->name))).']">';
                                                                echo ' <td class="contentCells">';
                                                                echo '  <a class="stored-edit '.$storeFile->documentType.'" href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.htmlentities($user).'" target="_blank">';
                                                                echo '   <span>'.$storeFile->name.'</span>';
                                                                echo '  </a>';
                                                                echo ' </td>';
                                                                if ($storeFile->canEdit) {
                                                                    echo ' <td class="contentCells contentCells-icon">';
                                                                    echo '  <a href="doceditor.php?fileID=' . urlencode($storeFile->name) . '&user=' . htmlentities($user) . '&action=edit&type=desktop" target="_blank">';
                                                                    echo '   <img src="css/images/desktop.svg" alt="Open in editor for full size screens" title="Open in editor for full size screens" /></a>';
                                                                    echo ' </td>';
                                                                    echo ' <td class="contentCells contentCells-icon">';
                                                                    echo '  <a href="doceditor.php?fileID=' . urlencode($storeFile->name) . '&user=' . htmlentities($user) . '&action=edit&type=mobile" target="_blank">';
                                                                    echo '   <img src="css/images/mobile.svg" alt="Open in editor for mobile devices" title="Open in editor for mobile devices" /></a>';
                                                                    echo ' </td>';
                                                                    echo ' <td class="contentCells contentCells-icon">';
                                                                    echo '  <a href="doceditor.php?fileID=' . urlencode($storeFile->name) . '&user=' . htmlentities($user) . '&action=comment&type=desktop" target="_blank">';
                                                                    echo '   <img src="css/images/comment.svg" alt="Open in editor for comment" title="Open in editor for comment" /></a>';
                                                                    echo ' </td>';
                                                                    if ($storeFile->documentType == "word") {
                                                                        echo ' <td class="contentCells contentCells-icon">';
                                                                        echo '  <a href="doceditor.php?fileID=' . urlencode($storeFile->name) . '&user=' . htmlentities($user) . '&action=review&type=desktop" target="_blank">';
                                                                        echo '   <img src="css/images/review.svg" alt="Open in editor for review" title="Open in editor for review" /></a>';
                                                                        echo ' </td>';
                                                                    } else if ($storeFile->documentType == "cell") {
                                                                        echo ' <td class="contentCells contentCells-icon">';
                                                                        echo '  <a href="doceditor.php?fileID=' . urlencode($storeFile->name) . '&user=' . htmlentities($user) . '&action=filter&type=desktop" target="_blank">';
                                                                        echo '   <img src="css/images/filter.svg" alt="Open in editor without access to change the filter" title="Open in editor without access to change the filter" /></a>';
                                                                        echo ' </td>';
                                                                    }
                                                                    if($storeFile->documentType!="word" && $storeFile->documentType!="cell"){
                                                                       echo ' <td class="contentCells contentCells-icon contentCellsEmpty"></td>';
                                                                    }
                                                                    if ($storeFile->documentType == "word") {
                                                                        echo ' <td class="contentCells contentCells-icon ">';
                                                                        echo '  <a href="doceditor.php?fileID=' . urlencode($storeFile->name) . '&user=' . htmlentities($user) . '&action=fillForms&type=desktop" target="_blank">';
                                                                        echo '   <img src="css/images/fill-forms.svg" alt="Open in editor for filling in forms" title="Open in editor for filling in forms" /></a>';
                                                                        echo ' </td>';
                                                                    }
                                                                    else{
                                                                        echo ' <td class="contentCells contentCells-icon "></td> ';
                                                                    }
                                                                    if ($storeFile->documentType == "word") {
                                                                        echo ' <td class="contentCells contentCells-icon contentCells-shift firstContentCellShift">';
                                                                        echo '  <a href="doceditor.php?fileID=' . urlencode($storeFile->name) . '&user=' . htmlentities($user) . '&action=blockcontent&type=desktop" target="_blank">';
                                                                        echo '   <img src="css/images/block-content.svg" alt="Open in editor without content control modification" title="Open in editor without content control modification" /></a>';
                                                                        echo ' </td>';
                                                                    } else{
                                                                       echo ' <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift"></td> ';
                                                                    }
                                                                    if($storeFile->documentType!="word" && $storeFile->documentType!="cell"){
                                                                        echo ' <td class="contentCells contentCells-icon"></td>';
                                                                    }
                                                                } else {
                                                                    echo '<td class="contentCells contentCells-shift contentCells-icon contentCellsEmpty" colspan="6"></td>';
                                                                }
                                                                echo ' <td class="contentCells contentCells-icon firstContentCellViewers">';
                                                                echo '  <a href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.htmlentities($user).'&action=view&type=desktop" target="_blank">';
                                                                echo '   <img src="css/images/desktop.svg" alt="Open in viewer for full size screens" title="Open in viewer for full size screens" /></a>';
                                                                echo ' </td>';
                                                                echo ' <td class="contentCells contentCells-icon">';
                                                                echo '  <a href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.htmlentities($user).'&action=view&type=mobile" target="_blank">';
                                                                echo '   <img src="css/images/mobile.svg" alt="Open in viewer for mobile devices" title="Open in viewer for mobile devices" /></a>';
                                                                echo ' </td>';
                                                                echo ' <td class="contentCells contentCells-icon contentCells-shift">';
                                                                echo '  <a href="doceditor.php?fileID='.urlencode($storeFile->name).'&user='.htmlentities($user).'&action=embedded&type=embedded" target="_blank">';
                                                                echo '   <img src="css/images/embeded.svg" alt="Open in embedded mode" title="Open in embedded mode" /></a>';
                                                                echo ' </td>';
                                                                echo ' <td class="contentCells contentCells-icon contentCells-shift downloadContentCellShift">';
                                                                echo '  <a href="webeditor-ajax.php?type=download&fileName='.urlencode($storeFile->name).'">';
                                                                echo '   <img class="icon-download" src="css/images/download.svg" alt="Download" title="Download" /></a>';
                                                                echo ' </td>';
                                                                echo ' <td class="contentCells contentCells-icon contentCells-shift">';
                                                                echo '  <a class="delete-file" data="'.$storeFile->name.'">';
                                                                echo '   <img class="icon-delete" src="css/images/delete.svg" alt="Delete" title="Delete" /></a>';
                                                                echo ' </td>';
                                                                echo '</tr>';
                                                            } ?>
                                                        </tbody>
                                                    </table>
                                                </div>
                                            </div>
                                    <?php
                                        } ?>
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
                <?php if (($GLOBALS['MODE']) != "view") { ?>
                    <div id="beginEdit" class="button orange disable">Edit</div>
                <?php } ?>
                <div id="beginView" class="button gray disable">View</div>
                <div id="beginEmbedded" class="button gray disable">Embedded view</div>
                <div id="cancelEdit" class="button gray">Cancel</div>
            </div>

            <span id="loadScripts" data-docs="<?php echo $GLOBALS['DOC_SERV_SITE_URL'].$GLOBALS['DOC_SERV_PRELOADER_URL'] ?>"></span>

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
                                &copy; Ascensio Systems SIA <?php echo date("Y") ?>. All rights reserved.
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </footer>
        </form>

        <script type="text/javascript" src="js/jquery-1.9.0.min.js"></script>
        <script type="text/javascript" src="js/jquery-ui.min.js"></script>
        <script type="text/javascript" src="js/jquery.blockUI.js"></script>
        <script type="text/javascript" src="js/jquery.iframe-transport.js"></script>
        <script type="text/javascript" src="js/jquery.fileupload.js"></script>
        <script type="text/javascript" src="js/jquery.dropdownToggle.js"></script>
        <script type="text/javascript" src="js/jscript.js"></script>
        <script type="text/javascript">
            var ConverExtList = '<?php echo implode(",", $GLOBALS["DOC_SERV_CONVERT"]) ?>';
            var EditedExtList = '<?php echo implode(",", $GLOBALS["DOC_SERV_EDITED"]) ?>';
        </script>
    </body>
</html>
