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

require_relative 'models/configuration_manager'

class ProxyManager
  extend T::Sig

  sig do
    params(
      config: ConfigurationManager,
      request: ActionDispatch::Request,
      user_host: T.nilable(String)
    )
      .void
  end
  def initialize(config:, request:, user_host: nil)
    @config = config
    @request = request
    @user_host = user_host
  end

  sig { returns(URI::Generic) }
  def example_uri
    @config.example_uri || URI(@request.base_url)
  end

  sig { returns(String) }
  def user_host
    (@user_host || @request.remote_ip).gsub(/[^0-9\-.a-zA-Z_=]/, '_')
  end
end
