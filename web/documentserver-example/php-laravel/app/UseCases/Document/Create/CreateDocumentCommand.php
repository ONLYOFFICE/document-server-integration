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

use App\Helpers\Path\Path;
use App\Helpers\Path\PathInfo;
use App\Helpers\UniqueFilename;
use App\Models\File;
use App\Models\Version;
use App\Models\VersionInfo;
use App\Repositories\FileRepository;
use App\Repositories\FormatRepository;
use App\Repositories\UserRepository;
use App\Repositories\VersionRepository;
use Illuminate\Support\Str;
use UnexpectedValueException;

class CreateDocumentCommand
{
    public function __construct(
        private FileRepository $fileRepository,
        private FormatRepository $formatRepository,
        private UserRepository $userRepository,
        private VersionRepository $versionRepository,
    ) {}

    public function __invoke(CreateDocumentRequest $request): array
    {
        $filePath = Path::join($request->userDirectory, $request->filename);
        $filePath = UniqueFilename::for($filePath);

        $format = $this->formatRepository->find($request->fileType);
        $user = $this->userRepository->find($request->user);

        if ($format === null || empty($format->actions)) {
            throw new UnexpectedValueException("The $request->fileType format is not supported");
        }

        $file = File::create($filePath, $request->fileContent, $request->fileSize);

        $this->fileRepository->save($file);

        $versionInfo = VersionInfo::create(
            Str::uuid(),
            $request->fileType,
            1,
            now(),
            $user->id,
        );

        $version = Version::create($filePath, $versionInfo);

        $this->versionRepository->save($version);

        return [
            'filename' => PathInfo::basename($filePath),
        ];
    }
}
