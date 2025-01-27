<?php

/**
 * (c) Copyright Ascensio System SIA 2025
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

namespace App\Services\Docs\Conversion;

use App\Exceptions\ConversionError;
use App\Exceptions\ConversionNotComplete;
use App\Services\JWT;
use App\Services\ServerConfig;
use Exception;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Str;

class ConversionRequest
{
    private array $headers = [];

    public function __construct(private ServerConfig $config, private JWT $jwt) {}

    private function withJWTHeader(array $content): void
    {
        $token = $this->jwt->encode(['payload' => $content]);
        $this->headers = [$this->config->get('jwt.header') => "Bearer $token"];
    }

    public function send(array $content, ?string $key = null): mixed
    {
        if ($this->config->get('jwt.enabled')) {
            $this->withJWTHeader($content);
            $content['token'] = $this->jwt->encode($content);
        }

        $client = Http::withHeaders($this->headers)
            ->timeout($this->config->get('conversion.timeout'))
            ->asJson()
            ->acceptJson();

        $url = $this->config->get('conversion.url');

        if (
            Str::of($url)->isUrl(['https'])
            && ! $this->config->get('ssl_verify')
        ) {
            $client = $client->withoutVerifying();
        }

        if ($key) {
            $url = "$url?shardkey=".urlencode($key);
        }

        $response = $client->post($url, $content);

        if (! $response->ok()) {
            throw new Exception('Could not convert the file');
        }

        $result = $response->json();

        if (array_key_exists('error', $result)) {
            throw new ConversionError($result['error']);
        }

        if (! $result['endConvert']) {
            throw new ConversionNotComplete($result['percent'], $result['filename'], $result['fileUrl']);
        }

        return $result;
    }
}
