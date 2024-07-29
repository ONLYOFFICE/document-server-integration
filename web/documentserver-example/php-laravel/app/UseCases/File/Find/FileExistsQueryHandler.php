<?php

namespace App\UseCases\File\Find;

use App\Helpers\Path\Path;
use App\Repositories\FileRepository;

class FileExistsQueryHandler
{
    public function __construct(private FileRepository $fileRepository) {}

    public function __invoke(FileExistsQuery $query): bool
    {
        $filePath = Path::join($query->userDirectory, $query->filename);

        return $this->fileRepository->exists($filePath);
    }
}
