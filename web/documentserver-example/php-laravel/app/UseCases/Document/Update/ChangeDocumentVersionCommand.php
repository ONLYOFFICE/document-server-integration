<?php

namespace App\UseCases\Document\Update;

use App\Helpers\Path\Path;
use App\Models\Version;
use App\Models\VersionInfo;
use App\Repositories\FileRepository;
use App\Repositories\UserRepository;
use App\Repositories\VersionRepository;
use App\Services\ServerConfig;
use Illuminate\Support\Str;

class ChangeDocumentVersionCommand
{
    public function __construct(
        private FileRepository $fileRepository,
        private VersionRepository $versionRepository,
        private UserRepository $userRepository,
        private ServerConfig $serverConfig,
    ) {}

    public function __invoke(ChangeDocumentVersionRequest $request): void
    {
        $filePath = Path::join($request->userDirectory, $request->filename);
        $absFilePath = $this->fileRepository->path($filePath);

        $user = $this->userRepository->find($request->userId);

        $currentVersion = $this->versionRepository->current($filePath);
        $versionFile = $this->versionRepository->file($filePath, $request->version);

        if ($request->url) {
            $data = file_get_contents(
                str_replace(
                    $this->serverConfig->get('url.public'),
                    $this->serverConfig->get('url.private'),
                    $request->url),
                false,
                stream_context_create(['http' => ['timeout' => 5]])
            );
            file_put_contents($absFilePath, $data, LOCK_EX);
        } else {
            copy($versionFile['path'], $absFilePath);
        }

        $versionInfo = VersionInfo::create(
            Str::uuid(),
            $request->fileType,
            $currentVersion + 1,
            now(),
            $user->id,
        );

        $version = Version::create($filePath, $versionInfo);

        $this->versionRepository->save($version);
    }
}
