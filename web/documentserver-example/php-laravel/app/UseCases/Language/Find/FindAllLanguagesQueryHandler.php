<?php

namespace App\UseCases\Language\Find;

use App\Repositories\LanguageRepository;

class FindAllLanguagesQueryHandler
{
    public function __construct(private LanguageRepository $languageRepository) {}

    public function __invoke(): array
    {
        return $this->languageRepository->all();
    }
}
