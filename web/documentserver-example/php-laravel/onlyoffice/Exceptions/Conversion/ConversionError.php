<?php

namespace OnlyOffice\Exceptions\Conversion;

use Exception;

class ConversionError extends Exception
{
    function __construct(int $code)
    {
        match ($code) {
            -1 => parent::__construct("Conversion API error - Unknown error", $code),
            -2 => parent::__construct("Conversion API error - Conversion timeout error", $code),
            -3 => parent::__construct("Conversion API error - Conversion error", $code),
            -4 => parent::__construct("Conversion API error - Error while downloading the document file to be converted", $code),
            -5 => parent::__construct("Conversion API error - Incorrect password", $code),
            -6 => parent::__construct("Conversion API error - Error while accessing the conversion result database", $code),
            -7 => parent::__construct("Conversion API error - Input error", $code),
            -8 => parent::__construct("Conversion API error - Invalid token", $code),
            -9 => parent::__construct("Conversion API error - Cannot automatically determine the output file format", $code),
        };
    }
}
