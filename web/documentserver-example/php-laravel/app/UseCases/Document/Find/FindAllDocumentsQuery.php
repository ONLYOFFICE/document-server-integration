<?php

namespace App\UseCases\Document\Find;

class FindAllDocumentsQuery
{
    public function __construct(public string $userDirectory) {}
}
