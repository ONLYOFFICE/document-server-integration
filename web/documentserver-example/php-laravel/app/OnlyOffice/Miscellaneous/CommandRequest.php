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

namespace App\OnlyOffice\Miscellaneous;

use App\OnlyOffice\Services\RequestService;

class CommandRequest
{
    public static function forceSave(string $key)
    {
        $requestService = app(RequestService::class);

        $data = [
            'c' => 'forcesave',
            'key' => $key,
        ];

        $result = $requestService->commandRequest("forcesave", $data);

        return $result;
    }

    public static function deleteForgotten(string $key)
    {
        $requestService = app(RequestService::class);

        $data = [
            'c' => 'deleteForgotten',
            'key' => $key,
        ];

        $result = $requestService->commandRequest("deleteForgotten", $data);

        return $result;
    }

    public static function getForgotten(string $key)
    {
        $requestService = app(RequestService::class);

        $data = [
            'c' => 'getForgotten',
            'key' => $key,
        ];

        $result = $requestService->commandRequest("getForgotten", $data);

        return $result;
    }

    public static function getForgottenList()
    {
        $requestService = app(RequestService::class);

        $result = $requestService->commandRequest("getForgottenList");

        return $result;
    }

    public static function updateMeta(string $key, array $meta)
    {
        $requestService = app(RequestService::class);

        $data = [
            'c' => 'meta',
            'key' => $key,
            'meta' => $meta,
        ];

        $result = $requestService->commandRequest("meta", $data);

        return $result;
    }
}
