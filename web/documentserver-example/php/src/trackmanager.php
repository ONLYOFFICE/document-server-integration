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
use Example\Common\URL;
use Example\Configuration\ConfigurationManager;
use Example\Helpers\JwtManager;
use Example\Proxy\ProxyManager;

/**
 * Read request body
 *
 * @return int|array
 */
function readBody()
{
    $configManager = new ConfigurationManager();

    $result["error"] = 0;
    $jwtManager = new JwtManager();
    // get the body of the post request and check if it is correct
    if (($bodyStream = file_get_contents('php://input')) === false) {
        $result["error"] = "Bad Request";
        return $result;
    }

    $data = json_decode($bodyStream, false);

    // check if the response is correct
    if ($data === null) {
        $result["error"] = "Bad Response";
        return $result;
    }

    sendlog("   InputStream data: " . serialize($data), "webedior-ajax.log");

    // check if the document token is enabled
    if ($jwtManager->isJwtEnabled() && $jwtManager->tokenUseForRequest()) {
        sendlog("   jwt enabled, checking tokens", "webedior-ajax.log");

        $inHeader = false;
        $data = "";
        $jwtHeader = $configManager->jwtHeader();

        if (!empty($data["token"])) {  // if the document token is in the data
            $data = $jwtManager->jwtDecode($data["token"]);  // decode it
            sendlog("   jwt in body", "webedior-ajax.log");
        } elseif (!empty(apache_request_headers()[$jwtHeader])) {  // if the Authorization header exists
            $data = $jwtManager->jwtDecode(
                mb_substr(
                    apache_request_headers()[$jwtHeader],
                    mb_strlen("Bearer ")
                )
            );  // decode its part after Authorization prefix
            $inHeader = true;
            sendlog("   jwt in header", "webedior-ajax.log");
        } else {  // otherwise, an error occurs
            sendlog("   jwt token wasn't found in body or headers", "webedior-ajax.log");
            $result["error"] = "Expected JWT";
            return $result;
        }

        if ($data === "") {  // invalid signature error
            sendlog("   token was found but signature is invalid", "webedior-ajax.log");
            $result["error"] = "Invalid JWT signature";
            return $result;
        }

        if ($inHeader) {
            $data = $data->payload;
        }
    }

    return $data;
}

/**
 * File saving process
 *
 * @param string $data
 * @param string $fileName
 * @param string $userAddress
 *
 * @return array
 */
function processSave($rawData, $fileName, $userAddress)
{
    $data = resolveProcessSaveData($rawData);

    $downloadUri = $data->url;
    if ($downloadUri === null) {
        $result["error"] = 1;
        return $result;
    }

    $curExt = mb_strtolower('.' . pathinfo($fileName, PATHINFO_EXTENSION));  // get current file extension
    $downloadExt = mb_strtolower('.' . $data->filetype);  // get the extension of the downloaded file

    $newFileName = $fileName;

    // convert downloaded file to the file with the current extension if these extensions aren't equal
    if ($downloadExt != $curExt) {
        $key = generateRevisionId($downloadUri);

        try {
            sendlog("   Convert " . $downloadUri . " from " . $downloadExt . " to " . $curExt, "webedior-ajax.log");
            // convert file and give url to a new file
            $convertedData = getConvertedData($downloadUri, $downloadExt, $curExt, $key, false, $convertedUri);
            if (!empty($convertedUri)) {
                $downloadUri = $convertedUri;
            } else {
                sendlog("   Convert after save convertedUri is empty", "webedior-ajax.log");
                $baseNameWithoutExt = mb_substr($fileName, 0, mb_strlen($fileName) - mb_strlen($curExt));

                // get the correct file name if it already exists
                $newFileName = GetCorrectName($baseNameWithoutExt . $downloadExt, $userAddress);
            }
        } catch (Exception $e) {
            sendlog("   Convert after save ".$e->getMessage(), "webedior-ajax.log");
            $baseNameWithoutExt = mb_substr($fileName, 0, mb_strlen($fileName) - mb_strlen($curExt));
            $newFileName = GetCorrectName($baseNameWithoutExt . $downloadExt, $userAddress);
        }
    }

    $saved = 1;

    if (!(($newData = file_get_contents(
        $downloadUri,
        false,
        stream_context_create(["http" => ["timeout" => 5]])
    )) === false)
    ) {
        $storagePath = getStoragePath($newFileName, $userAddress);  // get the file path
        $histDir = getHistoryDir($storagePath);  // get the path to the history direction
        $verDir = getVersionDir($histDir, getFileVersion($histDir));  // get the path to the file version

        mkdir($verDir);  // if the path doesn't exist, create it

        // get the path to the previous file version and rename the storage path with it
        rename(getStoragePath($fileName, $userAddress), $verDir .
            DIRECTORY_SEPARATOR . "prev" . $curExt);
        file_put_contents($storagePath, $newData, LOCK_EX);  // save file to the storage directory

        if ($changesData = file_get_contents(
            $data->changesurl,
            false,
            stream_context_create(["http" => ["timeout" => 5]])
        )
        ) {
            // save file changes to the diff.zip archive
            file_put_contents($verDir . DIRECTORY_SEPARATOR .
                "diff.zip", $changesData, LOCK_EX);
        }

        $histData = empty($data->changeshistory) ? null : $data->changeshistory;
        if (empty($histData)) {
            $histData = json_encode($data->history, JSON_PRETTY_PRINT);
        }
        if (!empty($histData)) {
            // write the history changes to the changes.json file
            file_put_contents($verDir .
                DIRECTORY_SEPARATOR . "changes.json", $histData, LOCK_EX);
        }
        // write the key value to the key.txt file
        file_put_contents($verDir .
            DIRECTORY_SEPARATOR . "key.txt", $data->key, LOCK_EX);

        // get the path to the forcesaved file version
        $forcesavePath = getForcesavePath($newFileName, $userAddress, false);
        if ($forcesavePath != "") {  // if the forcesaved file version exists
            unlink($forcesavePath);  // remove it
        }

        $saved = 0;
    }

    $result["error"] = $saved;

    return $result;
}

/**
 * File force saving process
 *
 * @param string $data
 * @param string $fileName
 * @param string $userAddress
 *
 * @return array
 */
function processForceSave($data, $fileName, $userAddress)
{
    $downloadUri = $data->url;
    if ($downloadUri === null) {
        $result["error"] = 1;
        return $result;
    }

    $curExt = mb_strtolower('.' . pathinfo($fileName, PATHINFO_EXTENSION));  // get current file extension
    $downloadExt = mb_strtolower('.' . $data->filetype);  // get the extension of the downloaded file

    $newFileName = false;

    // convert downloaded file to the file with the current extension if these extensions aren't equal
    if ($downloadExt != $curExt) {
        $key = generateRevisionId($downloadUri);

        try {
            sendlog("   Convert " . $downloadUri . " from " . $downloadExt . " to " . $curExt, "webedior-ajax.log");
            // convert file and give url to a new file
            $convertedData = getConvertedData($downloadUri, $downloadExt, $curExt, $key, false, $convertedUri);
            if (!empty($convertedUri)) {
                $downloadUri = $convertedUri;
            } else {
                sendlog("   Convert after save convertedUri is empty", "webedior-ajax.log");
                $baseNameWithoutExt = mb_substr($fileName, 0, mb_strlen($fileName) - mb_strlen($curExt));
                $newFileName = true;
            }
        } catch (Exception $e) {
            sendlog("   Convert after save ".$e->getMessage(), "webedior-ajax.log");
            $newFileName = true;
        }
    }

    $saved = 1;

    if (!(($newData = file_get_contents(
        $downloadUri,
        false,
        stream_context_create(["http" => ["timeout" => 5]])
    )) === false)
    ) {
        $baseNameWithoutExt = mb_substr($fileName, 0, mb_strlen($fileName) - mb_strlen($curExt));
        $isSubmitForm = $data->forcesavetype == 3;  // SubmitForm

        if ($isSubmitForm) {
            if ($newFileName) {
                $fileName = GetCorrectName($baseNameWithoutExt .
                    "-form" . $downloadExt, $userAddress);  // get the correct file name if it already exists
            } else {
                $fileName = GetCorrectName($baseNameWithoutExt . "-form" . $curExt, $userAddress);
            }
            $forcesavePath = getStoragePath($fileName, $userAddress);
        } else {
            if ($newFileName) {
                $fileName = GetCorrectName($baseNameWithoutExt . $downloadExt, $userAddress);
            }
            // create forcesave path if it doesn't exist
            $forcesavePath = getForcesavePath($fileName, $userAddress, false);
            if ($forcesavePath == "") {
                $forcesavePath = getForcesavePath($fileName, $userAddress, true);
            }
        }

        file_put_contents($forcesavePath, $newData, LOCK_EX);

        if ($isSubmitForm) {
            $uid = $data->actions[0]->userid;  // get the user id
            createMeta($fileName, $uid, "Filling Form", $userAddress);  // create meta data for the forcesaved file
        }

        $saved = 0;
    }

    $result["error"] = $saved;

    return $result;
}

/**
 * Create a command request
 *
 * @param string $method
 * @param string $key
 * @param string $meta
 *
 * @return false|string
 */
function commandRequest($method, $key, $meta = null)
{
    $configManager = new ConfigurationManager();

    $jwtManager = new JwtManager();
    $documentCommandUrl = $configManager->documentServerCommandURL()->string();

    $arr = [
        "c" => $method,
        "key" => $key,
    ];

    if ($meta) {
        $arr["meta"] = $meta;
    }

    $headerToken = "";
    $jwtHeader = $configManager->jwtHeader();

    // check if a secret key to generate token exists or not
    if ($jwtManager->isJwtEnabled() && $jwtManager->tokenUseForRequest()) {
        $headerToken = $jwtManager->jwtEncode(["payload" => $arr]);  // encode a payload object into a header token
        $arr["token"] = $jwtManager->jwtEncode($arr);  // encode a payload object into a body token
    }

    $data = json_encode($arr);

    $opts = ['http' => [
        'method' => 'POST',
        'header' => "Content-type: application/json\r\n" .
            // add a header Authorization with a header token and Authorization prefix in it
            (empty($headerToken) ? "" : $jwtHeader.
                ": Bearer $headerToken\r\n"),
        'content' => $data,
    ]];

    if (mb_substr($documentCommandUrl, 0, mb_strlen("https")) === "https") {
        if ($configManager->sslVerifyPeerModeEnabled()) {
            $opts['ssl'] = ['verify_peer' => false, 'verify_peer_name' => false];
        }
    }

    $context = stream_context_create($opts);
    $responseData = file_get_contents($documentCommandUrl, false, $context);

    return $responseData;
}

function resolveProcessSaveData($data)
{
    $configManager = new ConfigurationManager();
    $proxyManager = new ProxyManager($configManager);
    $copied = clone $data;

    $url = $copied->url;
    if ($url) {
        $parsedURL = new URL($url);
        $resolvedURL = $proxyManager->resolveURL($parsedURL);
        $copied->url = $resolvedURL->string();
    }

    $changesURL = $copied->changesurl;
    if ($changesURL) {
        $parsedURL = new URL($changesURL);
        $resolvedURL = $proxyManager->resolveURL($parsedURL);
        $copied->changesurl = $resolvedURL->string();
    }

    return $copied;
}
