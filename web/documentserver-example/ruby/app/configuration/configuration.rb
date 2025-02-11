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

# frozen_string_literal: true
# typed: true

require 'active_model'
require 'pathname'
require 'sorbet-runtime'
require 'uri'

# ConfigurationManager manages configuration settings for the application.
class ConfigurationManager
  extend T::Sig

  sig { returns(String) }
  attr_reader :version

  sig { void }
  def initialize
    @version = '1.13.0'
  end

  sig { returns(T.nilable(URI::Generic)) }
  def example_uri
    url = ENV.fetch('EXAMPLE_URL', nil)
    return nil if url.nil?

    URI(url)
  end

  sig { returns(URI::Generic) }
  def document_server_public_uri
    url = ENV['DOCUMENT_SERVER_PUBLIC_URL'] || 'http://documentserver'
    URI(url)
  end

  sig { returns(URI::Generic) }
  def document_server_private_uri
    url = ENV.fetch('DOCUMENT_SERVER_PRIVATE_URL', nil)
    return URI(url) if url

    document_server_public_uri
  end

  sig { returns(URI::Generic) }
  def document_server_api_uri
    path =
      ENV['DOCUMENT_SERVER_API_PATH'] ||
      '/web-apps/apps/api/documents/api.js'
    URI.join(document_server_public_uri, path)
  end

  sig { returns(URI::Generic) }
  def document_server_preloader_uri
    path =
      ENV['DOCUMENT_SERVER_PRELOADER_PATH'] ||
      '/web-apps/apps/api/documents/cache-scripts.html'
    URI.join(document_server_public_uri, path)
  end

  sig { returns(URI::Generic) }
  def document_server_command_uri
    path =
      ENV['DOCUMENT_SERVER_COMMAND_PATH'] ||
      '/command'
    URI.join(document_server_private_uri, path)
  end

  sig { returns(URI::Generic) }
  def document_server_converter_uri
    path =
      ENV['DOCUMENT_SERVER_CONVERTER_PATH'] ||
      '/converter'
    URI.join(document_server_private_uri, path)
  end

  sig { returns(String) }
  def jwt_secret
    ENV['JWT_SECRET'] || ''
  end

  sig { returns(String) }
  def jwt_header
    ENV['JWT_HEADER'] || 'Authorization'
  end

  sig { returns(T::Boolean) }
  def jwt_use_for_request
    env = ENV.fetch('JWT_USE_FOR_REQUEST', nil)
    return ActiveModel::Type::Boolean.new.cast(env) if env

    true
  end

  sig { returns(T::Boolean) }
  def ssl_verify_peer_mode_enabled
    env = ENV.fetch('SSL_VERIFY_PEER_MODE_ENABLED', nil)
    return ActiveModel::Type::Boolean.new.cast(env) if env

    false
  end

  sig { returns(T::Boolean) }
  def enable_forgotten
    env = ENV.fetch('ENABLE_FORGOTTEN', nil)
    return ActiveModel::Type::Boolean.new.cast(env) if env

    true
  end

  sig { returns(Pathname) }
  def storage_path
    storage_path = ENV['STORAGE_PATH'] || 'storage'
    storage_directory = Pathname(storage_path)
    return storage_directory if storage_directory.absolute?

    current_directory = Pathname(File.expand_path(__dir__))
    directory = current_directory.join('..', '..', storage_directory)
    directory.cleanpath
  end

  sig { returns(Numeric) }
  def maximum_file_size
    env = ENV.fetch('MAXIMUM_FILE_SIZE', nil)
    return Integer(env, 10) if env

    5 * 1024 * 1024
  end

  sig { returns(Numeric) }
  def convertation_timeout
    120
  end

  sig { returns(T::Hash[String, String]) }
  def languages
    {
      en: 'English',
      'sq-AL': 'Albanian (Albania)',
      ar: 'Arabic',
      hy: 'Armenian',
      az: 'Azerbaijani',
      eu: 'Basque',
      be: 'Belarusian',
      bg: 'Bulgarian',
      ca: 'Catalan',
      zh: 'Chinese (Simplified)',
      'zh-TW': 'Chinese (Traditional)',
      cs: 'Czech',
      da: 'Danish',
      nl: 'Dutch',
      'en-GB': 'English (United Kingdom)',
      fi: 'Finnish',
      fr: 'French',
      gl: 'Galego',
      de: 'German',
      el: 'Greek',
      'he-IL': 'Hebrew (Israel)',
      hu: 'Hungarian',
      id: 'Indonesian',
      it: 'Italian',
      ja: 'Japanese',
      ko: 'Korean',
      lo: 'Lao',
      lv: 'Latvian',
      ms: 'Malay (Malaysia)',
      no: 'Norwegian',
      pl: 'Polish',
      pt: 'Portuguese (Brazil)',
      'pt-PT': 'Portuguese (Portugal)',
      ro: 'Romanian',
      ru: 'Russian',
      'sr-Cyrl-RS': 'Serbian (Cyrillic)',
      'sr-Latn-RS': 'Serbian (Latin)',
      si: 'Sinhala (Sri Lanka)',
      sk: 'Slovak',
      sl: 'Slovenian',
      es: 'Spanish',
      sv: 'Swedish',
      tr: 'Turkish',
      uk: 'Ukrainian',
      vi: 'Vietnamese',
      'aa-AA': 'Test Language'
    }
  end
end
