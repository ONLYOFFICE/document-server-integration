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

final class URLStringTests extends TestCase
{
    public function testGenerates()
    {
        $url = new URL('http://user:password@localhost:8080/path?q=value#fragment');
        $string = $url->string();
        $this->assertEquals(
            'http://user:password@localhost:8080/path?q=value#fragment',
            $string
        );
    }
}
