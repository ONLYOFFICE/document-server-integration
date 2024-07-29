<?php

namespace App\UseCases\Docs\Conversion;

class ConvertRequest
{
    public function __construct(
        public string $filename,
        public string $fileType,
        public string $outputType,
        public ?string $url,
        public ?string $password,
        public string $user,
        public string $userAddress,
        public string $lang = 'en',
    ) {}
}