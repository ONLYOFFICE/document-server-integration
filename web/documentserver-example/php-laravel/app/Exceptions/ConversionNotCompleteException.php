<?php

namespace App\Exceptions;

use Exception;

class ConversionNotComplete extends Exception
{
    public function __construct(public int $step, public string $filename, public string $url)
    {
        parent::__construct("The file is still being converted. Currently on $this->step percent.");
    }
}
