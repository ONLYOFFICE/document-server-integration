<?php

namespace App\UseCases\Document\Find;

use App\Helpers\Path\Path;
use App\Helpers\Path\PathInfo;
use App\OnlyOffice\Managers\FormatManager;
use App\Repositories\FileRepository;
use App\Repositories\ForceSavedFilesRepository;
use App\Repositories\UserRepository;
use App\Repositories\VersionRepository;
use DomainException;

class FindDocumentQueryHandler
{
    public function __construct(
        private FileRepository $fileRepository,
        private VersionRepository $versionRepository,
        private ForceSavedFilesRepository $forceSavedFilesRepository,
        private UserRepository $userRepository,
        private FormatManager $formatManager,
    ) {}

    public function __invoke(FindDocumentQuery $request): array
    {
        $filePath = Path::join($request->userDirectory, $request->filename);

        $forceSavedfile = $this->forceSavedFilesRepository->find($filePath);
        $file = $forceSavedfile ?? $this->fileRepository->find($filePath);

        if ($file === null) {
            throw new DomainException("The file $request->filename does not exist.");
        }

        if ($forceSavedfile !== null) {
            $this->forceSavedFilesRepository->delete($filePath);
        }

        $currentVersion = $this->versionRepository->current($filePath);
        $version = $this->versionRepository->find($filePath, $currentVersion);

        if ($version === null) {
            throw new DomainException("Could not find the current version of the file $request->filename");
        }

        $document = [
            'key' => $version->key(),
            'filename' => $request->filename,
            'content' => $file->content,
            'size' => $file->size,
            'mimeType' => $file->mime,
            'format' => $this->formatManager->find(PathInfo::extension($request->filename)),
        ];

        return $document;
    }
}
