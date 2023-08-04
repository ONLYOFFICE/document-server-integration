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

from __future__ import annotations
from copy import deepcopy
from enum import StrEnum
from json import JSONDecoder, JSONEncoder
from typing import Any, Optional, Self, Type, get_args, get_origin, get_type_hints

class Monkey():
    key: str

    def __init__(self, key: str = '_slugs'):
        self.key = key

    def patch(self, obj: dict[str, Any]) -> dict[str, Any]:
        def inner(slug: list[str], value: Any):
            if isinstance(value, dict):
                value[self.key] = slug

                for child_slug, child_value in value.items():
                    inner(slug + [child_slug], child_value)

                return

            if isinstance(value, list):
                for child_value in value:
                    inner(slug, child_value)

        copied = deepcopy(obj)
        inner([], copied)
        return copied

    def slugs(self, obj: dict[str, Any]) -> list[str]:
        return obj[self.key]

    def clean(self, obj: dict[str, Any]) -> dict[str, Any]:
        copied = deepcopy(obj)
        del copied[self.key]
        return copied

class CodingKey(StrEnum):
    @classmethod
    def keywords(cls, obj: dict[str, Any]) -> dict[str, Any]:
        words = {}

        for pair in list(cls):
            # Errors are false positives.
            native = pair.name # type: ignore
            foreign = pair.value # type: ignore
            value = obj.get(foreign)
            words[native] = value

        return words

class Codable():
    __decoder = JSONDecoder()
    __encoder = JSONEncoder()
    __monkey = Monkey()

    class CodingKeys(CodingKey):
        pass

    @classmethod
    def decode(cls, content: str) -> Self:
        decoded = cls.__decoder.decode(content)
        patched = cls.__monkey.patch(decoded)
        encoded = cls.__encoder.encode(patched)
        decoder = Decoder(
            monkey=cls.__monkey,
            cls=cls
        )
        return decoder.decode(encoded)

    def encode(self) -> str:
        cls = type(self)
        encoder = Encoder(
            decoder=self.__decoder,
            cls=cls
        )
        return encoder.encode(self)

class Decoder(JSONDecoder):
    monkey: Monkey
    cls: Type[Codable]

    def __init__(
        self,
        monkey: Monkey,
        cls: Type[Codable],
        **kwargs
    ):
        self.monkey = monkey
        self.cls = cls
        kwargs['object_hook'] = self.__object_hook
        super().__init__(**kwargs)

    def __object_hook(self, obj):
        cls = self.cls

        for foreign in self.monkey.slugs(obj):
            native = cls.CodingKeys(foreign).name

            if native is None:
                return self.monkey.clean(obj)

            types = get_type_hints(cls)
            cls = self.__find_codable(types[native])

            if cls is None:
                return self.monkey.clean(obj)

        cleaned = self.monkey.clean(obj)
        return self.__init_codable(cls, cleaned)

    def __find_codable(self, cls: Type) -> Optional[Type[Codable]]:
        if issubclass(cls, Codable):
            return cls

        if get_origin(cls) is list:
            item = get_args(cls)[0]
            return self.__find_codable(item)

        return None

    def __init_codable(self, cls: Type[Codable], obj: dict[str, Any]) -> Codable:
        keywords = cls.CodingKeys.keywords(obj)
        return cls(**keywords)

class Encoder(JSONEncoder):
    decoder: JSONDecoder
    cls: Type[Codable]

    def __init__(
        self,
        decoder: JSONDecoder,
        cls: Type[Codable],
        indent: int = 2,
        **kwargs
    ):
        self.decoder = decoder
        self.cls = cls
        kwargs['indent'] = indent
        super().__init__(**kwargs)

    def default(self, o):
        obj = {}

        for pair in list(self.cls.CodingKeys):
            native = pair.name
            foreign = pair.value

            if not hasattr(o, native):
                continue

            value = getattr(o, native)
            obj[foreign] = self.__prepare_value(value)

        return obj

    def __prepare_value(self, value: Any) -> Any:
        if isinstance(value, Codable):
            return self.__prepare_codable(value)

        if isinstance(value, list):
            return self.__prepare_list(value)

        return value

    def __prepare_codable(self, value: Codable) -> Any:
        content = value.encode()
        return self.decoder.decode(content)

    def __prepare_list(self, value: list[Any]) -> list[Any]:
        mapped = map(self.__prepare_value, value)
        return list(mapped)
