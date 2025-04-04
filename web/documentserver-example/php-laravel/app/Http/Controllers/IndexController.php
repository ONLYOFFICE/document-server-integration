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

namespace App\Http\Controllers;

use App\Helpers\URL\URL;
use App\OnlyOffice\Managers\SettingsManager;
use App\UseCases\Document\Find\FindAllDocumentsQuery;
use App\UseCases\Document\Find\FindAllDocumentsQueryHandler;
use App\UseCases\Language\Find\FindAllLanguagesQueryHandler;
use App\UseCases\User\Find\FindAllUsersQuery;
use App\UseCases\User\Find\FindAllUsersQueryHandler;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class IndexController extends Controller
{
    public function index(Request $request, SettingsManager $settings)
    {
        $directUrlEnabled = $request->has('directUrl') && $request->directUrl === 'true';
        $directUrlArg = 'directUrl='.($directUrlEnabled ? 'true' : 'false');

        $preloaderUrl = $settings->getSetting('url.preloader');

        $files = app(FindAllDocumentsQueryHandler::class)
            ->__invoke(new FindAllDocumentsQuery($request->ip()));

        $languages = app(FindAllLanguagesQueryHandler::class)->__invoke();

        $users = app(FindAllUsersQueryHandler::class)->
            __invoke(new FindAllUsersQuery);

        $user = $request->input('user', cache('user'));

        foreach ($files as &$file) {
            $url = route('files.download', ['fileName' => urlencode($file['filename']), 'dmode' => true]);
            $file['url'] = Str::replace(URL::origin($url), $settings->getSetting('url.storage.public'), $url);
        }

        return view('index', [
            'user' => $user,
            'users' => $users,
            'languages' => $languages,
            'files' => $files,
            'preloaderUrl' => $preloaderUrl,
            'directUrlArg' => $directUrlArg,
        ]);
    }
}
