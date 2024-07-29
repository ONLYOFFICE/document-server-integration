<?php

namespace App\Helpers\Path;

class Path
{
    public static function join(...$args): string
    {

        $paths = array_map(fn ($arg) => rtrim($arg, '/'), $args);
        $paths = array_filter($paths);

        return implode(DIRECTORY_SEPARATOR, $paths);
    }
}
