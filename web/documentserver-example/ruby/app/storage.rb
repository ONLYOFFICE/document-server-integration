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

require 'pathname'
require_relative 'models/configuration_manager'
require_relative 'proxy'

# ```text
# /application_directory
# └─ storage_directory
#    └─ user_directory
#       └─ source_file
#
# /srv
# └─ storage
#    └─ 172.19.0.1
#       └─ document.docx
# ```
class StorageManager
  extend T::Sig

  sig do
    params(
      config: ConfigurationManager,
      proxy_manager: ProxyManager,
      source_basename: String
    )
      .void
  end
  def initialize(config:, proxy_manager:, source_basename:)
    @config = config
    @proxy_manager = proxy_manager
    @source_basename = source_basename
  end

  sig { returns(Pathname) }
  def source_file
    user_directory.join(@source_basename)
  end

  sig { returns(Pathname) }
  def user_directory
    directory = storage_directory.join(@proxy_manager.user_host)
    FileUtils.mkdir(directory) unless directory.exist?
    directory
  end

  sig { returns(Pathname) }
  def storage_directory
    # TODO: move to the Configuration.
    path = Pathname.new(@config.storage_path)
    storage_path = path.absolute? ? path : Rails.root.join('public', path)
    FileUtils.mkdir(storage_path) unless storage_path.exist?
    storage_path
  end
end
