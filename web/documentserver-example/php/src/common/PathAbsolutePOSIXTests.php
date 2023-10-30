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
use Example\Common\Path;

final class PathAbsolutePOSIXTests extends TestCase
{
    public function testRecognizesAnEmptyAsANonAbsolute()
    {
        $path = new Path('');
        $absolute = $path->absolute();
        $this->assertFalse($absolute);
    }

    public function testRecognizesARelativeAsANonAbsolute()
    {
        $path = new Path('.');
        $absolute = $path->absolute();
        $this->assertFalse($absolute);
    }

    public function testRecognizesAnAbsoluteAsAnAbsolute()
    {
        $path = new Path('/');
        $absolute = $path->absolute();
        $this->assertTrue($absolute);
    }
}
