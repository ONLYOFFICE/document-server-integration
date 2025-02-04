# frozen_string_literal: true

#
# (c) Copyright Ascensio System SIA 2025
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

# Determination file type based on extensions, utilizing `@format_manager` for format management.
class FileUtility
  @format_manager = FormatManager.new

  class << self
    attr_reader :format_manager
  end

  def self.get_file_type(file_name)
    ext = File.extname(file_name).downcase

    return 'diagram' if FileUtility.format_manager.diagram_extensions.include?(ext)
    return 'pdf' if FileUtility.format_manager.pdf_extensions.include?(ext)
    return 'word' if FileUtility.format_manager.document_extensinons.include?(ext)
    return 'cell' if FileUtility.format_manager.spreadsheet_extensinons.include?(ext)
    return 'slide' if FileUtility.format_manager.presentation_extensinons.include?(ext)

    'word'
  end
end
