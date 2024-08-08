<?php

namespace App\UseCases\Document\Find;

class FindDocumentHistoryQuery
{
    public function __construct(
        public string $filename,
        public string $userAddress,
    ) {}
}
