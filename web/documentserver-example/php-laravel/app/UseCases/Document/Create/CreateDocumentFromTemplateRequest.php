<?php

namespace App\UseCases\Document\Create;

class CreateDocumentFromTemplateRequest
{
    public function __construct(
        public string $fileExtension,
        public string $userDirectory,
        public string $user,
        public bool $withSample,
    ) {}
}
