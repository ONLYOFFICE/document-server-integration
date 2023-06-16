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

# rubocop:disable Metrics/AbcSize
# rubocop:disable Metrics/CyclomaticComplexity
# rubocop:disable Metrics/MethodLength

# Supported: String, Integer, URI::HTTP, URI:HTTPS, Array, Object
class Translator
  extend T::Sig

  sig { params(value: Object).returns(T.untyped) }
  def self.translate(value:)
    return value if
      value.instance_of?(String) ||
      value.instance_of?(Integer)

    return value.to_s if \
      value.instance_of?(URI::HTTP) || \
      value.instance_of?(URI::HTTPS)

    if value.instance_of?(Array)
      array = T.let(T.unsafe(value), T::Array[Object])
      return array.map do |item|
        Translator.translate(value: item)
      end
    end

    object = {}

    value.instance_variables.each do |name|
      translated_name = Translator.tanslate_name(name:)
      raw_value = T.let(value.instance_variable_get(name), Object)
      translated_value = Translator.translate(value: raw_value)
      object[translated_name] = translated_value
    end

    object
  end

  sig { params(name: Symbol).returns(String) }
  def self.tanslate_name(name:)
    # It is faster than Ruby on Rails implementations and is sufficient for an
    # object property name.
    name.to_s.split('_').map(&:capitalize).join.sub('@', '')
  end
end
