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

from dataclasses import dataclass
from pathlib import Path
from src.configuration import ConfigurationManager

@dataclass
class StorageManager():
    config_manager: ConfigurationManager
    user_host: str
    source_basename: str

    def source_file(self) -> Path:
        directory = self.user_directory()
        return directory.joinpath(self.source_basename)

    def user_directory(self) -> Path:
        parent_directory = self.storage_directory()
        directory = parent_directory.joinpath(self.user_host)
        if not directory.exists():
            directory.mkdir()
        return directory

    def storage_directory(self) -> Path:
        directory = self.config_manager.storage_path()
        if not directory.exists():
            directory.mkdir()
        return directory
