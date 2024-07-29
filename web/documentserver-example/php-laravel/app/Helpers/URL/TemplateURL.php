<?php

namespace App\Helpers\URL;

use App\Services\StorageConfig;

class TemplateURL extends URL
{
    public static function image(string $type)
    {
        $config = app(StorageConfig::class);
        $name = 'file_docx.svg';

        $name = match ($type) {
            'word' => 'file_docx.svg',
            'cell' => 'file_xlsx.svg',
            'slide' => 'file_pptx.svg',
            default => 'file_docx.svg',
        };

        return static::build($config->get('url.public'), "/images/$name");
    }
}
