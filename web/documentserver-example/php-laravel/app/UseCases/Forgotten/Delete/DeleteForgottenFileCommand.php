<?php

namespace App\UseCases\Forgotten\Delete;

use App\Services\Docs\Command\ForgottenDeleteRequest;

class DeleteForgottenFileCommand
{
    public function __invoke(DeleteForgottenFileRequest $request): void
    {
        app()->make(ForgottenDeleteRequest::class)->delete($request->key);
    }
}
