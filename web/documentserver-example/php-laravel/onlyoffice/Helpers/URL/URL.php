<?php

namespace OnlyOffice\Helpers\URL;

use Illuminate\Support\Arr;
use Illuminate\Support\Str;

class URL
{
    public static function build(string $domain, string $path = '', array $params = []): string
    {
        $path = explode('.', $path);
        array_unshift($path, $domain);
        $url = Arr::join($path, '/');

        if ($params) {
            $url = Str::finish($url, '?');
            foreach ($params as $key => $value) {
                $param = Arr::join([$key, urlencode($value)], '=');
                $url = Str::of($url)->append($param . '&');
            }
            $url = Str::rtrim($url, '&');
        }

        return $url;
    }

    public static function origin(string $url): string
    {
        $scheme = parse_url($url, PHP_URL_SCHEME);
        $host = parse_url($url, PHP_URL_HOST);
        $port = parse_url($url, PHP_URL_PORT);

        return "{$scheme}://{$host}:{$port}";
    }
}
