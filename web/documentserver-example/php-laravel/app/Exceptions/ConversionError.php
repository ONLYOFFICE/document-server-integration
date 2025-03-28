<?php

/**
 * (c) Copyright Ascensio System SIA 2025
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

class ConversionError extends Exception
{
    public function __construct(int $code)
    {
        match ($code) {
            -1 => parent::__construct('Conversion API error - Unknown error', $code),
            -2 => parent::__construct('Conversion API error - Conversion timeout error', $code),
            -3 => parent::__construct('Conversion API error - Conversion error', $code),
            -4 => parent::__construct('Conversion API error - Error while downloading the document file to be converted', $code),
            -5 => parent::__construct('Conversion API error - Incorrect password', $code),
            -6 => parent::__construct('Conversion API error - Error while accessing the conversion result database', $code),
            -7 => parent::__construct('Conversion API error - Input error', $code),
            -8 => parent::__construct('Conversion API error - Invalid token', $code),
            -9 => parent::__construct('Conversion API error - Cannot automatically determine the output file format', $code),
        };
    }
}
