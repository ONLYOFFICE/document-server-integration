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

final class URL {
    private string $string;

    public function __construct(string $url) {
        $this->string = $url;
    }

    public function string(): string {
        return $this->string;
    }

    public function scheme(): ?string {
        $string = $this->string();
        return parse_url($string, PHP_URL_SCHEME) ?: null;
    }

    public function host(): ?string {
        $string = $this->string();
        return parse_url($string, PHP_URL_HOST) ?: null;
    }

    public function port(): ?int {
        $string = $this->string();
        return parse_url($string, PHP_URL_PORT) ?: null;
    }

    public function user(): ?string {
        $string = $this->string();
        return parse_url($string, PHP_URL_USER) ?: null;
    }

    public function pass(): ?string {
        $string = $this->string();
        return parse_url($string, PHP_URL_PASS) ?: null;
    }

    public function path(): ?string {
        $string = $this->string();
        return parse_url($string, PHP_URL_PATH) ?: null;
    }

    public function query(): ?string {
        $string = $this->string();
        return parse_url($string, PHP_URL_QUERY) ?: null;
    }

    public function fragment(): ?string {
        $string = $this->string();
        return parse_url($string, PHP_URL_FRAGMENT) ?: null;
    }

    public static function from_components(
        ?string $scheme,
        ?string $host,
        ?int $port,
        ?string $user,
        ?string $pass,
        ?string $path,
        ?string $query,
        ?string $fragment
    ): URL {
        $string = '';
        if ($scheme) {
            $string .= "{$scheme}://";
        }
        if ($user) {
            $string .= $user;
        }
        if ($pass) {
            $string .= ":{$pass}@";
        }
        if ($host) {
            $string .= $host;
        }
        if ($port) {
            $string .= ":{$port}";
        }
        if ($path) {
            $string .= $path;
        }
        if ($query) {
            $string .= "?{$query}";
        }
        if ($fragment) {
            $string .= "#{$fragment}";
        }
        return new URL($string);
    }

    public function join_path(string $path): self {
        $current_path = $this->path();
        $separator =
            $current_path &&
            (
                str_ends_with($current_path, '/') ||
                str_starts_with($path, '/')
            ) ||
            !$current_path && str_starts_with($path, '/')
            ? ''
            : '/';
        $separated = "{$current_path}{$separator}{$path}";
        return URL::from_components(
            $this->scheme(),
            $this->host(),
            $this->port(),
            $this->user(),
            $this->pass(),
            $separated,
            $this->query(),
            $this->fragment()
        );
    }
}
