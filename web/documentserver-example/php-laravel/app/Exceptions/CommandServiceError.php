<?php

/**
 * (c) Copyright Ascensio System SIA 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace App\Exceptions;

use Exception;

class CommandServiceError extends Exception
{
    public function __construct(int $code)
    {
        match ($code) {
            1 => parent::__construct('Command Service error - Document key is missing or no document with such key could be found.', $code),
            2 => parent::__construct('Command Service error - Callback url not correct.', $code),
            3 => parent::__construct('Command Service error - Internal server error.', $code),
            4 => parent::__construct('Command Service error - No changes were applied to the document before the forcesave command was received.', $code),
            5 => parent::__construct('Command Service error - Command not correct.', $code),
            6 => parent::__construct('Command Service error - Invalid token.', $code),
        };
    }
}
