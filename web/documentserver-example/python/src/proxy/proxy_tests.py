"""

 (c) Copyright Ascensio System SIA 2023

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

"""

# pylint: disable=missing-class-docstring
# pylint: disable=missing-function-docstring

from unittest import TestCase
from unittest.mock import patch
from urllib.parse import urlparse
from src.configuration import ConfigurationManager
from . import ProxyManager

class ProxyManagerTests(TestCase):
    @patch.object(
        ConfigurationManager,
        'document_server_public_url',
        return_value=urlparse('http://localhost:3000')
    )
    @patch.object(
        ConfigurationManager,
        'document_server_private_url',
        return_value=urlparse('http://proxy:3001')
    )
    def test_resolves_a_url_that_refers_to_the_document_server_public_url(self, *_):
        config_manager = ConfigurationManager()
        proxy_manager = ProxyManager(config_manager)

        url = 'http://localhost:3000/endpoint?query=string'
        resolved_url = proxy_manager.resolve_document_server_url(url)

        self.assertEqual(
            resolved_url.geturl(),
            'http://proxy:3001/endpoint?query=string'
        )

    @patch.object(
        ConfigurationManager,
        'document_server_public_url',
        return_value=urlparse('http://localhost:3000')
    )
    def test_resolves_a_url_that_does_not_refers_to_the_document_server_public_url(self, _):
        config_manager = ConfigurationManager()
        proxy_manager = ProxyManager(config_manager)

        url = 'http://localhost:8080/endpoint?query=string'
        resolved_url = proxy_manager.resolve_document_server_url(url)

        self.assertEqual(
            resolved_url.geturl(),
            'http://localhost:8080/endpoint?query=string'
        )
