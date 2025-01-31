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

namespace App\OnlyOffice\Managers;

use App\Helpers\URL\FileURL;
use App\Helpers\URL\TemplateURL;
use Onlyoffice\DocsIntegrationSdk\Manager\Document\DocumentManager as OnlyOfficeDocumentManager;

class DocumentManager extends OnlyOfficeDocumentManager
{
    private array $file;

    public function __construct(array $file, $systemLangCode = 'en')
    {
        $formats = app(FormatManager::class);
        $settings = app(SettingsManager::class);
        parent::__construct($settings, $formats, $systemLangCode);
        $this->file = $file;
    }

    public function getDocumentKey(string $fileId, bool $embedded)
    {
        return $this->file['key'];
    }

    public function getDocumentName(string $fileId)
    {
        return $this->file['filename'];
    }

    public static function getLangMapping()
    {
        return null;
    }

    public function getFileUrl(string $fileId)
    {
        return FileURL::download($this->file['filename'], $this->file['address']);
    }

    public function getCallbackUrl(string $fileId)
    {
        return FileURL::callback($this->file['filename'], $this->file['address']);
    }

    public function getTemplateImageUrl(string $fileId)
    {
        return TemplateURL::image($this->file['format']->getType());
    }

    public function getGobackUrl(string $fileId)
    {
        return $this->file['goback'] !== null ? $this->file['goback'] : '';
    }

    public function getCreateUrl(string $fileId)
    {
        return FileURL::create($this->file['format']->extension(), $this->file['user']);
    }

    public function getFile(): array
    {
        return $this->file;
    }
}
