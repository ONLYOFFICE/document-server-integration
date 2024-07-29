<?php

namespace App\Helpers\Path;

class TemplatePath extends Path
{
    public static function for(string $extension, bool $withSample): string
    {
        $templatePath = env('TEMPLATE_PATH', public_path('assets/document-templates/'));
        $name = $withSample ? 'sample' : 'new';
        $filename = "$name.$extension";

        return self::join($templatePath, "$name/$filename");
    }
}
