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
