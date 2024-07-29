<?php

namespace App\Models;

class Document
{
    public function __construct(
        public string $title,
        public string $key,
        public string $url,
        public Format $format,
    ) {}
}
