<?php

namespace OnlyOffice\Helpers\URL;

use OnlyOffice\Entities\Format;
use OnlyOffice\Config;

class Template extends URL
{
    public static function image(string $type)
    {
        $config = app(Config::class);
        $name = "file_docx.svg";

        $name = match ($type) {
            Format::TYPE_WORD => "file_docx.svg",
            Format::TYPE_CELL => "file_xlsx.svg",
            Format::TYPE_SLIDE => "file_pptx.svg",
            default => "file_docx.svg",
        };

        return static::build($config->get('url.storage.private'), '/assets/images/' . $name);
    }
}
