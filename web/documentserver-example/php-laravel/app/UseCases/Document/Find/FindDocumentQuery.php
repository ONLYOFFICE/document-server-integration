<?php

namespace App\UseCases\Document\Find;

class FindDocumentQuery
{
    public function __construct(
        public string $filename,
        public string $userDirectory,
    ) {}
}
