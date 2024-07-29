<?php

namespace App\UseCases\Forgotten\Delete;

class DeleteForgottenFileRequest
{
    public function __construct(public string $key) {}
}
