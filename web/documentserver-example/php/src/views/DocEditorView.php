<?php
/**
 * (c) Copyright Ascensio System SIA 2025
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

namespace Example\Views;

use Example\Configuration\ConfigurationManager;
use Example\Format\FormatManager;
use Example\Helpers\ExampleUsers;
use Example\Helpers\JwtManager;
use function Example\doUpload;
use function Example\fileUri;
use function Example\getCallbackUrl;
use function Example\getCreateUrl;
use function Example\getDocEditorKey;
use function Example\getDocumentType;
use function Example\getDownloadUrl;
use function Example\getHistory;
use function Example\getStoragePath;
use function Example\getTemplateImageUrl;
use function Example\serverPath;
use function Example\tryGetDefaultByType;
use function Example\getCurUserHostAddress;

final class DocEditorView extends View
{
    public function __construct($request, $tempName = "docEditor")
    {
        parent::__construct($tempName);

        $configManager = new ConfigurationManager();
        $formatManager = new FormatManager();

        $externalUrl = $request["fileUrl"] ?? "";
        $jwtManager = new JwtManager();
        $userList = new ExampleUsers();
        $fileId = $request["fileID"] ?? "";
        $user = $userList->getUser($request["user"] ?? null);
        $isEnableDirectUrl = isset($request["directUrl"]) ? filter_var($request["directUrl"], FILTER_VALIDATE_BOOLEAN)
            : false;
        if (!empty($externalUrl)) {
            $filename = doUpload($externalUrl);
        } else { // if the file url doesn't exist, get file name and file extension
            $filename = basename($fileId);
        }
        $createExt = $request["fileExt"] ?? "";

        if (!empty($createExt)) {
            // and get demo file name by the extension
            $filename = tryGetDefaultByType($createExt, $user);

            // create the demo file url
            $newURL = "editor?action=edit&fileID=" . $filename . "&user=" . $request["user"];
            header('Location: ' . $newURL, true);
            exit;
        }

        $fileuri = fileUri($filename, true);
        $directUrl = getDownloadUrl($filename, false);
        $docKey = getDocEditorKey($filename);
        $filetype = mb_strtolower(pathinfo($filename, PATHINFO_EXTENSION));

        $ext = mb_strtolower(pathinfo($filename, PATHINFO_EXTENSION));
        $editorsMode = empty($request["action"]) ? "edit" : $request["action"];
        $canEdit = in_array($ext, $formatManager->editableExtensions());  // check if the file can be edited
        if ((!$canEdit && $editorsMode == "edit" || $editorsMode == "fillForms")
            && in_array($ext, $formatManager->fillableExtensions())) {
            $editorsMode = "fillForms";
            $canEdit = true;
        }

        // check if the Submit form button is displayed or not
        $submitForm = $editorsMode != "view" && $user->id == "uid-1";
        $mode = $canEdit && $editorsMode != "view" ? "edit" : "view";  // define if the editing mode is edit or view
        $type = empty($request["type"]) ? "desktop" : $request["type"];

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

        if ($user->goback !== null) {
            $user->goback["url"] = serverPath();
        }

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
                        && $editorsMode != "blockcontent",
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
                    "image" => $user->avatar ? serverPath(false) . "/assets/images/" . $user->id . ".png" : null
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
                    // settings for the Open file location menu button and upper right corner button
                    "goback" => $user->goback !== null ? $user->goback : "",
                    "close" => $user->close != null ? $user->close : "",
                ],
            ],
        ];

        // an image for inserting
        $dataInsertImage = $isEnableDirectUrl ? [
            "fileType" => "svg",
            "url" => serverPath(true) . "/assets/images/logo.svg",
            "directUrl" => serverPath(false) . "/assets/images/logo.svg",
        ] : [
            "fileType" => "svg",
            "url" => serverPath(true) . "/assets/images/logo.svg",
        ];

        // a document for comparing
        $dataDocument = $isEnableDirectUrl ? [
            "fileType" => "docx",
            "url" => serverPath(true) . "/assets/document-templates/sample/sample.docx",
            "directUrl" => serverPath(false) . "/assets/document-templates/sample/sample.docx",
        ] : [
            "fileType" => "docx",
            "url" => serverPath(true) . "/assets/document-templates/sample/sample.docx",
        ];

        // recipients data for mail merging
        $dataSpreadsheet = $isEnableDirectUrl ? [
            "fileType" => "csv",
            "url" => serverPath(true) . "/assets/document-templates/sample/csv.csv",
            "directUrl" => serverPath(false) . "/assets/document-templates/sample/csv.csv",
        ] : [
            "fileType" => "csv",
            "url" => serverPath(true) . "/assets/document-templates/sample/csv.csv",
        ];

        // users data for mentions
        $usersForMentions = $user->id != "uid-0" ? $userList->getUsersForMentions($user->id) : null;

        // users data for protect
        $usersForProtect = $user->id != "uid-0" ? $userList->getUsersForProtect($user->id) : null;

        $usersInfo = [];
        if ($user->id != 'uid-0') {
            foreach ($userList->getAllUsers() as $userInfo) {
                $u = $userInfo;
                $u->image = $userInfo->avatar ? serverPath(false) . "/assets/images/" . $userInfo->id . ".png" : null;
                array_push($usersInfo, $u);
            }
        }

        // check if the secret key to generate token exists
        if ($jwtManager->isJwtEnabled()) {
            $config["token"] = $jwtManager->jwtEncode($config);  // encode config into the token
            // encode the dataInsertImage object into the token
            $dataInsertImage["token"] = $jwtManager->jwtEncode($dataInsertImage);
            // encode the dataDocument object into the token
            $dataDocument["token"] = $jwtManager->jwtEncode($dataDocument);
            // encode the dataSpreadsheet object into the token
            $dataSpreadsheet["token"] = $jwtManager->jwtEncode($dataSpreadsheet);
        }

        $historyLayout = "";

        if ($user->id == "uid-3") {
            $historyLayout .= "config.events['onRequestHistoryClose'] = null;
                config.events['onRequestRestore'] = null;";
        }

        if ($user->id != "uid-0") {
            $historyLayout .= "// add mentions for not anonymous users
                config.events['onRequestRefreshFile'] = onRequestRefreshFile;
                config.events['onRequestUsers'] = onRequestUsers;
                config.events['onRequestSaveAs'] = onRequestSaveAs;
                // the user is mentioned in a comment
                config.events['onRequestSendNotify'] = onRequestSendNotify;
                // prevent file renaming for anonymous users
                config.events['onRequestRename'] = onRequestRename;
                // prevent switch the document from the viewing into the editing mode for anonymous users
                config.events['onRequestEditRights'] = onRequestEditRights;
                config.events['onRequestHistory'] = onRequestHistory;
                config.events['onRequestHistoryData'] = onRequestHistoryData;
                config.events['onRequestClose'] = onRequestClose;
                config.events['onRequestReferenceSource'] = onRequestReferenceSource;";
            if ($user->id != "uid-3") {
                $historyLayout .= "config.events['onRequestHistoryClose'] = onRequestHistoryClose;
                config.events['onRequestRestore'] = onRequestRestore;";
            }
        }
        $this->tagsValues = [
            "fileName" => $filename,
            "docType" => getDocumentType($filename),
            "apiUrl" => $configManager->documentServerAPIURL()->string(),
            "dataInsertImage" => mb_strimwidth(
                json_encode($dataInsertImage),
                1,
                mb_strlen(json_encode($dataInsertImage)) - 2
            ),
            "dataDocument" => json_encode($dataDocument),
            "dataSpreadsheet" => json_encode($dataSpreadsheet),
            "fileNotFoundAlert" => !file_exists(getStoragePath($filename)) ? "alert('File not found'); return;" : "",
            "config" => json_encode($config),
            "history" => $historyLayout,
            "usersForMentions" => json_encode($usersForMentions),
            "usersInfo" => json_encode($usersInfo),
            "usersForProtect" => json_encode($usersForProtect),
            ];
    }
}
