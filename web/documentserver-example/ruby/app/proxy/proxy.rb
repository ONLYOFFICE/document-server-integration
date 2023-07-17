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

require 'sorbet-runtime'
require 'uri'
require_relative '../configuration/configuration'

class ProxyManager
  extend T::Sig

  sig { params(config_manager: ConfigurationManager).void }
  def initialize(config_manager:)
    @config_manager = config_manager
  end

  sig { params(uri: URI::Generic).returns(URI::Generic) }
  def resolve_uri(uri)
    return uri unless refer_public_url(uri)
    redirect_public_url(uri)
  end

  private

  sig { params(uri: URI::Generic).returns(T::Boolean) }
  def refer_public_url(uri)
    public_uri = @config_manager.document_server_public_uri
    uri.scheme == public_uri.scheme &&
      uri.host == public_uri.host &&
      uri.port == public_uri.port
  end

  sig { params(uri: URI::Generic).returns(URI::Generic) }
  def redirect_public_url(uri)
    private_uri = @config_manager.document_server_private_uri
    redirected_uri = uri
    redirected_uri.scheme = private_uri.scheme
    redirected_uri.host = private_uri.host
    redirected_uri.port = private_uri.port
    redirected_uri
  end
end
