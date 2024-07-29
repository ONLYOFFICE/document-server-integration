<?php

namespace App\UseCases\Document\Delete;

use Illuminate\Support\Facades\Storage;

class DeleteAllDocumentsCommand
{
    public function __invoke(DeleteAllDocumentsRequest $request): void
    {
        Storage::disk('files')->deleteDirectory($request->userDirectory);
    }
}
