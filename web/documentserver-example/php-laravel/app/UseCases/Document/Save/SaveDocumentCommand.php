<?php

namespace App\UseCases\Document\Save;

use App\Models\File;
use App\Models\Version;
use App\Models\VersionInfo;
use App\Repositories\FileRepository;
use App\Repositories\ForceSavedFilesRepository;
use App\Repositories\VersionRepository;
use Illuminate\Support\Str;

class SaveDocumentCommand
{
    public function __construct(
        private FileRepository $fileRepository,
        private VersionRepository $versionRepository,
        private ForceSavedFilesRepository $forceSavedFilesRepository,
    ) {}

    public function __invoke(SaveDocumentRequest $request): void
    {
        $file = $this->fileRepository->find($request->filename);

        $currentVersion = $this->versionRepository->current($request->filename);

        $version = Version::create(
            $request->filename,
            VersionInfo::create(
                Str::uuid(),
                $request->fileType,
                $currentVersion + 1,
                now(),
                $request->user,
                $request->serverVersion,
            ),
            $request->history,
            $request->changes,
        );

        $newFile = File::create($request->filename, $request->fileContent);

        if ($this->forceSavedFilesRepository->exists($request->filename)) {
            $this->forceSavedFilesRepository->delete($request->filename);
        }

        $this->fileRepository->delete($file);
        $this->fileRepository->save($newFile);
        $this->versionRepository->save($version);
    }
}
