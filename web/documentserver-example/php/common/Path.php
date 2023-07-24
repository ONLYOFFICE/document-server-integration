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

    public ?string $dirname;
    public ?string $basename;
    public ?string $extension;
    public ?string $filename;

    public function __construct(string $path) {
        $parsed_path = pathinfo($path);
        $this->dirname = self::dirname($parsed_path);
        $this->basename = self::basename($parsed_path);
        $this->extension = self::extension($parsed_path);
        $this->filename = self::filename($parsed_path);
    }

    private static function dirname(array $path): ?string {
        return isset($path['dirname'])
            ? $path['dirname']
            : null;
    }

    private static function basename(array $path): ?string {
        return isset($path['basename'])
            ? $path['basename']
            : null;
    }

    private static function extension(array $path): ?string {
        return isset($path['extension'])
            ? $path['extension']
            : null;
    }

    private static function filename(array $path): ?string {
        return isset($path['filename'])
            ? $path['filename']
            : null;
    }

    public function string(): string {
        $parts = array();
        if (!$this->dirname && !$this->basename) {
            return '.';
        }
        if ($this->dirname === $this->separator && $this->basename) {
            return "{$this->dirname}{$this->basename}";
        }
        if ($this->dirname !== '.') {
            $parts[] = $this->dirname;
        }
        if ($this->basename) {
            $parts[] = $this->basename;
        }
        return implode($this->separator, $parts);
    }

    public function join(string ...$paths): self {
        if (!isset($paths[0])) {
            return $this;
        }

        $next_paths = array_slice($paths, 1);
        if ($paths[0] == '' || $paths[0] == '.') {
            return $this->join(...$next_paths);
        }

        $sub = new Path($paths[0]);
        $sub_string = $sub->string();

        $string = $this->string();
        $separator = str_starts_with($sub_string, $this->separator)
            ? ''
            : $this->separator;

        $joined_string = "{$string}{$separator}{$sub_string}";
        $joined = new Path($joined_string);

        return $joined->join(...$next_paths);
    }
}
