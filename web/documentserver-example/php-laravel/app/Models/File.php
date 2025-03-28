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

use UnexpectedValueException;

class File
{
    public function __construct(
        public string $filename,
        public mixed $content,
        public ?string $mime,
        public ?int $size,
        public ?int $modified,
    ) {}

    public static function create(
        string $filename,
        mixed $content = null,
        ?string $mime = null,
        ?int $size = null,
        ?int $modified = null,
    ): self {
        if ($size && $size <= 0) {
            throw new UnexpectedValueException('The file size cannot be less than zero');
        }

        return new self(
            $filename,
            $content,
            $mime,
            $size,
            $modified
        );
    }

    public function toArray(): array
    {
        return [
            'filename' => $this->filename,
            'content' => $this->content,
            'mimeType' => $this->mime,
            'size' => $this->size,
            'lastModified' => $this->modified,
        ];

    }
}
