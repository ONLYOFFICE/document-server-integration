#
# (c) Copyright Ascensio System SIA 2023
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from unittest import TestCase
from unittest.mock import patch
from urllib.parse import urlparse
from src.configuration import ConfigurationManager
from . import ProxyManager

class ProxyManagerTests(TestCase):
    @patch.object(
        ConfigurationManager,
        'document_server_public_url',
        return_value=urlparse('http://localhost')
    )
    @patch.object(
        ConfigurationManager,
        'document_server_private_url',
        return_value=urlparse('http://proxy')
    )
    def test_resolves_a_url_that_refers_to_the_public_url(self, *_):
        config_manager = ConfigurationManager()
        proxy_manager = ProxyManager(config_manager)

        raw_url = 'http://localhost/endpoint?query=string'
        url = urlparse(raw_url)
        resolved_url = proxy_manager.resolve_url(url)

        self.assertEqual(
            resolved_url.geturl(),
            'http://proxy/endpoint?query=string'
        )

    @patch.object(
        ConfigurationManager,
        'document_server_public_url',
        return_value=urlparse('http://localhost')
    )
    def test_resolves_a_url_that_does_not_refers_to_the_public_url(self, *_):
        config_manager = ConfigurationManager()
        proxy_manager = ProxyManager(config_manager)

        raw_url = 'http://proxy/endpoint?query=string'
        url = urlparse(raw_url)
        resolved_url = proxy_manager.resolve_url(url)

        self.assertEqual(
            resolved_url.geturl(),
            'http://proxy/endpoint?query=string'
        )
