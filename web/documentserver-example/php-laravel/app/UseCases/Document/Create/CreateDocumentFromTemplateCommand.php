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

namespace App\UseCases\Document\Create;

use App\Helpers\Path\Path;
use App\Helpers\Path\PathInfo;
use App\Helpers\Path\TemplatePath;
use App\Helpers\UniqueFilename;
use App\Models\Version;
use App\Models\VersionInfo;
use App\Repositories\VersionRepository;
use Illuminate\Support\Str;

class CreateDocumentFromTemplateCommand
{
    public function __construct(
        private VersionRepository $versionRepository,
    ) {}

    public function __invoke(CreateDocumentFromTemplateRequest $request): array
    {
        $from = TemplatePath::for($request->fileExtension, $request->withSample, $request->lang);

        $filePath = Path::join($request->userDirectory, PathInfo::basename($from));
        $filePath = UniqueFilename::for($filePath);

        $to = storage_path(Path::join('app/public/files', $filePath));

        copy($from, $to);

        $versionInfo = VersionInfo::create(
            key: Str::uuid(),
            fileType: $request->fileExtension,
            version: 1,
            created: now(),
            userId: $request->user
        );

        $version = Version::create($filePath, $versionInfo);
        $this->versionRepository->save($version);

        return [
            'filename' => PathInfo::basename($filePath),
        ];
    }
}
