<?php

namespace App\Helpers\URL;

use App\Services\StorageConfig;

class FileURL extends URL
{
    public static function download(string $filename, string $address = ''): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.private'), 'files/download', [
            'fileName' => $filename,
            'userAddress' => $address,
        ]);
    }

    public static function changes(string $filename, string $address, int $version): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.private'), 'files/versions/changes', [
            'filename' => $filename,
            'userAddress' => $address,
            'version' => $version,
        ]);
    }

    public static function previous(string $filename, string $address, int $version): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.private'), 'files/versions/previous', [
            'filename' => $filename,
            'userAddress' => $address,
            'version' => $version,
        ]);
    }

    public static function history(string $filename, string $address): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.private'), 'files/download', [
            'fileName' => $filename,
            'userAddress' => $address,
        ]);
    }

    public static function create(string $extension, string $user): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.public'), 'editor', [
            'fileExt' => $extension,
            'user' => $user,
        ]);
    }

    public static function callback(string $filename, string $user): string
    {
        $config = app(StorageConfig::class);

        return static::build($config->get('url.private'), 'editor/track', [
            'fileName' => $filename,
            'userAddress' => $user,
        ]);
    }
}
