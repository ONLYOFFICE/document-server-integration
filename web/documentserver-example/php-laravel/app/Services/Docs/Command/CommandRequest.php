<?php

namespace App\Services\Docs\Command;

use App\Exceptions\CommandServiceError;
use App\Services\JWT;
use App\Services\ServerConfig;
use Exception;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Str;

class CommandRequest
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
            ->asJson()
            ->acceptJson();

        $url = $this->config->get('url.command');

        if (Str::of($url)->isUrl(['https'])
            && ! $this->config->get('ssl_verify')) {
            $client = $client->withoutVerifying();
        }

        if ($key) {
            $url = "$url?shardkey=".urlencode($key);
        }

        $response = $client->post($url, $content);

        if (! $response->ok()) {
            throw new Exception('Could not execute the command.');
        }

        $result = $response->json();

        if (array_key_exists('error', $result) && $result['error'] !== 0) {
            throw new CommandServiceError($result['error']);
        }

        return $result;
    }
}
