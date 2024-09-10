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

namespace App\UseCases\Document\Create;

use UnexpectedValueException;

class CreateDocumentRequest
{
    public function __construct(
        public string $filename,
        public string $userDirectory,
        public string $fileType,
        public ?int $fileSize,
        public mixed $fileContent,
        public string $user,
    ) {
        if ($fileSize && ($fileSize <= 0 || $fileSize > env('STORAGE_MAXIMUM_FILE_SIZE', 5 * 1024 * 1024))) {
            throw new UnexpectedValueException("Incorrect file size: $fileSize");
        }
    }
}