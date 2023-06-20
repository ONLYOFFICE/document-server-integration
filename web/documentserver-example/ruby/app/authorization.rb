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

class AuthorizationService
  extend T::Sig

  sig { params(config: ConfigurationManager).void }
  def initialize(config:)
    @config = config
  end

  sig { returns(T::Boolean) }
  def enabled
    !@config.jwt_secret.empty?
  end

  # TODO: rename to enable_for_document_server
  # sig { returns(T::Boolean) }
  # def use_for_request
  #   @configuration.jwt_use_for_request
  # end

  sig { params(payload: T.untyped).returns(String) }
  def encode(payload:)
    JWT.encode(
      payload,
      @config.jwt_secret,
      AuthorizationService.algorithm
    )
  end

  # TODO: make returns nullable.
  sig { params(payload: String).returns(T.untyped) }
  def decode(payload:)
    decoded = JWT.decode(
      payload,
      @config.jwt_secret,
      true,
      {
        algorithm: AuthorizationService.algorithm
      }
    )
    decoded[0].to_json
  rescue StandardError
    ''
  end

  def self.algorithm
    'HS256'
  end
end
