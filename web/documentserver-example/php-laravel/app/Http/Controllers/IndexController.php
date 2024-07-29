<?php

namespace App\Http\Controllers;

use App\Helpers\URL\URL;
use App\Services\ServerConfig;
use App\Services\StorageConfig;
use App\UseCases\Document\Find\FindAllDocumentsQuery;
use App\UseCases\Document\Find\FindAllDocumentsQueryHandler;
use App\UseCases\Language\Find\FindAllLanguagesQueryHandler;
use App\UseCases\User\Find\FindAllUsersQuery;
use App\UseCases\User\Find\FindAllUsersQueryHandler;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class IndexController extends Controller
{
    public function index(Request $request, ServerConfig $serverConfig, StorageConfig $storageConfig)
    {
        $directUrlEnabled = $request->has('directUrl') && $request->directUrl === 'true';
        $directUrlArg = 'directUrl='.($directUrlEnabled ? 'true' : 'false');

        $preloaderUrl = $serverConfig->get('url.preloader');

        $files = app(FindAllDocumentsQueryHandler::class)
            ->__invoke(new FindAllDocumentsQuery($request->ip()));

        $languages = app(FindAllLanguagesQueryHandler::class)->__invoke();

        $users = app(FindAllUsersQueryHandler::class)->
            __invoke(new FindAllUsersQuery());

        $user = $request->input('user', cache('user'));

        foreach ($files as &$file) {
            $url = route('files.download', ['fileName' => urlencode($file['filename']), 'dmode' => true]);
            $file['url'] = Str::replace(URL::origin($url), $storageConfig->get('url.server.public'), $url);
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
