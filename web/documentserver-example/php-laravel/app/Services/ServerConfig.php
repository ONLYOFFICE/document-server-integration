<?php

/**
 * (c) Copyright Ascensio System SIA 2025
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

namespace App\Services;

class ServerConfig extends Config
{
    public function __construct()
    {
        $publicServerUrl = rtrim(env('DOCUMENT_SERVER_PUBLIC_URL', 'http://documentserver'), '/');
        $privateServerUrl = rtrim(env('DOCUMENT_SERVER_PRIVATE_URL', $publicServerUrl), '/');
        $apiUrl = $publicServerUrl.'/'.env('DOCUMENT_SERVER_API_PATH', 'web-apps/apps/api/documents/api.js');
        $preloaderUrl = $publicServerUrl.'/'.env('DOCUMENT_SERVER_PRELOADER_PATH', 'web-apps/apps/api/documents/cache-scripts.html');
        $conversionUrl = $privateServerUrl.'/'.env('DOCUMENT_SERVER_CONVERTER_PATH', 'convert');
        $commandUrl = $privateServerUrl.'/'.env('DOCUMENT_SERVER_COMMAND_PATH', 'command');
        $jwtSecret = env('DOCUMENT_SERVER_JWT_SECRET', 'secret');
        $jwtUseForRequest = env('DOCUMENT_SERVER_JWT_USE_FOR_REQUEST', true);

        $this->config = [
            'conversion' => [
                'timeout' => env('DOCUMENT_SERVER_CONVERSION_TIMEOUT', 120 * 1000),
                'url' => $conversionUrl,
            ],

            'ssl_verify' => env('DOCUMENT_SERVER_SSL_VERIFY_PEER_MODE_ENABLED', false),
            'jwt' => [
                'enabled' => $jwtSecret && $jwtUseForRequest,
                'secret' => $jwtSecret,
                'header' => env('DOCUMENT_SERVER_JWT_HEADER', 'Authorization'),
                'use_for_request' => $jwtUseForRequest,
                'algorithm' => env('DOCUMENT_SERVER_JWT_ALGORITHM', 'HS256'),
            ],
            'url' => [
                'api' => $apiUrl,
                'public' => $publicServerUrl,
                'private' => $privateServerUrl,
                'preloader' => $preloaderUrl,
                'command' => $commandUrl,
            ],
        ];
    }
}
