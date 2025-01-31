<?php

namespace App\UseCases\Document\Find;

use App\Helpers\Path\PathInfo;
use App\OnlyOffice\Managers\FormatManager;
use App\Repositories\FileRepository;
use App\Repositories\VersionRepository;

class FindAllDocumentsQueryHandler
{
    public function __construct(
        private FileRepository $fileRepository,
        private FormatManager $formatManager,
        private VersionRepository $versionRepository,
    ) {}

    public function __invoke(FindAllDocumentsQuery $request): array
    {
        $result = [];

        $files = $this->fileRepository->all($request->userDirectory);

        foreach ($files as $file) {
            $currentVersion = $this->versionRepository->current($file->filename);

            $result[] = [
                'filename' => PathInfo::basename($file->filename),
                'version' => $currentVersion,
                'format' => $this->formatManager->find(PathInfo::extension($file->filename)),
                'lastModified' => $file->modified,
            ];
        }

        $result = collect($result)->sortByDesc('lastModified')->toArray();

        return $result;
    }
}
