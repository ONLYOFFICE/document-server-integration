<?php
    /**
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
     */

    require_once( dirname(__FILE__) . '/config.php' );
    require_once( dirname(__FILE__) . '/common.php' );
    require_once( dirname(__FILE__) . '/functions.php' );
    require_once( dirname(__FILE__) . '/jwtmanager.php' );

    $filename;

    $externalUrl = $_GET["fileUrl"];
    if (!empty($externalUrl))
    {
        $filename = DoUpload($externalUrl);
    }
    else
    {
        $filename = basename($_GET["fileID"]);
    }
    $createExt = $_GET["fileExt"];

    if (!empty($createExt))
    {
        $filename = tryGetDefaultByType($createExt);

        $new_url = "doceditor.php?fileID=" . $filename . "&user=" . $_GET["user"];
        header('Location: ' . $new_url, true);
        exit;
    }

    $fileuri = FileUri($filename, true);
    $fileuriUser = FileUri($filename);
    $docKey = getDocEditorKey($filename);
    $filetype = strtolower(pathinfo($filename, PATHINFO_EXTENSION));

    $uid = empty($_GET["user"]) ? "0" : $_GET["user"];
    $uname = "";
    switch ($uid) {
        case 0:
            $uname = "John Smith";
            break;
        case 1:
            $uname = "Mark Pottato";
            break;
        case 2:
            $uname = "Hamish Mitchell";
            break;
    }

    $editorsMode = empty($_GET["action"]) ? "edit" : $_GET["action"];
    $canEdit = in_array(strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION)), $GLOBALS['DOC_SERV_EDITED']);
    $mode = $canEdit && $editorsMode != "view" ? "edit" : "view";

    $config = [
        "type" => empty($_GET["type"]) ? "desktop" : $_GET["type"],
        "documentType" => getDocumentType($filename),
        "document" => [
            "title" => $filename,
            "url" => $fileuri,
            "fileType" => $filetype,
            "key" => $docKey,
            "info" => [
                "author" => "Me",
                "created" => date('d.m.y')
            ],
            "permissions" => [
                "comment" => $editorsMode != "view" && $editorsMode != "fillForms" && $editorsMode != "embedded" && $editorsMode != "blockcontent",
                "download" => true,
                "edit" => $canEdit && ($editorsMode == "edit" || $editorsMode == "filter" || $editorsMode == "blockcontent"),
                "fillForms" => $editorsMode != "view" && $editorsMode != "comment" && $editorsMode != "embedded" && $editorsMode != "blockcontent",
                "modifyFilter" => $editorsMode != "filter",
                "modifyContentControl" => $editorsMode != "blockcontent",
                "review" => $editorsMode == "edit" || $editorsMode == "review"
            ]
        ],
        "editorConfig" => [
            "actionLink" => empty($_GET["actionLink"]) ? null : json_decode($_GET["actionLink"]),
            "mode" => $mode,
            "lang" => empty($_COOKIE["ulang"]) ? "en" : $_COOKIE["ulang"],
            "callbackUrl" => getCallbackUrl($filename),
            "user" => [
                "id" => $uid,
                "name" => $uname
            ],
            "embedded" => [
                "saveUrl" => $fileuriUser,
                "embedUrl" => $fileuriUser,
                "shareUrl" => $fileuriUser,
                "toolbarDocked" => "top",
            ],
            "customization" => [
                "about" => true,
                "feedback" => true,
                "goback" => [
                    "url" => serverPath(),
                ]
            ]
        ]
    ];

    if (isJwtEnabled()) {
        $config["token"] = jwtEncode($config);
    }

    function tryGetDefaultByType($createExt) {
        $demoName = ($_GET["sample"] ? "demo." : "new.") . $createExt;
        $demoFilename = GetCorrectName($demoName);

        if(!@copy(dirname(__FILE__) . DIRECTORY_SEPARATOR . "app_data" . DIRECTORY_SEPARATOR . $demoName, getStoragePath($demoFilename)))
        {
            sendlog("Copy file error to ". getStoragePath($demoFilename), "common.log");
            //Copy error!!!
        }

        createMeta($demoFilename, $_GET["user"]);

        return $demoFilename;
    }

    function getCallbackUrl($fileName) {
        return serverPath(TRUE) . '/'
                    . "webeditor-ajax.php"
                    . "?type=track"
                    . "&fileName=" . urlencode($fileName)
                    . "&userAddress=" . getClientIp();
    }

    function getHistory($filename, $filetype, $docKey, $fileuri) {
        $histDir = getHistoryDir(getStoragePath($filename));

        if (getFileVersion($histDir) > 0) {
            $curVer = getFileVersion($histDir);

            $hist = [];
            $histData = [];

            for ($i = 0; $i <= $curVer; $i++) {
                $obj = [];
                $dataObj = [];
                $verDir = getVersionDir($histDir, $i + 1);

                $key = $i == $curVer ? $docKey : file_get_contents($verDir . DIRECTORY_SEPARATOR . "key.txt");
                $obj["key"] = $key;
                $obj["version"] = $i;

                if ($i == 0) {
                    $createdInfo = file_get_contents($histDir . DIRECTORY_SEPARATOR . "createdInfo.json");
                    $json = json_decode($createdInfo, true);

                    $obj["created"] = $json["created"];
                    $obj["user"] = [
                        "id" => $json["uid"],
                        "name" => $json["name"]
                    ];
                }

                $prevFileName = $verDir . DIRECTORY_SEPARATOR . "prev." . $filetype;
                $prevFileName = substr($prevFileName, strlen(getStoragePath("")));
                $dataObj["key"] = $key;
                $dataObj["url"] = $i == $curVer ? $fileuri : getVirtualPath(true) . str_replace("%5C", "/", rawurlencode($prevFileName));
                $dataObj["version"] = $i;

                if ($i > 0) {
                    $changes = json_decode(file_get_contents(getVersionDir($histDir, $i) . DIRECTORY_SEPARATOR . "changes.json"), true);
                    $change = $changes["changes"][0];

                    $obj["changes"] = $changes["changes"];
                    $obj["serverVersion"] = $changes["serverVersion"];
                    $obj["created"] = $change["created"];
                    $obj["user"] = $change["user"];

                    $prev = $histData[$i -1];
                    $dataObj["previous"] = [
                        "key" => $prev["key"],
                        "url" => $prev["url"]
                    ];
                    $changesUrl = getVersionDir($histDir, $i) . DIRECTORY_SEPARATOR . "diff.zip";
                    $changesUrl = substr($changesUrl, strlen(getStoragePath("")));

                    $dataObj["changesUrl"] = getVirtualPath(true) . str_replace("%5C", "/", rawurlencode($changesUrl));
                }

                array_push($hist, $obj);
                $histData[$i] = $dataObj;
            }

            $out = [];
            array_push($out, [
                    "currentVersion" => $curVer,
                    "history" => $hist
                ],
                $histData);
            return $out;
        }
    }

?>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=no, minimal-ui" />
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="mobile-web-app-capable" content="yes" />
    <link rel="icon" href="./favicon.ico" type="image/x-icon" />
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

    <script type="text/javascript" src="<?php echo $GLOBALS["DOC_SERV_API_URL"] ?>"></script>

    <script type="text/javascript">

        var docEditor;
        var fileName = "<?php echo $filename ?>";
        var fileType = "<?php echo $filetype ?>";

        var innerAlert = function (message) {
            if (console && console.log)
                console.log(message);
        };

        var onAppReady = function () {
            innerAlert("Document editor ready");
        };

        var onDocumentStateChange = function (event) {
            var title = document.title.replace(/\*$/g, "");
            document.title = title + (event.data ? "*" : "");
        };

        var onRequestEditRights = function () {
            location.href = location.href.replace(RegExp("action=view\&?", "i"), "");
        };

        var onError = function (event) {
            if (event)
                innerAlert(event.data);
        };

        var onOutdatedVersion = function (event) {
            location.reload(true);
        };

        var replaceActionLink = function(href, linkParam) {
            var link;
            var actionIndex = href.indexOf("&actionLink=");
            if (actionIndex != -1) {
                var endIndex = href.indexOf("&", actionIndex + "&actionLink=".length);
                if (endIndex != -1) {
                    link = href.substring(0, actionIndex) + href.substring(endIndex) + "&actionLink=" + encodeURIComponent(linkParam);
                } else {
                    link = href.substring(0, actionIndex) + "&actionLink=" + encodeURIComponent(linkParam);
                }
            } else {
                link = href + "&actionLink=" + encodeURIComponent(linkParam);
            }
            return link;
        }

        var onMakeActionLink = function (event) {
            var actionData = event.data;
            var linkParam = JSON.stringify(actionData);
            docEditor.setActionLink(replaceActionLink(location.href, linkParam));
        };

        var сonnectEditor = function () {

            <?php
                if (!file_exists(getStoragePath($filename))) {
                    echo "alert('File not found'); return;";
                }
            ?>

            var config = <?php echo json_encode($config) ?>;

            config.width = "100%";
            config.height = "100%";

            config.events = {
                'onAppReady': onAppReady,
                'onDocumentStateChange': onDocumentStateChange,
                'onRequestEditRights': onRequestEditRights,
                'onError': onError,
                'onOutdatedVersion': onOutdatedVersion,
                'onMakeActionLink': onMakeActionLink,
            };

            <?php
                $out = getHistory($filename, $filetype, $docKey, $fileuri);
                $history = $out[0];
                $historyData = $out[1];
            ?>
            <?php if ($history != null && $historyData != null): ?>
            config.events['onRequestHistory'] = function () {
                docEditor.refreshHistory(<?php echo json_encode($history) ?>);
            };
            config.events['onRequestHistoryData'] = function (event) {
                var ver = event.data;
                var histData = <?php echo json_encode($historyData) ?>;
                docEditor.setHistoryData(histData[ver]);
            };
            config.events['onRequestHistoryClose'] = function () {
                document.location.reload();
            };
            <?php endif; ?>

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