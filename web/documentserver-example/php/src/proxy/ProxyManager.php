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

namespace Example\Proxy;

use Example\Common\URL;
use Example\Configuration\ConfigurationManager;

class ProxyManager
{
    public ConfigurationManager $configManager;

    public function __construct(ConfigurationManager $configManager)
    {
        $this->configManager = $configManager;
    }

    public function resolveURL(URL $url): URL
    {
        if (!$this->referPublicURL($url)) {
            return $url;
        }
        return $this->redirectPublicURL($url);
    }

    private function referPublicURL(URL $url): bool
    {
        $publicURL = $this->configManager->documentServerPublicURL();
        return
            $url->scheme() == $publicURL->scheme() &&
            $url->host() == $publicURL->host() &&
            $url->port() == $publicURL->port();
    }

    private function redirectPublicURL(URL $url): URL
    {
        $privateURL = $this->configManager->documentServerPrivateURL();
        return URL::fromComponents(
            $privateURL->scheme(),
            $privateURL->host(),
            $privateURL->port(),
            $url->user(),
            $url->pass(),
            $url->path(),
            $url->query(),
            $url->fragment()
        );
    }
}
