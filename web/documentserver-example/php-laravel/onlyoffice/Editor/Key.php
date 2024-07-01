<?php

namespace OnlyOffice\Editor;

use Illuminate\Support\Str;
use OnlyOffice\Config;

class Key
{
    public static function revision(string $expectedKey): string
    {
        if (mb_strlen($expectedKey) > 20) {
            $expectedKey = crc32($expectedKey);
        }  // if the expected key length is greater than 20, calculate the crc32 for it
        $key = preg_replace("[^0-9-.a-zA-Z_=]", "_", $expectedKey);
        $key = mb_substr($key, 0, min([mb_strlen($key), 20]));  // the resulting key length is 20 or less
        return $key;
    }

    public static function generate(string $filename, int $modified): string
    {
        $config = app(Config::class);

        $key = Str::of($config->get('client.ip'))->append($config->virtualPath() . rawurlencode($filename));
        $key = Str::of($key)->append($modified);
        
        return static::revision($key);
    }
}