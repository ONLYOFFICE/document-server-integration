# frozen_string_literal: true

#
# (c) Copyright Ascensio System SIA 2024
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

require 'json'
require 'net/http'
require 'mimemagic'
require_relative '../configuration/configuration'

# Handling requests controller
class HomeController < ApplicationController
  @config_manager = ConfigurationManager.new

  class << self
    attr_reader :config_manager
  end

  def index; end

  def editor
    DocumentHelper.init(request.remote_ip, request.base_url)
    user = Users.get_user(params[:userId])
    @file = FileModel.new(
      file_name: File.basename(params[:fileName]),
      mode: params[:editorsMode],
      type: params[:editorsType],
      user_ip: request.remote_ip,
      lang: cookies[:ulang],
      user:,
      action_data: params[:actionLink],
      direct_url: params[:directUrl]
    )
  end

  def forgotten
    unless HomeController.config_manager.enable_forgotten
      render(status: :forbidden, plain: '{"error": "The forgotten page is disabled"}')
      return
    end

    @files = []
    begin
      files_list = TrackHelper.command_request('getForgottenList', '')
      (files_list['keys']).each do |key|
        file = TrackHelper.command_request('getForgotten', key)
        public_uri = HomeController.config_manager.document_server_public_uri.to_s
        private_uri = HomeController.config_manager.document_server_private_uri.to_s
        if file['url'].include?(private_uri)
          file['url'].gsub!(private_uri, public_uri)
        end
        file['type'] = FileUtility.get_file_type(file['url'])
        @files.push(file)
      end
    rescue StandardError => e
      Rails.logger.error(e.message)
    end
  end

  def delete_forgotten
    unless HomeController.config_manager.enable_forgotten
      render(status: :forbidden)
      return
    end

    if params[:filename].present?
      TrackHelper.command_request('deleteForgotten', params[:filename])
    end
    render(status: :no_content)
  rescue StandardError
    render(plain: '{"error": "Server error"}')
  end

  # creating a sample document
  def sample
    DocumentHelper.init(request.remote_ip, request.base_url)
    user = Users.get_user(params[:userId])
    file_name = DocumentHelper.create_demo(params[:fileExt], params[:sample], user)
    redirect_to(controller: 'home', action: 'editor', fileName: file_name, userId: user.id, editorsMode: 'edit')
  end

  # uploading a file
  def upload
    DocumentHelper.init(request.remote_ip, request.base_url)

    begin
      http_posted_file = params[:file]
      file_name = http_posted_file.original_filename
      cur_size = http_posted_file.size

      # check if the file size exceeds the maximum file size
      raise('File size is incorrect') if DocumentHelper.file_size_max < cur_size || cur_size <= 0

      cur_ext = File.extname(file_name).downcase

      # check if the file extension is supported by the editor
      raise('File type is not supported') unless DocumentHelper.file_exts.include?(cur_ext)

      # get the correct file name if such a name already exists
      file_name = DocumentHelper.get_correct_name(file_name, nil)
      document_type = FileUtility.get_file_type(file_name)

      # write the uploaded file to the storage directory
      File.binwrite(DocumentHelper.storage_path(file_name, nil), http_posted_file.read)

      # create file meta information
      user = Users.get_user(params[:userId])

      DocumentHelper.create_meta(file_name, user.id, user.name, nil)

      # write a new file name to the response
      render(plain: "{ \"filename\": \"#{file_name}\", \"documentType\": \"#{document_type}\"}")
    rescue StandardError => e
      render(plain: "{ \"error\": \"#{e.message}\"}") # write an error message to the response
    end
  end

  # converting a file
  def convert
    file_data = request.body.read
    return '' if file_data.blank?

    body = JSON.parse(file_data)

    file_name = File.basename(body['filename'])
    lang = cookies[:ulang] || 'en'
    file_pass = body['filePass'] || nil
    file_uri = DocumentHelper.get_download_url(file_name)
    extension = File.extname(file_name).downcase
    # get an auto-conversion extension from the request body or set it to the ooxml extension
    conversion_extension = body['fileExt'] || 'ooxml'
    keep_original = body['keepOriginal'] || false

    if DocumentHelper.convert_exts.include?(extension) || conversion_extension != 'ooxml'
      key = ServiceConverter.generate_revision_id(file_uri) # generate document key
      percent, new_file_uri, new_file_type = ServiceConverter.get_converted_data(
        file_uri,
        extension.delete('.'),
        conversion_extension.delete('.'),
        key,
        true,
        file_pass,
        lang
      ) # get the url and file type of the converted file and the conversion percentage

      # if the conversion isn't completed, write file name and step values to the response
      if percent != 100
        render(plain: "{ \"step\" : \"#{percent}\", \"filename\" : \"#{file_name}\"}")
        return
      end

      unless FormatManager.new.all.map(&:serialize).any? { |f| f['name'] == new_file_type && f['actions'].any? }
        new_file_uri = new_file_uri.sub(
          HomeController.config_manager.document_server_private_uri.to_s,
          HomeController.config_manager.document_server_public_uri.to_s
        )
        render(plain: "{\"step\":\"#{percent}\",\"filename\":\"#{new_file_uri}\",\"error\":\"FileTypeIsNotSupported\"}")
        return
      end

      # get the correct file name if such a name already exists
      correct_name = DocumentHelper.get_correct_name("#{File.basename(file_name, extension)}.#{new_file_type}", nil)

      uri = URI.parse(new_file_uri) # create the request url
      http = Net::HTTP.new(uri.host, uri.port) # create a connection to the http server

      DocumentHelper.verify_ssl(new_file_uri, http)

      req = Net::HTTP::Get.new(uri.request_uri) # create the get requets
      res = http.request(req)
      data = res.body

      raise('stream is null') if data.nil?

      # write a file with a new extension, but with the content from the origin file
      File.binwrite(DocumentHelper.storage_path(correct_name, nil), data)

      unless keep_original
        old_storage_path = DocumentHelper.storage_path(file_name, nil)
        FileUtils.rm_f(old_storage_path)
      end

      file_name = correct_name
      user = Users.get_user(params[:userId])

      DocumentHelper.create_meta(file_name, user.id, user.name, nil) # create meta data of the new file
    end

    render(plain: "{ \"filename\" : \"#{file_name}\", \"step\" : \"#{percent}\"}")
  rescue StandardError => e
    render(plain: "{ \"error\": \"#{e.message}\"}")
  end

  def historyobj
    data = request.body.read
    if data.blank?
      return ''
    end
    file_data = JSON.parse(data)
    file = FileModel.new(
      file_name: File.basename(file_data['file_name']),
      mode: file_data['mode'],
      type: file_data['type'],
      user_ip: file_data['user_ip'],
      lang: file_data['lang'],
      user: file_data['user'],
      action_data: file_data['action_data'],
      direct_url: file_data['direct_url']
    )
    history = file.history
    render(json: history)
  rescue StandardError
    render(json: '{ "error": "File not found"}')
  end

  # downloading a history file from public
  def downloadhistory
    file_name = File.basename(params[:fileName])
    user_address = params[:userAddress]
    version = params[:ver]
    file = params[:file]
    params[:dmode]

    if JwtHelper.enabled? && JwtHelper.use_for_request
      jwt_header = HomeController.config_manager.jwt_header
      if request.headers[jwt_header]
        hdr = request.headers[jwt_header]
        hdr.slice!(0, 'Bearer '.length)
        token = JwtHelper.decode(hdr)
        if !token || token.eql?('')
          render(plain: 'JWT validation failed', status: :forbidden)
          return
        end
      else
        render(plain: 'JWT validation failed', status: :forbidden)
        return
      end
    end
    hist_path = "#{DocumentHelper.storage_path(file_name, user_address)}-hist" # or to the original document

    file_path = File.join(hist_path, version, file)

    # add headers to the response to specify the page parameters
    response.headers['Content-Length'] = File.size(file_path).to_s
    response.headers['Content-Type'] =
      MimeMagic.by_path(file_path).eql?(nil) ? nil : MimeMagic.by_path(file_path).type
    response.headers['Content-Disposition'] = "attachment;filename*=UTF-8''#{ERB::Util.url_encode(file)}"

    send_file(file_path, x_sendfile: true)
  rescue StandardError
    render(plain: '{ "error": "File not found"}')
  end

  # tracking file changes
  def track
    file_data = TrackHelper.read_body(request) # read the request body
    if file_data.blank?
      render(plain: '{"error":1}') # an error occurs if the file is empty
      return
    end

    status = file_data['status']

    user_address = params[:userAddress]
    file_name = File.basename(params[:fileName])

    if status == 1 && (file_data['actions'][0]['type']).zero? # finished edit
      user = file_data['actions'][0]['userid'] # get the user id
      unless file_data['users'].index(user)
        TrackHelper.command_request('forcesave', file_data['key']) # call the forcesave command
      end
    end

    if [2, 3].include?(status) # MustSave, Corrupted
      saved = TrackHelper.process_save(file_data, file_name, user_address) # save file
      render(plain: "{\"error\":#{saved}}")
      return
    end

    if [6, 7].include?(status) # MustForceave, CorruptedForcesave
      saved = TrackHelper.process_force_save(file_data, file_name, user_address) # force save file
      render(plain: "{\"error\":#{saved}}")
      return
    end

    render(plain: '{"error":0}')
    nil
  end

  # removing a file
  def remove
    DocumentHelper.init(request.remote_ip, request.base_url)

    if params[:filename].present?
      file_name = File.basename(params[:filename]) # get the file name
      unless file_name # if it doesn't exist
        render(plain: '{"success":false}') # report that the operation is unsuccessful
        return
      end

      storage_path = DocumentHelper.storage_path(file_name, nil)
      hist_dir = DocumentHelper.history_dir(storage_path)

      # if the file exists
      FileUtils.rm_f(storage_path) # delete it from the storage path

      # if the history directory of this file exists
      FileUtils.rm_rf(hist_dir) # delete it
    else
      storage_path = DocumentHelper.storage_path('', nil)
      FileUtils.rm_rf(storage_path) # remove the user's directory and all the containing files
    end
    render(plain: '{"success":true}') # report that the operation is successful
    nil
  rescue StandardError
    render(plain: '{"error": "Server error"}')
  end

  # getting files information
  def files
    file_id = params[:fileId]
    files_info = DocumentHelper.get_files_info(file_id) # get the information about the file specified by a file id
    render(json: files_info)
  end

  # downloading a csv file
  def csv
    file_name = 'csv.csv'
    csv_path = Rails.root.join('assets', 'document-templates', 'sample', file_name)

    # add headers to the response to specify the page parameters
    response.headers['Content-Length'] = File.size(csv_path).to_s
    response.headers['Content-Type'] = MimeMagic.by_path(csv_path).type
    response.headers['Content-Disposition'] = "attachment;filename*=UTF-8''#{ERB::Util.url_encode(file_name)}"

    send_file(csv_path, x_sendfile: true)
  end

  # downloading an assets file
  def assets
    file_name = File.basename(params[:fileName])
    asset_path = Rails.root.join('assets', 'document-templates', 'sample', file_name)

    response.headers['Content-Length'] = File.size(asset_path).to_s
    response.headers['Content-Type'] = MimeMagic.by_path(asset_path).type
    response.headers['Content-Disposition'] = "attachment;filename*=UTF-8''#{ERB::Util.url_encode(file_name)}"

    send_file(asset_path, x_sendfile: true)
  end

  # downloading a file
  def download
    file_name = File.basename(params[:fileName])
    user_address = params[:userAddress]
    is_embedded = params[:dmode]

    if JwtHelper.enabled? && is_embedded.nil? && !user_address.nil? && JwtHelper.use_for_request
      jwt_header = HomeController.config_manager.jwt_header
      if request.headers[jwt_header]
        hdr = request.headers[jwt_header]
        hdr.slice!(0, 'Bearer '.length)
        token = JwtHelper.decode(hdr)
      end
      if !token || token.eql?('')
        render(plain: 'JWT validation failed', status: :forbidden)
        return
      end
    end

    # get the path to the force saved document version
    file_path = DocumentHelper.forcesave_path(file_name, user_address, false)
    if file_path.eql?('')
      file_path = DocumentHelper.storage_path(file_name, user_address) # or to the original document
    end

    # add headers to the response to specify the page parameters
    response.headers['Content-Length'] = File.size(file_path).to_s
    response.headers['Content-Type'] =
      MimeMagic.by_path(file_path).eql?(nil) ? nil : MimeMagic.by_path(file_path).type
    response.headers['Content-Disposition'] = "attachment;filename*=UTF-8''#{ERB::Util.url_encode(file_name)}"

    send_file(file_path, x_sendfile: true)
  rescue StandardError
    render(plain: '{ "error": "File not found"}')
  end

  # Save Copy as...
  def saveas
    body = JSON.parse(request.body.read)
    file_url = body['url'].sub(
      HomeController.config_manager.document_server_public_uri.to_s,
      HomeController.config_manager.document_server_private_uri.to_s
    )
    title = body['title']
    file_name = DocumentHelper.get_correct_name(title, nil)
    extension = File.extname(file_name).downcase
    all_exts = DocumentHelper.convert_exts +
               DocumentHelper.edited_exts +
               DocumentHelper.viewed_exts +
               DocumentHelper.fill_forms_exts

    unless all_exts.include?(extension)
      render(plain: '{"error": "File type is not supported"}')
      return
    end

    uri = URI.parse(file_url) # create the request url
    http = Net::HTTP.new(uri.host, uri.port) # create a connection to the http server

    DocumentHelper.verify_ssl(file_url, http)

    req = Net::HTTP::Get.new(uri.request_uri) # create the get requets
    res = http.request(req)
    data = res.body

    if data.size <= 0 || data.size > HomeController.config_manager.maximum_file_size
      render(plain: '{"error": "File size is incorrect"}')
      return
    end

    File.binwrite(DocumentHelper.storage_path(file_name, nil), data)
    user = Users.get_user(params[:userId])
    DocumentHelper.create_meta(file_name, user.id, user.name, nil) # create meta data of the new file

    render(plain: "{\"file\" : \"#{file_name}\"}")
    nil
  rescue StandardError => e
    render(plain: JSON.generate({ error: 1, message: e.message }))
    nil
  end

  # Rename...
  def rename
    body = JSON.parse(request.body.read)
    dockey = body['dockey']
    newfilename = body['newfilename']

    orig_ext = ".#{body['ext']}"
    cur_ext = File.extname(newfilename).downcase
    newfilename += orig_ext if orig_ext != cur_ext

    meta = {
      title: newfilename
    }

    json_data = TrackHelper.command_request('meta', dockey, meta)
    render(plain: "{ \"result\" : \"#{JSON.dump(json_data)}\"}")
  end

  # ReferenceData
  def reference
    body = JSON.parse(request.body.read)
    file_name = ''

    if body.key?('referenceData')
      reference_data = body['referenceData']
      instance_id = reference_data['instanceId']
      if instance_id == DocumentHelper.get_server_url(false)
        file_key = JSON.parse(reference_data['fileKey'])
        user_address = file_key['userAddress']
        file_name = file_key['fileName'] if user_address == DocumentHelper.cur_user_host_address(nil)
      end
    end

    link = body['link']
    if file_name.empty? && body.key?('link')
      unless link.include?(DocumentHelper.get_server_url(false))
        data = {
          url: link,
          directUrl: link
        }
        render(plain: data.to_json)
        return
      end

      url_obj = URI(link)
      query_params = CGI.parse(url_obj.query)
      file_name = query_params['fileName'].first
      unless File.exist?(DocumentHelper.storage_path(file_name, nil))
        render(plain: '{ "error": "File is not exist"}')
        return
      end
    end

    if file_name.empty? && body.key?('path')
      path = File.basename(body['path'])
      file_name = path if File.exist?(DocumentHelper.storage_path(path, nil))
    end

    if file_name.empty?
      render(plain: '{ "error": "File not found"}')
      return
    end

    data = {
      fileType: File.extname(file_name).downcase.delete('.'),
      key: ServiceConverter.generate_revision_id(
        "#{DocumentHelper.cur_user_host_address(nil)}/#{file_name}" \
        ".#{File.mtime(DocumentHelper.storage_path(file_name, nil))}"
      ),
      url: DocumentHelper.get_download_url(file_name),
      directUrl: body['directUrl'] ? DocumentHelper.get_download_url(file_name, is_serverUrl: false) : nil,
      referenceData: {
        instanceId: DocumentHelper.get_server_url(false),
        fileKey: { fileName: file_name, userAddress: DocumentHelper.cur_user_host_address(nil) }.to_json
      },
      path: file_name,
      link: "#{DocumentHelper.get_server_url(false)}/editor?fileName=#{file_name}"
    }

    data['token'] = JwtHelper.encode(data) if JwtHelper.enabled?

    render(plain: data.to_json)
  end

  def restore
    body = JSON.parse(request.body.read)

    source_basename = body['fileName']
    version = body['version']
    user_id = body['userId']

    source_extension = Pathname(source_basename).extname
    user = Users.get_user(user_id)

    DocumentHelper.init(request.remote_ip, request.base_url)
    file_model = FileModel.new(
      {
        file_name: source_basename
      }
    )

    user_ip = DocumentHelper.cur_user_host_address(nil)
    source_file = DocumentHelper.storage_path(source_basename, user_ip)
    history_directory = DocumentHelper.history_dir(source_file)

    previous_basename = "prev#{source_extension}"

    recovery_version_directory = DocumentHelper.version_dir(history_directory, version)
    recovery_raw_file = Pathname(recovery_version_directory)
    recovery_file = recovery_raw_file.join(previous_basename)

    bumped_version = DocumentHelper.get_file_version(history_directory)
    bumped_version_raw_directory = DocumentHelper.version_dir(history_directory, bumped_version)
    bumped_version_directory = Pathname(bumped_version_raw_directory)
    FileUtils.mkdir(bumped_version_directory) unless bumped_version_directory.exist?

    bumped_key_file = bumped_version_directory.join('key.txt')
    bumped_key = file_model.key
    File.write(bumped_key_file, bumped_key)

    bumped_changes_file = bumped_version_directory.join('changes.json')
    bumped_changes = {
      serverVersion: nil,
      changes: [
        {
          created: Time.zone.now.to_fs(:db),
          user: {
            id: user.id,
            name: user.name
          }
        }
      ]
    }
    bumped_changes_content = JSON.generate(bumped_changes)
    File.write(bumped_changes_file, bumped_changes_content)

    bumped_file = bumped_version_directory.join(previous_basename)
    FileUtils.cp(source_file, bumped_file)
    FileUtils.cp(recovery_file, source_file)

    render(
      json: {
        error: nil,
        success: true
      }
    )
  rescue StandardError => e
    response.status = :internal_server_error
    render(
      json: {
        error: e.message,
        success: false
      }
    )
  end

  # return all supported formats
  def formats
    render(
      json: JSON.generate(
        {
          formats: FormatManager.new.all.map(&:serialize)
        }
      )
    )
  end

  def refresh_config
    file_name = params[:fileName]
    direct_url = params[:directUrl] == 'true'
    permissions = params[:permissions]

    unless File.exist?(DocumentHelper.storage_path(file_name, nil))
      render(
        json: JSON.generate(
          {
            error: 'File not found'
          }
        )
      )
    end

    uri = "#{DocumentHelper.cur_user_host_address(nil)}/#{file_name}"
    stat = File.mtime(DocumentHelper.storage_path(file_name, nil))
    key = ServiceConverter.generate_revision_id("#{uri}.#{stat}")

    config = {
      document: {
        title: file_name,
        url: DocumentHelper.get_download_url(file_name),
        directUrl: direct_url ? DocumentHelper.get_download_url(file_name, false) : nil,
        key:,
        permissions: JSON.parse(permissions),
        referenceData: {
          instanceId: DocumentHelper.get_server_url(false),
          fileKey: {
            fileName: file_name,
            userAddress: DocumentHelper.cur_user_host_address(nil)
          }.to_json
        }
      },
      editorConfig: {
        mode: 'edit',
        callbackUrl: DocumentHelper.get_callback(file_name)
      }
    }

    if JwtHelper.enabled?
      config['token'] = JwtHelper.encode(config)
    end

    render(
      json: JSON.generate(config)
    )
  end
end
