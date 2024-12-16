<?php

/**
 * (c) Copyright Ascensio System SIA 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

        $origin = $port ? "{$scheme}://{$host}:{$port}" : "{$scheme}://{$host}";

        return $origin;
    }
}
