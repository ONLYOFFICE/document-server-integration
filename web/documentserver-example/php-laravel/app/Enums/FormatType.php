<?php

namespace App\Enums;

enum FormatType: string
{
    case WORD = 'word';
    case CELL = 'cell';
    case SLIDE = 'slide';
    case PDF = 'pdf';
}
