<?php
//
// (c) Copyright Ascensio System SIA 2023
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

namespace Example\Configuration;

use Example\Common\Path;
use Example\Common\URL;

class ConfigurationManager
{
    public string $version = '1.7.0';

    public function exampleURL(): ?URL
    {
        $url = getenv('EXAMPLE_URL');
        if (!$url) {
            return null;
        }
        return new URL($url);
    }

    public function documentServerPublicURL(): URL
    {
        $url = getenv('DOCUMENT_SERVER_PUBLIC_URL') ?: 'http://document-server';
        return new URL($url);
    }

    public function documentServerPrivateURL(): URL
    {
        $url = getenv('DOCUMENT_SERVER_PRIVATE_URL');
        if (!$url) {
            return $this->documentServerPublicURL();
        }
        return new URL($url);
    }

    public function documentServerAPIURL(): URL
    {
        $serverURL = $this->documentServerPublicURL();
        $path = getenv('DOCUMENT_SERVER_API_PATH')
            ?: 'web-apps/apps/api/documents/api.js';
        return $serverURL->joinPath($path);
    }

    public function documentServerPreloaderURL(): URL
    {
        $serverURL = $this->documentServerPublicURL();
        $path = getenv('DOCUMENT_SERVER_PRELOADER_PATH')
            ?: 'web-apps/apps/api/documents/cache-scripts.html';
        return $serverURL->joinPath($path);
    }

    public function documentServerCommandURL(): URL
    {
        $serverURL = $this->documentServerPrivateURL();
        $path = getenv('DOCUMENT_SERVER_COMMAND_PATH')
            ?: 'coauthoring/CommandService.ashx';
        return $serverURL->joinPath($path);
    }

    public function documentServerConverterURL(): URL
    {
        $serverURL = $this->documentServerPrivateURL();
        $path = getenv('DOCUMENT_SERVER_CONVERTER_PATH')
            ?: 'ConvertService.ashx';
        return $serverURL->joinPath($path);
    }

    public function jwtSecret(): string
    {
        return getenv('JWT_SECRET') ?: '';
    }

    public function jwtHeader(): string
    {
        return getenv('JWT_HEADER') ?: 'Authorization';
    }

    public function jwtUseForRequest(): bool
    {
        $use = getenv('JWT_USE_FOR_REQUEST');
        if (!$use) {
            return true;
        }
        return filter_var($use, FILTER_VALIDATE_BOOLEAN);
    }

    public function sslVerifyPeerModeEnabled(): bool
    {
        $enabled = getenv('SSL_VERIFY_PEER_MODE_ENABLED');
        if (!$enabled) {
            return false;
        }
        return filter_var($enabled, FILTER_VALIDATE_BOOLEAN);
    }

    public function storagePath(): Path
    {
        $storagePath = getenv('STORAGE_PATH') ?: 'storage';
        $storageDirectory = new Path($storagePath);
        if ($storageDirectory->absolute()) {
            return $storageDirectory;
        }

        $storageStringDirectory = $storageDirectory->string();
        $currentDirectory = new Path(__DIR__);
        $directory = $currentDirectory
            ->joinPath('..')
            ->joinPath('..')
            ->joinPath($storageStringDirectory);
        return $directory->normalize();
    }

    public function singleUser(): bool
    {
        $single = getenv('SINGLE_USER');
        if (!$single) {
            return false;
        }
        return filter_var($single, FILTER_VALIDATE_BOOLEAN);
    }

    public function maximumFileSize(): int
    {
        $size = getenv('MAXIMUM_FILE_SIZE');
        if (!$size) {
            return 5 * 1024 * 1024;
        }
        return intval($size);
    }

    public function conversionTimeout(): int
    {
        $timeout = getenv('CONVERSION_TIMEOUT');
        if (!$timeout) {
            return 120 * 1000;
        }
        return intval($timeout);
    }

    /**
     * @return string[]
     */
    public function languages(): array
    {
        return [
            'en' => "English",
            'hy' => 'Armenian',
            'az' => 'Azerbaijani',
            'eu' => 'Basque',
            'be' => 'Belarusian',
            'bg' => 'Bulgarian',
            'ca' => 'Catalan',
            'zh' => 'Chinese (Simplified)',
            'zh-TW' => 'Chinese (Traditional)',
            'cs' => 'Czech',
            'da' => 'Danish',
            'nl' => 'Dutch',
            'fi' => 'Finnish',
            'fr' => 'French',
            'gl' => 'Galego',
            'de' => 'German',
            'el' => 'Greek',
            'hu' => 'Hungarian',
            'id' => 'Indonesian',
            'it' => 'Italian',
            'ja' => 'Japanese',
            'ko' => 'Korean',
            'lo' => 'Lao',
            'lv' => 'Latvian',
            'ms' => 'Malay (Malaysia)',
            'no' => 'Norwegian',
            'pl' => 'Polish',
            'pt' => 'Portuguese (Brazil)',
            'pt-PT' => 'Portuguese (Portugal)',
            'ro' => 'Romanian',
            'ru' => 'Russian',
            'si' => 'Sinhala (Sri Lanka)',
            'sk' => 'Slovak',
            'sl' => 'Slovenian',
            'es' => 'Spanish',
            'sv' => 'Swedish',
            'tr' => 'Turkish',
            'uk' => 'Ukrainian',
            'vi' => 'Vietnamese',
            'aa-AA' => 'Test Language'
        ];
    }
}
