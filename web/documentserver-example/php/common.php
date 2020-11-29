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
require_once( dirname(__FILE__) . '/functions.php' );

function sendlog($msg, $logFileName) {
    $logsFolder = "logs/";
    if (!file_exists($logsFolder)) {
        mkdir($logsFolder);
    }
    file_put_contents($logsFolder . $logFileName, $msg . PHP_EOL, FILE_APPEND);
}

function guid() {
    if (function_exists('com_create_guid')) {
        return com_create_guid();
    } else {
        mt_srand((double)microtime()*10000);//optional for php 4.2.0 and up.
        $charid = strtoupper(md5(uniqid(rand(), true)));
        $hyphen = chr(45);// "-"
        $uuid = chr(123)// "{"
                .substr($charid, 0, 8).$hyphen
                .substr($charid, 8, 4).$hyphen
                .substr($charid,12, 4).$hyphen
                .substr($charid,16, 4).$hyphen
                .substr($charid,20,12)
                .chr(125);// "}"
        return $uuid;
    }
}

if(!function_exists('mime_content_type')) {
    function mime_content_type($filename) {

        $mime_types = array(

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
        );

        $ext = strtolower(array_pop(explode('.',$filename)));
        if (array_key_exists($ext, $mime_types)) {
            return $mime_types[$ext];
        }
        elseif (function_exists('finfo_open')) {
            $finfo = finfo_open(FILEINFO_MIME);
            $mimetype = finfo_file($finfo, $filename);
            finfo_close($finfo);
            return $mimetype;
        }
        else {
            return 'application/octet-stream';
        }
    }
}

function getClientIp() {
    $ipaddress =
        getenv('HTTP_CLIENT_IP')?:
        getenv('HTTP_X_FORWARDED_FOR')?:
        getenv('HTTP_X_FORWARDED')?:
        getenv('HTTP_FORWARDED_FOR')?:
        getenv('HTTP_FORWARDED')?:
        getenv('REMOTE_ADDR')?:
        'Storage';

    $ipaddress = preg_replace("/[^0-9a-zA-Z.=]/", "_", $ipaddress);

    return $ipaddress;
}

function serverPath($forDocumentServer = NULL) {
    return $forDocumentServer && isset($GLOBALS['EXAMPLE_URL']) && $GLOBALS['EXAMPLE_URL'] != ""
        ? $GLOBALS['EXAMPLE_URL']
        : (getScheme() . '://' . $_SERVER['HTTP_HOST']);
}

function getCurUserHostAddress($userAddress = NULL) {
    if ($GLOBALS['ALONE']) {
        if (empty($GLOBALS['STORAGE_PATH'])) {
            return "Storage";
        } else {
            return "";
        }
    }
    if (is_null($userAddress)) {$userAddress = getClientIp();}
    return preg_replace("[^0-9a-zA-Z.=]", '_', $userAddress);
}

function getInternalExtension($filename) {
    $ext = strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION));

    if (in_array($ext, $GLOBALS['ExtsDocument'])) return ".docx";
    if (in_array($ext, $GLOBALS['ExtsSpreadsheet'])) return ".xlsx";
    if (in_array($ext, $GLOBALS['ExtsPresentation'])) return ".pptx";
    return "";
}

function getDocumentType($filename) {
    $ext = strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION));

    if (in_array($ext, $GLOBALS['ExtsDocument'])) return "text";
    if (in_array($ext, $GLOBALS['ExtsSpreadsheet'])) return "spreadsheet";
    if (in_array($ext, $GLOBALS['ExtsPresentation'])) return "presentation";
    return "";
}

function getScheme() {
    return (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
}

function getStoragePath($fileName, $userAddress = NULL) {
    $storagePath = trim(str_replace(array('/','\\'), DIRECTORY_SEPARATOR, $GLOBALS['STORAGE_PATH']), DIRECTORY_SEPARATOR);
    $directory = __DIR__ . DIRECTORY_SEPARATOR . $storagePath;

    if ($storagePath != "")
    {
        $directory =  $directory  . DIRECTORY_SEPARATOR;

        if (!file_exists($directory) && !is_dir($directory)) {
            mkdir($directory);
        }
    }

    $directory = $directory . getCurUserHostAddress($userAddress) . DIRECTORY_SEPARATOR;

    if (!file_exists($directory) && !is_dir($directory)) {
        mkdir($directory);
    } 
    sendlog("getStoragePath result: " . $directory . $fileName, "common.log");
    return $directory . $fileName;
}

function getHistoryDir($storagePath) {
    $directory = $storagePath . "-hist";
    if (!file_exists($directory) && !is_dir($directory)) {
        mkdir($directory);
    }
    return $directory;
}

function getVersionDir($histDir, $version) {
    return $histDir . DIRECTORY_SEPARATOR . $version;
}

function getFileVersion($histDir) {
    if (!file_exists($histDir) || !is_dir($histDir)) return 0;

    $cdir = scandir($histDir);
    $ver = 0;
    foreach($cdir as $key => $fileName) {
        if (!in_array($fileName,array(".", ".."))) {
            if (is_dir($histDir . DIRECTORY_SEPARATOR . $fileName)) {
                $ver++;
            }
        }
    }
    return $ver;
}

function getStoredFiles() {
    $storagePath = trim(str_replace(array('/','\\'), DIRECTORY_SEPARATOR, $GLOBALS['STORAGE_PATH']), DIRECTORY_SEPARATOR);
    $directory = __DIR__ . DIRECTORY_SEPARATOR . $storagePath;

    $result = array();
    if ($storagePath != "")
    {
        $directory =  $directory . DIRECTORY_SEPARATOR;

        if (!file_exists($directory) && !is_dir($directory)) {
            return $result;
        }
    }

    $directory = $directory . getCurUserHostAddress() . DIRECTORY_SEPARATOR;

    if (!file_exists($directory) && !is_dir($directory)) {
        return $result;
    }

    $cdir = scandir($directory);
    $result = array();
    foreach($cdir as $key => $fileName) {
        if (!in_array($fileName,array(".", ".."))) {
            if (!is_dir($directory . DIRECTORY_SEPARATOR . $fileName)) {
                $dat = filemtime($directory . DIRECTORY_SEPARATOR . $fileName);
                $result[$dat] = (object) array(
                        "name" => $fileName,
                        "documentType" => getDocumentType($fileName)
                    );
            }
        }
    }
    ksort($result);
    return array_reverse($result);
}

function getVirtualPath($forDocumentServer) {
    $storagePath = trim(str_replace(array('/','\\'), '/', $GLOBALS['STORAGE_PATH']), '/');
    $storagePath = $storagePath != "" ? $storagePath . '/' : "";


    $virtPath = serverPath($forDocumentServer) . '/' . $storagePath . getCurUserHostAddress() . '/';
    sendlog("getVirtualPath virtPath: " . $virtPath, "common.log");
    return $virtPath;
}

function createMeta($fileName, $uid = "0") {
    $histDir = getHistoryDir(getStoragePath($fileName));

    if (empty($uid)) $uid = "0";

    $name = "";
    switch ($uid) {
        case 0:
            $name = "John Smith";
            break;
        case 1:
            $name = "Mark Pottato";
            break;
        case 2:
            $name = "Hamish Mitchell";
            break;
    }

    $json = [
        "created" => date("Y-m-d H:i:s"),
        "uid" => $uid,
        "name" => $name,
    ];

    file_put_contents($histDir . DIRECTORY_SEPARATOR . "createdInfo.json", json_encode($json, JSON_PRETTY_PRINT));
}

function FileUri($file_name, $forDocumentServer = NULL) {
    $uri = getVirtualPath($forDocumentServer) . rawurlencode($file_name);
    return $uri;
}

function getFileExts() {
    return array_merge($GLOBALS['DOC_SERV_VIEWD'], $GLOBALS['DOC_SERV_EDITED'], $GLOBALS['DOC_SERV_CONVERT']);
}

function GetCorrectName($fileName) {
    $path_parts = pathinfo($fileName);

    $ext = $path_parts['extension'];
    $name = $path_parts['basename'];
    $baseNameWithoutExt = substr($name, 0, strlen($name) - strlen($ext) - 1);

    for ($i = 1; file_exists(getStoragePath($name)); $i++)
    {
        $name = $baseNameWithoutExt . " (" . $i . ")." . $ext;
    }
    return $name;
}

function getDocEditorKey($fileName) {
    $key = getCurUserHostAddress() . FileUri($fileName);
    $stat = filemtime(getStoragePath($fileName));
    $key = $key . $stat;
    return GenerateRevisionId($key);
}

?>