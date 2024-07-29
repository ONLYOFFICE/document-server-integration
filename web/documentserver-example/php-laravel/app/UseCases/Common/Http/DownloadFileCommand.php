<?php

namespace App\UseCases\Common\Http;

use Exception;
use Illuminate\Support\Facades\Http;
use UnexpectedValueException;

class DownloadFileCommand
{
    public function __invoke(DownloadFileRequest $request): array
    {
        $response = Http::head($request->url);

        if (! $response->ok()) {
            throw new Exception("$request->url is not reachable");
        }

        $contentSize = $response->header('Content-Length');

        if (empty($contentSize)) {
            throw new Exception("$request->url has an undefined content length.");
        }

        if ($contentSize > 5 * 1024 * 1024) {
            throw new UnexpectedValueException("$request->url exceeds the maximum file size");
        }

        $fileResponse = Http::get($request->url);

        return [
            'content' => $fileResponse->body(),
            'size' => $contentSize,
        ];
    }
}
