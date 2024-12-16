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

namespace App\UseCases\Document\Delete;

use App\Helpers\Path\Path;
use App\Repositories\FileRepository;
use App\Repositories\VersionRepository;

class DeleteDocumentCommand
{
    public function __construct(
        private FileRepository $fileRepository,
        private VersionRepository $versionRepository,
    ) {}

    public function __invoke(DeleteDocumentRequest $request): void
    {
        $filePath = Path::join($request->userDirectory, $request->filename);
        $file = $this->fileRepository->find($filePath);

        if ($file) {
            $this->fileRepository->delete($file);
            $this->versionRepository->deleteAll($filePath);
        }
    }
}
