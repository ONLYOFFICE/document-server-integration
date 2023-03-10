<?php
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

namespace OnlineEditorsExamplePhp\Views;

use OnlineEditorsExamplePhp\Helpers\ConfigManager;
use OnlineEditorsExamplePhp\Helpers\ExampleUsers;
use OnlineEditorsExamplePhp\Helpers\JwtManager;
use function OnlineEditorsExamplePhp\doUpload;
use function OnlineEditorsExamplePhp\fileUri;
use function OnlineEditorsExamplePhp\getCallbackUrl;
use function OnlineEditorsExamplePhp\getCreateUrl;
use function OnlineEditorsExamplePhp\getDocEditorKey;
use function OnlineEditorsExamplePhp\getDocumentType;
use function OnlineEditorsExamplePhp\getDownloadUrl;
use function OnlineEditorsExamplePhp\getHistory;
use function OnlineEditorsExamplePhp\getStoragePath;
use function OnlineEditorsExamplePhp\getTemplateImageUrl;
use function OnlineEditorsExamplePhp\serverPath;
use function OnlineEditorsExamplePhp\tryGetDefaultByType;
use function OnlineEditorsExamplePhp\getCurUserHostAddress;

final class DocEditorView extends View
{
    public function __construct($request, $tempName = "docEditor")
    {
        parent::__construct($tempName);
        $externalUrl = $request["fileUrl"] ?? "";
        $confgManager = new ConfigManager();
        $jwtManager = new JwtManager();
        $userList = new ExampleUsers();
        $user = $userList->getUser($request["user"]);
        $isEnableDirectUrl = isset($request["directUrl"]) ? filter_var($request["directUrl"], FILTER_VALIDATE_BOOLEAN)
            : false;
        if (!empty($externalUrl)) {
            $filename = doUpload($externalUrl);
        } else { // if the file url doesn't exist, get file name and file extension
            $filename = basename($request["fileID"]);
        }
        $createExt = $request["fileExt"] ?? "";

        if (!empty($createExt)) {
            // and get demo file name by the extension
            $filename = tryGetDefaultByType($createExt, $user);

            // create the demo file url
            $new_url = "doceditor.php?fileID=" . $filename . "&user=" . $request["user"];
            header('Location: ' . $new_url, true);
            exit;
        }

        $fileuri = fileUri($filename, true);
        $fileuriUser = realpath($confgManager->getConfig("storagePath")) ===
        $confgManager->getConfig("storagePath") ?
            getDownloadUrl($filename) . "&dmode=emb" : fileUri($filename);
        $directUrl = getDownloadUrl($filename, false);
        $docKey = getDocEditorKey($filename);
        $filetype = mb_strtolower(pathinfo($filename, PATHINFO_EXTENSION));

        $ext = mb_strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION));
        $editorsMode = empty($request["action"]) ? "edit" : $request["action"];  // get the editors mode
        $canEdit = in_array($ext, $confgManager->getConfig("docServEdited"));  // check if the file can be edited
        if ((!$canEdit && $editorsMode == "edit"
                || $editorsMode == "fillForms")
            && in_array($ext, $confgManager->getConfig("docServFillforms"))
        ) {
            $editorsMode = "fillForms";
            $canEdit = true;
        }

        // check if the Submit form button is displayed or not
        $submitForm = $editorsMode == "fillForms" && $user->id == "uid-1" && !1;
        $mode = $canEdit && $editorsMode != "view" ? "edit" : "view";  // define if the editing mode is edit or view
        $type = empty($request["type"]) ? "desktop" : $request["type"];
        if ($user->id == "uid-0" && $editorsMode == "view") {
            $canEdit = false;
        }

        $templatesImageUrl = getTemplateImageUrl($filename); // templates image url in the "From Template" section
        $createUrl = getCreateUrl($filename, $user->id, $type);
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
            "documentType" => getDocumentType($filename),
            "document" => [
                "title" => $filename,
                "url" => getDownloadUrl($filename),
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
                    "protect" => !in_array("protect", $user->deniedPermissions),
                ],
                "referenceData" => [
                    "fileKey" => $user->id != "uid-0" ? json_encode([
                        "fileName" => $filename,
                        "userAddress" =>  getCurUserHostAddress()
                    ]) : null,
                    "instanceId" => serverPath(),
                ],
            ],
            "editorConfig" => [
                "actionLink" => empty($request["actionLink"]) ? null : json_decode($request["actionLink"]),
                "mode" => $mode,
                "lang" => empty($_COOKIE["ulang"]) ? "en" : $_COOKIE["ulang"],
                "callbackUrl" => getCallbackUrl($filename),  // absolute URL to the document storage service
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
                    // the absolute URL to the document serving as a source file for the document embedded into
                    // the web page
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
                        "url" => serverPath(),
                    ],
                ],
            ],
        ];

        // an image for inserting
        $dataInsertImage = $isEnableDirectUrl ? [
            "fileType" => "png",
            "url" => serverPath(true) . "/css/images/logo.png",
            "directUrl" => serverPath(false) . "/css/images/logo.png",
        ] : [
            "fileType" => "png",
            "url" => serverPath(true) . "/css/images/logo.png",
        ];

        // a document for comparing
        $dataCompareFile = $isEnableDirectUrl ? [
            "fileType" => "docx",
            "url" => serverPath(true) . "/webeditor-ajax.php?type=assets&name=sample.docx",
            "directUrl" => serverPath(false) . "/webeditor-ajax.php?type=assets&name=sample.docx",
        ] : [
            "fileType" => "docx",
            "url" => serverPath(true) . "/webeditor-ajax.php?type=assets&name=sample.docx",
        ];

        // recipients data for mail merging
        $dataMailMergeRecipients = $isEnableDirectUrl ? [
            "fileType" => "csv",
            "url" => serverPath(true) . "/webeditor-ajax.php?type=csv",
            "directUrl" => serverPath(false) . "/webeditor-ajax.php?type=csv",
        ] : [
            "fileType" => "csv",
            "url" => serverPath(true) . "/webeditor-ajax.php?type=csv",
        ];

        // users data for mentions
        $usersForMentions = $user->id != "uid-0" ? $userList->getUsersForMentions($user->id) : null;

        // check if the secret key to generate token exists
        if ($jwtManager->isJwtEnabled()) {
            $config["token"] = $jwtManager->jwtEncode($config);  // encode config into the token
            // encode the dataInsertImage object into the token
            $dataInsertImage["token"] = $jwtManager->jwtEncode($dataInsertImage);
            // encode the dataCompareFile object into the token
            $dataCompareFile["token"] = $jwtManager->jwtEncode($dataCompareFile);
            // encode the dataMailMergeRecipients object into the token
            $dataMailMergeRecipients["token"] = $jwtManager->jwtEncode($dataMailMergeRecipients);
        }
        $out = getHistory($filename, $filetype, $docKey, $fileuri, $isEnableDirectUrl);
        $history = $out[0];
        $historyData = $out[1];
        $historyLayout = "";
        if ($user->id != "uid-0") {
            if ($history != null && $historyData != null) {
                $historyLayout .= " config.events['onRequestHistory'] = function () {
                        // show the document version history
                        docEditor.refreshHistory(".json_encode($history).");};";
                $historyLayout .= " config.events['onRequestHistoryData'] = function (event) {
                        var ver = event.data;
                        var histData = ".json_encode($historyData).";".
                    "docEditor.setHistoryData(histData[ver - 1]);};
                     config.events['onRequestHistoryClose'] = function () {
                        document.location.reload();
                    };";
            }
            $historyLayout .= "// add mentions for not anonymous users
                config.events['onRequestUsers'] = function () {
                    docEditor.setUsers({  // set a list of users to mention in the comments
                        \"users\": {usersForMentions}
                    });
                };
                // the user is mentioned in a comment
                config.events['onRequestSendNotify'] = function (event) {
                    event.data.actionLink = replaceActionLink(location.href, JSON.stringify(event.data.actionLink));
                    var data = JSON.stringify(event.data);
                    innerAlert(\"onRequestSendNotify: \" + data);
                };
                // prevent file renaming for anonymous users
                config.events['onRequestRename'] = onRequestRename;";
        }
        $this->tagsValues = [
            "docType" => getDocumentType($filename),
            "apiUrl" => $confgManager->getConfig("docServSiteUrl").$confgManager->getConfig("docServApiUrl"),
            "dataInsertImage" => mb_strimwidth(
                json_encode($dataInsertImage),
                1,
                mb_strlen(json_encode($dataInsertImage)) - 2
            ),
            "dataCompareFile" => json_encode($dataCompareFile),
            "dataMailMergeRecipients" => json_encode($dataMailMergeRecipients),
            "fileNotFoundAlert" => !file_exists(getStoragePath($filename)) ? "alert('File not found'); return;" : "",
            "config" => json_encode($config),
            "history" => $historyLayout,
            "usersForMentions" => json_encode($usersForMentions),
            ];
    }
}
