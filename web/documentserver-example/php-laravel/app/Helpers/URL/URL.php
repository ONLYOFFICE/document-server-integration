<?php

namespace App\Helpers\URL;

class URL
{
    public static function build(string $domain, string $path = '', array $params = []): string
    {
        $path = trim($path, '/');
        $url = empty($path) ? $domain : "$domain/$path";

        if ($params) {
            $url = "$url?";

            foreach ($params as $key => $value) {
                $param = "$key=".urlencode($value);
                $url = "$url$param&";
            }
            $url = rtrim($url, '&');
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
