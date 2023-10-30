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

# frozen_string_literal: true
# typed: true

require 'json'
require 'test/unit'
require_relative 'format'

class FormatTests < Test::Unit::TestCase
  def test_generates_extension
    content =
      '
      {
        "name": "djvu",
        "type": "word",
        "actions": ["view"],
        "convert": ["bmp", "gif", "jpg", "pdf", "pdfa", "png"],
        "mime": ["image/vnd.djvu"]
      }
      '
    hash = JSON.parse(content)
    format = Format.from_hash(hash)
    assert_equal('.djvu', format.extension)
  end
end

class FormatManagerAllTests < Test::Unit::TestCase
  def test_loads
    format_manager = FormatManager.new
    assert_false(format_manager.all.empty?)
  end
end

class FormatManagerDocumentsTests < Test::Unit::TestCase
  def test_loads
    format_manager = FormatManager.new
    assert_false(format_manager.documents.empty?)
  end
end

class FormatManagerPresentationsTests < Test::Unit::TestCase
  def test_loads
    format_manager = FormatManager.new
    assert_false(format_manager.presentations.empty?)
  end
end

class FormatManagerSpreadsheetsTests < Test::Unit::TestCase
  def test_loads
    format_manager = FormatManager.new
    assert_false(format_manager.spreadsheets.empty?)
  end
end

class FormatManagerConvertibleTests < Test::Unit::TestCase
  def test_loads
    format_manager = FormatManager.new
    assert_false(format_manager.all.empty?)
  end
end

class FormatManagerEditableTests < Test::Unit::TestCase
  def test_loads
    format_manager = FormatManager.new
    assert_false(format_manager.all.empty?)
  end
end

class FormatManagerViewableTests < Test::Unit::TestCase
  def test_loads
    format_manager = FormatManager.new
    assert_false(format_manager.all.empty?)
  end
end

class FormatManagerFilableTests < Test::Unit::TestCase
  def test_loads
    format_manager = FormatManager.new
    assert_false(format_manager.all.empty?)
  end
end
