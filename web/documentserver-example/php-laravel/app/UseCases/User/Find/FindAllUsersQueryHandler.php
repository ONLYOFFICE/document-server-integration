<?php

namespace App\UseCases\User\Find;

use App\Repositories\UserRepository;

class FindAllUsersQueryHandler
{
    public function __construct(private UserRepository $userRepository) {}

    public function __invoke(FindAllUsersQuery $query): array
    {
        if ($query->forMentions) {
            return $this->userRepository->getUsersForMentions($query->id);
        } elseif ($query->forProtect) {
            return $this->userRepository->getUsersForProtect($query->id);
        }

        $users = [];

        foreach ($this->userRepository->getAll() as $user) {
            $users[] = $user->toArray();
        }

        return $users;
    }
}
