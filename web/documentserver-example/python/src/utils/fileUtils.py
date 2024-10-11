"""

 (c) Copyright Ascensio System SIA 2024

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

from src.configuration import ConfigurationManager
from src.format import FormatManager

config_manager = ConfigurationManager()
format_manager = FormatManager()


# get file name from the document url
def getFileName(uri):
    ind = uri.rfind('/')
    return uri[ind+1:]


# get file name without extension from the document url
def getFileNameWithoutExt(uri):
    fn = getFileName(uri)
    ind = fn.rfind('.')
    return fn[:ind]


# get file extension from the document url
def getFileExt(uri):
    fn = getFileName(uri)
    ind = fn.rfind('.')
    return fn[ind:].lower()


# get file type
def getFileType(uri):
    ext = getFileExt(uri)
    if ext in format_manager.pdf_extensions():
        return 'pdf'
    if ext in format_manager.document_extensions():
        return 'word'
    if ext in format_manager.spreadsheet_extensions():
        return 'cell'
    if ext in format_manager.presentation_extensions():
        return 'slide'

    return 'word'  # default file type is word
