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
from unittest import TestCase
from msgspec.json import decode
from . import Format, FormatManager

class FormatTests(TestCase):
    json = \
        '''
        {
          "name": "djvu",
          "type": "word",
          "actions": ["view"],
          "convert": ["bmp", "gif", "jpg", "pdf", "pdfa", "png"],
          "mime": ["image/vnd.djvu"]
        }
        '''

    def test_generates_extension(self):
        form = decode(self.json, type=Format)
        self.assertEqual(form.extension(), '.djvu')

class FormatManagerAllTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.all()
        empty = len(formats) == 0
        self.assertFalse(empty)

class FormatManagerDocumentsTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.documents()
        empty = len(formats) == 0
        self.assertFalse(empty)

class FormatManagerPresentationsTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.presentations()
        empty = len(formats) == 0
        self.assertFalse(empty)

class FormatManagerSpreadsheetsTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.spreadsheets()
        empty = len(formats) == 0
        self.assertFalse(empty)

class FormatManagerConvertibleTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.convertible()
        empty = len(formats) == 0
        self.assertFalse(empty)

class FormatManagerEditableTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.editable()
        empty = len(formats) == 0
        self.assertFalse(empty)

class FormatManagerViewableTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.viewable()
        empty = len(formats) == 0
        self.assertFalse(empty)

class FormatManagerFillableTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.fillable()
        empty = len(formats) == 0
        self.assertFalse(empty)
