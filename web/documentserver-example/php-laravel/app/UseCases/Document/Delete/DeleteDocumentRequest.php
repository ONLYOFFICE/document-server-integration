<?php

namespace App\UseCases\Document\Delete;

class DeleteDocumentRequest
{
    public function __construct(
        public string $filename,
        public string $userDirectory,
    ) {}
}
