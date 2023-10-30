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

from unittest import TestCase
from .string import boolean

class BooleanDefaultTests(TestCase):
    def test_converts_to_the_default_value(self):
        value = boolean("unknown")
        self.assertFalse(value)

    def test_converts_to_the_negative_value_by_default(self):
        value = boolean("unknown", False)
        self.assertFalse(value)

    def test_converts_to_the_positive_value_by_default(self):
        value = boolean("unknown", True)
        self.assertTrue(value)

class BooleanOptionalTests(TestCase):
    def test_converts_to_the_default_value(self):
        value = boolean(None)
        self.assertFalse(value)

    def test_converts_to_the_negative_value_by_default(self):
        value = boolean(None, False)
        self.assertFalse(value)

    def test_converts_to_the_positive_value_by_default(self):
        value = boolean(None, True)
        self.assertTrue(value)

class BooleanNegativeTests(TestCase):
    def test_converts_a_negative_string_to_the_negative_value(self):
        for string in ["false", "f", "no", "n", "0"]:
            value = boolean(string)
            self.assertFalse(value)

class BooleanPositiveTests(TestCase):
    def test_converts_a_positive_string_to_the_positive_value(self):
        for string in ["true", "t", "yes", "y", "1"]:
            value = boolean(string)
            self.assertTrue(value)
