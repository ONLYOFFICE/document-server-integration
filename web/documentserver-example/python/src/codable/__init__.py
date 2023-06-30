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
class Parent(Codable):
    class CondingKeys(CodingKey):
        native_for_python: 'foreignForPython'

    native_for_python: str
```

The algorithm for converting JSON objects to Codable instances is far from
efficient, but it's simple and compatible with new Python features.

Perhaps in the future, it would be worth replacing the local implementation with
an external dependency that offers the same functionality, such as the
relatively popular [dataclasses-json](https://github.com/lidatong/dataclasses-json).
Unfortunately, this library is currently not friendly with type annotations (see [issues](https://github.com/lidatong/dataclasses-json/issues?q=is%3Aissue+annotations))
and struggles with type inference (see [#227](https://github.com/lidatong/dataclasses-json/issues/227)).

On the other hand, developing the current implementation into a full-fledged
library may be more attractive to us.
'''

from .codable import Codable, CodingKey

# TODO: isolate Decoder and Encoder initialization.
# Give the user the ability to override the decode and encode methods in order
# to change the object_hook for a specific property. For instance, this can be
# used to override the default behavior for ParseResult (urlparse).

# TODO: make the CodingKey definition optional.
# If the class doesn't provide the CodingKey, we must also use the native
# property names as foreign.

# TODO: add common presets.
# When overriding a specific CodingKeys method, define a common preset for all
# foreign keys. For example, convert all of them from camel case to snake case.
