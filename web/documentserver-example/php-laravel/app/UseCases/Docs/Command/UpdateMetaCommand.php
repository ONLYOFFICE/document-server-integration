<?php

namespace App\UseCases\Docs\Command;

use App\OnlyOffice\Miscellaneous\CommandRequest;

class UpdateMetaCommand
{
    public function __invoke(UpdateMetaRequest $request): void
    {
        $meta = ['title' => $request->title];

        app(CommandRequest::class)
            ->updateMeta($request->key, $meta);
    }
}
