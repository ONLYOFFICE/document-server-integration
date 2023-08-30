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
require_relative 'proxy'

class ProxyManagerRefersTests < Test::Unit::TestCase
  class MockedConfigurationManager < ConfigurationManager
    def document_server_public_uri
      URI('http://localhost')
    end

    def document_server_private_uri
      URI('http://proxy')
    end
  end

  def test_resolves_a_uri_that_refers_to_the_public_uri
    config_manager = MockedConfigurationManager.new
    proxy_manager = ProxyManager.new(config_manager:)

    url = 'http://localhost/endpoint?query=string'
    uri = URI(url)
    resolved_uri = proxy_manager.resolve_uri(uri)

    assert_equal(resolved_uri.to_s, 'http://proxy/endpoint?query=string')
  end
end

class ProxyManagerDoesNotRefersTests < Test::Unit::TestCase
  class MockedConfigurationManager < ConfigurationManager
    def document_server_public_uri
      URI('http://localhost')
    end
  end

  def test_resolves_a_url_that_does_not_refers_to_the_public_url
    config_manager = MockedConfigurationManager.new
    proxy_manager = ProxyManager.new(config_manager:)

    url = 'http://proxy/endpoint?query=string'
    uri = URI(url)
    resolved_uri = proxy_manager.resolve_uri(uri)

    assert_equal(resolved_uri.to_s, 'http://proxy/endpoint?query=string')
  end
end
