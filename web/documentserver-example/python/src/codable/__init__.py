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

'''
The Codable module provides the ability to decode a string JSON into a class
instance and encode it back. It also provides the ability to remap JSON keys and
work with nested Codable instances.

```python
from dataclasses import dataclass
from src.codable import Codable, CodingKey

@dataclass
class Fruit(Codable):
    class CodingKeys(CodingKey):
        native_for_python: 'foreignForPython'

    native_for_python: str
```
'''

from .codable import Codable, CodingKey
