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

# rubocop:disable Metrics/AbcSize
# rubocop:disable Metrics/ClassLength
# rubocop:disable Metrics/CyclomaticComplexity
# rubocop:disable Metrics/MethodLength
# rubocop:disable Metrics/ParameterLists
# rubocop:disable Metrics/PerceivedComplexity
# rubocop:disable Style/Documentation
# rubocop:disable Style/KeywordParametersOrder
# rubocop:disable Metrics/BlockLength

# TODO: add nil for the optional parameters.
# https://github.com/sorbet/sorbet/issues/7091

# TODO: drop the
# DocumentHelper.init

# https://api.onlyoffice.com/editors/callback#history

require 'mimemagic'
require_relative 'models/configuration_manager'
require_relative 'models/document_helper'
require_relative 'authorization'
require_relative 'proxy'
require_relative 'response'
require_relative 'storage'

class PseudoLogger
  def self.log(string)
    File.write('/srv/logs', "#{string}\n", mode: 'a+')
  end
end

class History
  extend T::Sig

  sig { returns(Integer) }
  attr_reader :current_version

  sig { returns(T::Array[HistoryItem]) }
  attr_reader :history

  sig do
    params(
      current_version: Integer,
      history: T::Array[HistoryItem]
    )
      .void
  end
  def initialize(current_version:, history: [])
    @current_version = current_version
    @history = history
  end

  def to_json(*options)
    as_json.to_json(*options)
  end

  def as_json(_ = {})
    {
      currentVersion: @current_version,
      history: @history
    }
  end

  sig { params(json: T.untyped).returns(History) }
  def self.from_json(json)
    history = json['history'].map do |item|
      HistoryItem.from_json(item)
    end
    History.new(
      current_version: json['currentVersion'].to_i,
      history:
    )
  end
end

class HistoryMetadata
  extend T::Sig

  sig { returns(String) }
  attr_reader :created

  sig { returns(String) }
  attr_reader :uid

  sig { returns(String) }
  attr_reader :uname

  sig do
    params(
      created: String,
      uid: String,
      uname: String
    )
      .void
  end
  def initialize(created:, uid:, uname:)
    @created = created
    @uid = uid
    @uname = uname
  end

  def to_json(*options)
    as_json.to_json(*options)
  end

  def as_json(_ = {})
    {
      created: @created,
      uid: @uid,
      uname: @uname
    }
  end

  sig { params(json: T.untyped).returns(HistoryMetadata) }
  def self.from_json(json)
    HistoryMetadata.new(
      created: json['created'],
      uid: json['uid'],
      uname: json['uname']
    )
  end
end

class HistoryItem
  extend T::Sig

  sig { returns(T.nilable(HistoryChanges)) }
  attr_reader :changes

  sig { returns(String) }
  attr_reader :created

  sig { returns(String) }
  attr_reader :key

  sig { returns(T.nilable(String)) }
  attr_reader :server_version

  sig { returns(T.nilable(HistoryUser)) }
  attr_reader :user

  sig { returns(Integer) }
  attr_reader :version

  sig do
    params(
      changes: T.nilable(HistoryChanges),
      created: String,
      key: String,
      server_version: T.nilable(String),
      user: T.nilable(HistoryUser),
      version: Integer
    )
      .void
  end
  def initialize(
    changes:,
    created:,
    key:,
    server_version:,
    user:,
    version:
  )
    @changes = changes
    @created = created
    @key = key
    @server_version = server_version
    @user = user
    @version = version
  end

  def to_json(*options)
    as_json.to_json(*options)
  end

  def as_json(_ = {})
    {
      changes: @changes,
      created: @created,
      key: @key,
      serverVersion: @server_version,
      user: @user,
      version: @version
    }
  end

  sig { params(json: T.untyped).returns(HistoryItem) }
  def self.from_json(json)
    changes = json['changes']
    user = json['user']
    HistoryItem.new(
      changes: (HistoryChanges.from_json(changes) if changes),
      created: json['created'],
      key: json['key'],
      server_version: json['serverVersion'],
      user: (HistoryUser.from_json(user) if user),
      version: json['version'].to_i
    )
  end
end

class HistoryChanges
  extend T::Sig

  sig { returns(String) }
  attr_reader :server_version

  sig { returns(T::Array[HistoryChangesItem]) }
  attr_reader :changes

  sig do
    params(
      server_version: String,
      changes: T::Array[HistoryChangesItem]
    )
      .void
  end
  def initialize(server_version:, changes:)
    @server_version = server_version
    @changes = changes
  end

  def to_json(*options)
    as_json.to_json(*options)
  end

  def as_json(_ = {})
    {
      serverVersion: @server_version,
      changes: @changes
    }
  end

  sig { params(json: T.untyped).returns(HistoryChanges) }
  def self.from_json(json)
    changes = json['changes'].map do |item|
      HistoryChangesItem.from_json(item)
    end
    HistoryChanges.new(
      server_version: json['serverVersion'],
      changes:
    )
  end
end

class HistoryChangesItem
  extend T::Sig

  sig { returns(String) }
  attr_reader :created

  sig { returns(HistoryUser) }
  attr_reader :user

  sig { params(created: String, user: HistoryUser).void }
  def initialize(created:, user:)
    @created = created
    @user = user
  end

  def to_json(*options)
    as_json.to_json(*options)
  end

  def as_json(_ = {})
    {
      created: @created,
      user: @user
    }
  end

  sig { params(json: T.untyped).returns(HistoryChangesItem) }
  def self.from_json(json)
    HistoryChangesItem.new(
      created: json['created'],
      user: HistoryUser.from_json(json['user'])
    )
  end
end

class HistoryUser
  extend T::Sig

  sig { returns(String) }
  attr_reader :id

  sig { returns(String) }
  attr_reader :name

  sig { params(id: String, name: String).void }
  def initialize(id:, name:)
    @id = id
    @name = name
  end

  def to_json(*options)
    as_json.to_json(*options)
  end

  def as_json(_ = {})
    {
      id: @id,
      name: @name
    }
  end

  sig { params(json: T.untyped).returns(HistoryUser) }
  def self.from_json(json)
    HistoryUser.new(
      id: json['id'],
      name: json['name']
    )
  end
end

class HistoryData
  extend T::Sig

  sig { returns(T.nilable(URI::Generic)) }
  attr_reader :changes_uri

  sig { returns(T.nilable(String)) }
  attr_reader :file_type

  sig { returns(String) }
  attr_reader :key

  sig { returns(T.nilable(HistoryData)) }
  attr_reader :previous

  sig { returns(T.nilable(String)) }
  attr_reader :token

  sig { returns(URI::Generic) }
  attr_reader :uri

  sig { returns(Integer) }
  attr_reader :version

  sig do
    params(
      changes_uri: T.nilable(URI::Generic),
      file_type: T.nilable(String),
      key: String,
      previous: T.nilable(HistoryData),
      token: T.nilable(String),
      uri: URI::Generic,
      version: Integer
    )
      .void
  end
  def initialize(
    changes_uri:,
    file_type:,
    key:,
    previous:,
    token:,
    uri:,
    version:
  )
    @changes_uri = changes_uri
    @file_type = file_type
    @key = key
    @previous = previous
    @token = token
    @uri = uri
    @version = version
  end

  def to_json(*options)
    as_json.to_json(*options)
  end

  def as_json(_ = {})
    {
      changesUrl: @changes_uri&.to_s,
      fileType: @file_type,
      key: @key,
      previous: @previous,
      token: @token,
      url: @uri.to_s,
      version: @version
    }
  end
end

class HistoryController < ApplicationController
  extend T::Sig

  # https://api.onlyoffice.com/editors/methods#refreshHistory
  #
  # ```http
  # GET {{example_url}}/history/{{source_basename}}?user_host={{user_host}} HTTP/1.1
  # Authorization: Bearer {{token}}
  # ``
  sig { void }
  def history
    DocumentHelper.init(request.remote_ip, request.base_url)

    config = ConfigurationManager.new
    auth_manager = AuthorizationManager.new(config:)

    unless auth_manager.authorize(headers: request.headers)
      render(AuthorizationResponseError.forbidden)
      return
    end

    source_basename = params['source_basename']
    user_host = params['user_host']

    proxy_manager = ProxyManager.new(
      config:,
      base_url: request.base_url,
      remote_ip: request.remote_ip,
      user_host:
    )
    storage_manager = StorageManager.new(
      config:,
      proxy_manager:,
      source_basename:
    )
    history_manager = HistoryManager.new(
      proxy_manager:,
      storage_manager:
    )

    render(json: history_manager.history)
  end

  # https://api.onlyoffice.com/editors/methods#setHistoryData
  #
  # ```http
  # GET {{example_url}}/history/{{source_basename}}/{{version}}/data?user_host={{user_host}} HTTP/1.1
  # Authorization: Bearer {{token}}
  # ```
  sig { void }
  def history_data
    # TODO: directUrl.
    DocumentHelper.init(request.remote_ip, request.base_url)

    config = ConfigurationManager.new
    auth_manager = AuthorizationManager.new(config:)

    unless auth_manager.authorize(headers: request.headers)
      render(AuthorizationResponseError.forbidden)
      return
    end

    source_basename = params['source_basename']
    version = params['version'].to_i
    user_host = params['user_host']

    proxy_manager = ProxyManager.new(
      config:,
      base_url: request.base_url,
      remote_ip: request.remote_ip,
      user_host:
    )
    storage_manager = StorageManager.new(
      config:,
      proxy_manager:,
      source_basename:
    )
    history_manager = HistoryManager.new(
      proxy_manager:,
      storage_manager:
    )

    history_data = history_manager.history_data(version:)
    unless history_data
      render(HistoryResponseError.could_not_create_history_data)
      return
    end

    unless auth_manager.enabled
      render(json: history_data)
      return
    end

    token = auth_manager.encode(payload: history_data)
    tokenized_history_data = HistoryData.new(
      changes_uri: history_data.changes_uri,
      file_type: history_data.file_type,
      key: history_data.key,
      previous: history_data.previous,
      token:,
      uri: history_data.uri,
      version: history_data.version
    )

    render(json: tokenized_history_data)
  end

  # ? url that used in history_data
  #
  # ```http
  # GET {{example_url}}/history/{{source_basename}}/{{version}}/download/{{requested_basename}}?user_host={{user_host}} HTTP/1.1
  # Authorization: Bearer {{token}}
  # ```
  sig { void }
  def history_download
    config = ConfigurationManager.new
    auth_manager = AuthorizationManager.new(config:)

    unless auth_manager.document_server_authorize(headers: request.headers)
      render(AuthorizationResponseError.forbidden)
      return
    end

    source_basename = params['source_basename']
    version = params['version'].to_i
    requested_basename = params['requested_basename']
    user_host = params['user_host']

    proxy_manager = ProxyManager.new(
      config:,
      base_url: request.base_url,
      remote_ip: request.remote_ip,
      user_host:
    )
    storage_manager = StorageManager.new(
      config:,
      proxy_manager:,
      source_basename:
    )
    history_manager = HistoryManager.new(
      proxy_manager:,
      storage_manager:
    )

    directory = history_manager.version_directory(version:)
    requested_file = directory.join(requested_basename)
    unless requested_file.exist?
      render(HistoryResponseError.could_not_find_requested)
      return
    end

    requested_size = File.size(requested_file)
    requested_mime = MimeMagic.by_path(requested_file)

    response.headers['Content-Length'] = requested_size.to_s
    response.headers['Content-Type'] = requested_mime&.type
    response.headers['Content-Disposition'] = "attachment;filename*=UTF-8''#{requested_basename}"
    send_file(requested_file)
  end

  # Bumps the source file's current version and restores it to the specified
  # version, thus designating it as the new source file.
  #
  # ```http
  # GET {{example_url}}/history/{{source_basename}}/{{version}}/restore?user_host={{user_host}} HTTP/1.1
  # Authorization: Bearer {{token}}
  # ```
  sig { void }
  def history_restore
    DocumentHelper.init(request.remote_ip, request.base_url)

    config = ConfigurationManager.new
    auth_manager = AuthorizationManager.new(config:)

    unless auth_manager.authorize(headers: request.headers)
      render(AuthorizationResponseError.forbidden)
      return
    end

    source_basename = params['source_basename']
    version = params['version'].to_i
    user_host = params['user_host']

    proxy_manager = ProxyManager.new(
      config:,
      base_url: request.base_url,
      remote_ip: request.remote_ip,
      user_host:
    )
    storage_manager = StorageManager.new(
      config:,
      proxy_manager:,
      source_basename:
    )
    history_manager = HistoryManager.new(
      proxy_manager:,
      storage_manager:
    )

    # history_manager.restore(version:)

    puts
  end
end

# ```text
# /user_directory
# └─ history_directory
#    ├─ metadata_file
#    └─ version_directory
#       └─ key_file
# ```
#
# For example:
#
# ```text
# /172.19.0.1
# └─ document.docx-hist
#    ├─ createdInfo.json
#    └─ 1
#       ├─ changes.json
#       └─ key.txt
# ```
class HistoryManager
  extend T::Sig

  sig do
    params(
      proxy_manager: ProxyManager,
      storage_manager: StorageManager
    )
      .void
  end
  def initialize(proxy_manager:, storage_manager:)
    @proxy_manager = proxy_manager
    @storage_manager = storage_manager
  end

  sig { returns(History) }
  def history
    history = History.new(current_version: latest_version)

    (HistoryManager.minimal_version..history.current_version).each do |version|
      item =
        if version == HistoryManager.minimal_version
          initial_item
        else
          item(version:)
        end
      next unless item

      key =
        if version == history.current_version
          generate_key
        else
          key(version:)
        end

      keyed_item = HistoryItem.new(
        changes: item.changes,
        created: item.created,
        key: key || item.key,
        server_version: item.server_version,
        user: item.user,
        version: item.version
      )

      history.history.append(keyed_item)
    end

    history
  end

  sig { params(version: Integer).returns(T.nilable(HistoryData)) }
  def history_data(version:)
    key =
      if version == latest_version
        generate_key
      else
        key(version:)
      end
    return nil unless key

    uri = history_item_download_uri(version:)
    file_type = @storage_manager.source_file.extname.delete_prefix('.')

    if version == HistoryManager.minimal_version
      return HistoryData.new(
        changes_uri: nil,
        file_type:,
        key:,
        previous: nil,
        token: nil,
        uri:,
        version:
      )
    end

    previous = history_data(version: version - 1)
    changes_uri = history_changes_download_uri(version:)

    HistoryData.new(
      changes_uri:,
      file_type:,
      key:,
      previous:,
      token: nil,
      uri:,
      version:
    )
  end

  sig { params(version: Integer).returns(T::Boolean) }
  def restore(version:)
    recovery_file = item_file(version:)
    return false unless recovery_file.exist?

    bumped_version = latest_version + 1
    bumped_file = item_file(version: bumped_version)

    write_key(version: bumped_version)
    FileUtils.cp(@storage_manager.source_file, bumped_file)
    FileUtils.cp(recovery_file, @storage_manager.source_file)

    true
  end

  sig { params(version: Integer).returns(URI::Generic) }
  def history_item_download_uri(version:)
    history_download_uri(
      version:,
      requested_file_basename: item_file(version:).basename.to_s
    )
  end

  sig { params(version: Integer).returns(URI::Generic) }
  def history_changes_download_uri(version:)
    history_download_uri(
      version:,
      requested_file_basename: 'diff.zip'
    )
  end

  sig do
    params(
      version: Integer,
      requested_file_basename: String
    )
      .returns(URI::Generic)
  end
  def history_download_uri(version:, requested_file_basename:)
    uri = URI.join(
      "#{@proxy_manager.example_uri}/",
      'history/',
      "#{ERB::Util.url_encode(@storage_manager.source_file.basename)}/",
      "#{version}/",
      'download/',
      requested_file_basename
    )
    query = {}
    query['user_host'] = @proxy_manager.user_host
    uri.query = URI.encode_www_form(query)
    uri
  end

  sig { returns(T.nilable(HistoryItem)) }
  def initial_item
    metadata = metadata()
    return nil unless metadata

    user = HistoryUser.new(
      id: metadata.uid,
      name: metadata.uname
    )

    HistoryItem.new(
      changes: nil,
      created: metadata.created,
      key: '',
      server_version: nil,
      user:,
      version: HistoryManager.minimal_version
    )
  end

  sig { params(version: Integer).returns(T.nilable(HistoryItem)) }
  def item(version:)
    changes = changes(version: version - 1)
    return nil unless changes

    latest_changes = changes.changes[0]
    return nil unless latest_changes

    HistoryItem.new(
      changes:,
      created: latest_changes.created,
      key: '',
      server_version: changes.server_version,
      user: latest_changes.user,
      version:
    )
  end

  sig { params(version: Integer).returns(Pathname) }
  def item_file(version:)
    directory = version_directory(version:)
    directory.join("prev#{@storage_manager.source_file.extname}")
  end

  sig { params(version: Integer).returns(T.nilable(HistoryChanges)) }
  def changes(version:)
    file = changes_file(version:)
    return nil unless file.exist?

    content = file.read
    json = JSON.parse(content)
    HistoryChanges.from_json(json)
  end

  sig { params(version: Integer).returns(Pathname) }
  def changes_file(version:)
    history_directory.join(version.to_s, 'changes.json')
  end

  sig { params(version: Integer).returns(Pathname) }
  def write_key(version:)
    key = generate_key
    key_file = key_file(version:)
    FileUtils.mkdir(key_file.dirname) unless File.exist?(key_file.dirname)
    File.write(key_file, key)
    key_file
  end

  sig { returns(String) }
  def generate_key
    time = File.mtime(@storage_manager.source_file)
    ServiceConverter.generate_revision_id(
      "#{@proxy_manager.user_host}/#{@storage_manager.source_file.basename}.#{time}"
    )
  end

  sig { params(version: Integer).returns(T.nilable(String)) }
  def key(version:)
    file = key_file(version:)
    return nil unless file.exist?
    file.read
  end

  sig { params(version: Integer).returns(Pathname) }
  def key_file(version:)
    directory = version_directory(version:)
    directory.join('key.txt')
  end

  sig { params(version: Integer).returns(Pathname) }
  def version_directory(version:)
    directory = history_directory.join(version.to_s)
    FileUtils.mkdir(directory) unless directory.exist?
    directory
  end

  # TODO: not sure if this method is supposed to be.
  # If in the /track we store metadata in the history directory, we can merge
  # the initial_item and item methods into a single one.
  sig { returns(T.nilable(HistoryMetadata)) }
  def metadata
    file = metadata_file(history_directory:)
    return nil unless file.exist?

    content = file.read
    json = JSON.parse(content)
    HistoryMetadata.from_json(json)
  end

  sig { params(history_directory: Pathname).returns(Pathname) }
  def metadata_file(history_directory:)
    history_directory.join('createdInfo.json')
  end

  sig { returns(Pathname) }
  def history_directory
    directory = Pathname(File.join("#{@storage_manager.source_file}-hist"))
    FileUtils.mkdir(directory) unless directory.exist?
    directory
  end

  sig { returns(Integer) }
  def latest_version
    version = HistoryManager.minimal_version
    return version unless history_directory.exist?

    Dir.each_child(history_directory) do |child|
      child_path = history_directory.join(child)
      version += 1 if child_path.directory?
    end

    version
  end

  sig { returns(Integer) }
  def self.minimal_version
    1
  end
end

class HistoryResponseError < ResponseError
  sig { returns(HistoryResponseError) }
  def self.could_not_find_requested
    HistoryResponseError.new(
      status: :not_found,
      error: "The requested file with the specified basename doesn't exist."
    )
  end

  def self.could_not_create_history_data
    HistoryResponseError.new(
      status: :bad_request,
      error: 'could_not_create_history_data'
    )
  end
end
