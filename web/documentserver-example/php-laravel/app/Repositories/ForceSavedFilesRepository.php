<?php

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
