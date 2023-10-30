<?php
//
// (c) Copyright Ascensio System SIA 2023
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

namespace Example\Common;

final class Path
{
    private string $separator = DIRECTORY_SEPARATOR;
    private string $string;

    public function __construct(string $path)
    {
        $this->string = $path;
    }

    public function string(): string
    {
        return $this->string;
    }

    public function dirname(): ?string
    {
        $string = $this->string();
        $parsed = pathinfo($string, PATHINFO_DIRNAME);
        return $parsed ?: null;
    }

    public function basename(): ?string
    {
        $string = $this->string();
        $parsed = pathinfo($string, PATHINFO_BASENAME);
        return $parsed ?: null;
    }

    public function extension(): ?string
    {
        $string = $this->string();
        $parsed = pathinfo($string, PATHINFO_EXTENSION);
        return $parsed ?: null;
    }

    public function filename(): ?string
    {
        $string = $this->string();
        $parsed = pathinfo($string, PATHINFO_FILENAME);
        return $parsed ?: null;
    }

    public function normalize(): self
    {
        $string = $this->string();
        $filtered = array();
        $slugs = explode($this->separator, $string);
        foreach ($slugs as $slug) {
            if ($slug === '.') {
                continue;
            }
            if ($slug === '..') {
                array_pop($filtered);
                continue;
            }
            $filtered[] = $slug;
        }
        $joined = implode($this->separator, $filtered);
        $escapedSeparator = preg_quote($this->separator, $this->separator);
        $separatorRegex = "/{$escapedSeparator}{2,}/";
        $separated = preg_replace($separatorRegex, $this->separator, $joined);
        return new Path($separated);
    }

    public function joinPath(string $path): self
    {
        $string = $this->string();
        $separator =
            str_ends_with($string, $this->separator) ||
            str_starts_with($path, $this->separator)
            ? ''
            : $this->separator;
        return new Path("{$string}{$separator}{$path}");
    }

    public function absolute(): bool
    {
        $string = $this->string();

        $ntRegex = '/^[A-Za-z]:\\\\/';
        if (preg_match($ntRegex, $string)) {
            return true;
        }

        $ntSeparator = '\\';
        if (str_starts_with($string, $ntSeparator)) {
            return true;
        }

        $posixSeparator = '/';
        if (str_starts_with($string, $posixSeparator)) {
            return true;
        }

        return false;
    }

    public function exists(): bool
    {
        $string = $this->string();
        return file_exists($string);
    }

    public function contents(): string
    {
        $string = $this->string();
        return file_get_contents($string);
    }

    public function directory(): bool
    {
        $string = $this->string();
        return is_dir($string);
    }

    public function makeDirectory(): bool
    {
        $string = $this->string();
        return mkdir($string);
    }
}
