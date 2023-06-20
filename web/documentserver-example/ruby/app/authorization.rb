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

# rubocop:disable Metrics/MethodLength

require 'jwt'
require_relative 'models/configuration_manager'
require_relative 'response'

class AuthorizationManager
  extend T::Sig

  sig { params(config: ConfigurationManager).void }
  def initialize(config:)
    @config = config
  end

  sig { params(headers: ActionDispatch::Http::Headers).returns(T::Boolean) }
  def authorize(headers:)
    enabled ? verify(headers:) : true
  end

  sig { returns(T::Boolean) }
  def enabled
    !@config.jwt_secret.empty?
  end

  sig { params(headers: ActionDispatch::Http::Headers).returns(T::Boolean) }
  def document_server_authorize(headers:)
    document_server_enabled ? verify(headers:) : true
  end

  sig { returns(T::Boolean) }
  def document_server_enabled
    enabled && @config.jwt_use_for_request
  end

  sig { params(headers: ActionDispatch::Http::Headers).returns(T::Boolean) }
  def verify(headers:)
    header = headers[@config.jwt_header]
    return false unless header
    token = header.sub('Bearer ', '')
    decoded = decode(token)
    !decoded.nil?
  end

  sig { params(payload: T.untyped).returns(String) }
  def encode(payload:)
    JWT.encode(
      payload,
      @config.jwt_secret,
      AuthorizationManager.algorithm
    )
  end

  sig { params(payload: String).returns(T.untyped) }
  def decode(payload:)
    decoded = JWT.decode(
      payload,
      @config.jwt_secret,
      true,
      {
        algorithm: AuthorizationManager.algorithm
      }
    )
    decoded[0]
  rescue StandardError
    nil
  end

  def self.algorithm
    'HS256'
  end
end

class AuthorizationResponseError < ResponseError
  sig { returns(AuthorizationResponseError) }
  def self.forbidden
    AuthorizationResponseError.new(
      status: :forbidden,
      error: 'forbidden'
    )
  end
end
