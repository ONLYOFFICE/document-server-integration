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

from urllib.parse import ParseResult
from src.configuration import ConfigurationManager

class ProxyManager():
    config_manager: ConfigurationManager

    def __init__(self, config_manager: ConfigurationManager):
        self.config_manager = config_manager

    def resolve_url(self, url: ParseResult) -> ParseResult:
        if not self.__refer_public_url(url):
            return url
        return self.__redirect_public_url(url)

    def __refer_public_url(self, url: ParseResult) -> bool:
        public_url = self.config_manager.document_server_public_url()
        return (
            url.scheme == public_url.scheme and
            url.hostname == public_url.hostname and
            url.port == public_url.port
        )

    def __redirect_public_url(self, url: ParseResult) -> ParseResult:
        private_url = self.config_manager.document_server_private_url()
        return ParseResult(
            scheme=private_url.scheme,
            netloc=private_url.netloc,
            path=url.path,
            params=url.params,
            query=url.query,
            fragment=url.fragment
        )
