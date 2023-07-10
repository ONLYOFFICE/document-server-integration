"""

 (c) Copyright Ascensio System SIA 2023

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

"""

import os
from pathlib import Path
from src.configuration import ConfigurationManager
from src.history import HistoryManager, HistoryUser
from src.optional import optional
from src.storage import StorageManager
from src.utils import users

def getHistoryDir(storagePath):
    source_file = Path(storagePath)
    config_manager = ConfigurationManager()
    storage_manager = StorageManager(
        config_manager=config_manager,
        user_host=source_file.parent.name,
        source_basename=source_file.name
    )
    history_manager = HistoryManager(
        storage_manager=storage_manager
    )
    directory = history_manager.history_directory()
    return f'{directory}'

# get file version of the given history directory
def getFileVersion(histDir):
    if not os.path.exists(histDir): # if the history directory doesn't exist
        return 0 # file version is 0

    cnt = 1

    for f in os.listdir(histDir): # run through all the files in the history directory
        path = os.path.join(histDir, f)
        directory = Path(path)

        if not directory.is_dir():
            continue

        if not len(list(directory.iterdir())) > 0:
            continue

        cnt += 1

    return cnt

def createMeta(storagePath, req):
    source_file = Path(storagePath)
    config_manager = ConfigurationManager()
    storage_manager = StorageManager(
        config_manager=config_manager,
        user_host=source_file.parent.name,
        source_basename=source_file.name
    )
    history_manager = HistoryManager(
        storage_manager=storage_manager
    )
    raw_user = users.getUserFromReq(req)
    user = HistoryUser(
        id=raw_user.id,
        name=raw_user.name
    )
    history_manager.bootstrap_initial_item(user)

def createMetaData(filename, uid, uname, usAddr):
    config_manager = ConfigurationManager()
    storage_manager = StorageManager(
        config_manager=config_manager,
        user_host=usAddr,
        source_basename=filename
    )
    history_manager = HistoryManager(
        storage_manager=storage_manager
    )
    user = HistoryUser(
        id=uid,
        name=uname
    )
    history_manager.bootstrap_initial_item(user)

def getMeta(storagePath):
    source_file = Path(storagePath)
    config_manager = ConfigurationManager()
    storage_manager = StorageManager(
        config_manager=config_manager,
        user_host=source_file.parent.name,
        source_basename=source_file.name
    )
    history_manager = HistoryManager(
        storage_manager=storage_manager
    )

    changes = history_manager.changes(HistoryManager.minimal_version)
    if changes is None:
        return None

    first_changes = optional(lambda: changes.changes[0])
    if first_changes is None:
        return None

    return {
        'created': first_changes.created,
        'uid': first_changes.user.id,
        'uname': first_changes.user.name
    }
