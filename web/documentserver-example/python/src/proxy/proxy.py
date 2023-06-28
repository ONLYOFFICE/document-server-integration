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

from dataclasses import dataclass
from urllib.parse import ParseResult, urlparse
from src.configuration import ConfigurationManager

@dataclass
class ProxyManager():
    config_manager: ConfigurationManager

    def resolve_document_server_url(self, url: str) -> ParseResult:
        parsed_url = urlparse(url)
        if not self.__refer_document_server_public_url(parsed_url):
            return parsed_url
        return self.__redirect_document_server_public_url(parsed_url)

    def __refer_document_server_public_url(self, url: ParseResult) -> bool:
        public_url = self.config_manager.document_server_public_url()
        return (
            url.scheme == public_url.scheme and
            url.hostname == public_url.hostname and
            url.port == public_url.port
        )

    def __redirect_document_server_public_url(self, url: ParseResult) -> ParseResult:
        private_url = self.config_manager.document_server_private_url()
        return ParseResult(
            scheme=private_url.scheme,
            netloc=f'{private_url.hostname}:{private_url.port}',
            path=url.path,
            params=url.params,
            query=url.query,
            fragment=url.fragment
        )
