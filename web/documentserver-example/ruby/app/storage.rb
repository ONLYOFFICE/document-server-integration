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
      config: Configuration,
      user_manager: UserManager,
      source_basename: String
    )
      .void
  end
  def initialize(config, user_manager, source_basename)
    @config = config
    @user_manager = user_manager
    @source_basename = source_basename
  end

  sig { returns(Pathname) }
  def source_file
    user_directory.join(@source_basename)
  end

  sig { returns(Pathname) }
  def user_directory
    directory = storage_directory.join(@user_manager.host)
    FileUtils.mkdir(directory) unless directory.exist?
    directory
  end

  sig { returns(Pathname) }
  def storage_directory
    # move to configuration
    # path = Pathname.new(@config.storage_path)
    # # @config.storage_path
    # directory = path ? @configuration.storage_path : Rails.root.join('public', @config.storage_path)
    storage_path = Pathname(@config.storage_path)
    FileUtils.mkdir(storage_path) unless storage_path.exist?
    storage_path
  end
end
