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
from pathlib import Path
from typing import Optional
from urllib.parse import ParseResult, urlparse, urljoin
from src.common import string

class ConfigurationManager:
    version = '1.7.0'

    def example_url(self) -> Optional[ParseResult]:
        url = environ.get('EXAMPLE_URL')
        if not url:
            return None
        return urlparse(url)

    def document_server_public_url(self) -> ParseResult:
        url = (
            environ.get('DOCUMENT_SERVER_PUBLIC_URL') or
            'http://document-server'
        )
        return urlparse(url)

    def document_server_private_url(self) -> ParseResult:
        url = environ.get('DOCUMENT_SERVER_PRIVATE_URL')
        if not url:
            return self.document_server_public_url()
        return urlparse(url)

    def document_server_api_url(self) -> ParseResult:
        server_url = self.document_server_public_url()
        base_url = server_url.geturl()
        path = (
            environ.get('DOCUMENT_SERVER_API_PATH') or
            '/web-apps/apps/api/documents/api.js'
        )
        url = urljoin(base_url, path)
        return urlparse(url)

    def document_server_preloader_url(self) -> ParseResult:
        server_url = self.document_server_public_url()
        base_url = server_url.geturl()
        path = (
            environ.get('DOCUMENT_SERVER_PRELOADER_PATH') or
            '/web-apps/apps/api/documents/cache-scripts.html'
        )
        url = urljoin(base_url, path)
        return urlparse(url)

    def document_server_command_url(self) -> ParseResult:
        server_url = self.document_server_private_url()
        base_url = server_url.geturl()
        path = (
            environ.get('DOCUMENT_SERVER_COMMAND_PATH') or
            '/coauthoring/CommandService.ashx'
        )
        url = urljoin(base_url, path)
        return urlparse(url)

    def document_server_converter_url(self) -> ParseResult:
        server_url = self.document_server_private_url()
        base_url = server_url.geturl()
        path = (
            environ.get('DOCUMENT_SERVER_CONVERTER_PATH') or
            '/ConvertService.ashx'
        )
        url = urljoin(base_url, path)
        return urlparse(url)

    def jwt_secret(self) -> str:
        return environ.get('JWT_SECRET') or ''

    def jwt_header(self) -> str:
        return environ.get('JWT_HEADER') or 'Authorization'

    def jwt_use_for_request(self) -> bool:
        use = environ.get('JWT_USE_FOR_REQUEST')
        return string.boolean(use, True)

    def ssl_verify_peer_mode_enabled(self) -> bool:
        enabled = environ.get('SSL_VERIFY_PEER_MODE_ENABLED')
        return string.boolean(enabled, False)

    def storage_path(self) -> Path:
        storage_path = environ.get('STORAGE_PATH') or 'storage'
        storage_directory = Path(storage_path)
        if storage_directory.is_absolute():
            return storage_directory
        file = Path(__file__)
        directory = file.parent.joinpath('../..', storage_directory)
        return directory.resolve()

    def maximum_file_size(self) -> int:
        size = environ.get('MAXIMUM_FILE_SIZE')
        if size:
            return int(size)
        return 5 * 1024 * 1024

    def conversion_timeout(self) -> int:
        timeout = environ.get('CONVERSION_TIMEOUT')
        if timeout:
            return int(timeout)
        return 120 * 1000

    def languages(self) -> dict[str, str]:
        return {
            'en': 'English',
            'hy': 'Armenian',
            'az': 'Azerbaijani',
            'eu': 'Basque',
            'be': 'Belarusian',
            'bg': 'Bulgarian',
            'ca': 'Catalan',
            'zh': 'Chinese (Simplified)',
            'zh-TW': 'Chinese (Traditional)',
            'cs': 'Czech',
            'da': 'Danish',
            'nl': 'Dutch',
            'fi': 'Finnish',
            'fr': 'French',
            'gl': 'Galego',
            'de': 'German',
            'el': 'Greek',
            'hu': 'Hungarian',
            'id': 'Indonesian',
            'it': 'Italian',
            'ja': 'Japanese',
            'ko': 'Korean',
            'lo': 'Lao',
            'lv': 'Latvian',
            'ms': 'Malay (Malaysia)',
            'no': 'Norwegian',
            'pl': 'Polish',
            'pt': 'Portuguese (Brazil)',
            'pt-PT': 'Portuguese (Portugal)',
            'ro': 'Romanian',
            'ru': 'Russian',
            'si': 'Sinhala (Sri Lanka)',
            'sk': 'Slovak',
            'sl': 'Slovenian',
            'es': 'Spanish',
            'sv': 'Swedish',
            'tr': 'Turkish',
            'uk': 'Ukrainian',
            'vi': 'Vietnamese',
            'aa-AA': 'Test Language'
        }
