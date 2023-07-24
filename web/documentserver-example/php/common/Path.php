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

namespace OnlineEditorsExamplePhp\Common;

final class Path {
    private string $separator = DIRECTORY_SEPARATOR;
    private string $string;

    public function __construct(string $path) {
        $this->string = $path;
    }

    public function string(): string {
        return $this->string;
    }

    public function dirname(): ?string {
        $string = $this->string();
        $parsed = pathinfo($string, PATHINFO_DIRNAME);
        return $parsed ?: null;
    }

    public function basename(): ?string {
        $string = $this->string();
        $parsed = pathinfo($string, PATHINFO_BASENAME);
        return $parsed ?: null;
    }

    public function extension(): ?string {
        $string = $this->string();
        $parsed = pathinfo($string, PATHINFO_EXTENSION);
        return $parsed ?: null;
    }

    public function filename(): ?string {
        $string = $this->string();
        $parsed = pathinfo($string, PATHINFO_FILENAME);
        return $parsed ?: null;
    }

    public function normalize(): self {
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
        $escaped_separator = preg_quote($this->separator, $this->separator);
        $separator_regex = "/{$escaped_separator}{2,}/";
        $separated = preg_replace($separator_regex, $this->separator, $joined);
        return new Path($separated);
    }

    public function join_path(string $path): self {
        $string = $this->string();
        $separator =
            str_ends_with($string, $this->separator) ||
            str_starts_with($path, $this->separator)
            ? ''
            : $this->separator;
        return new Path("{$string}{$separator}{$path}");
    }
}
