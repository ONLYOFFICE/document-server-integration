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
use Example\Configuration\ConfigurationManager;

final class ConfigurationManagerDocumentServerConverterURLTests extends TestCase {
    public array $env;

    public function __construct(string $name) {
        $this->env = getenv();
        parent::__construct($name);
    }

    protected function setUp(): void {
        foreach ($this->env as $key => $value) {
            putenv("{$key}={$value}");
        }
    }

    public function test_assigns_a_default_value() {
        $config_manager = new ConfigurationManager();
        $url = $config_manager->document_server_converter_url();
        $this->assertEquals(
            'http://document-server/ConvertService.ashx',
            $url->string()
        );
    }

    public function test_assigns_a_value_from_the_environment() {
        putenv('DOCUMENT_SERVER_CONVERTER_PATH=/converter');
        $config_manager = new ConfigurationManager();
        $url = $config_manager->document_server_converter_url();
        $this->assertEquals(
            'http://document-server/converter',
            $url->string()
        );
    }
}
