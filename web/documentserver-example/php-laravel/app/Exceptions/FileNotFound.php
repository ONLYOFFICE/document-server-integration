<?php

namespace App\Exceptions;

use Exception;

class FileNotFound extends Exception
{
    public function __construct(private readonly string $path)
    {
        parent::__construct();
    }

    public function errorCode(): string
    {
        return 'file_not_found';
    }

    protected function errorMessage(): string
    {
        return sprintf('The file <%s> has not been found', $this->path);
    }
}
