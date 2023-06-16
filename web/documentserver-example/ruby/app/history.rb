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
# rubocop:disable Metrics/BlockLength
# rubocop:disable Metrics/ClassLength
# rubocop:disable Metrics/CyclomaticComplexity
# rubocop:disable Metrics/MethodLength
# rubocop:disable Metrics/ParameterLists
# rubocop:disable Metrics/PerceivedComplexity
# rubocop:disable Style/KeywordParametersOrder

# https://api.onlyoffice.com/editors/callback#history

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
end

class HistoryItem
  extend T::Sig

  sig { returns(History) }
  attr_accessor :changes

  sig { returns(String) }
  attr_accessor :created

  sig { returns(String) }
  attr_accessor :server_version

  sig { returns(String) }
  attr_accessor :key

  sig { returns(HistoryUser) }
  attr_accessor :user

  sig { returns(Integer) }
  attr_accessor :version
end

# TODO: merge with the User class because it's the same things.
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
end

class HistoryData
  extend T::Sig

  sig { returns(T.nilable(URI::Generic)) }
  # changesUrl url! not uri for the docuemnt-sever
  attr_reader :changes_url

  sig { returns(T.nilable(String)) }
  attr_reader :file_type

  sig { returns(String) }
  attr_reader :key

  sig { returns(T.nilable(HistoryData)) }
  attr_reader :previous

  sig { returns(T.nilable(String)) }
  attr_reader :token

  sig { returns(URI::Generic) }
  # url! not uri for the docuemnt-sever
  attr_reader :uri

  sig { returns(Integer) }
  attr_reader :version

  # WARNING: don't change the order of parameters.
  # https://github.com/sorbet/sorbet/issues/7091
  sig do
    params(
      changes_url: T.nilable(URI::Generic),
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
    changes_url: nil,
    file_type: nil,
    key:,
    previous: nil,
    token: nil,
    uri:,
    version:
  )
    @changes_url = changes_url
    @file_type = file_type
    @key = key
    @previous = previous
    @token = token
    @uri = uri
    @version = version
  end
end

class HistoryController < ApplicationController
  extend T::Sig

  # https://api.onlyoffice.com/editors/methods#refreshHistory
  #
  # ```http
  # GET {{example_url}}/history/{{file_basename}}?user_host={{user_host}} HTTP/1.1
  # ?? Authorization: Bearer {{token}}
  # ``
  sig { void }
  def history
    # TODO: remove it.
    DocumentHelper.init(request.remote_ip, request.base_url)

    config = Configuration.new
    proxy_manager = ProxyManager.new(
      config:,
      request:,
      user_host: params['user_host']
    )

    storage_manager = StorageManager.new(
      config:,
      proxy_manager:,
      source_basename: params[:file_basename]
    )
    unless storage_manager.source_file.exist?
      render(
        status: :not_found,
        json: HistoryResponseError.could_not_find_file
      )
      return
    end

    history_manager = HistoryManager.new(storage_manager:)
    history = history_manager.history
    render(
      status: :ok,
      json: Translator.translate(value: history)
    )
  end

  # https://api.onlyoffice.com/editors/methods#setHistoryData
  #
  # ```http
  # GET {{example_url}}/history/{{file_basename}}/{{version}}/data?user_host={{user_host}} HTTP/1.1
  # ?? Authorization: Bearer {{token}}
  # ```
  sig { void }
  def history_data
    # TODO: directUrl.
    # TODO: remove it.
    DocumentHelper.init(request.remote_ip, request.base_url)

    config = Configuration.new
    proxy_manager = ProxyManager.new(
      config:,
      request:,
      user_host: params['user_host']
    )

    storage_manager = StorageManager.new(
      config:,
      proxy_manager:,
      source_basename: params[:file_basename]
    )
    unless storage_manager.source_file.exist?
      render(
        status: :not_found,
        json: HistoryResponseError.could_not_find_file
      )
      return
    end

    history_manager = HistoryManager.new(proxy_manager:, storage_manager:)
    history_data = history_manager.history_data(version: params[:version])
    unless history_data
      render(
        status: :not_found,
        json: nil
      )
      return
    end

    auth = AuthorizationService.new(config:)
    unless auth.enabled
      render(
        status: :ok,
        json: Translator.translate(value: history_data)
      )
      return
    end

    token = auth.encode(payload: history_data)
    tokenized_history_data = HistoryData.new(
      changes_url: history_data.changes_url,
      file_type: history_data.file_type,
      key: history_data.key,
      previous: history_data.previous,
      token:,
      uri: history_data.uri,
      version: history_data.version
    )
    render(
      status: :ok,
      json: Translator.translate(value: tokenized_history_data)
    )
  end

  # ? url that used in history_data
  #
  # ```http
  # GET /history/{{file_basename}}/{{version}}/download/{{requested_file_basename}}?user_host={{user_host}} HTTP/1.1
  # ?? Authorization: Bearer {{token}}
  # ```
  # def history_download
  # end

  # ```http
  # GET /history/{{file_basename}}/{{version}}/restore?user_host={{user_host}} HTTP/1.1
  # ?? Authorization: Bearer {{token}}
  # ```
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

  # WARNING: don't change the order of parameters.
  # https://github.com/sorbet/sorbet/issues/7091
  sig do
    params(
      proxy_manager: T.nilable(ProxyManager),
      storage_manager: StorageManager
    )
      .void
  end
  def initialize(proxy_manager: nil, storage_manager:)
    @proxy_manager = proxy_manager
    @storage_manager = storage_manager
  end

  sig { returns(History) }
  def history
    history = History.new(current_version: latest_version)

    (HistoryManager.minimal_version..history.current_version).each do |version|
      if version == HistoryManager.minimal_version
        item = initial_item
        next unless item

        history.history.append(item)
        next
      end

      previous_item = item(version: version - 1)
      next unless previous_item

      item = previous_item.changes.history[0]
      next unless item

      history.history.append(item)
    end

    history
  end

  sig { params(version: Integer).returns(T.nilable(HistoryData)) }
  def history_data(version:)
    key = key(version:)
    return nil unless key

    uri = history_source_download_uri(version:)
    return nil unless uri

    file_type = @storage_manager.source_file.extname.delete_prefix('.')

    if version == HistoryManager.minimal_version
      return HistoryData.new(
        file_type:,
        key:,
        uri:,
        version:
      )
    end

    previous = history_data(version: version - 1)
    changes_url = history_changes_download_uri(version:)

    HistoryData.new(
      changes_url:,
      file_type:,
      key:,
      previous:,
      uri:,
      version:
    )
  end

  sig { params(version: Integer).returns(T.nilable(URI::Generic)) }
  def history_source_download_uri(version:)
    history_download_uri(
      version:,
      requested_file_basename: "prev#{@storage_manager.source_file.extname}"
    )
  end

  sig { params(version: Integer).returns(T.nilable(URI::Generic)) }
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
      .returns(T.nilable(URI::Generic))
  end
  def history_download_uri(version:, requested_file_basename:)
    return nil unless @proxy_manager
    uri = URI.join(
      @proxy_manager.example_uri.to_s,
      'history',
      ERB::Util.url_encode(@storage_manager.source_file.basename),
      version.to_s,
      'download',
      requested_file_basename
    )
    query = {}
    query['user_host'] = @proxy_manager.user_host
    uri.query = URI.encode_www_form(query)
    uri
  end

  sig { returns(T.nilable(HistoryItem)) }
  def initial_item
    key = key(version: HistoryManager.minimal_version)
    return nil unless key

    metadata = metadata()
    return nil unless metadata

    user = HistoryUser.new(
      id: metadata.uid,
      name: metadata.uname
    )

    item = HistoryItem.new
    item.version = HistoryManager.minimal_version
    item.key = key
    item.created = metadata.created
    item.user = user
    item
  end

  sig { params(version: Integer).returns(T.nilable(HistoryItem)) }
  def item(version:)
    file = item_file(version:)
    return nil unless file.exist?

    # TODO: in the /track ednpoint we should save on the disk full of the object
    # (History), not only a history property['history'].
    content = file.read
    json = JSON.parse(content)
    history = History.new(
      current_version: -1,
      history: json
    )
    item = HistoryItem.new
    item.changes = history
    item
  end

  sig { params(version: Integer).returns(Pathname) }
  def item_file(version:)
    history_directory.join(version.to_s, 'changes.json')
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

  sig { returns(T.nilable(HistoryMetadata)) }
  def metadata
    file = metadata_file(history_directory:)
    return nil unless file.exist?

    content = file.read
    json = JSON.parse(content)
    HistoryMetadata.new(
      created: json['created'],
      uid: json['uid'],
      uname: json['uname']
    )
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

class HistoryResponseError
  extend T::Sig

  sig { params(error: Integer, message: T.nilable(String)).void }
  def initialize(error:, message: nil)
    @error = error
    @message = message
    @success = false
  end

  sig { returns(HistoryResponseError) }
  def self.could_not_find_file
    HistoryResponseError.new(
      error: 1,
      message: "The file with the specified basename doesn't exist."
    )
  end
end

# DOWNLOAD

# # ```http
# # GET /history/{{file_basename}}/{{version}}?user_host={{user_host}} HTTP/1.1
# # Authorization: Bearer {{token}}
# # ```
# def history_of_version
#   if JwtHelper.is_enabled && JwtHelper.use_for_request
#     header_name = Rails.configuration.header.empty? ? 'Authorization' : Rails.configuration.header
#     header = request.headers[header_name]
#     unless header
#       response.status = :forbidden
#       render json: {
#         error: 'forbidden',
#         success: false
#       }
#       return
#     end

#     token = header.sub('Bearer ', '')
#     decoded = JwtHelper.decode(token)
#     unless decoded && !decoded.eql?('')
#       response.status = :forbidden
#       render json: {
#         error: 'forbidden',
#         success: false
#       }
#       return
#     end
#   end

#   source_basename = T.let(params[:file_basename], String)
#   target_version = T.let(params[:version], String).to_i
#   unless target_version != 0
#     response.status = :bad_request
#     # if params[:version] == '0', version can't be null
#     return
#   end

#   DocumentHelper.init(request.remote_ip, request.base_url)

#   user_host = DocumentHelper.cur_user_host_address(params[:user_host])
#   source_file = DocumentHelper.storage_path(source_basename, user_host)
#   unless File.exist?(source_file)
#     response.status = :not_found
#     render json: {
#       error: "The file with the specified fileName doesn't exist.",
#       success: false
#     }
#     return
#   end

#   history_directory = DocumentHelper.history_dir(source_file)
#   target_directory = File.join(history_directory, target_version.to_s)
#   target_history_file = File.join(target_directory, 'changes.json')
#   unless File.exist?(target_history_file)
#     response.status = :not_found
#     render json: {
#       error: "The file with the specified version doesn't exist.",
#       success: false
#     }
#     return
#   end

#   target_history_mimetype = MimeMagic.by_path(target_history_file)

#   response.headers['Content-Length'] = File.size(target_history_file).to_s
#   response.headers['Content-Type'] = target_history_mimetype.eql?(nil) ? nil : target_history_mimetype.type
#   response.headers['Content-Disposition'] = "attachment;filename*=UTF-8''#{ERB::Util.url_encode('changes.json')}"
#   response.status = :ok
#   send_file target_history_file
# end

# RESTORE

# # Bumps the source file's current version and restores it to the specified
# # version, thus designating it as the new source file.
# #
# # ```http
# # PUT /restore HTTP/1.1
# # Content-Type: application/json
# #
# # {
# #   "fileName": "the source file name with extension"
# #   "version": "the file version that needs to be restored"
# # }
# # ```
# def restore
#   body = JSON.parse(request.body.read)

#   source_basename = body['fileName']
#   target_version = body['version']
#   unless source_basename && target_version
#     response.status = :bad_request
#     render json: {
#       error: 'The fileName or version parameters were not specified.',
#       success: false
#     }
#     return
#   end

#   DocumentHelper.init(request.remote_ip, request.base_url)
#   user_address = DocumentHelper.cur_user_host_address(nil)
#   source_file = DocumentHelper.storage_path(source_basename, user_address)
#   unless File.exist?(source_file)
#     response.status = :not_found
#     render json: {
#       error: "The file with the specified fileName doesn't exist.",
#       success: false
#     }
#     return
#   end

#   previous_name = 'prev'
#   previous_extension = File.extname(source_basename)
#   previous_basename = "#{previous_name}#{previous_extension}"
#   history_directory = DocumentHelper.history_dir(source_file)

#   target_directory = File.join(history_directory, target_version.to_s)
#   target_file = File.join(target_directory, previous_basename)
#   unless File.exist?(target_file)
#     response.status = :not_found
#     render json: {
#       error: "The file with the specified version doesn't exist.",
#       success: false
#     }
#     return
#   end

#   latest_version = DocumentHelper.get_file_version(history_directory)
#   bumped_version = latest_version + 1
#   bumped_directory = File.join(history_directory, bumped_version.to_s)
#   bumped_file = File.join(bumped_directory, previous_basename)
#   FileUtils.mkdir(bumped_directory) unless File.exist?(bumped_directory)

#   bumped_key = ServiceConverter.generate_revision_id(
#     "#{File.join(user_address, source_basename)}.#{File.mtime(source_file)}"
#   )
#   bumped_key_file = File.join(bumped_directory, 'key.txt')

#   FileUtils.cp(source_file, bumped_file)
#   FileUtils.cp(target_file, source_file)
#   File.write(bumped_key_file, bumped_key)

#   response_data = {
#     error: nil,
#     success: true,
#     # TODO:
#     user_address:,
#     target_version:,
#     file: 'changes.json',
#     bumped_version:
#   }

#   if JwtHelper.is_enabled
#     jwt_header = Rails.configuration.header.empty? ? 'Authorization' : Rails.configuration.header
#     jwt_token = JwtHelper.encode(response_data)
#     response.headers[jwt_header] = "Bearer #{jwt_token}"
#   end

#   response.status = :ok
#   render json: response_data
# end
