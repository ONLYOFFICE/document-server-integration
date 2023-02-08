<?php

namespace OnlineEditorsExamplePhp\Helpers;

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

final class FileUtility
{
    /**
     * Put log files into the log folder
     *
     * @param string  $msg
     * @param integer $logFileName
     *
     * @return void
     */
    public function sendlog($msg, $logFileName)
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
    public function guid()
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

    /**
     * Get ip address
     *
     * @return string
     */
    public function getClientIp()
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
    public function serverPath($forDocumentServer = null)
    {
        $configManager = new ConfigManager();
        return $forDocumentServer && $configManager->getConfig("exampleUrl") !== null
        && $configManager->getConfig("exampleUrl") != ""
            ? $configManager->getConfig("exampleUrl")
            : ($this->getScheme() . '://' . $_SERVER['HTTP_HOST']);
    }

    /**
     * Get current user host address
     *
     * @param string  $userAddress
     *
     * @return string
     */
    public function getCurUserHostAddress($userAddress = null)
    {
        $configManager = new ConfigManager();
        if ($configManager->getConfig("alone")) {
            if (empty($configManager->getConfig("storagePath"))) {
                return "Storage";
            }
            return "";
        }
        if (is_null($userAddress)) {
            $userAddress = $this->getClientIp();
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
    public function getInternalExtension($filename)
    {
        $configManager = new ConfigManager();
        $ext = mb_strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION));

        if (in_array($ext, $configManager->getConfig("extsDocument"))) {
            return ".docx";
        }  // .docx for text document extensions
        if (in_array($ext, $configManager->getConfig("extsSpreadsheet"))) {
            return ".xlsx";
        }  // .xlsx for spreadsheet extensions
        if (in_array($ext, $configManager->getConfig("extsPresentation"))) {
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
    public function getTemplateImageUrl($filename)
    {
        $configManager = new ConfigManager();
        $ext = mb_strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION));
        $path = $this->serverPath(true) . "/css/images/";

        if (in_array($ext, $configManager->getConfig("extsDocument"))) {
            return $path . "file_docx.svg";
        }  // for text document extensions
        if (in_array($ext, $configManager->getConfig("extsSpreadsheet"))) {
            return $path . "file_xlsx.svg";
        }  // for spreadsheet extensions
        if (in_array($ext, $configManager->getConfig("extsPresentation"))) {
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
    public function getDocumentType($filename)
    {
        $configManager = new ConfigManager();
        $ext = mb_strtolower('.' . pathinfo($filename, PATHINFO_EXTENSION));

        if (in_array($ext, $configManager->getConfig("extsDocument"))) {
            return "word";
        }  // word for text document extensions
        if (in_array($ext, $configManager->getConfig("extsSpreadsheet"))) {
            return "cell";
        }  // cell for spreadsheet extensions
        if (in_array($ext, $configManager->getConfig("extsPresentation"))) {
            return "slide";
        }  // slide for presentation extensions
        return "word";
    }

    /**
     * Get the protocol
     *
     * @return string
     */
    public function getScheme()
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
    public function getStoragePath($fileName, $userAddress = null)
    {
        $configManager = new ConfigManager();
        $storagePath = trim(
            str_replace(
                ['/', '\\'],
                DIRECTORY_SEPARATOR,
                $configManager->getConfig("storagePath")
            ),
            DIRECTORY_SEPARATOR
        );
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
            $directory = $directory . $this->getCurUserHostAddress($userAddress) . DIRECTORY_SEPARATOR;
        }

        if (!file_exists($directory) && !is_dir($directory)) {
            mkdir($directory);
        }
        $this->sendlog("getStoragePath result: " . $directory . basename($fileName), "common.log");
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
    public function getForcesavePath($fileName, $userAddress, $create)
    {
        $configManager = new ConfigManager();
        $storagePath = trim(
            str_replace(
                ['/', '\\'],
                DIRECTORY_SEPARATOR,
                $configManager->getConfig("storagePath")
            ),
            DIRECTORY_SEPARATOR
        );

        // create the directory to this file version
        if (realpath($storagePath) === $storagePath) {
            $directory = $storagePath . DIRECTORY_SEPARATOR;
        } else {
            $directory = __DIR__ . DIRECTORY_SEPARATOR . $storagePath . $this->getCurUserHostAddress($userAddress) .
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
    public function getHistoryDir($storagePath)
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
    public function getVersionDir($histDir, $version)
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
    public function getFileVersion($histDir)
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
    public function getStoredFiles()
    {
        $configManager = new ConfigManager();
        $storagePath = trim(
            str_replace(
                ['/', '\\'],
                DIRECTORY_SEPARATOR,
                $configManager->getConfig("storagePath")
            ),
            DIRECTORY_SEPARATOR
        );
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
            $directory = $directory . $this->getCurUserHostAddress() . DIRECTORY_SEPARATOR;
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
                    // get the time of element modification
                    $dat = filemtime($directory . DIRECTORY_SEPARATOR . $fileName);
                    $result[$dat] = (object) [  // and write the file to the result
                        "name" => $fileName,
                        "documentType" => $this->getDocumentType($fileName),
                        "canEdit" => in_array($ext, $configManager->getConfig("docServEdited")),
                        "isFillFormDoc" => in_array($ext, $configManager->getConfig("docServFillforms")),
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
    public function getVirtualPath($forDocumentServer)
    {
        $configManager = new ConfigManager();
        $storagePath = trim(str_replace(['/', '\\'], '/', $configManager->getConfig("storagePath")), '/');
        $storagePath = $storagePath != "" ? $storagePath . '/' : "";

        if (realpath($storagePath) === $storagePath) {
            $virtPath = $this->serverPath($forDocumentServer) . '/' . $storagePath . '/';
        } else {
            $virtPath = $this->serverPath($forDocumentServer) . '/' .
                $storagePath . $this->getCurUserHostAddress() . '/';
        }
        $this->sendlog("getVirtualPath virtPath: " . $virtPath, "common.log");
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
    public function createMeta($fileName, $uid, $uname, $userAddress = null)
    {
        $histDir = $this->getHistoryDir($this->getStoragePath($fileName, $userAddress));  // get the history directory

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
    public function fileUri($file_name, $forDocumentServer = null)
    {
        // add encoded file name to the virtual path
        $uri = $this->getVirtualPath($forDocumentServer) . rawurlencode($file_name);
        return $uri;
    }

    /**
     * Get file information
     *
     * @param string $fileId
     *
     * @return array|string
     */
    public function getFileInfo($fileId)
    {
        $storedFiles = $this->getStoredFiles();
        $result = [];
        $resultID = [];

        // run through all the stored files
        foreach ($storedFiles as $key => $value) {
            $result[$key] = (object) [  // write all the parameters to the map
                "version" => $this->getFileVersion($this->getHistoryDir($this->getStoragePath($value->name))),
                "id" => $this->getDocEditorKey($value->name),
                "contentLength" => number_format(filesize($this->getStoragePath($value->name)) / 1024, 2)." KB",
                "pureContentLength" => filesize($this->getStoragePath($value->name)),
                "title" => $value->name,
                "updated" => date(DATE_ATOM, filemtime($this->getStoragePath($value->name))),
            ];
            // get file information by its id
            if ($fileId != null) {
                if ($fileId == $this->getDocEditorKey($value->name)) {
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
    public function getFileExts()
    {
        $configManager = new ConfigManager();
        return array_merge(
            $configManager->getConfig("docServViewd"),
            $configManager->getConfig("docServEdited"),
            $configManager->getConfig("docServConvert"),
            $configManager->getConfig("docServFillforms"),
        );
    }

    /**
     * Get the correct file name if such a name already exists
     *
     * @param string $fileName
     * @param string $userAddress
     *
     * @return string
     */
    public function getCorrectName($fileName, $userAddress = null)
    {
        $path_parts = pathinfo($fileName);

        $ext = mb_strtolower($path_parts['extension']);
        $name = $path_parts['basename'];
        // get file name from the basename without extension
        $baseNameWithoutExt = mb_substr($name, 0, mb_strlen($name) - mb_strlen($ext) - 1);
        $name = $baseNameWithoutExt . "." . $ext;

        // if a file with such a name already exists in this directory
        for ($i = 1; file_exists($this->getStoragePath($name, $userAddress)); $i++) {
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
    public function getDocEditorKey($fileName)
    {
        $utils = new Utils();
        // get document key by adding local file url to the current user host address
        $key = $this->getCurUserHostAddress() . $this->fileUri($fileName);
        $stat = filemtime($this->getStoragePath($fileName));  // get creation time
        $key = $key . $stat;  // and add it to the document key
        return $utils->generateRevisionId($key);  // generate the document key value
    }

    /**
     * Get demo file name by the extension
     *
     * @param string $createExt
     * @param Users $user
     *
     * @return string
     */
    public function tryGetDefaultByType($createExt, $user)
    {
        $demoName = ($_GET["sample"] ? "sample." : "new.") . $createExt;
        $demoPath = "assets" . DIRECTORY_SEPARATOR . ($_GET["sample"] ? "sample" : "new") . DIRECTORY_SEPARATOR;
        $demoFilename = $this->getCorrectName($demoName);

        if (!@copy(
            dirname(__FILE__) .
            DIRECTORY_SEPARATOR .
            $demoPath .
            $demoName,
            $this->getStoragePath($demoFilename)
        )) {
            $this->sendlog("Copy file error to ". $this->getStoragePath($demoFilename), "common.log");
            // Copy error!!!
        }

        // create demo file meta information
        $this->createMeta($demoFilename, $user->id, $user->name);

        return $demoFilename;
    }

    /**
     * Get the callback url
     *
     * @param string $fileName
     *
     * @return string
     */
    public function getCallbackUrl($fileName)
    {
        return $this->serverPath(true) . '/'
            . "webeditor-ajax.php"
            . "?type=track"
            . "&fileName=" . urlencode($fileName)
            . "&userAddress=" . $this->getClientIp();
    }

    /**
     * Get url to the created file
     *
     * @param string $fileName
     * @param string $uid
     * @param string $type
     *
     * @return string
     */
    public function getCreateUrl($fileName, $uid, $type)
    {
        $ext = trim($this->getInternalExtension($fileName), '.');
        return $this->serverPath(false) . '/'
            . "doceditor.php"
            . "?fileExt=" . $ext
            . "&user=" . $uid
            . "&type=" . $type;
    }

    /**
     * Get url for history download
     *
     * @param string $fileName
     * @param string $version
     * @param string $file
     * @param bool $isServer
     *
     * @return string
     */
    public function getHistoryDownloadUrl($fileName, $version, $file, $isServer = true)
    {
        $userAddress = $isServer ? "&userAddress=" . $this->getClientIp() : "";
        return $this->serverPath($isServer) . '/'
            . "webeditor-ajax.php"
            . "?type=history"
            . "&fileName=" . urlencode($fileName)
            . "&ver=" . $version
            . "&file=" . urlencode($file)
            . $userAddress;
    }

    /**
     * Get url to download a file
     *
     * @param string $fileName
     * @param bool $isServer
     *
     * @return string
     */
    public function getDownloadUrl($fileName, $isServer = true)
    {
        $userAddress = $isServer ? "&userAddress=" . $this->getClientIp() : "";
        return $this->serverPath($isServer) . '/'
            . "webeditor-ajax.php"
            . "?type=download"
            . "&fileName=" . urlencode($fileName)
            . $userAddress;
    }

    /**
     * Get document history
     *
     * @param string $filename
     * @param string $filetype
     * @param string $docKey
     * @param string $fileuri
     * @param bool $isEnableDirectUrl
     *
     * @return array
     */
    public function getHistory($filename, $filetype, $docKey, $fileuri, $isEnableDirectUrl)
    {
        $configManager = new ConfigManager();
        $storagePath = $configManager->getConfig("storagePath");
        $histDir = $this->getHistoryDir($this->getStoragePath($filename));  // get the path to the file history

        // check if the file was modified (the file version is greater than 0)
        if ($this->getFileVersion($histDir) > 0) {
            $curVer = $this->getFileVersion($histDir);

            $hist = [];
            $histData = [];

            for ($i = 1; $i <= $curVer; $i++) {  // run through all the file versions
                $obj = [];
                $dataObj = [];
                $verDir = $this->getVersionDir($histDir, $i);  // get the path to the file version

                // get document key
                $key = $i == $curVer ? $docKey : file_get_contents($verDir . DIRECTORY_SEPARATOR . "key.txt");
                $obj["key"] = $key;
                $obj["version"] = $i;

                if ($i == 1) {  // check if the version number is equal to 1
                    // get meta data of this file
                    $createdInfo = file_get_contents($histDir . DIRECTORY_SEPARATOR . "createdInfo.json");
                    $json = json_decode($createdInfo, true);  // decode the meta data from the createdInfo.json file

                    $obj["created"] = $json["created"];
                    $obj["user"] = [
                        "id" => $json["uid"],
                        "name" => $json["name"],
                    ];
                }

                $fileExe = mb_strtolower(pathinfo($filename, PATHINFO_EXTENSION));

                $prevFileName = $verDir . DIRECTORY_SEPARATOR . "prev." . $filetype;
                $prevFileName = mb_substr($prevFileName, mb_strlen($this->getStoragePath("")));
                $dataObj["fileType"] = $fileExe;
                $dataObj["key"] = $key;

                $directUrl = $i == $curVer ? $this->fileUri($filename, false) :
                    $this->getHistoryDownloadUrl($filename, $i, "prev.".$fileExe, false);
                $prevFileUrl = $i == $curVer ? $fileuri : $this->getHistoryDownloadUrl($filename, $i, "prev.".$fileExe);
                if (realpath($storagePath) === $storagePath) {
                    $prevFileUrl = $i == $curVer ? $this->getDownloadUrl($filename) :
                        $this->getHistoryDownloadUrl($filename, $i, "prev.".$fileExe);
                    if ($isEnableDirectUrl) {
                        $directUrl = $i == $curVer ? $this->getDownloadUrl($filename, false) :
                            $this->getHistoryDownloadUrl($filename, $i, "prev.".$fileExe, false);
                    }
                }

                $dataObj["url"] = $prevFileUrl;  // write file url to the data object
                if ($isEnableDirectUrl) {
                    $dataObj["directUrl"] = $directUrl;  // write direct url to the data object
                }
                $dataObj["version"] = $i;

                if ($i > 1) {  // check if the version number is greater than 1 (the document was modified)
                    $changes = json_decode(file_get_contents($this->getVersionDir($histDir, $i - 1) .
                        DIRECTORY_SEPARATOR . "changes.json"), true);  // get the path to the changes.json file
                    $change = $changes["changes"][0];

                    // write information about changes to the object
                    $obj["changes"] = $changes ? $changes["changes"] : null;
                    $obj["serverVersion"] = $changes["serverVersion"];
                    $obj["created"] = $change ? $change["created"] : null;
                    $obj["user"] = $change ? $change["user"] : null;

                    $prev = $histData[$i - 2];  // get the history data from the previous file version
                    // write information about previous file version to the data object
                    $dataObj["previous"] = $isEnableDirectUrl ? [
                        "fileType" => $prev["fileType"],
                        "key" => $prev["key"],
                        "url" => $prev["url"],
                        "directUrl" => $prev["directUrl"],
                    ] : [
                        "fileType" => $prev["fileType"],
                        "key" => $prev["key"],
                        "url" => $prev["url"],
                    ];

                    // write the path to the diff.zip archive with differences in this file version
                    $dataObj["changesUrl"] = $this->getHistoryDownloadUrl($filename, $i - 1, "diff.zip");
                }

                $jwtManager = new JwtManager();
                if ($jwtManager->isJwtEnabled()) {
                    $dataObj["token"] = $jwtManager->jwtEncode($dataObj);
                }

                $hist[] = $obj;  // add object dictionary to the hist list
                $histData[$i - 1] = $dataObj;  // write data object information to the history data
            }

            // write history information about the current file version
            $out = [];
            array_push(
                $out,
                [
                    "currentVersion" => $curVer,
                    "history" => $hist,
                ],
                $histData
            );
            return $out;
        }
        return [];
    }
}
