<?php

namespace App\UseCases\Docs\Command;

use App\Exceptions\CommandServiceError;
use App\Services\Docs\Command\ForceSaveRequest as ForceSave;
use Illuminate\Support\Facades\Log;

class ForceSaveCommad
{
    public function __invoke(ForceSaveRequest $request): void
    {
        try {
            app(ForceSave::class)
            ->save($request->key);
        } catch (CommandServiceError $e) {
            Log::debug($e->getMessage());
        }
    }
}