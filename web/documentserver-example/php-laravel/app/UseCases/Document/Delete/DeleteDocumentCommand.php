<?php

namespace App\UseCases\Document\Delete;

use App\Helpers\Path\Path;
use App\Repositories\FileRepository;
use App\Repositories\VersionRepository;

class DeleteDocumentCommand
{
    public function __construct(
        private FileRepository $fileRepository,
        private VersionRepository $versionRepository,
    ) {}

    public function __invoke(DeleteDocumentRequest $request): void
    {
        $filePath = Path::join($request->userDirectory, $request->filename);
        $file = $this->fileRepository->find($filePath);

        if ($file) {
            $this->fileRepository->delete($file);
            $this->versionRepository->deleteAll($filePath);
        }
    }
}
