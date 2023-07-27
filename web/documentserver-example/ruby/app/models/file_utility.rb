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

require_relative '../format/format'

class FileUtility
  @format_manager = FormatManager.new

  class << self
    attr_reader :format_manager

    def get_file_type(file_name)
      ext = File.extname(file_name).downcase

      return 'word' if FileUtility.format_manager.document_extensinons.include?(ext)
      return 'cell' if FileUtility.format_manager.spreadsheet_extensinons.include?(ext)
      return 'slide' if FileUtility.format_manager.presentation_extensinons.include?(ext)

      'word'
    end
  end
end
