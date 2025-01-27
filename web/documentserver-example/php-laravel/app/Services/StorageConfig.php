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

class StorageConfig extends Config
{
    public function __construct()
    {
        $publicStorageUrl = rtrim(env('DOCUMENT_STORAGE_PUBLIC_URL', request()->schemeAndHttpHost()), '/');
        $privateStorageUrl = rtrim(env('DOCUMENT_STORAGE_PRIVATE_URL', $publicStorageUrl), '/');

        $this->config = [
            'url' => [
                'private' => $privateStorageUrl,
                'public' => $publicStorageUrl,
            ],
            'file' => [
                'max_size' => env('DOCUMENT_STORAGE_MAXIMUM_FILE_SIZE', 5 * 1024 * 1024),
            ],
        ];
    }
}
