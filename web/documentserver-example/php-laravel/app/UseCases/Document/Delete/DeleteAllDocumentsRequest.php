<?php

namespace App\UseCases\Document\Delete;

class DeleteAllDocumentsRequest
{
    public function __construct(public string $userDirectory) {}
}
