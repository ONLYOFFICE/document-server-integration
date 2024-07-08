<?php

namespace OnlyOffice;

use OnlyOffice\Helpers\Path;
use Exception;
use Illuminate\Support\Facades\Http;
use OnlyOffice\Document\Convert\ConvertRequest;
use OnlyOffice\Entities\File;
use Illuminate\Support\Str;
use OnlyOffice\Document\Command\CommandRequest;
use OnlyOffice\Document\Command\ForgottenDeleteRequest;
use OnlyOffice\Document\Command\ForgottenFileRequest;
use OnlyOffice\Document\Command\ForgottenListRequest;
use OnlyOffice\Exceptions\Conversion\ConversionError;
use OnlyOffice\Exceptions\Conversion\ConversionNotComplete;

class DocumentServer
{
    public function __construct(
        private Config $config,
        private Users $users,
        private Formats $formats,
        private JWT $jwt,
    ) {
    }

    public function convert(array $content): array
    {
        $format = $this->formats->find($content['filetype']);

        if (!$format->convertible()) {
            throw new Exception('The format is not convertible.');
        }

        $headers = [];

        if ($this->config->get('jwt.enabled')) {
            $token = $this->jwt->encode(["payload" => $content]);
            $headers = [$this->config->get('jwt.header') => 'Bearer ' . $token];
            $content['token'] = $this->jwt->encode($content);
        }

        $client = Http::withHeaders($headers)
            ->timeout($this->config->get('conversion.timeout'))
            ->asJson()
            ->acceptJson();

        if (
            Str::of($this->config->get('conversion.url'))->isUrl(['https'])
            && !$this->config->get('ssl_verify')
        ) {
            $client = $client->withoutVerifying();
        }

        $response = $client->post($this->config->get('conversion.url'), $content);

        if (!$response->ok()) {
            throw new Exception('Could not convert the file');
        }

        $result = $response->json();

        if (array_key_exists('error', $result)) {
            throw new ConversionError($result['error']);
        }

        if (!$result['endConvert']) {
            throw new ConversionNotComplete($result['percent'], $result['filename'], $result['fileUrl']);
        }

        return $result;
    }

    public function download(string $url): string
    {
        $response = Http::get($url);

        $size = $response->header('Content-Length');

        if ($size <= 0) {
            throw new Exception('The file has incorrect size.');
        }

        if (!$response->ok()) {
            throw new Exception('Could not download the file.');
        }

        return $response->body();
    }

    public function forceSave(string $key): void
    {
        $request = app()->make(CommandRequest::class);
        $request->send('forcesave', $key);
    }

    public function getForgottenFiles(): array
    {
        $files = [];

        $request = app()->make(ForgottenListRequest::class);
        $keys = $request->get();

        foreach ($keys as $key) {
            $request = app()->make(ForgottenFileRequest::class);
            $files[] = $request->get($key);
        }

        return $files;
    }

    public function deleteForgotten(string $key): string
    {
        $request = app()->make(ForgottenDeleteRequest::class);
        return $request->delete($key);
    }
}
