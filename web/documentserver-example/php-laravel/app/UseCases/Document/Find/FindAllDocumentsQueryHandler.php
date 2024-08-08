<?php

namespace App\UseCases\Document\Find;

use App\Helpers\Path\PathInfo;
use App\Repositories\FileRepository;
use App\Repositories\FormatRepository;
use App\Repositories\VersionRepository;

class FindAllDocumentsQueryHandler
{
    public function __construct(
        private FileRepository $fileRepository,
        private FormatRepository $formatRepository,
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
                'format' => $this->formatRepository->find(PathInfo::extension($file->filename)),
                'lastModified' => $file->modified,
            ];
        }

        $result = collect($result)->sortByDesc('lastModified')->toArray();

        return $result;
    }
}
