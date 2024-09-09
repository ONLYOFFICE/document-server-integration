<?php

namespace App\UseCases\Document\Save;

class SaveDocumentFormRequest
{
    public function __construct(
        public string $filename,
        public string $userDirectory,
        public string $user,
        public mixed $formContent,
        public mixed $formDataContent,
    ) {}
}
