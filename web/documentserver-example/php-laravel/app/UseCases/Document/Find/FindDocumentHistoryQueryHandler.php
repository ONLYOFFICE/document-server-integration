<?php

namespace App\UseCases\Document\Find;

use App\Helpers\Path\PathInfo;
use App\Helpers\URL\FileURL;
use App\Repositories\UserRepository;
use App\Repositories\VersionRepository;
use App\Services\JWT;

class FindDocumentHistoryQueryHandler
{
    public function __construct(
        private VersionRepository $versionRepository,
        private UserRepository $userRepository,
    ) {}

    public function __invoke(FindDocumentHistoryQuery $request): array
    {
        $versions = $this->versionRepository->all($request->filename);

        $history = [];
        $history['currentVersion'] = $this->versionRepository->current($request->filename);
        $history['history'] = [];

        for ($i = 0; $i < count($versions); $i++) {
            $version = $versions[$i];
            $user = $this->userRepository->find($version->user());

            $item = [];
            $item['version'] = $version->version();

            $item = array_merge($item, $version->info->toArray());

            $item['user'] = [
                'id' => $user->id,
                'name' => $user->name,
            ];

            if ($i > 0) {
                $previous = [
                    'fileType' => $versions[$i - 1]->fileType(),
                    'key' => $versions[$i - 1]->key(),
                    'url' => FileURL::previous(
                        PathInfo::basename($request->filename), $request->userAddress, $version->version()
                    ),
                ];

                $item['previous'] = $previous;
            }

            if ($version->history) {
                $item['changes'] = $version->history;
                $item['changesUrl'] = FileURL::changes(
                    PathInfo::basename($request->filename), $request->userAddress, $version->version()
                );
            }

            $item['url'] = FileURL::download(
                PathInfo::basename($request->filename), $request->userAddress
            );
            $item['token'] = app(JWT::class)->encode($item);

            $history['history'][] = $item;
        }

        return $history;
    }
}