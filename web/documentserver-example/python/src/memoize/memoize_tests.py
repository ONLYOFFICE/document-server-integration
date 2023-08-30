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
from . import memoize

class MemoizeMock():
    counter: int = 1

    @memoize
    def method(self) -> int:
        return self.counter

class MemoizeTests(TestCase):
    def test(self):
        mock = MemoizeMock()
        self.assertEqual(mock.counter, 1)
        self.assertEqual(mock.method(), 1)
        mock.counter += 1
        self.assertEqual(mock.counter, 2)
        self.assertEqual(mock.method(), 1)
