<?php

namespace App\UseCases\Document\Update;

class ChangeDocumentVersionRequest
{
    public function __construct(
        public string $filename,
        public string $userDirectory,
        public string $fileType,
        public string $version,
        public ?string $url,
        public string $userId,
    ) {}
}
