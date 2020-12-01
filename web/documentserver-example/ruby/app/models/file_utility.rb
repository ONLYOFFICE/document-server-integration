#
# (c) Copyright Ascensio System SIA 2020
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

class FileUtility

  @@exts_document = %w(.doc .docx .docm .dot .dotx .dotm .odt .fodt .ott .rtf .txt .html .htm .mht .pdf .djvu .fb2 .epub .xps)

  @@exts_spreadsheet = %w(.xls .xlsx .xlsm .xlt .xltx .xltm .ods .fods .ots .csv)

  @@exts_presentation = %w(.pps .ppsx .ppsm .ppt .pptx .pptm .pot .potx .potm .odp .fodp .otp)

  class << self

    def get_file_type(file_name)
        ext = File.extname(file_name)

        if @@exts_document.include? ext
          return 'text'
        end

        if @@exts_spreadsheet.include? ext
          return 'spreadsheet'
        end

        if @@exts_presentation.include? ext
          return 'presentation'
        end

        'text'
    end

  end

end