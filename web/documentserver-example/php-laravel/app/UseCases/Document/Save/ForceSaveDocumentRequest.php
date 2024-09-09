<?php

namespace App\UseCases\Document\Save;

class ForceSaveDocumentRequest
{
    public function __construct(
        public string $filename,
        public string $userDirectory,
        public string $content,
    ) {}
}
