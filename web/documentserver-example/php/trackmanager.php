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

require_once( dirname(__FILE__) . '/jwtmanager.php' );
require_once( dirname(__FILE__) . '/common.php' );
require_once( dirname(__FILE__) . '/config.php' );


function readBody() {
    $result["error"] = 0;

    if (($body_stream = file_get_contents('php://input')) === FALSE) {
        $result["error"] = "Bad Request";
        return $result;
    }

    $data = json_decode($body_stream, TRUE); //json_decode - PHP 5 >= 5.2.0

    if ($data === NULL) {
        $result["error"] = "Bad Response";
        return $result;
    }

    sendlog("   InputStream data: " . serialize($data), "webedior-ajax.log");

    if (isJwtEnabled()) {
        sendlog("   jwt enabled, checking tokens", "webedior-ajax.log");

        $inHeader = false;
        $token = "";
        $jwtHeader = $GLOBALS['DOC_SERV_JWT_HEADER'] == "" ? "Authorization" : $GLOBALS['DOC_SERV_JWT_HEADER'];

        if (!empty($data["token"])) {
            $token = jwtDecode($data["token"]);
        } elseif (!empty(apache_request_headers()[$jwtHeader])) {
            $token = jwtDecode(substr(apache_request_headers()[$jwtHeader], strlen("Bearer ")));
            $inHeader = true;
        } else {
            sendlog("   jwt token wasn't found in body or headers", "webedior-ajax.log");
            $result["error"] = "Expected JWT";
            return $result;
        }
        if (empty($token)) {
            sendlog("   token was found but signature is invalid", "webedior-ajax.log");
            $result["error"] = "Invalid JWT signature";
            return $result;
        }

        $data = json_decode($token, true);
        if ($inHeader) $data = $data["payload"];
    }

    return $data;
}

function processSave($data, $fileName, $userAddress) {
    $downloadUri = $data["url"];

    $curExt = strtolower('.' . pathinfo($fileName, PATHINFO_EXTENSION));
    $downloadExt = strtolower('.' . pathinfo($downloadUri, PATHINFO_EXTENSION));
    $newFileName = $fileName;

    if ($downloadExt != $curExt) {
        $key = GenerateRevisionId($downloadUri);

        try {
            sendlog("   Convert " . $downloadUri . " from " . $downloadExt . " to " . $curExt, "webedior-ajax.log");
            $convertedUri;
            $percent = GetConvertedUri($downloadUri, $downloadExt, $curExt, $key, FALSE, $convertedUri);
            if (!empty($convertedUri)) {
                $downloadUri = $convertedUri;
            } else {
                sendlog("   Convert after save convertedUri is empty", "webedior-ajax.log");
                $baseNameWithoutExt = substr($fileName, 0, strlen($fileName) - strlen($curExt));
                $newFileName = GetCorrectName($baseNameWithoutExt . $downloadExt);
            }
        } catch (Exception $e) {
            sendlog("   Convert after save ".$e->getMessage(), "webedior-ajax.log");
            $baseNameWithoutExt = substr($fileName, 0, strlen($fileName) - strlen($curExt));
            $newFileName = GetCorrectName($baseNameWithoutExt . $downloadExt);
        }
    }

    $saved = 1;

    if (!(($new_data = file_get_contents($downloadUri)) === FALSE)) {
        $storagePath = getStoragePath($newFileName, $userAddress);
        $histDir = getHistoryDir($storagePath);
        $verDir = getVersionDir($histDir, getFileVersion($histDir));

        mkdir($verDir);

        rename(getStoragePath($fileName, $userAddress), $verDir . DIRECTORY_SEPARATOR . "prev" . $curExt);
        file_put_contents($storagePath, $new_data, LOCK_EX);

        if ($changesData = file_get_contents($data["changesurl"])) {
            file_put_contents($verDir . DIRECTORY_SEPARATOR . "diff.zip", $changesData, LOCK_EX);
        }

        $histData = $data["changeshistory"];
        if (empty($histData)) {
            $histData = json_encode($data["history"], JSON_PRETTY_PRINT);
        }
        if (!empty($histData)) {
            file_put_contents($verDir . DIRECTORY_SEPARATOR . "changes.json", $histData, LOCK_EX);
        }
        file_put_contents($verDir . DIRECTORY_SEPARATOR . "key.txt", $data["key"], LOCK_EX);

        $forcesavePath = getForcesavePath($newFileName, $userAddress, false);
        if ($forcesavePath != "") {
            unlink($forcesavePath);
        }

        $saved = 0;
    }

    $result["error"] = $saved;

    return $result;
}

function processForceSave($data, $fileName, $userAddress) {
    $downloadUri = $data["url"];

    $curExt = strtolower('.' . pathinfo($fileName, PATHINFO_EXTENSION));
    $downloadExt = strtolower('.' . pathinfo($downloadUri, PATHINFO_EXTENSION));

    if ($downloadExt != $curExt) {
        $key = GenerateRevisionId($downloadUri);

        try {
            sendlog("   Convert " . $downloadUri . " from " . $downloadExt . " to " . $curExt, "webedior-ajax.log");
            $convertedUri;
            $percent = GetConvertedUri($downloadUri, $downloadExt, $curExt, $key, FALSE, $convertedUri);
            if (!empty($convertedUri)) {
                $downloadUri = $convertedUri;
            } else {
                sendlog("   Convert after save convertedUri is empty", "webedior-ajax.log");
                $baseNameWithoutExt = substr($fileName, 0, strlen($fileName) - strlen($curExt));
                $fileName = GetCorrectName($baseNameWithoutExt . $downloadExt);
            }
        } catch (Exception $e) {
            sendlog("   Convert after save ".$e->getMessage(), "webedior-ajax.log");
            $baseNameWithoutExt = substr($fileName, 0, strlen($fileName) - strlen($curExt));
            $fileName = GetCorrectName($baseNameWithoutExt . $downloadExt);
        }
    }

    $saved = 1;

    if (!(($new_data = file_get_contents($downloadUri)) === FALSE)) {
        $forcesavePath = getForcesavePath($fileName, $userAddress, false);
        if ($forcesavePath == "") {
            $forcesavePath = getForcesavePath($fileName, $userAddress, true);
        }

        file_put_contents($forcesavePath, $new_data, LOCK_EX);

        $saved = 0;
    }

    $result["error"] = $saved;

    return $result;
}

function commandRequest($method, $key){
    $documentCommandUrl = $GLOBALS['DOC_SERV_SITE_URL'].$GLOBALS['DOC_SERV_COMMAND_URL'];

    $arr = [
        "c" => $method,
        "key" => $key
    ];

    $headerToken = "";
    $jwtHeader = $GLOBALS['DOC_SERV_JWT_HEADER'] == "" ? "Authorization" : $GLOBALS['DOC_SERV_JWT_HEADER'];

    if (isJwtEnabled()) {
        $headerToken = jwtEncode([ "payload" => $arr ]);
        $arr["token"] = jwtEncode($arr);
    }

    $data = json_encode($arr);

    $opts = array('http' => array(
        'method'  => 'POST',
        'header'=> "Content-type: application/json\r\n" .
            (empty($headerToken) ? "" : $jwtHeader.": Bearer $headerToken\r\n"),
        'content' => $data
    ));

    if (substr($documentCommandUrl, 0, strlen("https")) === "https") {
        $opts['ssl'] = array( 'verify_peer'   => FALSE );
    }

    $context = stream_context_create($opts);
    $response_data = file_get_contents($documentCommandUrl, FALSE, $context);

    return $response_data;
}

?>