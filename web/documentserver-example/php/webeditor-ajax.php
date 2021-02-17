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

/**
 * WebEditor AJAX Process Execution.
 */
require_once( dirname(__FILE__) . '/config.php' );
require_once( dirname(__FILE__) . '/ajax.php' );
require_once( dirname(__FILE__) . '/common.php' );
require_once( dirname(__FILE__) . '/functions.php' );
require_once( dirname(__FILE__) . '/jwtmanager.php' );
require_once( dirname(__FILE__) . '/trackmanager.php' );

$_trackerStatus = array(
    0 => 'NotFound',
    1 => 'Editing',
    2 => 'MustSave',
    3 => 'Corrupted',
    4 => 'Closed',
    6 => 'MustForceSave',
    7 => 'CorruptedForceSave'
);

if (isset($_GET["type"]) && !empty($_GET["type"])) { //Checks if type value exists
    $response_array;
    @header( 'Content-Type: application/json; charset==utf-8');
    @header( 'X-Robots-Tag: noindex' );
    @header( 'X-Content-Type-Options: nosniff' );

    nocache_headers();

    sendlog(serialize($_GET), "webedior-ajax.log");

    $type = $_GET["type"];

    switch($type) { //Switch case for value of type
        case "upload":
            $response_array = upload();
            $response_array['status'] = isset($response_array['error']) ? 'error' : 'success';
            die (json_encode($response_array));
        case "download":
            $response_array = download();
            $response_array['status'] = 'success';
            die (json_encode($response_array));
        case "convert":
            $response_array = convert();
            $response_array['status'] = 'success';
            die (json_encode($response_array));
        case "track":
            $response_array = track();
            $response_array['status'] = 'success';
            die (json_encode($response_array));
        case "delete":
            $response_array = delete();
            $response_array['status'] = 'success';
            die (json_encode($response_array));
        case "assets":
            $response_array = assets();
            $response_array['status'] = 'success';
            die (json_encode($response_array));
        case "csv":
            $response_array = csv();
            $response_array['status'] = 'success';
            die (json_encode($response_array));
        case "files":
            $response_array = files();
            die (json_encode($response_array));
        default:
            $response_array['status'] = 'error';
            $response_array['error'] = '404 Method not found';
            die(json_encode($response_array));
    }
}

function upload() {
    $result; $filename;

    if ($_FILES['files']['error'] > 0) {
        $result["error"] = 'Error ' . json_encode($_FILES['files']['error']);
        return $result;
    }

    $tmp = $_FILES['files']['tmp_name'];

    if (empty(tmp)) {
        $result["error"] = 'No file sent';
        return $result;
    }

    if (is_uploaded_file($tmp))
    {
        $filesize = $_FILES['files']['size'];
        $ext = strtolower('.' . pathinfo($_FILES['files']['name'], PATHINFO_EXTENSION));

        if ($filesize <= 0 || $filesize > $GLOBALS['FILE_SIZE_MAX']) {
            $result["error"] = 'File size is incorrect';
            return $result;
        }

        if (!in_array($ext, getFileExts())) {
            $result["error"] = 'File type is not supported';
            return $result;
        }

        $filename = GetCorrectName($_FILES['files']['name']);
        if (!move_uploaded_file($tmp,  getStoragePath($filename)) ) {
            $result["error"] = 'Upload failed';
            return $result;
        }
        createMeta($filename);

    } else {
        $result["error"] = 'Upload failed';
        return $result;
    }

    $result["filename"] = $filename;
    return $result;
}

function track() {
    sendlog("Track START", "webedior-ajax.log");
    sendlog("   _GET params: " . serialize( $_GET ), "webedior-ajax.log");

    $result["error"] = 0;

    $data = readBody();
    if ($data["error"]){
        return $data;
    }

    global $_trackerStatus;
    $status = $_trackerStatus[$data["status"]];

    $userAddress = $_GET["userAddress"];
    $fileName = basename($_GET["fileName"]);

    switch ($status) {
        case "Editing":
            if ($data["actions"] && $data["actions"][0]["type"] == 0) {
                $user = $data["actions"][0]["userid"];
                if (array_search($user, $data["users"]) === FALSE) {
                    $commandRequest = commandRequest("forcesave", $data["key"]);
                    sendlog("   CommandRequest forcesave: " . serialize($commandRequest), "webedior-ajax.log");
                }
            }
            break;
        case "MustSave":
        case "Corrupted":
            $result = processSave($data, $fileName, $userAddress);
            break;
        case "MustForceSave":
        case "CorruptedForceSave":
            $result = processForceSave($data, $fileName, $userAddress);
            break;
    }

    sendlog("Track RESULT: " . serialize($result), "webedior-ajax.log");
    return $result;
}

function convert() {
    $fileName = basename($_GET["filename"]);
    $extension = strtolower(pathinfo($fileName, PATHINFO_EXTENSION));
    $internalExtension = trim(getInternalExtension($fileName),'.');

    if (in_array("." + $extension, $GLOBALS['DOC_SERV_CONVERT']) && $internalExtension != "") {

        $fileUri = $_GET["fileUri"];
        if ($fileUri == NULL || $fileUri == "") {
            $fileUri = FileUri($fileName, TRUE);
        }
        $key = getDocEditorKey($fileName);

        $newFileUri;
        $result;
        $percent;

        try {
            $percent = GetConvertedUri($fileUri, $extension, $internalExtension, $key, TRUE, $newFileUri);
        }
        catch (Exception $e) {
            $result["error"] = "error: " . $e->getMessage();
            return $result;
        }

        if ($percent != 100)
        {
            $result["step"] = $percent;
            $result["filename"] = $fileName;
            $result["fileUri"] = $fileUri;
            return $result;
        }

        $baseNameWithoutExt = substr($fileName, 0, strlen($fileName) - strlen($extension) - 1);

        $newFileName = GetCorrectName($baseNameWithoutExt . "." . $internalExtension);

        if (($data = file_get_contents(str_replace(" ","%20",$newFileUri))) === FALSE) {
            $result["error"] = 'Bad Request';
            return $result;
        } else {
            file_put_contents(getStoragePath($newFileName), $data, LOCK_EX);
            createMeta($newFileName);
        }

        $stPath = getStoragePath($fileName);
        unlink($stPath);
        delTree(getHistoryDir($stPath));

        $fileName = $newFileName;
    }

    $result["filename"] = $fileName;
    return $result;
}

function delete() {
    try {
        $fileName = basename($_GET["fileName"]);

        $filePath = getStoragePath($fileName);

        unlink($filePath);
        delTree(getHistoryDir($filePath));
    }
    catch (Exception $e) {
        sendlog("Deletion ".$e->getMessage(), "webedior-ajax.log");
        $result["error"] = "error: " . $e->getMessage();
        return $result;
    }
}

function files() {
    try {
        @header( "Content-Type", "application/json" );

        $fileId = $_GET["fileId"];
        $result = getFileInfo($fileId);

        return $result;
    }
    catch (Exception $e) {
        sendlog("Files ".$e->getMessage(), "webedior-ajax.log");
        $result["error"] = "error: " . $e->getMessage();
        return $result;
    }
}

function assets() {
    $fileName = basename($_GET["name"]);
    $filePath = dirname(__FILE__) . DIRECTORY_SEPARATOR . "assets" . DIRECTORY_SEPARATOR . "sample" . DIRECTORY_SEPARATOR . $fileName;
    downloadFile($filePath);
}

function csv() {
    $fileName =  "csv.csv";
    $filePath = dirname(__FILE__) . DIRECTORY_SEPARATOR . "assets" . DIRECTORY_SEPARATOR . "sample" . DIRECTORY_SEPARATOR . $fileName;
    downloadFile($filePath);
}

function download() {
    try {
        $fileName = basename($_GET["name"]);
        $filePath = getForcesavePath($fileName, null, false);
        if ($filePath == "") {
            $filePath = getStoragePath($fileName, null);
        }
        downloadFile($filePath);
    } catch (Exception $e) {
        sendlog("Download ".$e->getMessage(), "webedior-ajax.log");
        $result["error"] = "error: File not found";
        return $result;
    }
}

function downloadFile($filePath) {
    if (file_exists($filePath)) {
        if (ob_get_level()) {
            ob_end_clean();
        }

        @header('Content-Length: ' . filesize($filePath));
        @header('Content-Disposition: attachment; filename*=UTF-8\'\'' . urldecode(basename($filePath)));
        @header('Content-Type: ' . mime_content_type($filePath));

        if ($fd = fopen($filePath, 'rb')) {
            while (!feof($fd)) {
                print fread($fd, 1024);
            }
            fclose($fd);
        }
        exit;
    }
}

function delTree($dir) {
    if (!file_exists($dir) || !is_dir($dir)) return;

    $files = array_diff(scandir($dir), array('.','..'));
    foreach ($files as $file) {
        (is_dir("$dir/$file")) ? delTree("$dir/$file") : unlink("$dir/$file");
    }
    return rmdir($dir);
}

?>