<?php

namespace OnlyOffice\Exceptions\Command;

use Exception;

class CommandServiceError extends Exception
{
    function __construct(int $code)
    {
        match ($code) {
            1 => parent::__construct("Command Service error - Document key is missing or no document with such key could be found.", $code),
            2 => parent::__construct("Command Service error - Callback url not correct.", $code),
            3 => parent::__construct("Command Service error - Internal server error.", $code),
            4 => parent::__construct("Command Service error - No changes were applied to the document before the forcesave command was received.", $code),
            5 => parent::__construct("Command Service error - Command not correct.", $code),
            6 => parent::__construct("Command Service error - Invalid token.", $code),
        };
    }
}
