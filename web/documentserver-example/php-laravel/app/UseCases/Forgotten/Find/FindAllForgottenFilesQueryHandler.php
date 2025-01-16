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

namespace App\UseCases\Forgotten\Find;

use App\Helpers\Path\PathInfo;
use App\Helpers\URL\URL;
use App\OnlyOffice\Managers\FormatManager;
use App\OnlyOffice\Managers\SettingsManager;
use App\OnlyOffice\Miscellaneous\CommandRequest;
use Illuminate\Support\Str;

class FindAllForgottenFilesQueryHandler
{
    public function __construct(
        private SettingsManager $settings,
        private FormatManager $formatManager,
    ) {}

    public function __invoke(FindAllForgottenFilesQuery $query): array
    {
        $filesList = [];
        $commandRequest = app(CommandRequest::class);

        $result = $commandRequest->getForgottenList();
        $keys = $result->keys;

        foreach ($keys as $key) {
            $filesList[] = $commandRequest->getForgotten($key);
        }

        $files = [];

        foreach ($filesList as $fileItem) {
            $url = $fileItem->url;
            $url = Str::replace(URL::origin($url), $this->settings->getSetting('url.server.public'), $url);

            $files[] = [
                'key' => $fileItem->key,
                'filename' => $url,
                'url' => $url,
                'format' => $this->formatManager->find(PathInfo::extension($fileItem->url)),
            ];
        }

        return $files;
    }
}
