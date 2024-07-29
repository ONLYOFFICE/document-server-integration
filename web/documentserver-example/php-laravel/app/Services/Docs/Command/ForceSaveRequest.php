<?php

namespace App\Services\Docs\Command;

class ForceSaveRequest extends CommandRequest
{
    public function save(string $key): array
    {
        $content = [
            'c' => 'forcesave',
            'key' => $key,
        ];

        $result = $this->send($content, $key);

        return $result;
    }
}
