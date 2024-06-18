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

use OnlyOffice\Entities\File;
use OnlyOffice\Repositories\Files\FileRepository;
use OnlyOffice\Config;
use Exception;

class Storage
{
    public function __construct(
        private FileRepository $repository,
        private Config $config,
    ) {}

    public function find(string $path, bool $withContent = false): File
    {
        return $this->repository->find($path, $withContent);
    }

    public function save(File $file): void
    {
        if ($file->size > $this->config->get('file.max_size'))
            throw new Exception('The file size exceeds the allowed maximum file size.');

        $this->repository->save($file);
    }

    public function update(File $file): void
    {
        
    }

    public function all(): array
    {
        return $this->repository->get($this->config->get('client.ip'));
    }

    public function delete(File $file)
    {
        $file->path = $this->config->get('client.ip') . '/' . $file->basename;
        $this->repository->delete($file);
    }
}
