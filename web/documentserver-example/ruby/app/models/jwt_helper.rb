# frozen_string_literal: true

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

require 'jwt'
require_relative '../configuration/configuration'

# Helper class for JSON Web Token (JWT) operations, including encoding and decoding.
class JwtHelper
  @jwt_secret = ConfigurationManager.new.jwt_secret
  @token_use_for_request = ConfigurationManager.new.jwt_use_for_request
  @token_expires_in = ConfigurationManager.new.jwt_expires_in

  # check if a secret key to generate token exists or not
  def self.enabled?
    @jwt_secret.present?
  end

  # check if a secret key used for request
  def self.use_for_request
    @token_use_for_request
  end

  # encode a payload object into a token using a secret key
  def self.encode(payload)
    now = Time.now.to_i
    payload[:iat] = now
    payload[:exp] = now + (@token_expires_in * 60)
    JWT.encode(payload, @jwt_secret, 'HS256') # define the hashing algorithm and get token
  end

  # decode a token into a payload object using a secret key
  def self.decode(token)
    begin
      decoded = JWT.decode(token, @jwt_secret, true, { algorithm: 'HS256' })
    rescue StandardError
      return ''
    end
    # decoded = Array [ {"data"=>"test"}, # payload
    #                   {"alg"=>"HS256"} # header   ]
    decoded[0].to_json # get json payload
  end
end
