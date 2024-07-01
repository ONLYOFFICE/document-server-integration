<?php

namespace App\OnlyOffice\Storage;

use Exception;
use Illuminate\Support\Facades\Log;
use OnlyOffice\Helpers\Path;
use OnlyOffice\Storage\Storage;
use Illuminate\Support\Facades\Storage as LaravelStorage;
use SplFileInfo;

class LocalStorage implements Storage
{
    public function size(string $path): int
    {
        $absolutePath = LaravelStorage::disk('files')->path($path);
        $info = new SplFileInfo($absolutePath);
        return $info->getSize();
    }

    public function lastModified(string $path): int
    {
        $absolutePath = LaravelStorage::disk('files')->path($path);
        $info = new SplFileInfo($absolutePath);
        return $info->getMTime();
    }

    public function mimeType(string $path): string
    {
        $absolutePath = LaravelStorage::disk('files')->path($path);
        return mime_content_type($absolutePath);
    }

    public function put(string $path, mixed $content): string
    {
        $tmpfile = tmpfile();
        fwrite($tmpfile, $content);

        $properPath = $this->getProperPath($path);

        $path = LaravelStorage::disk('files')->put($properPath, $tmpfile);

        fclose($tmpfile);

        if ($path === false) {
            throw new Exception('Could not save the file.');
        }

        return $properPath;
    }

    public function move(string $from, string $to): void
    {
        LaravelStorage::disk('files')->move($from, $to);
    }

    public function copy(string $from, string $to): string
    {
        $directory = pathinfo($to, PATHINFO_DIRNAME);
        if (!LaravelStorage::disk('files')->directoryExists($directory)) {
            LaravelStorage::disk('files')->makeDirectory($directory);
        }
        $properPath = $this->getProperPath($to);
        $to = LaravelStorage::disk('files')->path($properPath);
        copy($from, $to);
        return $properPath;
    }

    public function exists(string $path): bool
    {
        return LaravelStorage::disk('files')->exists($path);
    }

    public function fileExists(string $path): bool
    {
        return LaravelStorage::disk('files')->fileExists($path);
    }

    public function directoryExists(string $path): bool
    {
        return LaravelStorage::disk('files')->directoryExists($path);
    }

    public function get(string $path): mixed
    {
        return LaravelStorage::disk('files')->get($path);
    }

    public function files(string $path): array
    {
        return LaravelStorage::disk('files')->files($path);
    }

    public function directories(string $path): array
    {
        return LaravelStorage::disk('files')->directories($path);
    }

    public function path(string $path): string
    {
        return LaravelStorage::disk('files')->path($path);
    }

    public function delete(string $path): void
    {
        LaravelStorage::disk('files')->delete($path);
    }

    public function deleteDirectory(string $path): void
    {
        LaravelStorage::disk('files')->deleteDirectory($path);
    }

    private function getProperPath(string $path): string
    {
        $newPath = $path;
        $basename = '';
        $filename = Path::filename($path);
        $extension = Path::extension($path);

        for ($i = 1; LaravelStorage::disk('files')->fileExists($newPath); $i++) {
            $basename = $filename . " (" . $i . ")." . $extension;
            $newPath = Path::join(Path::directory($path), $basename);
        }

        return $newPath;
    }
}
