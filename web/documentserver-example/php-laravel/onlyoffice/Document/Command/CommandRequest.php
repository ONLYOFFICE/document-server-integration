<?php

namespace OnlyOffice\Document\Command;

use Illuminate\Support\Facades\Http;
use OnlyOffice\Exceptions\Conversion\ConversionNotComplete;
use OnlyOffice\Document\Convert\ConvertResponse;
use Exception;
use Illuminate\Support\Str;
use Illuminate\Support\Arr;
use OnlyOffice\Config;
use OnlyOffice\Exceptions\Command\CommandServiceError;
use OnlyOffice\Exceptions\Conversion\ConversionError;
use OnlyOffice\JWT;

class CommandRequest
{
    private array $headers = [];

    public function __construct(private Config $config, private JWT $jwt) {}

    private function withJWTHeader(array $content): void
    {
        $token = $this->jwt->encode(["payload" => $content]);
        $this->headers = [$this->config->get('jwt.header') => 'Bearer ' . $token];
    }

    public function send(string $type, string $key, mixed $meta = null): mixed
    {
        $content = [
            "c" => $type,
            "key" => $key,
        ];
    
        if ($meta) {
            $content["meta"] = $meta;
        }

        if ($this->config->get('jwt.enabled')) {
            $this->withJWTHeader($content);
            $content['token'] = $this->jwt->encode($content);
        }
        
        $client = Http::withHeaders($this->headers)
            ->timeout($this->config->get('conversion.timeout'))
            ->asJson()
            ->acceptJson();


        if (Str::of($this->config->get('conversion.url'))->isUrl(['https'])
            && !$this->config->get('ssl_verify')) {
            $client = $client->withoutVerifying();
        }

        $response = $client->post($this->config->get('url.server.command'), $content);

        if (!$response->ok()) {
            throw new Exception('Could not execute the command.');
        }

        $result = $response->json();

        if (array_key_exists('error', $result) && $result['error'] !== 0) {
            throw new CommandServiceError($result['error']);
        }

        return $result;
    }
}