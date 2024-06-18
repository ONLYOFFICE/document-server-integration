<?php

namespace OnlyOffice\Repositories\Files;

use OnlyOffice\Entities\File;

interface FileRepository
{
    public function get(string $path = ''): array;
    public function save(File $file): void;
    public function update(File $file): void;
    public function find(string $path, bool $withContent = false): File;
    public function delete(File $file): void;
    public function copy(File $file): void;
}
