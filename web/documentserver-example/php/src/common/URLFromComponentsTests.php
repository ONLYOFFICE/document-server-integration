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

namespace Example\Common\Tests;

use PHPUnit\Framework\TestCase;
use Example\Common\URL;

final class URLFromComponentsTests extends TestCase
{
    public function testCreates()
    {
        $url = URL::fromComponents(
            'http',
            'localhost',
            8080,
            'user',
            'password',
            '/path',
            'q=value',
            'fragment'
        );
        $this->assertEquals('http', $url->scheme());
        $this->assertEquals('localhost', $url->host());
        $this->assertEquals(8080, $url->port());
        $this->assertEquals('user', $url->user());
        $this->assertEquals('password', $url->pass());
        $this->assertEquals('/path', $url->path());
        $this->assertEquals('q=value', $url->query());
        $this->assertEquals('fragment', $url->fragment());
    }
}
