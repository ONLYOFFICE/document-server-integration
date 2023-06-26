# pylint: disable=missing-module-docstring
# pylint: disable=missing-class-docstring
# pylint: disable=missing-function-docstring
from os import environ
from os.path import abspath, dirname
from pathlib import Path
from typing import Dict
from urllib.parse import ParseResult, urlparse, urljoin

class ConfigurationManager:
    version = '1.5.1'

    def example_url(self) -> (ParseResult | None):
        url = environ.get('EXAMPLE_URL')
        if not url:
            return None
        return urlparse(url)

    def document_server_url(self) -> ParseResult:
        url = (
            environ.get('DOCUMENT_SERVER_URL') or
            'http://document-server/'
        )
        return urlparse(url)

    def document_server_api_url(self) -> ParseResult:
        base = self.document_server_url().geturl()
        path = (
            environ.get('DOCUMENT_SERVER_API_PATH') or
            'web-apps/apps/api/documents/api.js'
        )
        url = urljoin(base, path)
        return urlparse(url)

    def document_server_preloader_url(self) -> ParseResult:
        base = self.document_server_url().geturl()
        path = (
            environ.get('DOCUMENT_SERVER_PRELOADER_PATH') or
            'web-apps/apps/api/documents/cache-scripts.html'
        )
        url = urljoin(base, path)
        return urlparse(url)

    def document_server_command_url(self) -> ParseResult:
        base = self.document_server_url().geturl()
        path = (
            environ.get('DOCUMENT_SERVER_COMMAND_PATH') or
            'coauthoring/CommandService.ashx'
        )
        url = urljoin(base, path)
        return urlparse(url)

    def document_server_converter_url(self) -> ParseResult:
        base = self.document_server_url().geturl()
        path = (
            environ.get('DOCUMENT_SERVER_CONVERTER_PATH') or
            'ConvertService.ashx'
        )
        url = urljoin(base, path)
        return urlparse(url)

    def jwt_secret(self) -> str:
        return environ.get('JWT_SECRET') or ''

    def jwt_header(self) -> str:
        return environ.get('JWT_HEADER') or 'Authorization'

    def jwt_use_for_request(self) -> bool:
        use = environ.get('JWT_USE_FOR_REQUEST')
        if use is None:
            return True
        if use == 'true':
            return True
        return False

    def ssl_verify_peer_mode_enabled(self) -> bool:
        enabled = environ.get('SSL_VERIFY_PEER_MODE_ENABLED')
        if enabled is None:
            return False
        if enabled == 'true':
            return True
        return False

    def storage_path(self) -> Path:
        storage_path = environ.get('STORAGE_PATH') or 'storage'
        storage_directory = Path(storage_path)
        if storage_directory.is_absolute():
            return storage_directory
        current_directory = Path(dirname(abspath(__file__)))
        return current_directory.joinpath('../..', storage_directory).resolve()

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

    def fillable_file_extensions(self) -> list[str]:
        return [
            '.docx',
            '.oform'
        ]

    def viewable_file_extensions(self) -> list[str]:
        return [
            '.djvu',
            '.oxps',
            '.pdf',
            '.xps'
        ]

    def editable_file_extensions(self) -> list[str]:
        return [
            '.csv',   '.docm', '.docx',
            '.docxf', '.dotm', '.dotx',
            '.epub',  '.fb2',  '.html',
            '.odp',   '.ods',  '.odt',
            '.otp',   '.ots',  '.ott',
            '.potm',  '.potx', '.ppsm',
            '.ppsx',  '.pptm', '.pptx',
            '.rtf',   '.txt',  '.xlsm',
            '.xlsx',  '.xltm', '.xltx'
        ]

    def convertible_file_extensions(self) -> list[str]:
        return [
            '.doc',  '.dot',  '.dps',   '.dpt',
            '.epub', '.et',   '.ett',   '.fb2',
            '.fodp', '.fods', '.fodt',  '.htm',
            '.html', '.mht',  '.mhtml', '.odp',
            '.ods',  '.odt',  '.otp',   '.ots',
            '.ott',  '.pot',  '.pps',   '.ppt',
            '.rtf',  '.stw',  '.sxc',   '.sxi',
            '.sxw',  '.wps',  '.wpt',   '.xls',
            '.xlsb', '.xlt',  '.xml'
        ]

    def spreadsheet_file_extensions(self) -> list[str]:
        return [
            '.xls',  '.xlsx',
            '.xlsm', '.xlsb',
            '.xlt',  '.xltx',
            '.xltm', '.ods',
            '.fods', '.ots',
            '.csv'
        ]

    def presentation_file_extensions(self) -> list[str]:
        return [
            '.pps',  '.ppsx',
            '.ppsm', '.ppt',
            '.pptx', '.pptm',
            '.pot',  '.potx',
            '.potm', '.odp',
            '.fodp', '.otp'
        ]

    def document_file_extensions(self) -> list[str]:
        return [
            '.doc',   '.docx', '.docm',
            '.dot',   '.dotx', '.dotm',
            '.odt',   '.fodt', '.ott',
            '.rtf',   '.txt',  '.html',
            '.htm',   '.mht',  '.xml',
            '.pdf',   '.djvu', '.fb2',
            '.epub',  '.xps',  '.oxps',
            '.oform'
        ]

    def languages(self) -> Dict[str, str]:
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
