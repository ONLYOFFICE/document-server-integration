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

namespace App\Http\Controllers;

use App\Helpers\Path\Path;
use App\Repositories\VersionRepository;
use Illuminate\Http\Request;

class VersionController extends Controller
{
    public function __construct(private VersionRepository $versionRepository) {}

    public function changes(Request $request)
    {
        $filename = Path::join($request->userAddress, $request->filename);

        $changes = $this->versionRepository->changes($filename, $request->version);

        return response()->streamDownload(function () use ($changes) {
            echo $changes['content'];
        }, $changes['filename'], [
            'Content-Length' => $changes['size'],
            'Content-Type' => $changes['mime'],
            'Content-Disposition' => 'attachment; filename*=UTF-8\'\''.str_replace('+', '%20', urlencode($changes['filename'])),
            'Access-Control-Allow-Origin' => '*',
        ]);
    }

    public function previous(Request $request)
    {
        $filename = Path::join($request->userAddress, $request->filename);

        $file = $this->versionRepository->file($filename, $request->version - 1);

        return response()->streamDownload(function () use ($file) {
            echo $file['content'];
        }, $file['filename'], [
            'Content-Length' => $file['size'],
            'Content-Type' => $file['mime'],
            'Content-Disposition' => 'attachment; filename*=UTF-8\'\''.str_replace('+', '%20', urlencode($file['filename'])),
            'Access-Control-Allow-Origin' => '*',
        ]);
    }
}
