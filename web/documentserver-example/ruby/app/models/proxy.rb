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

require 'uri'

# The Proxy class provides interfaces that may be useful if a document-server
# instance and an example instance communicate through a proxy server.
class Proxy
  extend T::Sig

  sig { params(configuration: ConfigurationManager).void }
  def initialize(configuration)
    @config = configuration
  end

  # Resolves with a private document-server URI if the specified URI based on a
  # public document-server URI.
  sig { params(url: String).returns(URI::Generic) }
  def resolve_document_server_uri(url)
    uri = URI(url)

    return uri unless equal_document_server_public_uri(uri)

    swap_docuemnt_server_public_uri(uri)
  end

  private

  sig { params(uri: URI::Generic).returns(T::Boolean) }
  def equal_document_server_public_uri(uri)
    uri.scheme == @config.document_server_public_uri.scheme && \
      uri.host == @config.document_server_public_uri.host && \
      uri.port == @config.document_server_public_uri.port
  end

  sig { params(uri: URI::Generic).returns(URI::Generic) }
  def swap_docuemnt_server_public_uri(uri)
    swaped_uri = uri
    swaped_uri.scheme = @config.document_server_private_uri.scheme
    swaped_uri.host = @config.document_server_private_uri.host
    swaped_uri.port = @config.document_server_private_uri.port
    swaped_uri
  end
end
