<?php

namespace App\UseCases\Docs\Command;

use App\Services\Docs\Command\CommandRequest;

class UpdateMetaCommand
{
    public function __invoke(UpdateMetaRequest $request): void
    {
        $content = [
            'c' => 'meta',
            'key' => $request->key,
            'meta' => [
                'title' => $request->title,
            ],
        ];

        app(CommandRequest::class)
                ->send($content, $request->key);
    }
}