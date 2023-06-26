# pylint: disable=missing-module-docstring
# pylint: disable=missing-class-docstring
# pylint: disable=missing-function-docstring
from os import environ
from unittest import TestCase, mock
from . import ConfigurationManager

class ConfigurationManagerTests(TestCase):
    def test_corresponds_the_latest_version(self):
        config = ConfigurationManager()
        self.assertEqual(config.version, '1.5.1')

class ConfigurationManagerExampleURLTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        url = config.example_url()
        self.assertIsNone(url)

    @mock.patch.dict(environ, {
        'EXAMPLE_URL': 'http://localhost'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        url = config.example_url()
        self.assertEqual(url.geturl(), 'http://localhost')

class ConfigurationManagerDocumentServerURLTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        url = config.document_server_url()
        self.assertEqual(url.geturl(), 'http://document-server/')

    @mock.patch.dict(environ, {
        'DOCUMENT_SERVER_URL': 'http://localhost'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        url = config.document_server_url()
        self.assertEqual(url.geturl(), 'http://localhost')

class ConfigurationManagerDocumentServerAPIURLTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        url = config.document_server_api_url()
        self.assertEqual(
            url.geturl(),
            'http://document-server/web-apps/apps/api/documents/api.js'
        )

    @mock.patch.dict(environ, {
        'DOCUMENT_SERVER_API_PATH': '/api'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        url = config.document_server_api_url()
        self.assertEqual(
            url.geturl(),
            'http://document-server/api'
        )

class ConfigurationManagerDocumentServerPreloaderURLTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        url = config.document_server_preloader_url()
        self.assertEqual(
            url.geturl(),
            'http://document-server/web-apps/apps/api/documents/cache-scripts.html'
        )

    @mock.patch.dict(environ, {
        'DOCUMENT_SERVER_PRELOADER_PATH': '/preloader'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        url = config.document_server_preloader_url()
        self.assertEqual(
            url.geturl(),
            'http://document-server/preloader'
        )

class ConfigurationManagerDocumentServerCommandURLTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        url = config.document_server_command_url()
        self.assertEqual(
            url.geturl(),
            'http://document-server/coauthoring/CommandService.ashx'
        )

    @mock.patch.dict(environ, {
        'DOCUMENT_SERVER_COMMAND_PATH': '/command'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        url = config.document_server_command_url()
        self.assertEqual(
            url.geturl(),
            'http://document-server/command'
        )

class ConfigurationManagerDocumentServerConverterURLTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        url = config.document_server_converter_url()
        self.assertEqual(
            url.geturl(),
            'http://document-server/ConvertService.ashx'
        )

    @mock.patch.dict(environ, {
        'DOCUMENT_SERVER_CONVERTER_PATH': '/converter'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        url = config.document_server_converter_url()
        self.assertEqual(
            url.geturl(),
            'http://document-server/converter'
        )

class ConfigurationManagerJWTSecretTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        secret = config.jwt_secret()
        self.assertEqual(secret, '')

    @mock.patch.dict(environ, {
        'JWT_SECRET': 'your-256-bit-secret'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        secret = config.jwt_secret()
        self.assertEqual(secret, 'your-256-bit-secret')

class ConfigurationManagerJWTHeaderTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        header = config.jwt_header()
        self.assertEqual(header, 'Authorization')

    @mock.patch.dict(environ, {
        'JWT_HEADER': 'Proxy-Authorization'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        header = config.jwt_header()
        self.assertEqual(header, 'Proxy-Authorization')

class ConfigurationManagerJWTUseForRequest(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        use = config.jwt_use_for_request()
        self.assertTrue(use)

    @mock.patch.dict(environ, {
        'JWT_USE_FOR_REQUEST': 'false'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        use = config.jwt_use_for_request()
        self.assertFalse(use)

class ConfigurationManagerSSLTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        enabled = config.ssl_verify_peer_mode_enabled()
        self.assertFalse(enabled)

    @mock.patch.dict(environ, {
        'SSL_VERIFY_PEER_MODE_ENABLED': 'true'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        enabled = config.ssl_verify_peer_mode_enabled()
        self.assertTrue(enabled)

class ConfigurationManagerStoragePathTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        path = config.storage_path()
        self.assertTrue(path.is_absolute())
        self.assertEqual(path.name, 'storage')

    @mock.patch.dict(environ, {
        'STORAGE_PATH': 'directory'
    })
    def test_assigns_a_relative_path_from_the_environment(self):
        config = ConfigurationManager()
        path = config.storage_path()
        self.assertTrue(path.is_absolute())
        self.assertEqual(path.name, 'directory')

    @mock.patch.dict(environ, {
        'STORAGE_PATH': '/directory'
    })
    def test_assigns_an_absolute_path_from_the_environment(self):
        config = ConfigurationManager()
        path = config.storage_path()
        self.assertEqual(path.as_uri(), 'file:///directory')

class ConfigurationManagerMaximumFileSizeTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        size = config.maximum_file_size()
        self.assertEqual(size, 5_242_880)

    @mock.patch.dict(environ, {
        'MAXIMUM_FILE_SIZE': '10'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        size = config.maximum_file_size()
        self.assertEqual(size, 10)

class ConfigurationManagerConversionTimeoutTests(TestCase):
    def test_assigns_a_default_value(self):
        config = ConfigurationManager()
        timeout = config.conversion_timeout()
        self.assertEqual(timeout, 120_000)

    @mock.patch.dict(environ, {
        'CONVERSION_TIMEOUT': '10'
    })
    def test_assigns_a_value_from_the_environment(self):
        config = ConfigurationManager()
        timeout = config.conversion_timeout()
        self.assertEqual(timeout, 10)
