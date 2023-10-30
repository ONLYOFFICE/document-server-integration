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

final class PathJoinPOSIXTests extends TestCase
{
    public function testJoinsARelativeToAnEmptyOne()
    {
        $path = new Path('');
        $joined = $path->joinPath('srv');
        $this->assertEquals($joined->dirname(), '/');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function testJoinsARelativeToARelativeOne()
    {
        $path = new Path('.');
        $joined = $path->joinPath('srv');
        $this->assertEquals($joined->dirname(), '.');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function testJoinsARelativeToAnAbsoluteOne()
    {
        $path = new Path('/');
        $joined = $path->joinPath('srv');
        $this->assertEquals($joined->dirname(), '/');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function testJoinsAnAbsoluteToAnEmptyOne()
    {
        $path = new Path('');
        $joined = $path->joinPath('/srv');
        $this->assertEquals($joined->dirname(), '/');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function testJoinsAnAbsoluteToARelativeOne()
    {
        $path = new Path('.');
        $joined = $path->joinPath('/srv');
        $this->assertEquals($joined->dirname(), '.');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function testJoinsAnAbsoluteToAnAbsoluteOne()
    {
        $path = new Path('/');
        $joined = $path->joinPath('/srv');
        $this->assertEquals($joined->dirname(), '/');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }

    public function testJoinsAnUnnormalized()
    {
        $path = new Path('');
        $joined = $path->joinPath('../srv');
        $this->assertEquals($joined->dirname(), '/..');
        $this->assertEquals($joined->basename(), 'srv');
        $this->assertEquals($joined->extension(), null);
        $this->assertEquals($joined->filename(), 'srv');
    }
}
