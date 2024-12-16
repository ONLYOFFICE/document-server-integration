<?php

/**
 * (c) Copyright Ascensio System SIA 2024
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

namespace App\Helpers\URL;

use App\Services\StorageConfig;

class FileURL extends URL
{
    public static function download(string $filename, string $address = ''): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.private'), 'files/download', [
            'fileName' => $filename,
            'userAddress' => $address,
        ]);
    }

    public static function changes(string $filename, string $address, int $version): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.private'), 'files/versions/changes', [
            'filename' => $filename,
            'userAddress' => $address,
            'version' => $version,
        ]);
    }

    public static function previous(string $filename, string $address, int $version): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.private'), 'files/versions/previous', [
            'filename' => $filename,
            'userAddress' => $address,
            'version' => $version,
        ]);
    }

    public static function history(string $filename, string $address): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.private'), 'files/download', [
            'fileName' => $filename,
            'userAddress' => $address,
        ]);
    }

    public static function create(string $extension, string $user): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.public'), 'editor', [
            'fileExt' => $extension,
            'user' => $user,
        ]);
    }

    public static function callback(string $filename, string $user): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.private'), 'editor/track', [
            'fileName' => $filename,
            'userAddress' => $user,
        ]);
    }
}
