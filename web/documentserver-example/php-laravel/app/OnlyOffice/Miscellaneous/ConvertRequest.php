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

namespace App\OnlyOffice\Miscellaneous;

use App\Exceptions\ConversionError;
use App\Exceptions\ConversionNotComplete;
use App\OnlyOffice\Services\RequestService;

class ConvertRequest
{
    public function convert(array $data)
    {
        $requestService = app(RequestService::class);

        $result = $requestService->sendRequestToConvertService(
            $data['url'],
            $data['filetype'],
            $data['outputtype'],
            $data['key'],
            false,
            $data['lang'],
        );

        if (property_exists($result, 'Error')) {
            throw new ConversionError($result->Error);
        }

        if (! property_exists($result, 'EndConvert')) {
            throw new ConversionNotComplete($result->Percent, $result->Filename, $result->FileUrl);
        }

        return [
            'fileType' => $result->FileType,
            'fileUrl' => $result->FileUrl,
        ];
    }
}
