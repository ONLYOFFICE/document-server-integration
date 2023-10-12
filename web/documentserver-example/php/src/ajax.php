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

namespace Example;

use Exception;
use Example\Common\Path;
use Example\Configuration\ConfigurationManager;
use Example\Format\FormatManager;
use Example\Helpers\ExampleUsers;
use Example\Helpers\JwtManager;

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
 * Save copy as...
 *
 * @return array
 */
function saveas()
{
    try {
        $configManager = new ConfigurationManager();
        $formatManager = new FormatManager();

        $post = json_decode(file_get_contents('php://input'), true);
        $fileurl = $post["url"];
        $title = $post["title"];
        $extension = mb_strtolower(pathinfo($title, PATHINFO_EXTENSION));
        $allexts = $formatManager->allExtensions();
        $filename = GetCorrectName($title);

        if (!in_array($extension, $allexts)) {
            $result["error"] = "File type is not supported";
            return $result;
        }
        $headers = get_headers($fileurl, 1);
        $contentLength = $headers["Content-Length"];
        $data = file_get_contents(str_replace(" ", "%20", $fileurl));

        if ($data === false || $contentLength <= 0 || $contentLength > $configManager->maximumFileSize()) {
            $result["error"] = "File size is incorrect";
            return $result;
        }

        file_put_contents(getStoragePath($filename), $data, LOCK_EX);  // write data to the new file
        $userList = new ExampleUsers();
        $user = $userList->getUser($_GET["user"]);
        createMeta($filename, $user->id, $user->name);  // and create meta data for this file

        $result["file"] = $filename;
        return $result;
    } catch (Exception $e) {
        sendlog("SaveAs: ".$e->getMessage(), "webedior-ajax.log");
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
    $configManager = new ConfigurationManager();
    $formatManager = new FormatManager();

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
        $ext = mb_strtolower(pathinfo($_FILES['files']['name'], PATHINFO_EXTENSION));  // get file extension

        // check if the file size is correct (it should be less than the max file size, but greater than 0)
        if ($filesize <= 0 || $filesize > $configManager->maximumFileSize()) {
            $result["error"] = 'File size is incorrect';  // if not, then an error occurs
            return $result;
        }

        // check if the file extension is supported by the editor
        if (!in_array($ext, $formatManager->allExtensions())) {
            $result["error"] = 'File type is not supported';  // if not, then an error occurs
            return $result;
        }

        // get the correct file name with an index if the file with such a name already exists
        $filename = GetCorrectName($_FILES['files']['name']);
        if (!move_uploaded_file($tmp, getStoragePath($filename))) {
            $result["error"] = 'Upload failed';  // file upload error
            return $result;
        }
        $userList = new ExampleUsers();
        $user = $userList->getUser($_GET["user"]);
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
 * @return array|int
 */
function track()
{
    sendlog("Track START", "webedior-ajax.log");
    sendlog("   _GET params: " . serialize($_GET), "webedior-ajax.log");

    $result["error"] = 0;

    // get the body of the post request and check if it is correct
    $data = readBody();

    if (!empty($data->error)) {
        return $data;
    }

    $trackerStatus = [
        0 => 'NotFound',
        1 => 'Editing',
        2 => 'MustSave',
        3 => 'Corrupted',
        4 => 'Closed',
        6 => 'MustForceSave',
        7 => 'CorruptedForceSave',
    ];
    $status = $trackerStatus[$data->status];  // get status from the request body

    $userAddress = $_GET["userAddress"];
    $fileName = basename($_GET["fileName"]);

    sendlog("   CommandRequest status: " . $data->status, "webedior-ajax.log");
    switch ($status) {
        case "Editing":  // status == 1
            if ($data->actions && $data->actions[0]->type == 0) {   // finished edit
                $user = $data->actions[0]->userid;  // the user who finished editing
                if (array_search($user, $data->users) === false) {
                    // create a command request with the forcasave method
                    $commandRequest = commandRequest("forcesave", $data->key);
                    sendlog("   CommandRequest forcesave: " . serialize($commandRequest), "webedior-ajax.log");
                }
            }
            break;
        case "MustSave":  // status == 2
        case "Corrupted":  // status == 3
            $result = processSave($data, $fileName, $userAddress);
            break;
        case "MustForceSave":  // status == 6
        case "CorruptedForceSave":  // status == 7
            $result = processForceSave($data, $fileName, $userAddress);
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
    $formatManager = new FormatManager();

    $post = json_decode(file_get_contents('php://input'), true);
    $fileName = basename($post["filename"]);
    $filePass = $post["filePass"];
    $lang = $_COOKIE["ulang"] ?? "";
    $extension = mb_strtolower(pathinfo($fileName, PATHINFO_EXTENSION));
    $internalExtension = "ooxml";

    // check if the file with such an extension can be converted
    if (in_array($extension, $formatManager->convertibleExtensions()) &&
        $internalExtension != "") {
        $fileUri = $post["fileUri"];
        if ($fileUri == null || $fileUri == "") {
            $fileUri = serverPath(true) . '/'
                . "download"
                . "?fileName=" . urlencode($fileName)
                . "&userAddress=" . getClientIp();
        }
        $key = getDocEditorKey($fileName);
        try {
            // convert file and get the percentage of the conversion completion
            $convertedData = getConvertedData(
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

        if ($convertedData["percent"] != 100) {
            $result["step"] = $convertedData["percent"];
            $result["filename"] = $fileName;
            $result["fileUri"] = $fileUri;
            return $result;
        }

        // get file name without extension
        $baseNameWithoutExt = mb_substr($fileName, 0, mb_strlen($fileName) - mb_strlen($extension) - 1);

        // get the correct file name with an index if the file with such a name already exists
        $newFileName = GetCorrectName($baseNameWithoutExt . "." . $convertedData["fileType"]);

        if (($data = file_get_contents(str_replace(" ", "%20", $newFileUri))) === false) {
            $result["error"] = 'Bad Request';
            return $result;
        }
        file_put_contents(getStoragePath($newFileName), $data, LOCK_EX);  // write data to the new file
        $userList = new ExampleUsers();
        $user = $userList->getUser($_GET["user"]);
        createMeta($newFileName, $user->id, $user->name);  // and create meta data for this file

        // delete the original file and its history
        $stPath = getStoragePath($fileName);
        unlink($stPath);
        delTree(getHistoryDir($stPath));

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
    $filePath = dirname(__FILE__) . DIRECTORY_SEPARATOR . '..' .
        DIRECTORY_SEPARATOR . "assets" . DIRECTORY_SEPARATOR . "document-templates"
        . DIRECTORY_SEPARATOR . "sample" . DIRECTORY_SEPARATOR . $fileName;
    downloadFile($filePath);
}

/**
 * Download a csv file
 *
 * @return void
 */
function csv()
{
    $fileName = "csv.csv";
    $filePath = dirname(__FILE__) . DIRECTORY_SEPARATOR . '..' .
        DIRECTORY_SEPARATOR . "assets" . DIRECTORY_SEPARATOR . "document-templates"
        . DIRECTORY_SEPARATOR . "sample" . DIRECTORY_SEPARATOR . $fileName;
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
        $configManager = new ConfigurationManager();

        $fileName = basename($_GET["fileName"]);  // get the file name
        $userAddress = $_GET["userAddress"];

        $ver = $_GET["ver"];
        $file = $_GET["file"];

        $jwtManager = new JwtManager();
        if ($jwtManager->isJwtEnabled()) {
            $jwtHeader = $configManager->jwtHeader();
            if (!empty(apache_request_headers()[$jwtHeader])) {
                $token = $jwtManager->jwtDecode(mb_substr(
                    apache_request_headers()[$jwtHeader],
                    mb_strlen("Bearer ")
                ));
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
        $configManager = new ConfigurationManager();

        $fileName = $_GET["fileName"];
        $userAddress = $_GET["userAddress"] ?? null;
        $isEmbedded = $_GET["&dmode"] ?? null;
        $jwtManager = new JwtManager();

        if ($jwtManager->isJwtEnabled() && $isEmbedded == null && $userAddress) {
            $jwtHeader = $configManager->jwtHeader();
            if (!empty(apache_request_headers()[$jwtHeader])) {
                $token = $jwtManager->jwtDecode(mb_substr(
                    apache_request_headers()[$jwtHeader],
                    mb_strlen("Bearer ")
                ));
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
        @header(
            'Content-Disposition: attachment; filename*=UTF-8\'\'' .
            str_replace("+", "%20", urlencode(basename($filePath)))
        );
        @header('Content-Type: ' . mime_content_type($filePath));
        @header('Access-Control-Allow-Origin: *');

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

    $newfilenameArr = explode('.', $newfilename);
    $curExt = mb_strtolower(array_pop($newfilenameArr));
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

/**
 * Reference data
 *
 * @return array
 */
function reference()
{
    $post = json_decode(file_get_contents('php://input'), true);

    @header("Content-Type: application/json");

    $referenceData = $post["referenceData"] ?? null;

    $jwtManager = new JwtManager();

    if ($referenceData) {
        $instanceId = $referenceData["instanceId"];
        if ($instanceId == serverPath()) {
            $fileKey =  json_decode(str_replace("'", "\"", $referenceData["fileKey"]));
            $userAddress = $fileKey->userAddress;
            if ($userAddress == getCurUserHostAddress()) {
                $fileName = $fileKey->fileName;
            }
        }
    }

    if (!isset($filename) && isset($post["path"])) {
        $path = basename($post["path"]);
        if (file_exists(getStoragePath($path))) {
            $fileName = $path;
        }
    }

    if (!isset($fileName)) {
        return ["error" => "File is not found"];
    }

    $data = [
        "fileType" => trim(getInternalExtension($fileName), '.'),
        "key" => getDocEditorKey($fileName),
        "url" => getDownloadUrl($fileName),
        "directUrl" => $post["directUrl"] ? getDownloadUrl($fileName, false) : null,
        "referenceData" => [
            "fileKey" => json_encode([
                "fileName" => $fileName,
                "userAddress" =>  getCurUserHostAddress()
            ]),
            "instanceId" => serverPath(),
        ],
        "path" => $fileName
    ];

    if ($jwtManager->isJwtEnabled()) {
        $data["token"] = $jwtManager->jwtEncode($data);
    }

    return $data;
}

function restore()
{
    try {
        $input = file_get_contents('php://input');
        $body = json_decode($input);

        $sourceBasename = $body->fileName;
        $version = $body->version;
        $userID = $body->userId;

        $sourceFile = getStoragePath($sourceBasename);
        $historyDirectory = getHistoryDir($sourceFile);

        $bumpedVersion = getFileVersion($historyDirectory);
        $bumpedVersionStringDirectory = getVersionDir($historyDirectory, $bumpedVersion);
        if (!file_exists($bumpedVersionStringDirectory)) {
            mkdir($bumpedVersionStringDirectory);
        }
        $bumpedVersionDirectory = new Path($bumpedVersionStringDirectory);

        $bumpedKeyFile = $bumpedVersionDirectory->joinPath('key.txt');
        $bumpedKeyStringFile = $bumpedKeyFile->string();
        $bumpedKey = getDocEditorKey($sourceBasename);
        file_put_contents($bumpedKeyStringFile, $bumpedKey, LOCK_EX);

        $users = new ExampleUsers();
        $user = $users->getUser($userID);

        $bumpedChangesFile = $bumpedVersionDirectory->joinPath('changes.json');
        $bumpedChangesStringFile = $bumpedChangesFile->string();
        $bumpedChanges = [
            'serverVersion' => null,
            'changes' => array(
                [
                    'created' => date('Y-m-d H:i:s'),
                    'user' => [
                        'id' => $user->id,
                        'name' => $user->name
                    ]
                ]
            )
        ];
        $bumpedChangesContent = json_encode($bumpedChanges, JSON_PRETTY_PRINT);
        file_put_contents($bumpedChangesStringFile, $bumpedChangesContent, LOCK_EX);

        $sourceExtension = pathinfo($sourceBasename, PATHINFO_EXTENSION);
        $previousBasename = "prev.{$sourceExtension}";

        $bumpedFile = $bumpedVersionDirectory->joinPath($previousBasename);
        $bumpedStringFile = $bumpedFile->string();
        copy($sourceFile, $bumpedStringFile);

        $recoveryVersionStringDirectory = getVersionDir($historyDirectory, $version);
        $recoveryVersionDirectory = new Path($recoveryVersionStringDirectory);
        $recoveryFile = $recoveryVersionDirectory->joinPath($previousBasename);
        $recoveryStringFile = $recoveryFile->string();
        copy($recoveryStringFile, $sourceFile);

        return [
            'error' => null,
            'success' => true
        ];
    } catch (Exception $error) {
        $message = $error->getMessage();
        return [
            'error' => $message,
            'success' => false
        ];
    }
}
