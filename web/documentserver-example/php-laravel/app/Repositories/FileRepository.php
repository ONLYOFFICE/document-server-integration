<?php

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
