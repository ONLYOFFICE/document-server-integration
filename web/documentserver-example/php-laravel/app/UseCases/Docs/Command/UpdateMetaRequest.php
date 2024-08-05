<?php

namespace App\UseCases\Docs\Command;

class UpdateMetaRequest
{
    public function __construct(
        public string $key,
        public string $title,
    ) {}
}
