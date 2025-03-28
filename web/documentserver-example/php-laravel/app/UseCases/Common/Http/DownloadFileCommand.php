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
