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

namespace Example\Configuration\Tests;

use PHPUnit\Framework\TestCase;
use Example\Configuration\ConfigurationManager;

final class ConfigurationManagerDocumentServerCommandURLTests extends TestCase
{
    public array $env;

    public function __construct(string $name)
    {
        $this->env = getenv();
        parent::__construct($name);
    }

    protected function setUp(): void
    {
        foreach ($this->env as $key => $value) {
            putenv("{$key}={$value}");
        }
    }

    public function testAssignsADefaultValue()
    {
        $configManager = new ConfigurationManager();
        $url = $configManager->documentServerCommandURL();
        $this->assertEquals(
            'http://document-server/coauthoring/CommandService.ashx',
            $url->string()
        );
    }

    public function testAssignsAValueFromTheEnvironment()
    {
        putenv('DOCUMENT_SERVER_COMMAND_PATH=/command');
        $configManager = new ConfigurationManager();
        $url = $configManager->documentServerCommandURL();
        $this->assertEquals(
            'http://document-server/command',
            $url->string()
        );
    }
}
