<?php

namespace App\UseCases\User\Find;

use App\Repositories\UserRepository;

class FindUserQueryHandler
{
    public function __construct(private UserRepository $userRepository) {}

    public function __invoke(FindUserQuery $query): array
    {
        $user = $this->userRepository->find($query->id);

        return $user->toArray();
    }
}
