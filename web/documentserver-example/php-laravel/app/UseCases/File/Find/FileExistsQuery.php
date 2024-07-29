<?php

namespace App\UseCases\File\Find;

class FileExistsQuery
{
    public function __construct(
        public string $filename,
        public string $userDirectory
    ) {}
}
