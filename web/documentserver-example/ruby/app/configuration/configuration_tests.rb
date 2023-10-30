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

require 'test/unit'
require_relative 'configuration'

module Enviroment
  def initialize(name)
    @env = ENV.to_hash
    super(name)
  end

  def setup
    ENV.replace @env
  end
end

class ConfigurationManagerTests < Test::Unit::TestCase
  def test_corresponds_the_latest_version
    config_manager = ConfigurationManager.new
    assert_equal(config_manager.version, '1.6.0')
  end
end

class ConfigurationManagerExampleURITests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    uri = config_manager.example_uri
    assert_nil(uri)
  end

  def test_assigns_a_value_from_the_environment
    ENV['EXAMPLE_URL'] = 'http://localhost'
    config_manager = ConfigurationManager.new
    uri = config_manager.example_uri
    assert_equal(uri.to_s, 'http://localhost')
  end
end

class ConfigurationManagerDocumentServerPublicURITests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_public_uri
    assert_equal(uri.to_s, 'http://document-server')
  end

  def test_assigns_a_value_from_the_environment
    ENV['DOCUMENT_SERVER_PUBLIC_URL'] = 'http://localhost'
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_public_uri
    assert_equal(uri.to_s, 'http://localhost')
  end
end

class ConfigurationManagerDocumentServerPrivateURITests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_private_uri
    assert_equal(uri.to_s, 'http://document-server')
  end

  def test_assigns_a_value_from_the_environment
    ENV['DOCUMENT_SERVER_PRIVATE_URL'] = 'http://localhost'
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_private_uri
    assert_equal(uri.to_s, 'http://localhost')
  end
end

class ConfigurationManagerDocumentServerAPIURITests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_api_uri
    assert_equal(
      uri.to_s,
      'http://document-server/web-apps/apps/api/documents/api.js'
    )
  end

  def test_assigns_a_value_from_the_environment
    ENV['DOCUMENT_SERVER_API_PATH'] = '/api'
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_api_uri
    assert_equal(
      uri.to_s,
      'http://document-server/api'
    )
  end
end

class ConfigurationManagerDocumentServerPreloaderURITests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_preloader_uri
    assert_equal(
      uri.to_s,
      'http://document-server/web-apps/apps/api/documents/cache-scripts.html'
    )
  end

  def test_assigns_a_value_from_the_environment
    ENV['DOCUMENT_SERVER_PRELOADER_PATH'] = '/preloader'
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_preloader_uri
    assert_equal(
      uri.to_s,
      'http://document-server/preloader'
    )
  end
end

class ConfigurationManagerDocumentServerCommandURITests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_command_uri
    assert_equal(
      uri.to_s,
      'http://document-server/coauthoring/CommandService.ashx'
    )
  end

  def test_assigns_a_value_from_the_environment
    ENV['DOCUMENT_SERVER_COMMAND_PATH'] = '/command'
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_command_uri
    assert_equal(
      uri.to_s,
      'http://document-server/command'
    )
  end
end

class ConfigurationManagerDocumentServerConverterURITests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_converter_uri
    assert_equal(
      uri.to_s,
      'http://document-server/ConvertService.ashx'
    )
  end

  def test_assigns_a_value_from_the_environment
    ENV['DOCUMENT_SERVER_CONVERTER_PATH'] = '/converter'
    config_manager = ConfigurationManager.new
    uri = config_manager.document_server_converter_uri
    assert_equal(
      uri.to_s,
      'http://document-server/converter'
    )
  end
end

class ConfigurationManagerJWTSecretTests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    secret = config_manager.jwt_secret
    assert_equal(secret, '')
  end

  def test_assigns_a_value_from_the_environment
    ENV['JWT_SECRET'] = 'your-256-bit-secret'
    config_manager = ConfigurationManager.new
    secret = config_manager.jwt_secret
    assert_equal(secret, 'your-256-bit-secret')
  end
end

class ConfigurationManagerJWTHeaderTests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    header = config_manager.jwt_header
    assert_equal(header, 'Authorization')
  end

  def test_assigns_a_value_from_the_environment
    ENV['JWT_HEADER'] = 'Proxy-Authorization'
    config_manager = ConfigurationManager.new
    header = config_manager.jwt_header
    assert_equal(header, 'Proxy-Authorization')
  end
end

class ConfigurationManagerJWTUseForRequest < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    use = config_manager.jwt_use_for_request
    assert_true(use)
  end

  def test_assigns_a_value_from_the_environment
    ENV['JWT_USE_FOR_REQUEST'] = 'false'
    config_manager = ConfigurationManager.new
    use = config_manager.jwt_use_for_request
    assert_false(use)
  end
end

class ConfigurationManagerSSLTests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    enabled = config_manager.ssl_verify_peer_mode_enabled
    assert_false(enabled)
  end

  def test_assigns_a_value_from_the_environment
    ENV['SSL_VERIFY_PEER_MODE_ENABLED'] = 'true'
    config_manager = ConfigurationManager.new
    enabled = config_manager.ssl_verify_peer_mode_enabled
    assert_true(enabled)
  end
end

class ConfigurationManagerStoragePathTests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    path = config_manager.storage_path
    assert_true(path.absolute?)
    assert_equal(path.basename.to_s, 'storage')
  end

  def test_assigns_a_relative_path_from_the_environment
    ENV['STORAGE_PATH'] = 'directory'
    config_manager = ConfigurationManager.new
    path = config_manager.storage_path
    assert_true(path.absolute?)
    assert_equal(path.basename.to_s, 'directory')
  end

  def test_assigns_an_absolute_path_from_the_environment
    ENV['STORAGE_PATH'] = '/directory'
    config_manager = ConfigurationManager.new
    path = config_manager.storage_path
    assert_equal(path.to_s, '/directory')
  end
end

class ConfigurationManagerMaximumFileSizeTests < Test::Unit::TestCase
  include Enviroment

  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    size = config_manager.maximum_file_size
    assert_equal(size, 5_242_880)
  end

  def test_assigns_a_value_from_the_environment
    ENV['MAXIMUM_FILE_SIZE'] = '10'
    config_manager = ConfigurationManager.new
    size = config_manager.maximum_file_size
    assert_equal(size, 10)
  end
end

class ConfigurationManagerConversionTimeoutTests < Test::Unit::TestCase
  def test_assigns_a_default_value
    config_manager = ConfigurationManager.new
    timeout = config_manager.convertation_timeout
    assert_equal(timeout, 120)
  end
end
