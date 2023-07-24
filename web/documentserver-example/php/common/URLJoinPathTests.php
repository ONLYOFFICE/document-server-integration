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
use OnlineEditorsExamplePhp\Common\URL;

final class URLJoinPathTests extends TestCase {
    public function test_joins_a_relative_to_an_empty_one() {
        $url = new URL('http://localhost');
        $joined = $url->join_path('first');
        $this->assertEquals('http', $joined->scheme());
        $this->assertEquals('localhost', $joined->host());
        $this->assertEquals(null, $joined->port());
        $this->assertEquals(null, $joined->user());
        $this->assertEquals(null, $joined->pass());
        $this->assertEquals('/first', $joined->path());
        $this->assertEquals(null, $joined->query());
        $this->assertEquals(null, $joined->fragment());
    }

    public function test_joins_a_relative() {
        $url = new URL('http://localhost/first');
        $joined = $url->join_path('second');
        $this->assertEquals('http', $joined->scheme());
        $this->assertEquals('localhost', $joined->host());
        $this->assertEquals(null, $joined->port());
        $this->assertEquals(null, $joined->user());
        $this->assertEquals(null, $joined->pass());
        $this->assertEquals('/first/second', $joined->path());
        $this->assertEquals(null, $joined->query());
        $this->assertEquals(null, $joined->fragment());
    }

    public function test_joins_an_absolute_to_an_empty_one() {
        $url = new URL('http://localhost');
        $joined = $url->join_path('/first');
        $this->assertEquals('http', $joined->scheme());
        $this->assertEquals('localhost', $joined->host());
        $this->assertEquals(null, $joined->port());
        $this->assertEquals(null, $joined->user());
        $this->assertEquals(null, $joined->pass());
        $this->assertEquals('/first', $joined->path());
        $this->assertEquals(null, $joined->query());
        $this->assertEquals(null, $joined->fragment());
    }

    public function test_joins_an_absolute() {
        $url = new URL('http://localhost/first');
        $joined = $url->join_path('/second');
        $this->assertEquals('http', $joined->scheme());
        $this->assertEquals('localhost', $joined->host());
        $this->assertEquals(null, $joined->port());
        $this->assertEquals(null, $joined->user());
        $this->assertEquals(null, $joined->pass());
        $this->assertEquals('/first/second', $joined->path());
        $this->assertEquals(null, $joined->query());
        $this->assertEquals(null, $joined->fragment());
    }
}
