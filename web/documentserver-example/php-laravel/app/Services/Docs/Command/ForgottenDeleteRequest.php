<?php

namespace App\Services\Docs\Command;

class ForgottenDeleteRequest extends CommandRequest
{
    public function delete(string $key): string
    {
        $content = [
            'c' => 'deleteForgotten',
            'key' => $key,
        ];

        $result = $this->send($content, $key);

        return $result['key'];
    }
}