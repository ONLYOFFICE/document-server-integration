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

  
  # the document extension list
  @@exts_document = %w(.doc .docx .docm .dot .dotx .dotm .odt .fodt .ott .rtf .txt .html .htm .mht .xml .pdf .djvu .fb2 .epub .xps)

  # the spreadsheet extension list
  @@exts_spreadsheet = %w(.xls .xlsx .xlsm .xlt .xltx .xltm .ods .fods .ots .csv)

  # the presentation extension list
  @@exts_presentation = %w(.pps .ppsx .ppsm .ppt .pptx .pptm .pot .potx .potm .odp .fodp .otp)

  class << self

    # get file type by its name
    def get_file_type(file_name)
        ext = File.extname(file_name)  # get file extension by its name

        if @@exts_document.include? ext  # word type for document extensions
          return 'word'
        end

        if @@exts_spreadsheet.include? ext  # cell type for spreadsheet extensions
          return 'cell'
        end

        if @@exts_presentation.include? ext  # slide type for presentation extensions
          return 'slide'
        end

        'word'  # the default file type is word
    end

  end

end