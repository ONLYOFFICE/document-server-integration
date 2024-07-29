<?php

namespace App\Helpers;

use Illuminate\Support\Facades\Storage;

class UniqueFilename
{
    public static function for(string $oldFilename): string
    {
        $newPath = $oldFilename;
        $dirname = pathinfo($oldFilename, PATHINFO_DIRNAME);
        $basename = pathinfo($oldFilename, PATHINFO_BASENAME);
        $filename = pathinfo($oldFilename, PATHINFO_FILENAME);
        $extension = pathinfo($oldFilename, PATHINFO_EXTENSION);

        for ($i = 1; Storage::disk('files')->exists($newPath); $i++) {
            $basename = "$filename ($i).$extension";
            $newPath = $dirname.DIRECTORY_SEPARATOR.$basename;
        }

        return $newPath;
    }
}
