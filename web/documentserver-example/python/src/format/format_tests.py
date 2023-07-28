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
        form = Format.decode(self.json)
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
        mapped = map(lambda format: format.type == 'word', formats)
        self.assertTrue(all(mapped))

class FormatManagerPresentationsTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.presentations()
        mapped = map(lambda format: format.type == 'slide', formats)
        self.assertTrue(all(mapped))

class FormatManagerSpreadsheetsTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.spreadsheets()
        mapped = map(lambda format: format.type == 'cell', formats)
        self.assertTrue(all(mapped))

class FormatManagerConvertibleTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.convertible()
        mapped = map(
            lambda format: (
                format.type == 'cell' and 'xlsx' in format.actions or
                format.type == 'slide' and 'pptx' in format.actions or
                format.type == 'word' and 'docx' in format.actions
            ),
            formats
        )
        self.assertTrue(all(mapped))

class FormatManagerEditableTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.editable()
        mapped = map(
            lambda format: (
                'edit' in format.actions or
                'lossy-edit' in format.actions
            ),
            formats
        )
        self.assertTrue(all(mapped))

class FormatManagerViewableTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.viewable()
        mapped = map(lambda format: 'view' in format.actions, formats)
        self.assertTrue(all(mapped))

class FormatManagerFillableTests(TestCase):
    def test_loads(self):
        format_manager = FormatManager()
        formats = format_manager.fillable()
        mapped = map(lambda format: 'fill' in format.actions, formats)
        self.assertTrue(all(mapped))
