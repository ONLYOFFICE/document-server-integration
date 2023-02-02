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

require_once dirname(__FILE__) . '/config.php';
require_once dirname(__FILE__) . '/functions.php';

/**
 * Put log files into the log folder
 *
 * @param string  $msg
 * @param integer $logFileName
 *
 * @return void
 */
function sendlog($msg, $logFileName)
{
    $logsFolder = "logs/";
    if (!file_exists($logsFolder)) {  // if log folder doesn't exist, make it
        mkdir($logsFolder);
    }
    file_put_contents($logsFolder . $logFileName, $msg . PHP_EOL, FILE_APPEND);
}

/**
 * Create new uuid
 *
 * @return string
 */
function guid()
{
    if (function_exists('com_create_guid')) {
        return com_create_guid();
    }
    mt_srand((float) microtime() * 10000);  // optional for php 4.2.0 and up
    $charid = mb_strtoupper(md5(uniqid(rand(), true)));
    $hyphen = chr(45);  // "-"
    $uuid = chr(123)  // "{"
            .mb_substr($charid, 0, 8).$hyphen
            .mb_substr($charid, 8, 4).$hyphen
            .mb_substr($charid, 12, 4).$hyphen
            .mb_substr($charid, 16, 4).$hyphen
            .mb_substr($charid, 20, 12)
            .chr(125);  // "}"
    return $uuid;
}

if (!function_exists('mime_content_type')) {
    /**
     * Create new uuid
     *
     * @param string  $filename
     *
     * @return string
     */
    function mime_content_type($filename)
    {
        $mime_types = [

            'txt' => 'text/plain',
            'htm' => 'text/html',
            'html' => 'text/html',
            'php' => 'text/html',
            'css' => 'text/css',
            'js' => 'application/javascript',
            'json' => 'application/json',
            'xml' => 'application/xml',
            'swf' => 'application/x-shockwave-flash',
            'flv' => 'video/x-flv',

            // images
            'png' => 'image/png',
            'jpe' => 'image/jpeg',
            'jpeg' => 'image/jpeg',
            'jpg' => 'image/jpeg',
            'gif' => 'image/gif',
            'bmp' => 'image/bmp',
            'ico' => 'image/vnd.microsoft.icon',
            'tiff' => 'image/tiff',
            'tif' => 'image/tiff',
            'svg' => 'image/svg+xml',
            'svgz' => 'image/svg+xml',

            // archives
            'zip' => 'application/zip',
            'rar' => 'application/x-rar-compressed',
            'exe' => 'application/x-msdownload',
            'msi' => 'application/x-msdownload',
            'cab' => 'application/vnd.ms-cab-compressed',

            // audio/video
            'mp3' => 'audio/mpeg',
            'qt' => 'video/quicktime',
            'mov' => 'video/quicktime',

            // adobe
            'pdf' => 'application/pdf',
            'psd' => 'image/vnd.adobe.photoshop',
            'ai' => 'application/postscript',
            'eps' => 'application/postscript',
            'ps' => 'application/postscript',

            // ms office
            'doc' => 'application/msword',
            'rtf' => 'application/rtf',
            'xls' => 'application/vnd.ms-excel',
            'ppt' => 'application/vnd.ms-powerpoint',

            // open office
            'odt' => 'application/vnd.oasis.opendocument.text',
            'ods' => 'application/vnd.oasis.opendocument.spreadsheet',
        ];

        // check if the file extension is in the mime type array
        $ext = mb_strtolower(array_pop(explode('.', $filename)));
        if (array_key_exists($ext, $mime_types)) {
            return $mime_types[$ext];  // get the mime type of this extension
        } elseif (function_exists('finfo_open')) { // or get the mime type from the file information
            $finfo = finfo_open(FILEINFO_MIME);
            $mimetype = finfo_file($finfo, $filename);
            finfo_close($finfo);
            return $mimetype;
        }

        return 'application/octet-stream';
    }
}

/**
 * Get ip address
 *
 * @return string
 */
function getClientIp()
{
    $ipaddress = getenv('HTTP_CLIENT_IP') ?:
        getenv('HTTP_X_FORWARDED_FOR') ?:
        getenv('HTTP_X_FORWARDED') ?:
        getenv('HTTP_FORWARDED_FOR') ?:
        getenv('HTTP_FORWARDED') ?:
        getenv('REMOTE_ADDR') ?:
        'Storage';

    $ipaddress = preg_replace("/[^0-9a-zA-Z.=]/", "_", $ipaddress);

    return $ipaddress;
}

/**
 * Get server url
 *
 * @param string  $forDocumentServer
 *
 * @return string
 */
function serverPath($forDocumentServer = null)
{
    return $forDocumentServer && isset($GLOBALS['EXAMPLE_URL']) && $GLOBALS['EXAMPLE_URL'] != ""
        ? $GLOBALS['EXAMPLE_URL']
        : (getScheme() . '://' . $_SERVER['HTTP_HOST']);
}

/**
 * Get current user host address
 *
 * @param string  $userAddress
 *
 * @return string
 */
function getCurUserHostAddress($userAddress = null)
{
    if ($GLOBALS['ALONE']) {
        if (empty($GLOBALS['STORAGE_PATH'])) {
            return "Storage";
        }
        return "";
    }
    if (is_null($userAddress)) {
        $userAddress = getClientIp();
    }
    return preg_replace("[^0-9a-zA-Z.=]", '_', $userAddress);
}

/**
 * Get an internal file extension
 *
 * @param string  $filename
 *
 * @return string
 */
function getInternalExtension($filename)
{
    $ext = mb_strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION));

    if (in_array($ext, $GLOBALS['ExtsDocument'])) {
        return ".docx";
    }  // .docx for text document extensions
    if (in_array($ext, $GLOBALS['ExtsSpreadsheet'])) {
        return ".xlsx";
    }  // .xlsx for spreadsheet extensions
    if (in_array($ext, $GLOBALS['ExtsPresentation'])) {
        return ".pptx";
    }  // .pptx for presentation extensions
    return "";
}

/**
 * Get image url for templates
 *
 * @param string  $filename
 *
 * @return string
 */
function getTemplateImageUrl($filename)
{
    $ext = mb_strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION));
    $path = serverPath(true) . "/css/images/";

    if (in_array($ext, $GLOBALS['ExtsDocument'])) {
        return $path . "file_docx.svg";
    }  // for text document extensions
    if (in_array($ext, $GLOBALS['ExtsSpreadsheet'])) {
        return $path . "file_xlsx.svg";
    }  // for spreadsheet extensions
    if (in_array($ext, $GLOBALS['ExtsPresentation'])) {
        return $path . "file_pptx.svg";
    }  // for presentation extensions
    return $path . "file_docx.svg";
}

/**
 * Get the document type
 *
 * @param string  $filename
 *
 * @return string
 */
function getDocumentType($filename)
{
    $ext = mb_strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION));

    if (in_array($ext, $GLOBALS['ExtsDocument'])) {
        return "word";
    }  // word for text document extensions
    if (in_array($ext, $GLOBALS['ExtsSpreadsheet'])) {
        return "cell";
    }  // cell for spreadsheet extensions
    if (in_array($ext, $GLOBALS['ExtsPresentation'])) {
        return "slide";
    }  // slide for presentation extensions
    return "word";
}

/**
 * Get the protocol
 *
 * @return string
 */
function getScheme()
{
    return (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
}

/**
 * Get the storage path of the given file
 *
 * @param string  $fileName
 * @param string  $userAddress
 *
 * @return string
 */
function getStoragePath($fileName, $userAddress = null)
{
    $storagePath = trim(str_replace(['/', '\\'], DIRECTORY_SEPARATOR, $GLOBALS['STORAGE_PATH']), DIRECTORY_SEPARATOR);
    if (!empty($storagePath) && !file_exists($storagePath) && !is_dir($storagePath)) {
        mkdir($storagePath);
    }

    if (realpath($storagePath) === $storagePath) {
        $directory = $storagePath;
    } else {
        $directory = __DIR__ . DIRECTORY_SEPARATOR . $storagePath;
    }

    if ($storagePath != "") {
        $directory = $directory  . DIRECTORY_SEPARATOR;

        // if the file directory doesn't exist, make it
        if (!file_exists($directory) && !is_dir($directory)) {
            mkdir($directory);
        }
    }

    if (realpath($storagePath) !== $storagePath) {
        $directory = $directory . getCurUserHostAddress($userAddress) . DIRECTORY_SEPARATOR;
    }

    if (!file_exists($directory) && !is_dir($directory)) {
        mkdir($directory);
    }
    sendlog("getStoragePath result: " . $directory . basename($fileName), "common.log");
    return realpath($storagePath) === $storagePath ? $directory . $fileName : $directory . basename($fileName);
}

/**
 * Get the path to the forcesaved file version
 *
 * @param string  $fileName
 * @param string  $userAddress
 * @param bool    $create
 *
 * @return string
 */
function getForcesavePath($fileName, $userAddress, $create)
{
    $storagePath = trim(str_replace(['/', '\\'], DIRECTORY_SEPARATOR, $GLOBALS['STORAGE_PATH']), DIRECTORY_SEPARATOR);

    // create the directory to this file version
    if (realpath($storagePath) === $storagePath) {
        $directory = $storagePath . DIRECTORY_SEPARATOR;
    } else {
        $directory = __DIR__ . DIRECTORY_SEPARATOR . $storagePath . getCurUserHostAddress($userAddress) .
            DIRECTORY_SEPARATOR;
    }

    if (!is_dir($directory)) {
        return "";
    }

    // create the directory to the history of this file version
    $directory = $directory . $fileName . "-hist" . DIRECTORY_SEPARATOR;
    if (!$create && !is_dir($directory)) {
        return "";
    }

    if (!file_exists($directory) && !is_dir($directory)) {
        mkdir($directory);
    }
    $directory = $directory . $fileName;
    if (!$create && !file_exists($directory)) {
        return "";
    }

    return $directory;
}

/**
 * Get the path to the file history
 *
 * @param string  $storagePath
 *
 * @return string
 */
function getHistoryDir($storagePath)
{
    $directory = $storagePath . "-hist";
    // if the history directory doesn't exist, make it
    if (!file_exists($directory) && !is_dir($directory)) {
        mkdir($directory);
    }
    return $directory;
}

/**
 * Get the path to the specified file version
 *
 * @param string  $histDir
 * @param string  $version
 *
 * @return string
 */
function getVersionDir($histDir, $version)
{
    return $histDir . DIRECTORY_SEPARATOR . $version;
}

/**
 * Get a number of the last file version from the history directory
 *
 * @param string $histDir
 *
 * @return int
 */
function getFileVersion($histDir)
{
    if (!file_exists($histDir) || !is_dir($histDir)) {
        return 1;
    }  // check if the history directory exists

    $cdir = scandir($histDir);
    $ver = 1;
    foreach ($cdir as $key => $fileName) {
        if (!in_array($fileName, [".", ".."])) {
            if (is_dir($histDir . DIRECTORY_SEPARATOR . $fileName)) {
                $ver++;
            }
        }
    }
    return $ver;
}

/**
 * Get all the stored files from the folder
 *
 * @return array
 */
function getStoredFiles()
{
    $storagePath = trim(str_replace(['/', '\\'], DIRECTORY_SEPARATOR, $GLOBALS['STORAGE_PATH']), DIRECTORY_SEPARATOR);
    if (!empty($storagePath) && !file_exists($storagePath) && !is_dir($storagePath)) {
        mkdir($storagePath);
    }

    if (realpath($storagePath) === $storagePath) {
        $directory = $storagePath;
    } else {
        $directory = __DIR__ . DIRECTORY_SEPARATOR . $storagePath;
    }

    // get the storage path and check if it exists
    $result = [];
    if ($storagePath != "") {
        $directory = $directory . DIRECTORY_SEPARATOR;

        if (!file_exists($directory) && !is_dir($directory)) {
            return $result;
        }
    }

    if (realpath($storagePath) !== $storagePath) {
        $directory = $directory . getCurUserHostAddress() . DIRECTORY_SEPARATOR;
    }

    if (!file_exists($directory) && !is_dir($directory)) {
        return $result;
    }

    $cdir = scandir($directory);  // get all the files and folders from the directory
    $result = [];
    foreach ($cdir as $key => $fileName) {  // run through all the file and folder names
        if (!in_array($fileName, [".", ".."])) {
            if (!is_dir($directory . DIRECTORY_SEPARATOR . $fileName)) {  // if an element isn't a directory
                $ext = mb_strtolower('.' . pathinfo($fileName, PATHINFO_EXTENSION));
                $dat = filemtime($directory . DIRECTORY_SEPARATOR . $fileName);  // get the time of element modification
                $result[$dat] = (object) [  // and write the file to the result
                    "name" => $fileName,
                    "documentType" => getDocumentType($fileName),
                    "canEdit" => in_array($ext, $GLOBALS['DOC_SERV_EDITED']),
                    "isFillFormDoc" => in_array($ext, $GLOBALS['DOC_SERV_FILLFORMS']),
                ];
            }
        }
    }
    ksort($result);  // sort files by the modification date
    return array_reverse($result);
}

/**
 * Get the virtual path
 *
 * @param string $forDocumentServer
 *
 * @return string
 */
function getVirtualPath($forDocumentServer)
{
    $storagePath = trim(str_replace(['/', '\\'], '/', $GLOBALS['STORAGE_PATH']), '/');
    $storagePath = $storagePath != "" ? $storagePath . '/' : "";

    if (realpath($storagePath) === $storagePath) {
        $virtPath = serverPath($forDocumentServer) . '/' . $storagePath . '/';
    } else {
        $virtPath = serverPath($forDocumentServer) . '/' . $storagePath . getCurUserHostAddress() . '/';
    }
    sendlog("getVirtualPath virtPath: " . $virtPath, "common.log");
    return $virtPath;
}

/**
 * Get a file with meta information
 *
 * @param string $fileName
 * @param string $uid
 * @param string $uname
 * @param string $userAddress
 *
 * @return void
 */
function createMeta($fileName, $uid, $uname, $userAddress = null)
{
    $histDir = getHistoryDir(getStoragePath($fileName, $userAddress));  // get the history directory

    // turn the file information into the json format
    $json = [
        "created" => date("Y-m-d H:i:s"),
        "uid" => $uid,
        "name" => $uname,
    ];

    // write the encoded file information to the createdInfo.json file
    file_put_contents($histDir . DIRECTORY_SEPARATOR . "createdInfo.json", json_encode($json, JSON_PRETTY_PRINT));
}

/**
 * Get the file url
 *
 * @param string $file_name
 * @param string $forDocumentServer
 *
 * @return string
 */
function FileUri($file_name, $forDocumentServer = null)
{
    $uri = getVirtualPath($forDocumentServer) . rawurlencode($file_name);  // add encoded file name to the virtual path
    return $uri;
}

/**
 * Get file information
 *
 * @param string $fileId
 *
 * @return array|string
 */
function getFileInfo($fileId)
{
    $storedFiles = getStoredFiles();
    $result = [];
    $resultID = [];

    // run through all the stored files
    foreach ($storedFiles as $key => $value) {
        $result[$key] = (object) [  // write all the parameters to the map
            "version" => getFileVersion(getHistoryDir(getStoragePath($value->name))),
            "id" => getDocEditorKey($value->name),
            "contentLength" => number_format(filesize(getStoragePath($value->name)) / 1024, 2)." KB",
            "pureContentLength" => filesize(getStoragePath($value->name)),
            "title" => $value->name,
            "updated" => date(DATE_ATOM, filemtime(getStoragePath($value->name))),
        ];
        // get file information by its id
        if ($fileId != null) {
            if ($fileId == getDocEditorKey($value->name)) {
                $resultID[count($resultID)] = $result[$key];
            }
        }
    }

    if ($fileId != null) {
        if (count($resultID) != 0) {
            return $resultID;
        }
        return "File not found";
    }

    return $result;
}

/**
 * Get all the supported file extensions
 *
 * @return array
 */
function getFileExts()
{
    return array_merge($GLOBALS['DOC_SERV_VIEWD'],
        $GLOBALS['DOC_SERV_EDITED'],
        $GLOBALS['DOC_SERV_CONVERT'],
        $GLOBALS['DOC_SERV_FILLFORMS']);
}

/**
 * Get the correct file name if such a name already exists
 *
 * @param string $fileName
 * @param string $userAddress
 *
 * @return string
 */
function GetCorrectName($fileName, $userAddress = null)
{
    $path_parts = pathinfo($fileName);

    $ext = mb_strtolower($path_parts['extension']);
    $name = $path_parts['basename'];
    // get file name from the basename without extension
    $baseNameWithoutExt = mb_substr($name, 0, mb_strlen($name) - mb_strlen($ext) - 1);
    $name = $baseNameWithoutExt . "." . $ext;

    // if a file with such a name already exists in this directory
    for ($i = 1; file_exists(getStoragePath($name, $userAddress)); $i++) {
        $name = $baseNameWithoutExt . " (" . $i . ")." . $ext;  // add an index after its base name
    }
    return $name;
}

/**
 * Get document key
 *
 * @param string $fileName
 *
 * @return string
 */
function getDocEditorKey($fileName)
{
    // get document key by adding local file url to the current user host address
    $key = getCurUserHostAddress() . FileUri($fileName);
    $stat = filemtime(getStoragePath($fileName));  // get creation time
    $key = $key . $stat;  // and add it to the document key
    return GenerateRevisionId($key);  // generate the document key value
}
