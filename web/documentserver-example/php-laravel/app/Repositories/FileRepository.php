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

use App\Models\File;
use Illuminate\Contracts\Filesystem\Filesystem;
use Illuminate\Support\Facades\Storage;

class FileRepository
{
    private Filesystem $storage;

    public function __construct()
    {
        $this->storage = Storage::disk('files');
    }

    public function save(File $file): void
    {
        Storage::disk('files')->put($file->filename, $file->content);
    }

    public function find(string $filename): ?File
    {
        $file = null;

        if (Storage::disk('files')->exists($filename)) {
            $absPath = Storage::disk('files')->path($filename);
            $content = Storage::disk('files')->get($filename);
            $mimeType = mime_content_type($absPath);
            $size = Storage::disk('files')->size($filename);
            $lastModified = Storage::disk('files')->lastModified($filename);

            $file = File::create($filename, $content, $mimeType, $size, $lastModified);
        }

        return $file;
    }

    public function path(string $filename): string
    {
        return Storage::disk('files')->path($filename);
    }

    public function exists(string $filename): bool
    {
        return $this->storage->exists($filename);
    }

    public function all(string $userDirectory): array
    {
        $files = [];

        $filenames = Storage::disk('files')->files($userDirectory);

        foreach ($filenames as $filename) {
            $files[] = $this->find($filename);
        }

        return $files;
    }

    public function delete(File $file): void
    {
        if (Storage::disk('files')->exists($file->filename)) {
            Storage::disk('files')->delete($file->filename);
        }
    }
}
