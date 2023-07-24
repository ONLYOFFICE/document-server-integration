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
use Example\Common\Path;

final class PathJoinPOSIXTests extends TestCase {
    public function test_joins_a_relative_to_an_empty_one() {
        $path = new Path('');
        $joined = $path->join_path('srv');
        $this->assertEquals($joined->dirname(), '/');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function test_joins_a_relative_to_a_relative_one() {
        $path = new Path('.');
        $joined = $path->join_path('srv');
        $this->assertEquals($joined->dirname(), '.');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function test_joins_a_relative_to_an_absolute_one() {
        $path = new Path('/');
        $joined = $path->join_path('srv');
        $this->assertEquals($joined->dirname(), '/');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function test_joins_an_absolute_to_an_empty_one() {
        $path = new Path('');
        $joined = $path->join_path('/srv');
        $this->assertEquals($joined->dirname(), '/');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function test_joins_an_absolute_to_a_relative_one() {
        $path = new Path('.');
        $joined = $path->join_path('/srv');
        $this->assertEquals($joined->dirname(), '.');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function test_joins_an_absolute_to_an_absolute_one() {
        $path = new Path('/');
        $joined = $path->join_path('/srv');
        $this->assertEquals($joined->dirname(), '/');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function test_joins_an_unnormalized() {
        $path = new Path('');
        $joined = $path->join_path('../srv');
        $this->assertEquals($joined->dirname(), '/..');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }
}
