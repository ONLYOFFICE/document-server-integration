<?php

namespace App\Services\Docs\Command;

class ForgottenFileRequest extends CommandRequest
{
    public function get(string $key): array
    {
        $content = [
            'c' => 'getForgotten',
            'key' => $key,
        ];

        $result = $this->send($content, $key);

        return [
            'key' => $result['key'],
            'url' => $result['url'],
        ];
    }
}
