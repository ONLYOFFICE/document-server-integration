<?php

namespace App\UseCases\Docs\Command;

class ForceSaveRequest
{
    public function __construct(public string $key) {}
}