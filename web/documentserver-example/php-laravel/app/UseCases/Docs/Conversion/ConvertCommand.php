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

namespace App\UseCases\Docs\Conversion;

use App\Exceptions\ConversionError;
use App\Exceptions\ConversionNotComplete;
use App\Helpers\Path\PathInfo;
use App\Helpers\URL\FileURL;
use App\Repositories\FormatRepository;
use App\Services\Docs\Conversion\ConversionRequest;
use App\Services\JWT;
use App\Services\ServerConfig;
use Exception;
use Illuminate\Support\Str;

class ConvertCommand
{
    public function __construct(
        private ServerConfig $serverConfig,
        private FormatRepository $formatRepository,
        private JWT $jwt,
    ) {}

    public function __invoke(ConvertRequest $request): mixed
    {
        $format = $this->formatRepository->find($request->fileType);

        if (! $format->convertible() && $request->outputType == 'ooxml') {
            throw new Exception("The format $request->fileType is not convertible.");
        }

        $url = $request->url ?? FileURL::download(urlencode($request->filename), $request->userAddress);

        $key = Str::uuid();

        $content = [
            'filename' => $request->filename,
            'filetype' => $request->fileType,
            'outputtype' => $request->outputType,
            'password' => $request->password,
            'url' => $url,
            'key' => $key,
            'user' => $request->user,
            'lang' => $request->lang,
        ];

        try {
            $result = app(ConversionRequest::class)
                ->send($content, $key);
            $result['filename'] = PathInfo::filename($request->filename).'.'.$result['fileType'];
        } catch (ConversionNotComplete $e) {
            return [
                'step' => $e->step,
                'filename' => $e->filename,
                'fileUri' => $e->url,
            ];
        } catch (ConversionError $e) {
            return [
                'error' => $e->getMessage(),
                'code' => $e->getCode(),
            ];
        }

        return $result;
    }
}
