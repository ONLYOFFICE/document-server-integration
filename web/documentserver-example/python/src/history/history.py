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

# TODO: add types for kwargs.
# https://github.com/python/mypy/issues/14697

from __future__ import annotations
from dataclasses import dataclass
from datetime import datetime
from functools import reduce
from http import HTTPMethod
# from http import HTTPStatus
from json import loads
from pathlib import Path
from shutil import copy
from typing import Any, Optional
from uuid import uuid1
from urllib.parse import \
    ParseResult, \
    parse_qs, \
    quote, \
    urlencode, \
    urljoin, \
    urlparse
from django.http import FileResponse, HttpRequest, HttpResponse
# Pylance doesn't see the HttpResponseBase export from the django.http.
from django.http.response import HttpResponseBase
from src.codable import Codable, CodingKey
from src.configuration import ConfigurationManager
from src.http import http_method
from src.optional import optional
from src.request import RequestManager
from src.storage import StorageManager
from src.utils import jwtManager
from src.utils.users import find_user

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
        direct_url = 'directUrl'
        version = 'version'

    changes_url: Optional[str]
    file_type: Optional[str]
    key: str
    previous: Optional[HistoryData]
    token: Optional[str]
    url: Optional[str]
    direct_url: Optional[str]
    version: int

class HistoryController():
    @http_method(HTTPMethod.GET)
    def history(self, request: HttpRequest, **kwargs: Any) -> HttpResponseBase:
        '''
        https://api.onlyoffice.com/editors/methods#refreshHistory

        ```http
        GET {{base_url}}/history/{{source_basename}}?user_host={{user_host}} HTTP/1.1
        ```
        '''
        config_manager = ConfigurationManager()
        request_manager = RequestManager(
            request=request
        )

        source_basename: str = kwargs['source_basename']
        optional_user_host = request.GET.get('user_host')
        user_host = request_manager.resolve_user_host(optional_user_host)

        storage_manager = StorageManager(
            config_manager=config_manager,
            user_host=user_host,
            source_basename=source_basename
        )
        history_manager = HistoryManager(
            storage_manager=storage_manager
        )

        history = history_manager.history()
        return HttpResponse(
            history.encode(),
            content_type='application/json'
        )

    @http_method(HTTPMethod.GET)
    def data(self, request: HttpRequest, **kwargs: Any) -> HttpResponseBase:
        '''
        https://api.onlyoffice.com/editors/methods#setHistoryData

        ```http
        GET {{base_url}}/history/{{source_basename}}/{{version}}/data?user_host={{user_host}}&direct HTTP/1.1
        ```
        '''
        config_manager = ConfigurationManager()
        request_manager = RequestManager(
            request=request
        )

        direct = 'direct' in kwargs

        example_url: Optional[ParseResult] = None
        if direct:
            example_url = config_manager.example_url()

        base_url = request_manager.resolve_base_url(example_url)
        source_basename: str = kwargs['source_basename']
        version: int = kwargs['version']
        optional_user_host = request.GET.get('user_host')
        user_host = request_manager.resolve_user_host(optional_user_host)

        storage_manager = StorageManager(
            config_manager=config_manager,
            user_host=user_host,
            source_basename=source_basename
        )
        history_manager = HistoryManager(
            storage_manager=storage_manager
        )

        history_data = history_manager.data(
            base_url,
            version,
            user_host,
            direct
        )

        if jwtManager.isEnabled():
            history_data.token = jwtManager.encode(loads(history_data.encode()))

        return HttpResponse(
            history_data.encode(),
            content_type='application/json'
        )

    @http_method(HTTPMethod.GET)
    def download(self, request: HttpRequest, **kwargs: Any) -> HttpResponseBase:
        '''
        ```http
        GET {{base_url}}/history/{{source_basename}}/{{version}}/download/{{basename}}?user_host={{user_host}} HTTP/1.1
        ```
        '''
        config_manager = ConfigurationManager()
        request_manager = RequestManager(
            request=request
        )

        source_basename: str = kwargs['source_basename']
        version: int = kwargs['version']
        basename: str = kwargs['basename']
        optional_user_host = request.GET.get('user_host')
        user_host = request_manager.resolve_user_host(optional_user_host)

        storage_manager = StorageManager(
            config_manager=config_manager,
            user_host=user_host,
            source_basename=source_basename
        )
        history_manager = HistoryManager(
            storage_manager=storage_manager
        )

        version_directory = history_manager.version_directory(version)
        file = version_directory.joinpath(basename)

        # if not file.exists():
        #     return HttpResponse(
        #         '{ "error": "not exists" }',
        #         content_type='application/json'
        #     )

        return FileResponse(
            open(file, 'rb'),
            as_attachment=True
        )

    @http_method(HTTPMethod.PUT)
    def restore(self, request: HttpRequest, **kwargs: Any) -> HttpResponseBase:
        '''
        ```http
        PUT {{base_url}}/history/{{source_basename}}/{{version}}/restore?user_host={{user_host}}&user_id={{user_id}} HTTP/1.1
        ```
        '''
        config_manager = ConfigurationManager()
        request_manager = RequestManager(
            request=request
        )

        source_basename: str = kwargs['source_basename']
        version: int = kwargs['version']
        optional_user_host = request.GET.get('user_host')
        user_host = request_manager.resolve_user_host(optional_user_host)
        user_id = request.GET.get('user_id')

        storage_manager = StorageManager(
            config_manager=config_manager,
            user_host=user_host,
            source_basename=source_basename
        )
        history_manager = HistoryManager(
            storage_manager=storage_manager
        )

        raw_user = find_user(user_id)
        user = HistoryUser(
            id=raw_user.id,
            name=raw_user.name
        )

        history_manager.restore(version, user)

        return HttpResponse()

@dataclass
class HistoryManager():
    storage_manager: StorageManager

    # History Management

    def history(self) -> History:
        history = History(
            current_version=self.latest_version(),
            history=[]
        )

        for version in range(
            HistoryManager.minimal_version,
            history.current_version + 1
        ):
            item = self.item(version)
            if item is None:
                continue

            history.history.append(item)

        return history

    # Data Management

    def data(
        self,
        base_url: ParseResult,
        version: int,
        user_host: str,
        direct: bool
    ) -> Optional[HistoryData]:
        key = self.key(version)
        if key is None:
            return None

        previous_version = version - 1
        previous = self.data(
            base_url,
            previous_version,
            user_host,
            direct
        )

        history_url = self.history_url(base_url)
        version_url = self.version_url(history_url, version)

        changes_url: Optional[str] = None
        if previous is not None:
            file = self.diff_file(version)
            download_url = self.download_url(version_url, file.name)
            personal_url = self.personalize_url(download_url, user_host)
            changes_url = personal_url.geturl()

        file = self.item_file(version)
        file_type = file.suffix.replace('.', '')
        download_url = self.download_url(version_url, file.name)
        personal_url = self.personalize_url(download_url, user_host)
        url = personal_url.geturl()

        direct_url: Optional[str] = None
        if direct:
            direct_url = download_url.geturl()

        return HistoryData(
            changes_url=changes_url,
            file_type=file_type,
            key=key,
            previous=previous,
            token=None,
            url=url,
            direct_url=direct_url,
            version=version
        )

    def personalize_url(self, url: ParseResult, user_host: str) -> ParseResult:
        parsed_query = parse_qs(url.query)
        parsed_query.update({
            # False positive: the update supports dict.
            'user_host': user_host # type: ignore # noqa: E261
        })
        query = urlencode(parsed_query)
        return ParseResult(
            scheme=url.scheme,
            netloc=url.netloc,
            path=url.path,
            params=url.params,
            query=query,
            fragment=url.fragment
        )

    def download_url(self, base_url: ParseResult, basename: str) -> ParseResult:
        base = base_url.geturl()
        url = reduce(urljoin, [
            f'{base}/',
            'download/',
            basename
        ])
        return urlparse(f'{url}')

    def version_url(self, base_url: ParseResult, version: int) -> ParseResult:
        base = base_url.geturl()
        url = reduce(urljoin, [
            f'{base}/',
            f'{version}'
        ])
        return urlparse(f'{url}')

    def history_url(self, base_url: ParseResult) -> ParseResult:
        base = base_url.geturl()
        source_basename = quote(self.storage_manager.source_basename)
        url = reduce(urljoin, [
            f'{base}/',
            'history/',
            source_basename
        ])
        return urlparse(f'{url}')

    # Item Management

    def restore(self, version: int, user: HistoryUser):
        recovery_file = self.item_file(version)
        if not recovery_file:
            raise Exception()

        latest_version = self.latest_version()
        bumped_version = latest_version + 1

        bumped_key = HistoryManager.generate_key()
        self.write_key(bumped_version, bumped_key)

        bumped_changes = HistoryManager.generate_changes(user)
        self.write_changes(bumped_version, bumped_changes)

        bumped_file = self.item_file(bumped_version)
        copy(f'{recovery_file}', f'{bumped_file}')

        source_file = self.storage_manager.source_file()
        copy(f'{recovery_file}', f'{source_file}')

    def item(self, version: int) -> Optional[HistoryItem]:
        key = self.key(version)
        if key is None:
            return None

        changes = self.changes(version)
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

    def item_file(self, version: int) -> Path:
        directory = self.version_directory(version)
        source_file = self.storage_manager.source_file()
        return directory.joinpath(f'prev{source_file.suffix}')

    # Changes Management

    def write_changes(self, version: int, changes: HistoryChanges):
        content = changes.encode()
        file = self.changes_file(version)
        file.write_text(content, 'utf-8')

    def changes(self, version: int) -> Optional[HistoryChanges]:
        file = self.changes_file(version)
        if not file.exists():
            return None

        content = file.read_text('utf-8')
        return HistoryChanges.decode(content)

    def changes_file(self, version: int) -> Path:
        directory = self.version_directory(version)
        return directory.joinpath('changes.json')

    def diff_file(self, version: int) -> Path:
        directory = self.version_directory(version)
        return directory.joinpath('diff.zip')

    @classmethod
    def generate_changes(cls, user: HistoryUser) -> HistoryChanges:
        today = datetime.today()
        created = today.strftime('%Y-%m-%d %H:%M:%S')
        item = HistoryChangesItem(
            created=created,
            user=user
        )
        return HistoryChanges(
            server_version=None,
            changes=[
                item
            ]
        )

    # Key Management

    def write_key(self, version: int, key: str):
        file = self.key_file(version)
        file.write_text(key, 'utf-8')

    def key(self, version: int) -> Optional[str]:
        file = self.key_file(version)
        if not file.exists():
            return None

        content = file.read_text('utf-8')
        return content

    def key_file(self, version: int) -> Path:
        directory = self.version_directory(version)
        return directory.joinpath('key.txt')

    @classmethod
    def generate_key(cls) -> str:
        key = uuid1()
        return f'{key}'

    # Version Management

    def version_directory(self, version: int) -> Path:
        parent_directory = self.history_directory()
        directory = parent_directory.joinpath(f'{version}')
        if not directory.exists():
            directory.mkdir()
        return directory

    # Storage Management

    minimal_version = 1

    def latest_version(self) -> int:
        directory = self.history_directory()
        version = 0

        for file in directory.iterdir():
            if not file.is_dir():
                continue

            if not len(list(file.iterdir())) > 0:
                continue

            version += 1

        return version

    def history_directory(self) -> Path:
        file = self.storage_manager.source_file()
        directory = file.parent.joinpath(f'{file.name}-hist')
        if not directory.exists():
            directory.mkdir()
        return directory
