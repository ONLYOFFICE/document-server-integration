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

namespace App\OnlyOffice\Managers;

use App\OnlyOffice\Models\Format;
use Onlyoffice\DocsIntegrationSdk\Manager\Formats\FormatsManager;

class FormatManager extends FormatsManager
{
    public function __construct($nameAssoc = false)
    {
        parent::__construct();
        $this->createCustomList();
    }

    private function createCustomList()
    {
        $newFormats = [];

        foreach ($this->formatsList as $format) {
            $newFormats[] = new Format(
                $format->getName(),
                $format->getType(),
                $format->getActions(),
                $format->getConvert(),
                $format->getMimes(),
            );
        }

        $this->formatsList = $newFormats;
    }

    public function find(string $extension): ?Format
    {
        foreach ($this->formatsList as $format) {
            if ($format->extension() === $extension) {
                return $format;
            }
        }

        return null;
    }
}
