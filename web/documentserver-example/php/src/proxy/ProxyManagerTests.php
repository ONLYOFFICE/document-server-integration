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

namespace Example\Proxy\Tests;

use Example\Common\URL;
use PHPUnit\Framework\TestCase;
use Example\Configuration\ConfigurationManager;
use Example\Proxy\ProxyManager;

final class ProxyManagerTests extends TestCase
{
    public function testResolvesAURLThatRefersToThePublicURL()
    {
        $configManager = $this->createStub(ConfigurationManager::class);
        $configManager
            ->method('documentServerPublicURL')
            ->willReturn(new URL('http://localhost'));
        $configManager
            ->method('documentServerPrivateURL')
            ->willReturn(new URL('http://proxy'));

        $proxyManager = new ProxyManager($configManager);

        $rawURL = 'http://localhost/endpoint?query=string';
        $url = new URL($rawURL);
        $resolvedURL = $proxyManager->resolveURL($url);

        $this->assertEquals(
            'http://proxy/endpoint?query=string',
            $resolvedURL->string()
        );
    }

    public function testResolvesAURLThatDoesNotRefersToThePublicURL()
    {
        $configManager = $this->createStub(ConfigurationManager::class);
        $configManager
            ->method('documentServerPublicURL')
            ->willReturn(new URL('http://localhost'));

        $proxyManager = new ProxyManager($configManager);

        $rawURL = 'http://proxy/endpoint?query=string';
        $url = new URL($rawURL);
        $resolvedURL = $proxyManager->resolveURL($url);

        $this->assertEquals(
            'http://proxy/endpoint?query=string',
            $resolvedURL->string()
        );
    }
}
