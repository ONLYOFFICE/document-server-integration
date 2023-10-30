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

final class PathStringPOSIXTests extends TestCase
{
    public function testGeneratesWithAnEmpty()
    {
        $path = new Path('');
        $string = $path->string();
        $this->assertEquals($string, '');
    }

    public function testGeneratesWithAnEmptyRelative()
    {
        $path = new Path('.');
        $string = $path->string();
        $this->assertEquals($string, '.');
    }

    public function testGeneratesWithAnEmptyAbsolute()
    {
        $path = new Path('/');
        $string = $path->string();
        $this->assertEquals($string, '/');
    }

    public function testGeneratesWithARelative()
    {
        $path = new Path('srv');
        $string = $path->string();
        $this->assertEquals($string, 'srv');
    }

    public function testGeneratesWithARelativeContainingADirectory()
    {
        $path = new Path('srv/sub');
        $string = $path->string();
        $this->assertEquals($string, 'srv/sub');
    }

    public function testGeneratesWithARelativeContainingAFile()
    {
        $path = new Path('srv/file.json');
        $string = $path->string();
        $this->assertEquals($string, 'srv/file.json');
    }

    public function testGeneratesWithARelativeContainingADirectoryWithAFile()
    {
        $path = new Path('srv/sub/file.json');
        $string = $path->string();
        $this->assertEquals($string, 'srv/sub/file.json');
    }

    public function testGeneratesWithAnUnnormalizedRelative()
    {
        $path = new Path('srv////sub///file.json');
        $string = $path->string();
        $this->assertEquals($string, 'srv////sub///file.json');
    }

    public function testGeneratesWithAnNormalizedRelative()
    {
        $path = new Path('srv////sub///file.json');
        $normalized = $path->normalize();
        $string = $normalized->string();
        $this->assertEquals($string, 'srv/sub/file.json');
    }

    public function testGeneratesWithAnExplicitRelative()
    {
        $path = new Path('./srv');
        $string = $path->string();
        $this->assertEquals($string, './srv');
    }

    public function testGeneratesWithAnExplicitRelativeContainingADirectory()
    {
        $path = new Path('./srv/sub');
        $string = $path->string();
        $this->assertEquals($string, './srv/sub');
    }

    public function testGeneratesWithAnExplicitRelativeContainingAFile()
    {
        $path = new Path('./srv/file.json');
        $string = $path->string();
        $this->assertEquals($string, './srv/file.json');
    }

    public function testGeneratesWithAnExplicitRelativeContainingADirectoryWithAFile()
    {
        $path = new Path('./srv/sub/file.json');
        $string = $path->string();
        $this->assertEquals($string, './srv/sub/file.json');
    }

    public function testGeneratesWithAnExplicitUnnormalizedRelative()
    {
        $path = new Path('./srv////sub///file.json');
        $string = $path->string();
        $this->assertEquals($string, './srv////sub///file.json');
    }

    public function testGeneratesWithAnExplicitNormalizedRelative()
    {
        $path = new Path('./srv////sub///file.json');
        $normalized = $path->normalize();
        $string = $normalized->string();
        $this->assertEquals($string, 'srv/sub/file.json');
    }

    public function testGeneratesWithAnAbsolute()
    {
        $path = new Path('/srv');
        $string = $path->string();
        $this->assertEquals($string, '/srv');
    }

    public function testGeneratesWithAnAbsoluteContainingADirectory()
    {
        $path = new Path('/srv/sub');
        $string = $path->string();
        $this->assertEquals($string, '/srv/sub');
    }

    public function testGeneratesWithAnAbsoluteContainingAFile()
    {
        $path = new Path('/srv/file.json');
        $string = $path->string();
        $this->assertEquals($string, '/srv/file.json');
    }

    public function testGeneratesWithAnAbsoluteContainingADirectoryWithAFile()
    {
        $path = new Path('/srv/sub/file.json');
        $string = $path->string();
        $this->assertEquals($string, '/srv/sub/file.json');
    }

    public function testGeneratesWithAnUnnormalizedAbsolute()
    {
        $path = new Path('/srv////sub///file.json');
        $string = $path->string();
        $this->assertEquals($string, '/srv////sub///file.json');
    }

    public function testGeneratesWithAnNormalizedAbsolute()
    {
        $path = new Path('/srv////sub///file.json');
        $normalized = $path->normalize();
        $string = $normalized->string();
        $this->assertEquals($string, '/srv/sub/file.json');
    }
}
