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

from os import environ
from unittest import TestCase
from unittest.mock import patch
from urllib.parse import urlparse
from . import ConfigurationManager

class ConfigurationManagerTests(TestCase):
    def test_corresponds_the_latest_version(self):
        config_manager = ConfigurationManager()
        self.assertEqual(config_manager.version, '1.6.0')

class ConfigurationManagerExampleURLTests(TestCase):
    def test_assigns_a_default_value(self):
        config_manager = ConfigurationManager()
        url = config_manager.example_url()
        self.assertIsNone(url)

    @patch.dict(environ, {
        'EXAMPLE_URL': 'http://localhost'
    })
    def test_assigns_a_value_from_the_environment(self):
        config_manager = ConfigurationManager()
        url = config_manager.example_url()
        self.assertEqual(url.geturl(), 'http://localhost')

class ConfigurationManagerDocumentServerPublicURLTests(TestCase):
    def test_assigns_a_default_value(self):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_public_url()
        self.assertEqual(url.geturl(), 'http://document-server')

    @patch.dict(environ, {
        'DOCUMENT_SERVER_PUBLIC_URL': 'http://localhost'
    })
    def test_assigns_a_value_from_the_environment(self):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_public_url()
        self.assertEqual(url.geturl(), 'http://localhost')

class ConfigurationManagerDocumentServerPrivateURLTests(TestCase):
    def test_assigns_a_default_value(self):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_private_url()
        self.assertEqual(url.geturl(), 'http://document-server')

    @patch.dict(environ, {
        'DOCUMENT_SERVER_PRIVATE_URL': 'http://localhost'
    })
    def test_assigns_a_value_from_the_environment(self):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_private_url()
        self.assertEqual(url.geturl(), 'http://localhost')

class ConfigurationManagerDocumentServerAPIURLTests(TestCase):
    @patch.object(
        ConfigurationManager,
        'document_server_public_url',
        return_value=urlparse('http://localhost')
    )
    def test_assigns_a_default_value(self, _):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_api_url()
        self.assertEqual(
            url.geturl(),
            'http://localhost/web-apps/apps/api/documents/api.js'
        )

    @patch.object(
        ConfigurationManager,
        'document_server_public_url',
        return_value=urlparse('http://localhost')
    )
    @patch.dict(environ, {
        'DOCUMENT_SERVER_API_PATH': '/api'
    })
    def test_assigns_a_value_from_the_environment(self, _):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_api_url()
        self.assertEqual(
            url.geturl(),
            'http://localhost/api'
        )

class ConfigurationManagerDocumentServerPreloaderURLTests(TestCase):
    @patch.object(
        ConfigurationManager,
        'document_server_public_url',
        return_value=urlparse('http://localhost')
    )
    def test_assigns_a_default_value(self, _):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_preloader_url()
        self.assertEqual(
            url.geturl(),
            'http://localhost/web-apps/apps/api/documents/cache-scripts.html'
        )

    @patch.object(
        ConfigurationManager,
        'document_server_public_url',
        return_value=urlparse('http://localhost')
    )
    @patch.dict(environ, {
        'DOCUMENT_SERVER_PRELOADER_PATH': '/preloader'
    })
    def test_assigns_a_value_from_the_environment(self, _):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_preloader_url()
        self.assertEqual(
            url.geturl(),
            'http://localhost/preloader'
        )

class ConfigurationManagerDocumentServerCommandURLTests(TestCase):
    @patch.object(
        ConfigurationManager,
        'document_server_private_url',
        return_value=urlparse('http://localhost')
    )
    def test_assigns_a_default_value(self, _):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_command_url()
        self.assertEqual(
            url.geturl(),
            'http://localhost/coauthoring/CommandService.ashx'
        )

    @patch.object(
        ConfigurationManager,
        'document_server_private_url',
        return_value=urlparse('http://localhost')
    )
    @patch.dict(environ, {
        'DOCUMENT_SERVER_COMMAND_PATH': '/command'
    })
    def test_assigns_a_value_from_the_environment(self, _):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_command_url()
        self.assertEqual(
            url.geturl(),
            'http://localhost/command'
        )

class ConfigurationManagerDocumentServerConverterURLTests(TestCase):
    @patch.object(
        ConfigurationManager,
        'document_server_private_url',
        return_value=urlparse('http://localhost')
    )
    def test_assigns_a_default_value(self, _):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_converter_url()
        self.assertEqual(
            url.geturl(),
            'http://localhost/ConvertService.ashx'
        )

    @patch.object(
        ConfigurationManager,
        'document_server_private_url',
        return_value=urlparse('http://localhost')
    )
    @patch.dict(environ, {
        'DOCUMENT_SERVER_CONVERTER_PATH': '/converter'
    })
    def test_assigns_a_value_from_the_environment(self, _):
        config_manager = ConfigurationManager()
        url = config_manager.document_server_converter_url()
        self.assertEqual(
            url.geturl(),
            'http://localhost/converter'
        )

class ConfigurationManagerJWTSecretTests(TestCase):
    def test_assigns_a_default_value(self):
        config_manager = ConfigurationManager()
        secret = config_manager.jwt_secret()
        self.assertEqual(secret, '')

    @patch.dict(environ, {
        'JWT_SECRET': 'your-256-bit-secret'
    })
    def test_assigns_a_value_from_the_environment(self):
        config_manager = ConfigurationManager()
        secret = config_manager.jwt_secret()
        self.assertEqual(secret, 'your-256-bit-secret')

class ConfigurationManagerJWTHeaderTests(TestCase):
    def test_assigns_a_default_value(self):
        config_manager = ConfigurationManager()
        header = config_manager.jwt_header()
        self.assertEqual(header, 'Authorization')

    @patch.dict(environ, {
        'JWT_HEADER': 'Proxy-Authorization'
    })
    def test_assigns_a_value_from_the_environment(self):
        config_manager = ConfigurationManager()
        header = config_manager.jwt_header()
        self.assertEqual(header, 'Proxy-Authorization')

class ConfigurationManagerJWTUseForRequest(TestCase):
    def test_assigns_a_default_value(self):
        config_manager = ConfigurationManager()
        use = config_manager.jwt_use_for_request()
        self.assertTrue(use)

    @patch.dict(environ, {
        'JWT_USE_FOR_REQUEST': 'false'
    })
    def test_assigns_a_value_from_the_environment(self):
        config_manager = ConfigurationManager()
        use = config_manager.jwt_use_for_request()
        self.assertFalse(use)

class ConfigurationManagerSSLTests(TestCase):
    def test_assigns_a_default_value(self):
        config_manager = ConfigurationManager()
        enabled = config_manager.ssl_verify_peer_mode_enabled()
        self.assertFalse(enabled)

    @patch.dict(environ, {
        'SSL_VERIFY_PEER_MODE_ENABLED': 'true'
    })
    def test_assigns_a_value_from_the_environment(self):
        config_manager = ConfigurationManager()
        enabled = config_manager.ssl_verify_peer_mode_enabled()
        self.assertTrue(enabled)

class ConfigurationManagerStoragePathTests(TestCase):
    def test_assigns_a_default_value(self):
        config_manager = ConfigurationManager()
        path = config_manager.storage_path()
        self.assertTrue(path.is_absolute())
        self.assertEqual(path.name, 'storage')

    @patch.dict(environ, {
        'STORAGE_PATH': 'directory'
    })
    def test_assigns_a_relative_path_from_the_environment(self):
        config_manager = ConfigurationManager()
        path = config_manager.storage_path()
        self.assertTrue(path.is_absolute())
        self.assertEqual(path.name, 'directory')

    @patch.dict(environ, {
        'STORAGE_PATH': '/directory'
    })
    def test_assigns_an_absolute_path_from_the_environment(self):
        config_manager = ConfigurationManager()
        path = config_manager.storage_path()
        self.assertEqual(f'{path}', '/directory')

class ConfigurationManagerMaximumFileSizeTests(TestCase):
    def test_assigns_a_default_value(self):
        config_manager = ConfigurationManager()
        size = config_manager.maximum_file_size()
        self.assertEqual(size, 5_242_880)

    @patch.dict(environ, {
        'MAXIMUM_FILE_SIZE': '10'
    })
    def test_assigns_a_value_from_the_environment(self):
        config_manager = ConfigurationManager()
        size = config_manager.maximum_file_size()
        self.assertEqual(size, 10)

class ConfigurationManagerConversionTimeoutTests(TestCase):
    def test_assigns_a_default_value(self):
        config_manager = ConfigurationManager()
        timeout = config_manager.conversion_timeout()
        self.assertEqual(timeout, 120_000)

    @patch.dict(environ, {
        'CONVERSION_TIMEOUT': '10'
    })
    def test_assigns_a_value_from_the_environment(self):
        config_manager = ConfigurationManager()
        timeout = config_manager.conversion_timeout()
        self.assertEqual(timeout, 10)
