<?php

namespace OnlineEditorsExamplePhp;

use OnlineEditorsExamplePhp\Helpers\ConfigManager;
use OnlineEditorsExamplePhp\Helpers\ExampleUsers;
use OnlineEditorsExamplePhp\Helpers\FileUtility;
use OnlineEditorsExamplePhp\Helpers\JwtManager;
use OnlineEditorsExamplePhp\Helpers\Users;
use OnlineEditorsExamplePhp\Helpers\Utils;

/**
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
 */

require_once dirname(__FILE__) . '/vendor/autoload.php';

$users = new ExampleUsers();
$configManager = new ConfigManager();
$fileUtility = new FileUtility();
$utils = new Utils();
$user = $users->getUser($_GET["user"]);
$isEnableDirectUrl = isset($_GET["directUrl"]) ? filter_var($_GET["directUrl"], FILTER_VALIDATE_BOOLEAN) : false;

// get the file url and upload it
$externalUrl = $_GET["fileUrl"] ?? "";
if (!empty($externalUrl)) {
    try {
        $filename = $utils->doUpload($externalUrl);
    } catch (\Exception $e) {
    }
} else { // if the file url doesn't exist, get file name and file extension
    $filename = basename($_GET["fileID"]);
}
$createExt = $_GET["fileExt"] ?? "";

if (!empty($createExt)) {
    // and get demo file name by the extension
    $filename = $fileUtility->tryGetDefaultByType($createExt, $user);

    // create the demo file url
    $new_url = "doceditor.php?fileID=" . $filename . "&user=" . $_GET["user"];
    header('Location: ' . $new_url, true);
    exit;
}

$fileuri = $fileUtility->fileUri($filename, true);
$fileuriUser = realpath($configManager->getConfig("storagePath")) === $configManager->getConfig("storagePath") ?
    $fileUtility->getDownloadUrl($filename) . "&dmode=emb" : $fileUtility->fileUri($filename);
$directUrl = $fileUtility->getDownloadUrl($filename, false);
$docKey = $fileUtility->getDocEditorKey($filename);
$filetype = mb_strtolower(pathinfo($filename, PATHINFO_EXTENSION));

$ext = mb_strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION));
$editorsMode = empty($_GET["action"]) ? "edit" : $_GET["action"];  // get the editors mode
$canEdit = in_array($ext, $configManager->getConfig("docServEdited"));  // check if the file can be edited
if ((!$canEdit && $editorsMode == "edit"
        || $editorsMode == "fillForms")
    && in_array($ext, $configManager->getConfig("docServFillforms"))
) {
    $editorsMode = "fillForms";
    $canEdit = true;
}

// check if the Submit form button is displayed or not
$submitForm = $editorsMode == "fillForms" && $user->id == "uid-1" && !1;
$mode = $canEdit && $editorsMode != "view" ? "edit" : "view";  // define if the editing mode is edit or view
$type = empty($_GET["type"]) ? "desktop" : $_GET["type"];

$templatesImageUrl = $fileUtility->getTemplateImageUrl($filename); // templates image url in the "From Template" section
$createUrl = $fileUtility->getCreateUrl($filename, $user->id, $type);
$templates = [
    [
        "image" => "",
        "title" => "Blank",
        "url" => $createUrl,
    ],
    [
        "image" => $templatesImageUrl,
        "title" => "With sample content",
        "url" => $createUrl . "&sample=true",
    ],
];

// specify the document config
$config = [
    "type" => $type,
    "documentType" => $fileUtility->getDocumentType($filename),
    "document" => [
        "title" => $filename,
        "url" => $fileUtility->getDownloadUrl($filename),
        "directUrl" => $isEnableDirectUrl ? $directUrl : "",
        "fileType" => $filetype,
        "key" => $docKey,
        "info" => [
            "owner" => "Me",
            "uploaded" => date('d.m.y'),
            "favorite" => $user->favorite,
        ],
        "permissions" => [  // the permission for the document to be edited and downloaded or not
            "comment" => $editorsMode != "view" && $editorsMode
                != "fillForms" && $editorsMode != "embedded" && $editorsMode != "blockcontent",
            "copy" => !in_array("copy", $user->deniedPermissions),
            "download" => !in_array("download", $user->deniedPermissions),
            "edit" => $canEdit && ($editorsMode == "edit" ||
                    $editorsMode == "view" || $editorsMode == "filter" || $editorsMode == "blockcontent"),
            "print" => !in_array("print", $user->deniedPermissions),
            "fillForms" => $editorsMode != "view" && $editorsMode != "comment"
                && $editorsMode != "embedded" && $editorsMode != "blockcontent",
            "modifyFilter" => $editorsMode != "filter",
            "modifyContentControl" => $editorsMode != "blockcontent",
            "review" => $canEdit && ($editorsMode == "edit" || $editorsMode == "review"),
            "chat" => $user->id != "uid-0",
            "reviewGroups" => $user->reviewGroups,
            "commentGroups" => $user->commentGroups,
            "userInfoGroups" => $user->userInfoGroups,
        ],
    ],
    "editorConfig" => [
        "actionLink" => empty($_GET["actionLink"]) ? null : json_decode($_GET["actionLink"]),
        "mode" => $mode,
        "lang" => empty($_COOKIE["ulang"]) ? "en" : $_COOKIE["ulang"],
        "callbackUrl" => $fileUtility->getCallbackUrl($filename),  // absolute URL to the document storage service
        "coEditing" => $editorsMode == "view" && $user->id == "uid-0" ? [
            "mode" => "strict",
            "change" => false,
        ] : null,
        "createUrl" => $user->id != "uid-0" ? $createUrl : null,
        "templates" => $user->templates ? $templates : null,
        "user" => [  // the user currently viewing or editing the document
            "id" => $user->id != "uid-0" ? $user->id : null,
            "name" => $user->name,
            "group" => $user->group,
        ],
        "embedded" => [  // the parameters for the embedded document type
            // the absolute URL that will allow the document to be saved onto the user personal computer
            "saveUrl" => $directUrl,
            // the absolute URL to the document serving as a source file for the document embedded into the web page
            "embedUrl" => $directUrl,
            // the absolute URL that will allow other users to share this document
            "shareUrl" => $directUrl,
            "toolbarDocked" => "top",  // the place for the embedded viewer toolbar (top or bottom)
        ],
        "customization" => [  // the parameters for the editor interface
            "about" => true,  // the About section display
            "comments" => true,
            "feedback" => true,  // the Feedback & Support menu button display
            // adds the request for the forced file saving to the callback handler when saving the document
            "forcesave" => false,
            "submitForm" => $submitForm,  // if the Submit form button is displayed or not
            "goback" => [  // settings for the Open file location menu button and upper right corner button
                // the absolute URL to the website address which will be opened
                // when clicking the Open file location menu button
                "url" => $fileUtility->serverPath(),
            ],
        ],
    ],
];

// an image for inserting
$dataInsertImage = $isEnableDirectUrl ? [
    "fileType" => "png",
    "url" => $fileUtility->serverPath(true) . "/css/images/logo.png",
    "directUrl" => $fileUtility->serverPath(false) . "/css/images/logo.png",
] : [
    "fileType" => "png",
    "url" => $fileUtility->serverPath(true) . "/css/images/logo.png",
];

// a document for comparing
$dataCompareFile = $isEnableDirectUrl ? [
    "fileType" => "docx",
    "url" => $fileUtility->serverPath(true) . "/webeditor-ajax.php?type=assets&name=sample.docx",
    "directUrl" => $fileUtility->serverPath(false) . "/webeditor-ajax.php?type=assets&name=sample.docx",
] : [
    "fileType" => "docx",
    "url" => $fileUtility->serverPath(true) . "/webeditor-ajax.php?type=assets&name=sample.docx",
];

// recipients data for mail merging
$dataMailMergeRecipients = $isEnableDirectUrl ? [
    "fileType" => "csv",
    "url" => $fileUtility->serverPath(true) . "/webeditor-ajax.php?type=csv",
    "directUrl" => $fileUtility->serverPath(false) . "/webeditor-ajax.php?type=csv",
] : [
    "fileType" => "csv",
    "url" => $fileUtility->serverPath(true) . "/webeditor-ajax.php?type=csv",
];

// users data for mentions
$usersForMentions = $user->id != "uid-0" ? $users->getUsersForMentions($user->id) : null;

// check if the secret key to generate token exists
$jwtManager = new JwtManager();
if ($jwtManager->isJwtEnabled()) {
    $config["token"] = $jwtManager->jwtEncode($config);  // encode config into the token
    // encode the dataInsertImage object into the token
    $dataInsertImage["token"] = $jwtManager->jwtEncode($dataInsertImage);
    // encode the dataCompareFile object into the token
    $dataCompareFile["token"] = $jwtManager->jwtEncode($dataCompareFile);
    // encode the dataMailMergeRecipients object into the token
    $dataMailMergeRecipients["token"] = $jwtManager->jwtEncode($dataMailMergeRecipients);
}

?>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1,
            maximum-scale=1, minimum-scale=1, user-scalable=no, minimal-ui" />
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="mobile-web-app-capable" content="yes" />
    <link rel="icon" href="css/images/<?php echo $fileUtility->getDocumentType($filename) ?>.ico" type="image/x-icon" />
    <title>ONLYOFFICE</title>

    <style>
        html {
            height: 100%;
            width: 100%;
        }

        body {
            background: #fff;
            color: #333;
            font-family: Arial, Tahoma,sans-serif;
            font-size: 12px;
            font-weight: normal;
            height: 100%;
            margin: 0;
            overflow-y: hidden;
            padding: 0;
            text-decoration: none;
        }

        form {
            height: 100%;
        }

        div {
            margin: 0;
            padding: 0;
        }
    </style>

    <script type="text/javascript" src="
        <?php echo $configManager->getConfig("docServSiteUrl").
        $configManager->getConfig("docServApiUrl") ?>">
    </script>

    <script type="text/javascript">

        var docEditor;
        var config;

        var innerAlert = function (message, inEditor) {
            if (console && console.log)
                console.log(message);
            if (inEditor && docEditor)
                docEditor.showMessage(message);
        };

        // the application is loaded into the browser
        var onAppReady = function () {
            innerAlert("Document editor ready");
        };

        // the document is modified
        var onDocumentStateChange = function (event) {
            var title = document.title.replace(/\*$/g, "");
            document.title = title + (event.data ? "*" : "");
        };

        // the user is trying to switch the document from the viewing into the editing mode
        var onRequestEditRights = function () {
            location.href = location.href.replace(RegExp("action=view\&?", "i"), "");
        };

        // an error or some other specific event occurs
        var onError = function (event) {
            if (event)
                innerAlert(event.data);
        };

        // the document is opened for editing with the old document.key value
        var onOutdatedVersion = function (event) {
            location.reload(true);
        };

        // replace the link to the document which contains a bookmark
        var replaceActionLink = function(href, linkParam) {
            var link;
            var actionIndex = href.indexOf("&actionLink=");
            if (actionIndex != -1) {
                var endIndex = href.indexOf("&", actionIndex + "&actionLink=".length);
                if (endIndex != -1) {
                    link = href.substring(0, actionIndex) + href.substring(endIndex) +
                        "&actionLink=" + encodeURIComponent(linkParam);
                } else {
                    link = href.substring(0, actionIndex) + "&actionLink=" + encodeURIComponent(linkParam);
                }
            } else {
                link = href + "&actionLink=" + encodeURIComponent(linkParam);
            }
            return link;
        }

        // the user is trying to get link for opening the document which contains a bookmark,
        // scrolling to the bookmark position
        var onMakeActionLink = function (event) {
            var actionData = event.data;
            var linkParam = JSON.stringify(actionData);
            // set the link to the document which contains a bookmark
            docEditor.setActionLink(replaceActionLink(location.href, linkParam));
        };

        // the meta information of the document is changed via the meta command
        var onMetaChange = function (event) {
            if (event.data.favorite) {
                var favorite = !!event.data.favorite;
                var title = document.title.replace(/^\☆/g, "");
                document.title = (favorite ? "☆" : "") + title;
                docEditor.setFavorite(favorite);  // change the Favorite icon state
            }

            innerAlert("onMetaChange: " + JSON.stringify(event.data));
        };

        // the user is trying to insert an image by clicking the Image from Storage button
        var onRequestInsertImage = function(event) {
            docEditor.insertImage({  // insert an image into the file
                "c": event.data.c,
                <?php echo mb_strimwidth(
                    json_encode($dataInsertImage),
                    1,
                    mb_strlen(json_encode($dataInsertImage)) - 2
                )?>
            })
        };

        // the user is trying to select document for comparing by clicking the Document from Storage button
        var onRequestCompareFile = function() {
            docEditor.setRevisedFile(<?php echo json_encode($dataCompareFile)?>);  // select a document for comparing
        };

        // the user is trying to select recipients data by clicking the Mail merge button
        var onRequestMailMergeRecipients = function (event) {
            // insert recipient data for mail merge into the file
            docEditor.setMailMergeRecipients(<?php echo json_encode($dataMailMergeRecipients) ?>);
        };

        var onRequestSaveAs = function (event) {  //  the user is trying to save file by clicking Save Copy as... button
            var title = event.data.title;
            var url = event.data.url;
            var data = {
                title: title,
                url: url
            };
            let xhr = new XMLHttpRequest();
            xhr.open("POST", "webeditor-ajax.php?type=saveas");
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.send(JSON.stringify(data));
            xhr.onload = function () {
                innerAlert(xhr.responseText);
                innerAlert(JSON.parse(xhr.responseText.substring(xhr.responseText.indexOf("{"))).file, true);
            }
        };

        var onRequestRename = function(event) { //  the user is trying to rename file by clicking Rename... button
            innerAlert("onRequestRename: " + JSON.stringify(event.data));

            var newfilename = event.data;
            var data = {
                newfilename: newfilename,
                dockey: config.document.key,
                ext: config.document.fileType
            };

            let xhr = new XMLHttpRequest();
            xhr.open("POST", "webeditor-ajax.php?type=rename");
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.send(JSON.stringify(data));
            xhr.onload = function () {
                innerAlert(xhr.responseText);
            }
        };

        var сonnectEditor = function () {

            <?php
            if (!file_exists($fileUtility->getStoragePath($filename))) {
                echo "alert('File not found'); return;";
            }
            ?>

            config = <?php echo json_encode($config) ?>;

            config.width = "100%";
            config.height = "100%";

            config.events = {
                'onAppReady': onAppReady,
                'onDocumentStateChange': onDocumentStateChange,
                'onRequestEditRights': onRequestEditRights,
                'onError': onError,
                'onOutdatedVersion': onOutdatedVersion,
                'onMakeActionLink': onMakeActionLink,
                'onMetaChange': onMetaChange,
                'onRequestInsertImage': onRequestInsertImage,
                'onRequestCompareFile': onRequestCompareFile,
                'onRequestMailMergeRecipients': onRequestMailMergeRecipients,
            };

            <?php
            $out = $fileUtility->getHistory($filename, $filetype, $docKey, $fileuri, $isEnableDirectUrl);
            $history = $out[0];
            $historyData = $out[1];
            ?>

            <?php if ($user->id != "uid-0") { ?>
            <?php if ($history != null && $historyData != null) { ?>
            // the user is trying to show the document version history
            config.events['onRequestHistory'] = function () {
                // show the document version history
                docEditor.refreshHistory(<?php echo json_encode($history) ?>);
            };
            // the user is trying to click the specific document version in the document version history
            config.events['onRequestHistoryData'] = function (event) {
                var ver = event.data;
                var histData = <?php echo json_encode($historyData) ?>;
                // send the link to the document for viewing the version history
                docEditor.setHistoryData(histData[ver - 1]);
            };
            // the user is trying to go back to the document from viewing the document version history
            config.events['onRequestHistoryClose'] = function () {
                document.location.reload();
            };
            <?php } ?>
            // add mentions for not anonymous users
            config.events['onRequestUsers'] = function () {
                docEditor.setUsers({  // set a list of users to mention in the comments
                    "users": <?php echo json_encode($usersForMentions) ?>
                });
            };
            // the user is mentioned in a comment
            config.events['onRequestSendNotify'] = function (event) {
                event.data.actionLink = replaceActionLink(location.href, JSON.stringify(event.data.actionLink));
                var data = JSON.stringify(event.data);
                innerAlert("onRequestSendNotify: " + data);
            };
            // prevent file renaming for anonymous users
            config.events['onRequestRename'] = onRequestRename;
            <?php } ?>

            if (config.editorConfig.createUrl) {
                config.events.onRequestSaveAs = onRequestSaveAs;
            };

            if ((config.document.fileType === "docxf" || config.document.fileType === "oform")
                && DocsAPI.DocEditor.version().split(".")[0] < 7) {
                innerAlert("Please update ONLYOFFICE Docs to version 7.0 to work on fillable forms online.");
                return;
            }

            docEditor = new DocsAPI.DocEditor("iframeEditor", config);
        };

        if (window.addEventListener) {
            window.addEventListener("load", сonnectEditor);
        } else if (window.attachEvent) {
            window.attachEvent("load", сonnectEditor);
        }

    </script>
</head>
<body>
<form id="form1">
    <div id="iframeEditor">
    </div>
</form>
</body>
</html>