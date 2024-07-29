<?php

namespace App\UseCases\User\Find;

class FindAllUsersQuery
{
    public function __construct(
        public ?string $id = null,
        public ?bool $forProtect = false,
        public ?bool $forMentions = false,
    ) {}
}
