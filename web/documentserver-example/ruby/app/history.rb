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
# rubocop:disable Metrics/CyclomaticComplexity
# rubocop:disable Metrics/MethodLength
# rubocop:disable Metrics/PerceivedComplexity
# rubocop:disable Metrics/BlockLength
# rubocop:disable Metrics/ClassLength

# https://api.onlyoffice.com/editors/callback#history

# This faster that the Ruby on Rails implementations and enoght for us.
# 'app_user'.split('_').map{|e| e.capitalize}.join

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
  def initialize(current_version, history = [])
    @current_version = T.let(current_version, Integer)
    @history = T.let(history, T::Array[HistoryItem])
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
  def initialize(created, uid, uname)
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
  def initialize(id, name)
    @id = id
    @name = name
  end
end

class HistoryData
  extend T::Sig

  sig { returns(String) }
  # uri?
  attr_accessor :changes_url

  sig { returns(String) }
  attr_accessor :file_type

  sig { returns(String) }
  attr_accessor :key

  sig { returns(HistoryData) }
  attr_accessor :previous

  sig { returns(String) }
  attr_accessor :token

  sig { returns(String) }
  attr_accessor :url

  sig { returns(Integer) }
  attr_accessor :version
end

class HistoryController < ApplicationController
  extend T::Sig

  # https://api.onlyoffice.com/editors/methods#refreshHistory
  #
  # ```http
  # GET /history/{{file_basename}}?user_host={{user_host}} HTTP/1.1
  # ?? Authorization: Bearer {{token}}
  # ``
  sig { void }
  def history
    DocumentHelper.init(request.remote_ip, request.base_url)
    user_host = DocumentHelper.cur_user_host_address(params[:user_host])
    user_manager = UserManager.new(user_host)
    source_basename = params[:file_basename]
    config = Configuration.new
    storage_manager = StorageManager.new(config, user_manager, source_basename)
    unless storage_manager.source_file.exist?
      response.status = :not_found
      render json: {
        error: "The file with the specified basename doesn't exist.",
        success: false
      }
      return
    end

    history_manager = HistoryManager.new(storage_manager)
    history = history_manager.history
    response.status = :ok
    # TODO: PascalCase.
    render json: history
  end

  # https://api.onlyoffice.com/editors/methods#setHistoryData
  #
  # ```http
  # GET /history/{{file_basename}}/{{version}}/data?user_host={{user_host}} HTTP/1.1
  # ?? Authorization: Bearer {{token}}
  # ``
  sig { void }
  def history_data
    DocumentHelper.init(request.remote_ip, request.base_url)
    user_host = DocumentHelper.cur_user_host_address(params[:user_host])
    user_manager = UserManager.new(user_host)
    source_basename = params[:file_basename]
    config = Configuration.new
    storage_manager = StorageManager.new(config, user_manager, source_basename)
    unless storage_manager.source_file.exist?
      response.status = :not_found
      render json: {
        error: "The file with the specified basename doesn't exist.",
        success: false
      }
      return
    end

    # source_extension = File.extname(source_file)
    # source_key = ServiceConverter.generate_revision_id(
    #   "#{File.join(user_host, source_basename)}.#{File.mtime(source_file)}"
    # )
    # url = ?

    history_manager = HistoryManager.new(storage_manager)

    puts
  end

  # ? url that used in history_data
  #
  # ```http
  # GET /history/{{file_basename}}/{{version}}/download/{{requested_file_basename}}?user_host={{user_host}} HTTP/1.1
  # ?? Authorization: Bearer {{token}}
  # ```
  # def history_download
  # end
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

  sig { params(storage_manager: StorageManager).void }
  def initialize(storage_manager)
    @storage_manager = storage_manager
  end

  sig { returns(History) }
  def history
    history = History.new(latest_version)

    (HistoryManager.minimal_version..history.current_version).each do |version|
      if version == HistoryManager.minimal_version
        item = initial_item
        next unless item

        history.history.append(item)
        next
      end

      previous_item = item(version - 1)
      next unless previous_item

      item = previous_item.changes.history[0]
      next unless item

      history.history.append(item)
    end

    history
  end

  sig { params(version: Integer).returns(T.nilable(HistoryData)) }
  def history_data(version)
    file_type = @storage_manager.source_file.extname.delete_prefix('.')

    # this ok if version != 0, why? why not u s b
    item_key = key(version)
    return nil unless item_key

    uri = URI.join(
      '?',
      'history',
      ERB::Util.url_encode(@storage_manager.source_file.basename),
      version.to_s,
      'download',
      "prev#{@storage_manager.source_file.extname}"
    )
    query = {}
    # query['user_host'] = '?' from args
    uri.query = URI.encode_www_form(query)

    # item_version = vesrion
    # directUrl from args

    puts
  end

  sig { returns(T.nilable(HistoryItem)) }
  def initial_item
    item_key = key(HistoryManager.minimal_version)
    return nil unless item_key

    item_metadata = metadata
    return nil unless item_metadata

    user = HistoryUser.new(item_metadata.uid, item_metadata.uname)
    item = HistoryItem.new
    item.version = HistoryManager.minimal_version
    item.key = item_key
    item.created = item_metadata.created
    item.user = user
    item
  end

  sig { params(version: Integer).returns(T.nilable(HistoryItem)) }
  def item(version)
    file = item_file(version)
    return nil unless file.exist?

    # TODO: in the /track ednpoint we should save on the disk full of the object
    # (History), not only a history property['history'].
    content = file.read
    json = JSON.parse(content)
    history = History.new(-1, json)
    item = HistoryItem.new
    item.changes = history
    item
  end

  # sig { params(version_directory: Pathname).returns(Pathname) }
  # def changes_file(version_directory)
  sig { params(version: Integer).returns(Pathname) }
  def item_file(version)
    history_directory.join(version.to_s, 'changes.json')
  end

  sig { params(version: Integer).returns(T.nilable(String)) }
  def key(version)
    file = key_file(version)
    return nil unless file.exist?
    file.read
  end

  sig { params(version: Integer).returns(Pathname) }
  def key_file(version)
    directory = version_directory(version)
    directory.join('key.txt')
  end

  sig { params(version: Integer).returns(Pathname) }
  def version_directory(version)
    directory = history_directory.join(version.to_s)
    FileUtils.mkdir(directory) unless directory.exist?
    directory
  end

  sig { returns(T.nilable(HistoryMetadata)) }
  def metadata
    file = metadata_file(history_directory)
    return nil unless file.exist?

    content = file.read
    json = JSON.parse(content)
    HistoryMetadata.new(json['created'], json['uid'], json['uname'])
  end

  sig { params(history_directory: Pathname).returns(Pathname) }
  def metadata_file(history_directory)
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
