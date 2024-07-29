<?php

namespace App\UseCases\Forgotten\Find;

use App\Helpers\Path\PathInfo;
use App\Helpers\URL\URL;
use App\Repositories\FormatRepository;
use App\Services\Docs\Command\ForgottenFileRequest;
use App\Services\Docs\Command\ForgottenListRequest;
use App\Services\ServerConfig;
use Illuminate\Support\Str;

class FindAllForgottenFilesQueryHandler
{
    public function __construct(
        private ServerConfig $serverConfig,
        private FormatRepository $formatRepository,
    ) {}

    public function __invoke(FindAllForgottenFilesQuery $query): array
    {
        $filesList = [];

        $keys = app()->make(ForgottenListRequest::class)->get();

        foreach ($keys as $key) {
            $filesList[] = app()->make(ForgottenFileRequest::class)->get($key);
        }

        $files = [];

        foreach ($filesList as $fileItem) {
            $url = $fileItem['url'];
            $url = Str::replace(URL::origin($url), $this->serverConfig->get('url.public'), $url);

            $files[] = [
                'key' => $fileItem['key'],
                'filename' => $url,
                'url' => $url,
                'format' => $this->formatRepository->find(PathInfo::extension($fileItem['url'])),
            ];
        }

        return $files;
    }
}
