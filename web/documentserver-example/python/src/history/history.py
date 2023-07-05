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

# TODO: add types for kwards.
# https://github.com/python/mypy/issues/14697

# from http import HTTPStatus
# from re import sub
from __future__ import annotations
from dataclasses import dataclass
from functools import reduce
from pathlib import Path
from typing import Any, Optional
from urllib.parse import ParseResult, quote, urlencode, urljoin, urlparse
from uuid import uuid1
from django.http import HttpRequest, HttpResponse
from src.codable import Codable, CodingKey
from src.configuration import ConfigurationManager
from src.optional import optional
from src.request import RequestManager
from src.storage import StorageManager

@dataclass
class History(Codable):
    class CodingKeys(CodingKey):
        current_version = 'currentVersion'
        history = 'history'

    current_version: int
    history: list[HistoryItem]

@dataclass
class HistoryItem(Codable):
    class CodingKeys(CodingKey):
        changes = 'changes'
        created = 'created'
        key = 'key'
        server_version = 'serverVersion'
        user = 'user'
        version = 'version'

    changes: list[HistoryChangesItem]
    created: str
    key: str
    server_version: Optional[str]
    user: Optional[HistoryUser]
    version: int

@dataclass
class HistoryMetadata(Codable):
    class CodingKeys(CodingKey):
        created = 'created'
        uid = 'uid'
        uname = 'uname'

    created: str
    uid: str
    uname: str

@dataclass
class HistoryChanges(Codable):
    class CodingKeys(CodingKey):
        server_version = 'serverVersion'
        changes = 'changes'

    server_version: Optional[str]
    changes: list[HistoryChangesItem]

@dataclass
class HistoryChangesItem(Codable):
    class CodingKeys(CodingKey):
        created = 'created'
        user = 'user'

    created: str
    user: HistoryUser

@dataclass
class HistoryUser(Codable):
    class CodingKeys(CodingKey):
        id = 'id'
        name = 'name'

    id: str
    name: str

@dataclass
class HistoryData(Codable):
    class CodingKeys(CodingKey):
        changes_url = 'changesUrl'
        file_type = 'fileType'
        key = 'key'
        previous = 'previous'
        token = 'token'
        url = 'url'
        version = 'version'

    changes_url: Optional[str]
    file_type: Optional[str]
    key: str
    previous: Optional[HistoryData]
    token: Optional[str]
    url: Optional[str]
    version: int

class HistoryController():
    def history(self, request: HttpRequest, **kwargs: Any) -> HttpResponse:
        '''
        https://api.onlyoffice.com/editors/methods#refreshHistory

        ```http
        GET {{example_url}}/history/{{source_basename}}?user_host={{user_host}} HTTP/1.1
        ```
        '''
        config_manager = ConfigurationManager()
        request_manager = RequestManager(
            config_manager=config_manager,
            request=request
        )

        example_url = request_manager.example_url()
        source_basename: str = kwargs['source_basename']
        user_host = request_manager.user_host()

        storage_manager = StorageManager(
            config_manager=config_manager,
            user_host=user_host,
            source_basename=source_basename
        )
        history_manager = HistoryManager(
            storage_manager=storage_manager,
            example_url=example_url,
            user_host=user_host
        )

        history = history_manager.history()
        return HttpResponse(
            history.encode(),
            content_type='application/json'
        )

    def data(self, request: HttpRequest, **kwargs: Any) -> HttpResponse:
        '''
        https://api.onlyoffice.com/editors/methods#setHistoryData

        ```http
        GET {{example_url}}/history/{{source_basename}}/{{version}}/data?user_host={{user_host}} HTTP/1.1
        ```
        '''
        config_manager = ConfigurationManager()
        request_manager = RequestManager(
            config_manager=config_manager,
            request=request
        )

        example_url = request_manager.example_url()
        source_basename: str = kwargs['source_basename']
        version: int = kwargs['version']
        user_host = request_manager.user_host()

        storage_manager = StorageManager(
            config_manager=config_manager,
            user_host=user_host,
            source_basename=source_basename
        )
        history_manager = HistoryManager(
            storage_manager=storage_manager,
            example_url=example_url,
            user_host=user_host
        )

        history_data = history_manager.data(version)

        # tokenized_history_data

        return HttpResponse(
            history_data.encode(),
            content_type='application/json'
        )

@dataclass
class HistoryManager():
    storage_manager: StorageManager
    example_url: ParseResult
    user_host: str

    def history(self) -> History:
        history = History(current_version=self.latest_version(), history=[])

        for version in range(
            HistoryManager.minimal_version,
            history.current_version + 1
        ):
            if version == HistoryManager.minimal_version:
                item = self.initial_item()
            else:
                if version == history.current_version:
                    key = self.generate_key()
                    item = self.item(key, version)
                else:
                    item = self.item(None, version)

            # item = self.item(version)
            if item is None:
                continue

            history.history.append(item)

        return history

    def data(self, version: int) -> Optional[HistoryData]:
        if version == self.latest_version():
            key = self.generate_key()
        else:
            key = self.key(version)

        # key = self.key(version)
        if key is None:
            return None

        url = self.item_download_url(version)
        file = self.storage_manager.source_file()
        file_type = file.suffix.replace('.', '')

        if version == HistoryManager.minimal_version:
            return HistoryData(
                changes_url=None,
                file_type=file_type,
                key=key,
                previous=None,
                token=None,
                url=url.geturl(),
                version=version
            )

        previous_version = version - 1
        previous = self.data(previous_version)
        changes_url = self.changes_download_url(previous_version)

        return HistoryData(
            changes_url=changes_url.geturl(),
            file_type=file_type,
            key=key,
            previous=previous,
            token=None,
            url=url.geturl(),
            version=version
        )

    # Item

    def initial_item(self) -> HistoryItem:
        key = self.generate_key()

        metadata = self.metadata()
        if metadata is None:
            return None

        user = HistoryUser(
            id=metadata.uid,
            name=metadata.uname
        )

        return HistoryItem(
            changes=[],
            created=metadata.created,
            key=key,
            server_version=None,
            user=user,
            version=HistoryManager.minimal_version
        )

    def item(self, key: Optional[str], version: int) -> Optional[HistoryItem]:
        key = key or self.key(version)
        if key is None:
            return None

        changes = self.changes(version - 1)
        if changes is None:
            return None

        first_changes = optional(lambda: changes.changes[0])
        if first_changes is None:
            return None

        return HistoryItem(
            changes=changes.changes,
            created=first_changes.created,
            key=key,
            server_version=changes.server_version,
            user=first_changes.user,
            version=version
        )

    def item_download_url(self, version: int) -> ParseResult:
        file = self.item_file(version)
        return self.download_url(version, file.name)

    def item_file(self, version: int) -> Path:
        parent_directory = self.version_directory(version)
        directory = parent_directory.joinpath(f'{version}')
        source_file = self.storage_manager.source_file()
        return directory.joinpath(f'prev{source_file.suffix}')

    # Changes

    # def generate_changes(user:)
    # def append_changes(version:, changes:)
    # def write_changes(version:, changes:)

    def changes(self, version: int) -> Optional[HistoryChanges]:
        file = self.changes_file(version)
        if not file.exists:
            return None

        content = file.read_text('utf-8')
        return HistoryChanges.decode(content)

    def changes_file(self, version: int) -> Path:
        directory = self.history_directory()
        return directory.joinpath(f'{version}', 'changes.json')

    def changes_download_url(self, version: int) -> ParseResult:
        return self.download_url(f'{version}', 'diff.zip')

    # Key

    def generate_key(self) -> str:
        key = uuid1()
        return f'{key}'

    # def write_key(version, key)

    def key(self, version: int) -> Optional[str]:
        file = self.key_file(version)
        if not file.exists():
            return None

        content = file.read_text('utf-8')
        return content

    def key_file(self, version: int) -> Path:
        directory = self.version_directory(version)
        return directory.joinpath('key.txt')

    # Metadata

    def metadata(self) -> Optional[HistoryMetadata]:
        file = self.metadata_file()
        if not file.exists():
            return None

        content = file.read_text('utf-8')
        return HistoryMetadata.decode(content)

    def metadata_file(self) -> Path:
        directory = self.history_directory()
        return directory.joinpath('createdInfo.json')

    # Versions

    minimal_version = 1

    # TODO: make the minimal version equal 1.
    def latest_version(self) -> int:
        directory = self.history_directory()
        version = 0

        for file in directory.iterdir():
            if not file.is_dir():
                continue

            if not len(list(file.iterdir())) > 0:
                continue

            version += 1

        if version == 0:
            return version

        return version + HistoryManager.minimal_version

    # URL's

    def download_url(self, version: int, file_basename: str) -> ParseResult:
        base_url = self.example_url.geturl()
        source_file = self.storage_manager.source_file()
        source_file_basename = quote(source_file.name)
        url = reduce(urljoin, [
            f'{base_url}/',
            'history/',
            f'{source_file_basename}/',
            f'{version}/',
            'download/',
            file_basename
        ])
        query = urlencode({
            'user_host': self.user_host
        })
        return ParseResult(
            scheme=url.scheme,
            netloc=url.netloc,
            path=url.path,
            params=url.params,
            query=query,
            fragment=url.fragment
        )

    # Directories

    def version_directory(self, version: int) -> Path:
        parent_directory = self.history_directory()
        directory = parent_directory.joinpath(f'{version}')
        if not directory.exists():
            directory.mkdir()
        return directory

    def history_directory(self) -> Path:
        file = self.storage_manager.source_file()
        directory = file.parent.joinpath(f'{file.name}-hist')
        if not directory.exists():
            directory.mkdir()
        return directory
