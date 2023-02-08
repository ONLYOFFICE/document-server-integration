<?php

namespace OnlineEditorsExamplePhp;

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

use Exception;
use OnlineEditorsExamplePhp\Helpers\ConfigManager;
use OnlineEditorsExamplePhp\Helpers\ExampleUsers;
use OnlineEditorsExamplePhp\Helpers\FileUtility;
use OnlineEditorsExamplePhp\Helpers\JwtManager;
use OnlineEditorsExamplePhp\Helpers\TrackManager;
use OnlineEditorsExamplePhp\Helpers\Utils;

/**
 * Check if the request is an AJAX request
 *
 * @return bool
 */
function isAjax()
{
    return isset($_SERVER['HTTP_X_REQUESTED_WITH'])
        && mb_strtolower($_SERVER['HTTP_X_REQUESTED_WITH']) == 'xmlhttprequest';
}

/**
 * Get the http origin
 *
 * @return string
 */
function getHttpOrigin()
{
    $origin = '';
    if (!empty($_SERVER['HTTP_ORIGIN'])) {
        $origin = $_SERVER['HTTP_ORIGIN'];
    }
    return $origin;
}

/**
 * Set headers that prevent caching in all the browsers
 *
 * @return void
 */
function nocacheHeaders()
{
    $headers = [
        'Expires' => 'Wed, 11 Jan 1984 05:00:00 GMT',
        'Cache-Control' => 'no-cache, must-revalidate, max-age=0',
        'Pragma' => 'no-cache',
    ];
    $headers['Last-Modified'] = false;

    unset($headers['Last-Modified']);

    // In PHP 5.3+, make sure we are not sending a Last-Modified header.
    if (function_exists('header_remove')) {
        @header_remove('Last-Modified');
    } else {
        // In PHP 5.2, send an empty Last-Modified header, but only as a
        // last resort to override a header already sent. #WP23021
        foreach (headers_list() as $header) {
            if (0 === mb_stripos($header, 'Last-Modified')) {
                $headers['Last-Modified'] = '';
                break;
            }
        }
    }

    foreach ($headers as $name => $field_value) {
        @header("{$name}: {$field_value}");
    }
}

/**
 * Save copy as...
 *
 * @return array
 */
function saveas()
{
    $fileUtility = new FileUtility();
    try {
        $result;
        $post = json_decode(file_get_contents('php://input'), true);
        $fileurl = $post["url"];
        $title = $post["title"];
        $extension = mb_strtolower(pathinfo($title, PATHINFO_EXTENSION));
        $configManager = new ConfigManager();
        $allexts = array_merge(
            $configManager->getConfig("docServConvert"),
            $configManager->getConfig("docServEdited"),
            $configManager->getConfig("docServViewd"),
            $configManager->getConfig("docServFillforms")
        );
        $filename = $fileUtility->getCorrectName($title);

        if (!in_array("." . $extension, $allexts)) {
            $result["error"] = "File type is not supported";
            return $result;
        }
        $headers = get_headers($fileurl, 1);
        $content_length = $headers["Content-Length"];
        $data = file_get_contents(str_replace(" ", "%20", $fileurl));

        if ($data === false || $content_length <= 0 || $content_length > $configManager->getConfig("fileSizeMax")) {
            $result["error"] = "File size is incorrect";
            return $result;
        }

        file_put_contents($fileUtility->getStoragePath($filename), $data, LOCK_EX);  // write data to the new file
        $users = new ExampleUsers();
        $user = $users->getUser($_GET["user"]);
        $fileUtility->createMeta($filename, $user->id, $user->name);  // and create meta data for this file

        $result["file"] = $filename;
        return $result;
    } catch (Exception $e) {
        $fileUtility->sendlog("SaveAs: ".$e->getMessage(), "webedior-ajax.log");
        $result["error"] = "error: " . 1 . "message:" . $e->getMessage();
        return $result;
    }
}

/**
 * Uploading a file
 *
 * @return array
 */
function upload()
{
    $result;
    $filename;

    if ($_FILES['files']['error'] > 0) {
        $result["error"] = 'Error ' . json_encode($_FILES['files']['error']);
        return $result;
    }

    // get the temporary name with which the received file was saved on the server
    $tmp = $_FILES['files']['tmp_name'];

    // if the temporary name doesn't exist, then an error occurs
    if (empty($tmp)) {
        $result["error"] = 'No file sent';
        return $result;
    }

    // check if the file was uploaded using HTTP POST
    if (is_uploaded_file($tmp)) {
        $filesize = $_FILES['files']['size'];  // get the file size
        $ext = mb_strtolower('.' . pathinfo($_FILES['files']['name'], PATHINFO_EXTENSION));  // get file extension

        // check if the file size is correct (it should be less than the max file size, but greater than 0)
        $configManager = new ConfigManager();
        if ($filesize <= 0 || $filesize > $configManager->getConfig("fileSizeMax")) {
            $result["error"] = 'File size is incorrect';  // if not, then an error occurs
            return $result;
        }

        // check if the file extension is supported by the editor
        if (!in_array($ext, getFileExts())) {
            $result["error"] = 'File type is not supported';  // if not, then an error occurs
            return $result;
        }

        // get the correct file name with an index if the file with such a name already exists
        $filename = getCorrectName($_FILES['files']['name']);
        if (!move_uploaded_file($tmp, getStoragePath($filename))) {
            $result["error"] = 'Upload failed';  // file upload error
            return $result;
        }
        $users = new ExampleUsers();
        $user = $users->getUser($_GET["user"]);
        createMeta($filename, $user->id, $user->name);  // create file meta data
    } else {
        $result["error"] = 'Upload failed';
        return $result;
    }

    $result["filename"] = $filename;
    $result["documentType"] = getDocumentType($filename);
    return $result;
}

/**
 * Tracking file changes
 *
 * @return mixed
 */
function track()
{
    $fileUtility = new FileUtility();
    $trackManager = new TrackManager();
    $fileUtility->sendlog("Track START", "webedior-ajax.log");
    $fileUtility->sendlog("   _GET params: " . serialize($_GET), "webedior-ajax.log");

    $result["error"] = 0;

    // get the body of the post request and check if it is correct
    $data = $trackManager->readBody();

    if (!empty($data->error)) {
        return $data;
    }

    global $_trackerStatus;
    $status = $_trackerStatus[$data->status];  // get status from the request body

    $userAddress = $_GET["userAddress"];
    $fileName = basename($_GET["fileName"]);

    $fileUtility->sendlog("   CommandRequest status: " . $data->status, "webedior-ajax.log");
    switch ($status) {
        case "Editing":  // status == 1
            if ($data->actions && $data->actions[0]->type == 0) {   // finished edit
                $user = $data->actions[0]->userid;  // the user who finished editing
                if (array_search($user, $data->users) === false) {
                    // create a command request with the forcasave method
                    $commandRequest = $trackManager->commandRequest("forcesave", $data->key);
                    $fileUtility->sendlog(
                        "   CommandRequest forcesave: " . serialize($commandRequest),
                        "webedior-ajax.log"
                    );
                }
            }
            break;
        case "MustSave":  // status == 2
        case "Corrupted":  // status == 3
            $result = $trackManager->processSave($data, $fileName, $userAddress);
            break;
        case "MustForceSave":  // status == 6
        case "CorruptedForceSave":  // status == 7
            $result = $trackManager->processForceSave($data, $fileName, $userAddress);
            break;
    }

    sendlog("Track RESULT: " . serialize($result), "webedior-ajax.log");
    return $result;
}

/**
 * Converting a file
 *
 * @return array
 */
function convert()
{
    $post = json_decode(file_get_contents('php://input'), true);
    $fileName = basename($post["filename"]);
    $filePass = $post["filePass"];
    $lang = $_COOKIE["ulang"];
    $extension = mb_strtolower(pathinfo($fileName, PATHINFO_EXTENSION));
    $internalExtension = trim(getInternalExtension($fileName), '.');
    $fileUtility = new FileUtility();

    // check if the file with such an extension can be converted
    $configManager = new ConfigManager();
    if (in_array("." . $extension, $configManager->getConfig("docServConvert")) && $internalExtension != "") {
        $fileUri = $post["fileUri"];
        $fileUtility = new FileUtility();
        if ($fileUri == null || $fileUri == "") {
            $fileUri = $fileUtility->serverPath(true) . '/'
                . "webeditor-ajax.php"
                . "?type=download"
                . "&fileName=" . urlencode($fileName)
                . "&userAddress=" . $fileUtility->getClientIp();
        }
        $key = $fileUtility->getDocEditorKey($fileName);

        $newFileUri;
        $result;
        $percent;

        try {
            // convert file and get the percentage of the conversion completion
            $utils = new Utils();
            $percent = $utils->getConvertedUri(
                $fileUri,
                $extension,
                $internalExtension,
                $key,
                true,
                $newFileUri,
                $filePass,
                $lang
            );
        } catch (Exception $e) {
            $result["error"] = "error: " . $e->getMessage();
            return $result;
        }

        if ($percent != 100) {
            $result["step"] = $percent;
            $result["filename"] = $fileName;
            $result["fileUri"] = $fileUri;
            return $result;
        }

        // get file name without extension
        $baseNameWithoutExt = mb_substr($fileName, 0, mb_strlen($fileName) - mb_strlen($extension) - 1);

        // get the correct file name with an index if the file with such a name already exists
        $newFileName = $fileUtility->getCorrectName($baseNameWithoutExt . "." . $internalExtension);

        if (($data = file_get_contents(str_replace(" ", "%20", $newFileUri))) === false) {
            $result["error"] = 'Bad Request';
            return $result;
        }
        file_put_contents($fileUtility->getStoragePath($newFileName), $data, LOCK_EX);  // write data to the new file
        $users = new ExampleUsers();
        $user = $users->getUser($_GET["user"]);
        $fileUtility->createMeta($newFileName, $user->id, $user->name);  // and create meta data for this file

        // delete the original file and its history
        $stPath = $fileUtility->getStoragePath($fileName);
        unlink($stPath);
        \PhpExample\delTree(getHistoryDir($stPath));

        $fileName = $newFileName;
    }

    $result["filename"] = $fileName;
    return $result;
}

/**
 * Removing a file
 *
 * @return array|void
 */
function delete()
{
    try {
        $fileName = basename($_GET["fileName"]);

        $filePath = getStoragePath($fileName);

        unlink($filePath);  // delete a file
        delTree(getHistoryDir($filePath));  // delete all the elements from the history directory
    } catch (Exception $e) {
        sendlog("Deletion ".$e->getMessage(), "webedior-ajax.log");
        $result["error"] = "error: " . $e->getMessage();
        return $result;
    }
}

/**
 * Get file information
 *
 * @return array
 */
function files()
{
    try {
        @header("Content-Type", "application/json");

        $fileId = $_GET["fileId"];
        $result = getFileInfo($fileId);

        return $result;
    } catch (Exception $e) {
        sendlog("Files ".$e->getMessage(), "webedior-ajax.log");
        $result["error"] = "error: " . $e->getMessage();
        return $result;
    }
}

/**
 * Download assets
 *
 * @return void
 */
function assets()
{
    $fileName = basename($_GET["name"]);
    $filePath = dirname(__FILE__) .
        DIRECTORY_SEPARATOR . "assets" . DIRECTORY_SEPARATOR . "sample" . DIRECTORY_SEPARATOR . $fileName;
    \PhpExample\downloadFile($filePath);
}

/**
 * Download a csv file
 *
 * @return void
 */
function csv()
{
    $fileName = "csv.csv";
    $filePath = dirname(__FILE__) .
        DIRECTORY_SEPARATOR . "assets" . DIRECTORY_SEPARATOR . "sample" . DIRECTORY_SEPARATOR . $fileName;
    downloadFile($filePath);
}

/**
 * Download a file from history
 *
 * @return array|void
 */
function historyDownload()
{
    try {
        $fileName = basename($_GET["fileName"]);  // get the file name
        $userAddress = $_GET["userAddress"];
        $jwtManager = new JwtManager();

        $ver = $_GET["ver"];
        $file = $_GET["file"];

        if ($jwtManager->isJwtEnabled()) {
            $configManager = new ConfigManager();
            $jwtHeader = $configManager->getConfig("docServJwtHeader") == "" ?
                "Authorization" : $configManager->getConfig("docServJwtHeader");
            if (!empty(apache_request_headers()[$jwtHeader])) {
                $token = $jwtManager->jwtDecode(mb_substr(apache_request_headers()[$jwtHeader], mb_strlen("Bearer ")));
                if (empty($token)) {
                    http_response_code(403);
                    die("Invalid JWT signature");
                }
            } else {
                http_response_code(403);
                die("Invalid JWT signature");
            }
        }

        $histDir = getHistoryDir(getStoragePath($fileName, $userAddress));

        $filePath = getVersionDir($histDir, $ver) . DIRECTORY_SEPARATOR . $file;
        ;

        downloadFile($filePath);  // download this file
    } catch (Exception $e) {
        sendlog("Download ".$e->getMessage(), "webedior-ajax.log");
        $result["error"] = "error: File not found";
        return $result;
    }
}

/**
 * Download a file
 *
 * @return array|void
 */
function download()
{
    try {
        $configManager = new ConfigManager();
        $fileName = realpath($configManager->getConfig("storagePath"))
        === $configManager->getConfig("storagePath") ? $_GET["fileName"] :
            basename($_GET["fileName"]);  // get the file name
        $userAddress = $_GET["userAddress"];
        $isEmbedded = $_GET["&dmode"];
        $jwtManager = new JwtManager();

        if ($jwtManager->isJwtEnabled() && $isEmbedded == null && $userAddress) {
            $jwtHeader = $configManager->getConfig("docServJwtHeader") == "" ?
                "Authorization" : $configManager->getConfig("docServJwtHeader");
            if (!empty(apache_request_headers()[$jwtHeader])) {
                $token = $jwtManager->jwtDecode(mb_substr(apache_request_headers()[$jwtHeader], mb_strlen("Bearer ")));
                if (empty($token)) {
                    http_response_code(403);
                    die("Invalid JWT signature");
                }
            }
        }
        $filePath = getForcesavePath($fileName, $userAddress, false);  // get the path to the forcesaved file version
        if ($filePath == "") {
            $filePath = getStoragePath($fileName, $userAddress);  // get file from the storage directory
        }
        downloadFile($filePath);  // download this file
    } catch (Exception $e) {
        sendlog("Download ".$e->getMessage(), "webedior-ajax.log");
        $result["error"] = "error: File not found";
        return $result;
    }
}

/**
 * Download the specified file
 *
 * @param string $filePath
 *
 * @return void
 */
function downloadFile($filePath)
{
    if (file_exists($filePath)) {
        if (ob_get_level()) {
            ob_end_clean();
        }

        // write headers to the response object
        @header('Content-Length: ' . filesize($filePath));
        @header('Content-Disposition: attachment; filename*=UTF-8\'\'' . urldecode(basename($filePath)));
        @header('Content-Type: ' . mime_content_type($filePath));

        if ($fd = fopen($filePath, 'rb')) {
            while (!feof($fd)) {
                echo fread($fd, 1024);
            }
            fclose($fd);
        }
        exit;
    }
}

/**
 * Delete all the elements from the directory
 *
 * @param string $dir
 *
 * @return void|bool
 */
function delTree($dir)
{
    if (!file_exists($dir) || !is_dir($dir)) {
        return;
    }

    $files = array_diff(scandir($dir), ['.', '..']);
    foreach ($files as $file) {
        (is_dir("$dir/$file")) ? delTree("$dir/$file") : unlink("$dir/$file");
    }
    return rmdir($dir);
}

/**
 * Rename file
 *
 * @return array
 */
function renamefile()
{
    $post = json_decode(file_get_contents('php://input'), true);
    $newfilename = $post["newfilename"];

    $curExt = mb_strtolower(array_pop(explode('.', $newfilename)));
    $origExt = $post["ext"];
    if ($origExt !== $curExt) {
        $newfilename .= '.' . $origExt;
    }

    $dockey = $post["dockey"];
    $meta = ["title" => $newfilename];

    $commandRequest = commandRequest("meta", $dockey, $meta);  // create a command request with the forcasave method
    sendlog("   CommandRequest rename: " . serialize($commandRequest), "webedior-ajax.log");

    return ["result" => $commandRequest];
}