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

from pathlib import Path
from msgspec.json import decode
from msgspec import Struct
from src.memoize import memoize

class Format(Struct):
    name: str
    type: str
    actions: list[str]
    convert: list[str]
    mime: list[str]

    def extension(self) -> str:
        return f'.{self.name}'

class FormatManager():
    def fillable_extensions(self) -> list[str]:
        formats = self.fillable()
        mapped = map(lambda format: format.extension(), formats)
        return list(mapped)

    def fillable(self) -> list[Format]:
        formats = self.all()
        filtered = filter(lambda format: 'fill' in format.actions, formats)
        return list(filtered)

    def viewable_extensions(self) -> list[str]:
        formats = self.viewable()
        mapped = map(lambda format: format.extension(), formats)
        return list(mapped)

    def viewable(self) -> list[Format]:
        formats = self.all()
        filtered = filter(lambda format: 'view' in format.actions, formats)
        return list(filtered)

    def editable_extensions(self) -> list[str]:
        formats = self.editable()
        mapped = map(lambda format: format.extension(), formats)
        return list(mapped)

    def editable(self) -> list[Format]:
        formats = self.all()
        filtered = filter(
            lambda format: (
                'edit' in format.actions or
                'lossy-edit' in format.actions
            ),
            formats
        )
        return list(filtered)

    def convertible_extensions(self) -> list[str]:
        formats = self.convertible()
        mapped = map(lambda format: format.extension(), formats)
        return list(mapped)

    def convertible(self) -> list[Format]:
        formats = self.all()
        filtered = filter(
            lambda format: (
                format.type == 'cell' and 'xlsx' in format.convert or
                format.type == 'slide' and 'pptx' in format.convert or
                format.type == 'word' and 'docx' in format.convert
            ),
            formats
        )
        return list(filtered)

    def spreadsheet_extensions(self) -> list[str]:
        formats = self.spreadsheets()
        mapped = map(lambda format: format.extension(), formats)
        return list(mapped)

    def spreadsheets(self) -> list[Format]:
        formats = self.all()
        filtered = filter(lambda format: format.type == 'cell', formats)
        return list(filtered)

    def presentation_extensions(self) -> list[str]:
        formats = self.presentations()
        mapped = map(lambda format: format.extension(), formats)
        return list(mapped)

    def presentations(self) -> list[Format]:
        formats = self.all()
        filtered = filter(lambda format: format.type == 'slide', formats)
        return list(filtered)

    def document_extensions(self) -> list[str]:
        formats = self.documents()
        mapped = map(lambda format: format.extension(), formats)
        return list(mapped)

    def documents(self) -> list[Format]:
        formats = self.all()
        filtered = filter(lambda format: format.type == 'word', formats)
        return list(filtered)

    def all_extensions(self) -> list[str]:
        formats = self.all()
        mapped = map(lambda format: format.extension(), formats)
        return list(mapped)

    @memoize
    def all(self) -> list[Format]:
        path = self.__file()
        with open(path, 'r', encoding='utf-8') as file:
            contents = file.read()
            return decode(contents, type=list[Format])

    def __file(self) -> Path:
        directory = self.__directory()
        return directory.joinpath('onlyoffice-docs-formats.json')

    def __directory(self) -> Path:
        current_file = Path(__file__)
        directory = current_file.joinpath(
            '..',
            '..',
            '..',
            'assets',
            'document-formats'
        )
        return directory.resolve()
