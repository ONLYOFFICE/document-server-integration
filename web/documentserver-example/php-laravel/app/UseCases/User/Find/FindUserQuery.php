<?php

namespace App\UseCases\User\Find;

class FindUserQuery
{
    public function __construct(public string $id) {}
}
