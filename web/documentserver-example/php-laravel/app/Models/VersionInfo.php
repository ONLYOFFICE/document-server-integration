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

namespace App\Models;

class VersionInfo
{
    public function __construct(
        public string $key,
        public string $fileType,
        public int $version,
        public string $created,
        public string $userId,
        public ?string $serverVersion,
    ) {}

    public static function create(
        string $key,
        string $fileType,
        int $version,
        string $created,
        string $userId,
        ?string $serverVersion = null,
    ): self {
        return new self(
            $key,
            $fileType,
            $version,
            $created,
            $userId,
            $serverVersion,
        );
    }

    public function toArray(): array
    {
        return [
            'key' => $this->key,
            'fileType' => $this->fileType,
            'version' => $this->version,
            'created' => $this->created,
            'user' => $this->userId,
            'serverVersion' => $this->serverVersion,
        ];
    }
}
