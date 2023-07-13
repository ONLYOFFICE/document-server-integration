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

from typing import Optional

def boolean(string: Optional[str], default: bool = False) -> bool:
    '''
    Converts a string that represents a boolean value to its corresponding
    boolean value. It supports case-insensitive `true`, `t`, `yes`, `y`, and `1`
    for the positive value, and `false`, `f`, `no`, `n`, and `0` for the
    negative value. If the string doesn't match any of these values, returns the
    default value.
    '''
    if string is None:
        return default

    lower = string.lower()

    positive = lower in ["true", "t", "yes", "y", "1"]
    if positive:
        return True

    negative = lower in ["false", "f", "no", "n", "0"]
    if negative:
        return False

    return default
