<?php

namespace App\UseCases\Common\Http;

use UnexpectedValueException;

class DownloadFileRequest
{
    public function __construct(public string $url)
    {
        if (! filter_var($url, FILTER_VALIDATE_URL)) {
            throw new UnexpectedValueException("$url is not a valid URL");
        }
    }
}
