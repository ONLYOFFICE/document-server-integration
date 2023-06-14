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

# app_directory
# |- storage_directory
#    |- user_directory
class StorageManager
  extend T::Sig

  sig { params(config: Configuration).void }
  def initialize(config)
    @config = T.let(config, Configuration)
  end

  # Creates a storage directory intended for storing each user's directories and
  # returns the path to that directory.
  sig { returns(Pathname) }
  def storage_directory
    # move to configuration
    # path = Pathname.new(@config.storage_path)
    # # @config.storage_path
    # directory = path ? @configuration.storage_path : Rails.root.join('public', @config.storage_path)
    FileUtils.mkdir(@config.storage_path) unless File.exist?(@config.storage_path)
    Pathname.new(@config.storage_path)
  end

  # Creates a user directory and returns the path to that directory.
  sig do
    params(
      storage_directory: Pathname,
      user_host: String
    )
      .returns(Pathname)
  end
  def user_directory(storage_directory, user_host)
    directory = Pathname(File.join(storage_directory, user_host))
    FileUtils.mkdir(directory) unless File.exist?(directory)
    directory
  end
end
