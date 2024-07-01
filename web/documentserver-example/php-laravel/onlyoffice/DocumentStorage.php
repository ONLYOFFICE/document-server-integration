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

namespace OnlyOffice;

use Carbon\Carbon;
use OnlyOffice\Entities\File;
use OnlyOffice\Storage\Storage;
use OnlyOffice\Config;
use Exception;
use Illuminate\Support\Facades\Log;
use OnlyOffice\Helpers\Path;

class DocumentStorage
{
    const HISTORY_INDEX = 'history';

    public function __construct(
        private Storage $storage,
        private Config $config,
        private Formats $formats,
        private Users $users,
    ) {}

    public function find(string $path, bool $withContent = false): File
    {
        if (!$this->storage->fileExists($path)) {
            throw new Exception('The file was not found.');
        }

        $file = new File;
        $file->basename = Path::basename($path);
        $file->filename = Path::filename($path);
        $file->format = $this->formats->find(Path::extension($path));
        $file->size = $this->storage->size($path);
        $file->mime = $this->storage->mimeType($path);
        $file->lastModified = $this->storage->lastModified($path);
        $file->path = $path;

        if ($withContent) {
            $file->content = $this->storage->get($path);
        }

        return $file;
    }

    public function create(File $file): void
    {
        if ($file->size > $this->config->get('file.max_size')) {
            throw new Exception('The file size exceeds the allowed maximum file size.');
        }

        $this->storage->put($file->path, $file->content);

        $this->createMeta($file->path, $file->author->id, $file->basename);
    }

    public function createForm(array $data): void
    {
        $filename = Path::filename($data['filename']) . '-form.' . $data['extension'];
        $path = Path::join(Path::directory($data['path']), $filename);

        $this->storage->put($path, $data['content']);

        $path = Path::join($this->getHistoryDirectory($path), 'createdInfo.json');

        $meta = [
            "created" => Carbon::now(),
            "uid" => $data['user'],
            "name" => 'Filling Form',
        ];

        $this->storage->put($path, json_encode($meta, JSON_PRETTY_PRINT));
    }

    public function createFromTemplate(array $data, bool $withSample = false): File
    {
        $name = $withSample ? 'sample' : 'new';
        $extension = $data['extension'];
        $filename = "$name.$extension";
        $from = Path::join($this->config->get('path.template'), "$name/$filename");
        $to = Path::join($data['path'], $filename);

        $to = $this->storage->copy($from, $to);
        $this->createMeta($to, $data['user'], $filename);

        $file = new File;
        $file->basename = $filename;
        $file->path = $filename;
        $file->extension = $extension;
        $file->author = $this->users->find($data['user']);

        return $file;
    }

    public function save(array $data): void
    {
        $this->storage->put($data['path'], $data['content']);
    }

    public function forceSave(File $file): void
    {
        $file->path = Path::join($this->getHistoryDirectory($file->path), $file->basename);
        $this->storage->put($file->path, $file->content);
    }

    public function update(File $file, ?string $changes, ?string $history): void
    {
        $to = Path::join($this->getHistoryDirectory($file->path), 'prev.' . Path::extension($file->path));
        $this->storage->move($file->path, $to);

        $file->path = $this->storage->put($file->path, $file->content);
        $versionDirectory = $this->getVersionDirectory($file->path);

        if ($changes) {
            $this->storage->put(Path::join($versionDirectory, 'diff.zip'), $changes);
        }
        if ($history) {
            $this->storage->put(Path::join($versionDirectory, 'changes.json'), $history);
        }
        $this->storage->put(Path::join($versionDirectory, 'key.txt'), $file->key);
    }

    public function all(string $path): array
    {
        $files = [];
        $filesList = $this->storage->files($path);

        foreach ($filesList as $filePath) {
            $file = new File();
            $file->basename = Path::basename($filePath);
            $file->lastModified = $this->storage->lastModified($filePath);
            $file->format = $this->formats->find(Path::extension($filePath));
            $files[] = $file;
        }

        return $files;
    }

    public function deleteFile(File $file)
    {
        $this->storage->delete($file->path);
    }

    public function deleteHistory(File $file)
    {
        $this->storage->deleteDirectory($this->getHistoryDirectory($file->path));
    }

    public function deleteDirectory(string $path): void
    {
        $this->storage->deleteDirectory($path);
    }

    private function getHistoryDirectory(string $path): string
    {
        return $path . '-' . static::HISTORY_INDEX;
    }

    private function createMeta($path, $author, $name): void
    {
        $path = Path::join($this->getHistoryDirectory($path), 'createdInfo.json');

        $meta = [
            "created" => Carbon::now(),
            "uid" => $author,
            "name" => $name,
        ];

        $this->storage->put($path, json_encode($meta, JSON_PRETTY_PRINT));
    }

    private function determineVersion(string $path): int
    {
        $version = 1;
        $historyDirectory = $this->getHistoryDirectory($path);

        if (!$this->storage->directoryExists($historyDirectory)) {
            return $version;
        }

        $directories = $this->storage->directories($historyDirectory);
        $version += count($directories); 

        return $version;
    }

    private function getVersionDirectory(string $path): string
    {
        return Path::join($this->getHistoryDirectory($path), $this->determineVersion($path));
    }
}
