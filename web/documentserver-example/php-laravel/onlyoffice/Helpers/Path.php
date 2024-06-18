<?php

namespace OnlyOffice\Helpers;

class Path
{
    public static function join(...$args): string
    {
        
        $paths = array_map(fn($arg) => rtrim($arg, '/'), $args);
        $paths = array_filter($paths);
        return join(DIRECTORY_SEPARATOR, $paths);
    }

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

    public static function directory(string $filename): string
    {
        return pathinfo($filename, PATHINFO_DIRNAME);
    }
}