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

require 'pathname'
require 'sorbet-runtime'

class Format < T::Struct
  extend T::Sig

  const :name, String
  const :type, String
  const :actions, T::Array[String]
  const :convert, T::Array[String]
  const :mime, T::Array[String]

  sig { params(hash: T.untyped, strict: T.untyped).returns(Format) }
  def self.from_hash(hash, strict = nil)
    super(hash, strict)
  end

  sig { returns(String) }
  def extension
    ".#{name}"
  end
end

class FormatManager
  extend T::Sig

  sig { returns(T::Array[String]) }
  def fillable_extensions
    fillable.map(&:extension)
  end

  sig { returns(T::Array[Format]) }
  def fillable
    all.filter do |format|
      format.actions.include?('fill')
    end
  end

  sig { returns(T::Array[String]) }
  def viewable_extensions
    viewable.map(&:extension)
  end

  sig { returns(T::Array[Format]) }
  def viewable
    all.filter do |format|
      format.actions.include?('view')
    end
  end

  sig { returns(T::Array[String]) }
  def editable_extensions
    editable.map(&:extension)
  end

  sig { returns(T::Array[Format]) }
  def editable
    all.filter do |format|
      format.actions.include?('edit') ||
      format.actions.include?('lossy-edit')
    end
  end

  sig { returns(T::Array[String]) }
  def convertible_extensions
    convertible.map(&:extension)
  end

  sig { returns(T::Array[Format]) }
  def convertible
    all.filter do |format|
      format.type == 'cell' && format.convert.include?('xlsx') ||
      format.type == 'slide' && format.convert.include?('pptx') ||
      format.type == 'word' && format.convert.include?('docx')
    end
  end

  sig { returns(T::Array[String]) }
  def spreadsheet_extensinons
    spreadsheets.map(&:extension)
  end

  sig { returns(T::Array[Format]) }
  def spreadsheets
    all.filter do |format|
      format.type == 'cell'
    end
  end

  sig { returns(T::Array[String]) }
  def presentation_extensinons
    presentations.map(&:extension)
  end

  sig { returns(T::Array[Format]) }
  def presentations
    all.filter do |format|
      format.type == 'slide'
    end
  end

  sig { returns(T::Array[String]) }
  def document_extensinons
    documents.map(&:extension)
  end

  sig { returns(T::Array[Format]) }
  def documents
    all.filter do |format|
      format.type == 'word'
    end
  end

  sig { returns(T::Array[String]) }
  def all_extensions
    all.map(&:extension)
  end

  sig { returns(T::Array[Format]) }
  def all
    return @all if defined?(@all)
    content = file.read
    hash = JSON.parse(content)
    @all ||= hash.map do |item|
      Format.from_hash(item)
    end
  end

  private

  sig { returns(Pathname) }
  def file
    directory.join('onlyoffice-docs-formats.json')
  end

  sig { returns(Pathname) }
  def directory
    current_directory = Pathname(T.must(__dir__))
    directory = current_directory.join('..', '..', 'assets', 'document-formats')
    directory.cleanpath
  end
end
