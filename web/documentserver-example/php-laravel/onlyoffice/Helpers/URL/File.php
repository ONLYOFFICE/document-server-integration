<?php

namespace OnlyOffice\Helpers\URL;

use OnlyOffice\Config;

class File extends URL
{
    public static function download(string $filename, string $address): string
    {
        $config = app(Config::class);
        return static::build($config->get('url.storage.private'), 'files/download', [
            'fileName' => $filename,
            'userAddress' => $address,
        ]);
    }

    public static function create(string $extension, string $user): string
    {
        $config = app(Config::class);
        return static::build($config->get('url.storage.private'), 'editor', [
            'fileExt' => $extension,
            'user' => $user,
        ]);
    }

    public static function callback(string $filename, string $user): string
    {
        $config = app(Config::class);
        return static::build($config->get('url.storage.private'), 'editor/track', [
            'fileName' => $filename,
            'userAddress' => $user,
        ]);
    }
}
