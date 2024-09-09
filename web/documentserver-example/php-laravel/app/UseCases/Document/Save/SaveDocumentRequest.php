<?php

namespace App\UseCases\Document\Save;

class SaveDocumentRequest
{
    public function __construct(
        public string $filename,
        public string $fileType,
        public string $key,
        public mixed $fileContent,
        public string $user,
        public string $serverVersion,
        public mixed $history,
        public mixed $changes,
    ) {}
}
