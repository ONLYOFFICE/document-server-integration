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

namespace App\UseCases\Editor\Create;

class CreateConfigRequest
{
    public function __construct(
        public string $filename,
        public string $fileExtension,
        public string $fileKey,
        public string $fileUrl,
        public string $user,
        public string $mode,
        public string $type,
        public string $lang,
        public string $userAddress,
        public string $serverAddress,
        public string $createUrl,
        public string $templatesImageUrl,
        public string $actionLink,
        public string $callbackUrl,
        public string $imagesUrl,
        public string $directUrl,
    ) {}
}
