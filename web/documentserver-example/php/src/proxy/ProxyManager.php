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
        $public_url = $this->configManager->document_server_public_url();
        return
            $url->scheme() == $public_url->scheme() &&
            $url->host() == $public_url->host() &&
            $url->port() == $public_url->port();
    }

    private function redirectPublicURL(URL $url): URL
    {
        $private_url = $this->configManager->document_server_private_url();
        return URL::from_components(
            $private_url->scheme(),
            $private_url->host(),
            $private_url->port(),
            $url->user(),
            $url->pass(),
            $url->path(),
            $url->query(),
            $url->fragment()
        );
    }
}
