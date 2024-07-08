<?php

namespace OnlyOffice\Document\Command;

class ForgottenListRequest extends CommandRequest
{
    public function get(): array
    {
        $content = [
            "c" => 'getForgottenList',
        ];

        $result = $this->send($content);

        return $result['keys'];
    }
}