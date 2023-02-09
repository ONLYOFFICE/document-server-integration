<?php

namespace OnlineEditorsExamplePhp;

use OnlineEditorsExamplePhp\Helpers\ConfigManager;

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

/**
 * WebEditor AJAX Process Execution.
 */

require_once dirname(__FILE__) . '/ajax.php';
require_once dirname(__FILE__) . '/functions.php';
require_once dirname(__FILE__) . '/trackmanager.php';
require_once dirname(__FILE__) . '/vendor/autoload.php';

$configManager = new ConfigManager();
// define tracker status
$_trackerStatus = [
    0 => 'NotFound',
    1 => 'Editing',
    2 => 'MustSave',
    3 => 'Corrupted',
    4 => 'Closed',
    6 => 'MustForceSave',
    7 => 'CorruptedForceSave',
];

// ignore self-signed certificate
if ($configManager->getConfig("docServVerifyPeerOff") === true) {
    stream_context_set_default([
        'ssl' => [
            'verify_peer' => false,
            'verify_peer_name' => false,
        ],
    ]);
}

// check if type value exists
if (isset($_GET["type"]) && !empty($_GET["type"])) {
    $response_array;
    @header('Content-Type: application/json; charset==utf-8');
    @header('X-Robots-Tag: noindex');
    @header('X-Content-Type-Options: nosniff');

    // set headers that prevent caching in all the browsers
    nocacheHeaders();

    // write the request result to the log file
    sendlog(serialize($_GET), "webedior-ajax.log");

    $type = $_GET["type"];

    // switch case for type value
    switch ($type) {
        case "upload":
            $response_array = upload();
            $response_array['status'] = isset($response_array['error']) ? 'error' : 'success';
            die(json_encode($response_array));
        case "download":
            $response_array = download();
            $response_array['status'] = 'success';
            die(json_encode($response_array));
        case "history":
            $response_array = historyDownload();
            $response_array['status'] = 'success';
            die(json_encode($response_array));
        case "convert":
            $response_array = convert();
            $response_array['status'] = 'success';
            die(json_encode($response_array));
        case "track":
            $response_array = track();
            $response_array['status'] = 'success';
            die(json_encode($response_array));
        case "delete":
            $response_array = delete();
            $response_array['status'] = 'success';
            die(json_encode($response_array));
        case "assets":
            $response_array = assets();
            $response_array['status'] = 'success';
            die(json_encode($response_array));
        case "csv":
            $response_array = csv();
            $response_array['status'] = 'success';
            die(json_encode($response_array));
        case "files":
            $response_array = files();
            die(json_encode($response_array));
        case "saveas":
            $response_array = saveas();
            $response_array['status'] = 'success';
            die(json_encode($response_array));
        case "rename":
            $response_array = renamefile();
            die(json_encode($response_array));
        default:
            $response_array['status'] = 'error';
            $response_array['error'] = '404 Method not found';
            die(json_encode($response_array));
    }
}
