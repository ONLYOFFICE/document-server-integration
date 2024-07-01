<?php

namespace OnlyOffice\Document\Convert;

use Illuminate\Support\Facades\Http;
use OnlyOffice\Exceptions\Conversion\ConversionNotComplete;
use OnlyOffice\Document\Convert\ConvertResponse;
use Exception;
use Illuminate\Support\Str;
use Illuminate\Support\Arr;
use OnlyOffice\Config;
use OnlyOffice\Exceptions\Conversion\ConversionError;
use OnlyOffice\JWT;

class ConvertRequest
{
    private array $headers = [];

    public function __construct(private Config $config, private JWT $jwt) {}

    private function withJWTHeader(array $content): void
    {
        $token = $this->jwt->encode(["payload" => $content]);
        $this->headers = [$this->config->get('jwt.header') => 'Bearer ' . $token];
    }

    public function send(array $content): ConvertResponse
    {
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

        $response = $client->post($this->config->get('conversion.url'), $content);

        if (!$response->ok()) {
            throw new Exception('Could not convert the file');
        }

        $result = $response->json();

        if (Arr::exists($result, 'error')) {
            throw new ConversionError($result['error']);
        }

        if (!$result['endConvert']) {
            throw new ConversionNotComplete($result['percent'], $result['filename'], $result['fileUrl']);
        }

        $convertResponse = new ConvertResponse();
        $convertResponse->url = $result['fileUrl'];
        $convertResponse->type = $result['fileType'];

        return $convertResponse;
    }
}