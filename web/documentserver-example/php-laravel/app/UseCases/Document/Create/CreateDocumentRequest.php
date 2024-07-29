<?php

namespace App\UseCases\Document\Create;

use UnexpectedValueException;

class CreateDocumentRequest
{
    public function __construct(
        public string $filename,
        public string $userDirectory,
        public string $fileType,
        public ?int $fileSize,
        public mixed $fileContent,
        public string $user,
    ) {
        if ($fileSize && ($fileSize <= 0 || $fileSize > env('STORAGE_MAXIMUM_FILE_SIZE', 5 * 1024 * 1024))) {
            throw new UnexpectedValueException("Incorrect file size: $fileSize");
        }
    }
}
