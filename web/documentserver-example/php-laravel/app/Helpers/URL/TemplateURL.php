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

namespace App\Helpers\URL;

use App\Services\StorageConfig;

class TemplateURL extends URL
{
    public static function image(string $type)
    {
        $config = app(StorageConfig::class);
        $name = 'file_docx.svg';

        $name = match ($type) {
            'word' => 'file_docx.svg',
            'cell' => 'file_xlsx.svg',
            'slide' => 'file_pptx.svg',
            default => 'file_docx.svg',
        };

        return static::build($config->get('url.public'), "/images/$name");
    }
}
