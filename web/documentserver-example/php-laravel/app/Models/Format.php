<?php

//
// (c) Copyright Ascensio System SIA 2025
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

namespace App\Models;

use App\Enums\FormatType;

final class Format
{
    public string $name;

    public ?FormatType $type;

    /**
     * @var string[]
     */
    public array $actions;

    /**
     * @var string[]
     */
    public array $convert;

    /**
     * @var string[]
     */
    public array $mime;

    public function __construct(
        string $name,
        ?FormatType $type,
        array $actions,
        array $convert,
        array $mime
    ) {
        $this->name = $name;
        $this->type = $type;
        $this->actions = $actions;
        $this->convert = $convert;
        $this->mime = $mime;
    }

    public function extension(): string
    {
        return $this->name;
    }

    public function convertible(): bool
    {
        return in_array('auto-convert', $this->actions);
    }

    public function editable(): bool
    {
        return array_intersect(['lossy-edit', 'edit'], $this->actions) !== [];
    }

    public function fillable(): bool
    {
        return in_array('fill', $this->actions);
    }

    public function isWord(): bool
    {
        return $this->type === FormatType::WORD;
    }

    public function isCell(): bool
    {
        return $this->type === FormatType::CELL;
    }

    public function isSlide(): bool
    {
        return $this->type === FormatType::SLIDE;
    }

    public function isPDF(): bool
    {
        return $this->type === FormatType::PDF;
    }
}
