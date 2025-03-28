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

class Version
{
    public function __construct(
        public string $filename,
        public VersionInfo $info,
        public mixed $history,
        public mixed $changes,
    ) {}

    public static function create(
        string $filename,
        VersionInfo $info,
        mixed $history = null,
        mixed $changes = null,
    ): self {
        return new self(
            $filename,
            $info,
            $history,
            $changes,
        );
    }

    public function version(): int
    {
        return $this->info->version;
    }

    public function fileType(): string
    {
        return $this->info->fileType;
    }

    public function key(): string
    {
        return $this->info->key;
    }

    public function user(): string
    {
        return $this->info->userId;
    }
}
