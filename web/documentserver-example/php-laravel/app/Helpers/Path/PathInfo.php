<?php

namespace App\Helpers\Path;

class PathInfo
{
    public static function basename(string $filename): string
    {
        return pathinfo($filename, PATHINFO_BASENAME);
    }

    public static function filename(string $filename): string
    {
        return pathinfo($filename, PATHINFO_FILENAME);
    }

    public static function extension(string $filename): string
    {
        return mb_strtolower(pathinfo($filename, PATHINFO_EXTENSION));
    }

    public static function dirname(string $filename): string
    {
        return pathinfo($filename, PATHINFO_DIRNAME);
    }
}
