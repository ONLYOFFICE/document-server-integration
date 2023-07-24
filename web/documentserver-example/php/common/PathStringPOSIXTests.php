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

use PHPUnit\Framework\TestCase;
use OnlineEditorsExamplePhp\Common\Path;

final class PathStringPOSIXTests extends TestCase {
    public function test_generates_with_an_empty() {
        $path = new Path('');
        $string = $path->string();
        $this->assertEquals($string, '');
    }

    public function test_generates_with_an_empty_relative() {
        $path = new Path('.');
        $string = $path->string();
        $this->assertEquals($string, '.');
    }

    public function test_generates_with_an_empty_absolute() {
        $path = new Path('/');
        $string = $path->string();
        $this->assertEquals($string, '/');
    }

    public function test_generates_with_a_relative() {
        $path = new Path('srv');
        $string = $path->string();
        $this->assertEquals($string, 'srv');
    }

    public function test_generates_with_a_relative_containing_a_directory() {
        $path = new Path('srv/sub');
        $string = $path->string();
        $this->assertEquals($string, 'srv/sub');
    }

    public function test_generates_with_a_relative_containing_a_file() {
        $path = new Path('srv/file.json');
        $string = $path->string();
        $this->assertEquals($string, 'srv/file.json');
    }

    public function test_generates_with_a_relative_containing_a_directory_with_a_file() {
        $path = new Path('srv/sub/file.json');
        $string = $path->string();
        $this->assertEquals($string, 'srv/sub/file.json');
    }

    public function test_generates_with_an_unnormalized_relative() {
        $path = new Path('srv////sub///file.json');
        $string = $path->string();
        $this->assertEquals($string, 'srv////sub///file.json');
    }

    public function test_generates_with_an_normalized_relative() {
        $path = new Path('srv////sub///file.json');
        $normalized = $path->normalize();
        $string = $normalized->string();
        $this->assertEquals($string, 'srv/sub/file.json');
    }

    public function test_generates_with_an_explicit_relative() {
        $path = new Path('./srv');
        $string = $path->string();
        $this->assertEquals($string, './srv');
    }

    public function test_generates_with_an_explicit_relative_containing_a_directory() {
        $path = new Path('./srv/sub');
        $string = $path->string();
        $this->assertEquals($string, './srv/sub');
    }

    public function test_generates_with_an_explicit_relative_containing_a_file() {
        $path = new Path('./srv/file.json');
        $string = $path->string();
        $this->assertEquals($string, './srv/file.json');
    }

    public function test_generates_with_an_explicit_relative_containing_a_directory_with_a_file() {
        $path = new Path('./srv/sub/file.json');
        $string = $path->string();
        $this->assertEquals($string, './srv/sub/file.json');
    }

    public function test_generates_with_an_explicit_unnormalized_relative() {
        $path = new Path('./srv////sub///file.json');
        $string = $path->string();
        $this->assertEquals($string, './srv////sub///file.json');
    }

    public function test_generates_with_an_explicit_normalized_relative() {
        $path = new Path('./srv////sub///file.json');
        $normalized = $path->normalize();
        $string = $normalized->string();
        $this->assertEquals($string, 'srv/sub/file.json');
    }

    public function test_generates_with_an_absolute() {
        $path = new Path('/srv');
        $string = $path->string();
        $this->assertEquals($string, '/srv');
    }

    public function test_generates_with_an_absolute_containing_a_directory() {
        $path = new Path('/srv/sub');
        $string = $path->string();
        $this->assertEquals($string, '/srv/sub');
    }

    public function test_generates_with_an_absolute_containing_a_file() {
        $path = new Path('/srv/file.json');
        $string = $path->string();
        $this->assertEquals($string, '/srv/file.json');
    }

    public function test_generates_with_an_absolute_containing_a_directory_with_a_file() {
        $path = new Path('/srv/sub/file.json');
        $string = $path->string();
        $this->assertEquals($string, '/srv/sub/file.json');
    }

    public function test_generates_with_an_unnormalized_absolute() {
        $path = new Path('/srv////sub///file.json');
        $string = $path->string();
        $this->assertEquals($string, '/srv////sub///file.json');
    }

    public function test_generates_with_an_normalized_absolute() {
        $path = new Path('/srv////sub///file.json');
        $normalized = $path->normalize();
        $string = $normalized->string();
        $this->assertEquals($string, '/srv/sub/file.json');
    }
}
