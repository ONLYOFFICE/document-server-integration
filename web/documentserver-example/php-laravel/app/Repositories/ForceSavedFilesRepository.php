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

namespace App\Repositories;

use App\Helpers\Path\PathInfo;
use App\Models\File;
use Illuminate\Contracts\Filesystem\Filesystem;
use Illuminate\Support\Facades\Storage;

class ForceSavedFilesRepository
{
    private Filesystem $storage;

    public function __construct()
    {
        $this->storage = Storage::disk('forcesaved');
    }

    public function find(string $id): ?File
    {
        $file = null;

        if ($this->storage->exists($id)) {
            $file = File::create(
                PathInfo::basename($id),
                $this->storage->get($id),
                $this->storage->mimeType($id),
                $this->storage->size($id),
            );
        }

        return $file;
    }

    public function exists(string $id): bool
    {
        return $this->storage->exists($id);
    }

    public function delete(string $id): void
    {
        if ($this->storage->exists($id)) {
            $this->storage->delete($id);
        }
    }
}
