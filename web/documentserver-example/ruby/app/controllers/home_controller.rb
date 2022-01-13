#
# (c) Copyright Ascensio System SIA 2021
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

require 'net/http'
require 'mimemagic'

class HomeController < ApplicationController
  def index
  end

  def editor

    DocumentHelper.init(request.remote_ip, request.base_url)
    user = Users.get_user(cookies[:uid])

    @file = FileModel.new(:file_name => File.basename(params[:fileName]), :mode => params[:editorsMode], :type => params[:editorsType], :user_ip => request.remote_ip, :lang => cookies[:ulang], :user => user, :action_data => params[:actionLink])

  end

  # creating a sample document
  def sample

    DocumentHelper.init(request.remote_ip, request.base_url)
    user = Users.get_user(cookies[:uid])

    file_name = DocumentHelper.create_demo(params[:fileExt], params[:sample], user)
    redirect_to :controller => 'home', :action => 'editor', :fileName => file_name

  end

  # uploading a file
  def upload

    DocumentHelper.init(request.remote_ip, request.base_url)

    begin
      http_posted_file = params[:file]
      file_name = http_posted_file.original_filename
      cur_size = http_posted_file.size

      # check if the file size exceeds the maximum file size
      if DocumentHelper.file_size_max < cur_size || cur_size <= 0
        raise 'File size is incorrect'
      end

      cur_ext = File.extname(file_name).downcase

      # check if the file extension is supported by the editor
      unless DocumentHelper.file_exts.include? cur_ext
        raise 'File type is not supported'
      end

      # get the correct file name if such a name already exists
      file_name = DocumentHelper.get_correct_name(file_name, nil)
      document_type = FileUtility.get_file_type(file_name)

      # write the uploaded file to the storage directory
      File.open(DocumentHelper.storage_path(file_name, nil), 'wb') do |file|
        file.write(http_posted_file.read)
      end

      # create file meta information
      user = Users.get_user(cookies[:uid])

      DocumentHelper.create_meta(file_name, user.id, user.name, nil)

      render plain: '{ "filename": "' + file_name + '", "documentType": "' + document_type + '"}'  # write a new file name to the response
    rescue => ex
      render plain: '{ "error": "' + ex.message + '"}'  # write an error message to the response
    end

  end

  # converting a file
  def convert

    begin
      file_data = request.body.read
      if file_data == nil || file_data.empty?
          return ""
      end

      body = JSON.parse(file_data)
      
      file_name = File.basename(body["filename"])
      lang = cookies[:ulang] ? cookies[:ulang] : "en"
      file_pass = body["filePass"] ? body["filePass"] : nil
      file_uri = DocumentHelper.get_download_url(file_name)
      extension = File.extname(file_name).downcase
      internal_extension = DocumentHelper.get_internal_extension(FileUtility.get_file_type(file_name))

      if DocumentHelper.convert_exts.include? (extension)  # check if the file with such an extension can be converted
        key = ServiceConverter.generate_revision_id(file_uri)  # generate document key
        percent, new_file_uri  = ServiceConverter.get_converted_uri(file_uri, extension.delete('.'), internal_extension.delete('.'), key, true, file_pass, lang)  # get the url of the converted file and the conversion percentage

        # if the conversion isn't completed, write file name and step values to the response
        if percent != 100
          render plain: '{ "step" : "' + percent.to_s + '", "filename" : "' + file_name + '"}'
          return
        end

        # get the correct file name if such a name already exists
        correct_name = DocumentHelper.get_correct_name(File.basename(file_name, extension) + internal_extension, nil)

        uri = URI.parse(new_file_uri)  # create the request url
        http = Net::HTTP.new(uri.host, uri.port)  # create a connection to the http server

        if new_file_uri.start_with?('https')
          http.use_ssl = true
          http.verify_mode = OpenSSL::SSL::VERIFY_NONE  # set the flags for the server certificate verification at the beginning of SSL session
        end

        req = Net::HTTP::Get.new(uri.request_uri)  # create the get requets
        res = http.request(req)
        data = res.body

        if data == nil
          raise 'stream is null'
        end

        # write a file with a new extension, but with the content from the origin file
        File.open(DocumentHelper.storage_path(correct_name, nil), 'wb') do |file|
          file.write(data)
        end

        file_name = correct_name
        user = Users.get_user(cookies[:uid])

        DocumentHelper.create_meta(file_name, user.id, user.name, nil)  # create meta data of the new file
      end

      render plain: '{ "filename" : "' + file_name + '"}'
    rescue => ex
      render plain: '{ "error": "' + ex.message + '"}'
    end

  end

  # downloading a history file from public
  def downloadhistory
    begin
      file_name = File.basename(params[:fileName])
      user_address = params[:userAddress]
      version = params[:ver]
      file = params[:file]
      isEmbedded = params[:dmode]

      if JwtHelper.is_enabled
        jwtHeader = Rails.configuration.header.empty? ? "Authorization" : Rails.configuration.header;
        if request.headers[jwtHeader]
          hdr = request.headers[jwtHeader]
          hdr.slice!(0, "Bearer ".length)
          token = JwtHelper.decode(hdr)
          if !token || token.eql?("")
            render plain: "JWT validation failed", :status => 403
            return
          end
        else
          render plain: "JWT validation failed", :status => 403
          return
        end
      end
      hist_path = DocumentHelper.storage_path(file_name, user_address) + "-hist" # or to the original document

      file_path = File.join(hist_path, version, file)

      # add headers to the response to specify the page parameters
      response.headers['Content-Length'] = File.size(file_path).to_s
      response.headers['Content-Type'] = MimeMagic.by_path(file_path).eql?(nil) ? nil : MimeMagic.by_path(file_path).type
      response.headers['Content-Disposition'] = "attachment;filename*=UTF-8\'\'" + URI.escape(file, Regexp.new("[^#{URI::PATTERN::UNRESERVED}]"))

      send_file file_path, :x_sendfile => true
    rescue => ex
      render plain: '{ "error": "File not found"}'
    end
  end

  # tracking file changes
  def track
    file_data = TrackHelper.read_body(request)  # read the request body
    if file_data == nil || file_data.empty?
      render plain: '{"error":1}'  # an error occurs if the file is empty
      return
    end

    status = file_data['status'].to_i

    user_address = params[:userAddress]
    file_name = File.basename(params[:fileName])

    if status == 1  # editing
      if file_data['actions'][0]['type'] == 0  # finished edit
        user = file_data['actions'][0]['userid']  # get the user id
         if !file_data['users'].index(user)
          json_data = TrackHelper.command_request("forcesave", file_data['key'])  # call the forcesave command
         end
      end
    end

    if status == 2 || status == 3  # MustSave, Corrupted
      saved = TrackHelper.process_save(file_data, file_name, user_address)  # save file
      render plain: '{"error":' + saved.to_s + '}'
      return
    end

    if status == 6 || status == 7  # MustForceave, CorruptedForcesave
      saved = TrackHelper.process_force_save(file_data, file_name, user_address)  # force save file
      render plain: '{"error":' + saved.to_s + '}'
      return
    end

    render plain: '{"error":0}'
    return
  end

  # removing a file
  def remove
    file_name = File.basename(params[:filename])  # get the file name
    if !file_name  # if it doesn't exist
      render plain: '{"success":false}'  # report that the operation is unsuccessful
      return
    end

    DocumentHelper.init(request.remote_ip, request.base_url)
    storage_path = DocumentHelper.storage_path(file_name, nil)
    hist_dir = DocumentHelper.history_dir(storage_path)

    if File.exist?(storage_path)  # if the file exists
      File.delete(storage_path)  # delete it from the storage path
    end

    if Dir.exist?(hist_dir)  # if the history directory of this file exists
      FileUtils.remove_entry_secure(hist_dir)  # delete it
    end

    render plain: '{"success":true}'  # report that the operation is successful
    return
  end

  # getting files information
  def files
    file_id = params[:fileId]
    filesInfo = DocumentHelper.get_files_info(file_id)  # get the information about the file specified by a file id
    render json: filesInfo
  end

  # downloading a csv file
  def csv
    file_name = "csv.csv"
    csvPath = Rails.root.join('public', 'assets', 'sample', file_name)

    # add headers to the response to specify the page parameters
    response.headers['Content-Length'] = File.size(csvPath).to_s
    response.headers['Content-Type'] = MimeMagic.by_path(csvPath).type
    response.headers['Content-Disposition'] = "attachment;filename*=UTF-8\'\'" + URI.escape(file_name, Regexp.new("[^#{URI::PATTERN::UNRESERVED}]"))

    send_file csvPath, :x_sendfile => true
  end

  # downloading a file
  def download
    begin
      file_name = File.basename(params[:fileName])
      user_address = params[:userAddress]
      isEmbedded = params[:dmode]

      if JwtHelper.is_enabled && isEmbedded == nil
        jwtHeader = Rails.configuration.header.empty? ? "Authorization" : Rails.configuration.header;
        if request.headers[jwtHeader]
            hdr = request.headers[jwtHeader]
            hdr.slice!(0, "Bearer ".length)
            token = JwtHelper.decode(hdr)
            if !token || token.eql?("")
              render plain: "JWT validation failed", :status => 403
              return
            end
          end
      end

      file_path = DocumentHelper.forcesave_path(file_name, user_address, false)  # get the path to the force saved document version
      if file_path.eql?("")
        file_path = DocumentHelper.storage_path(file_name, user_address)  # or to the original document
      end

      # add headers to the response to specify the page parameters
      response.headers['Content-Length'] = File.size(file_path).to_s
      response.headers['Content-Type'] = MimeMagic.by_path(file_path).eql?(nil) ? nil : MimeMagic.by_path(file_path).type
      response.headers['Content-Disposition'] = "attachment;filename*=UTF-8\'\'" + URI.escape(file_name, Regexp.new("[^#{URI::PATTERN::UNRESERVED}]"))

      send_file file_path, :x_sendfile => true
    rescue => ex
      render plain: '{ "error": "File not found"}'
    end
  end

  # Save Copy as...
  def saveas
    begin
      body = JSON.parse(request.body.read)
      file_url = body["url"]
      title = body["title"]
      file_name = DocumentHelper.get_correct_name(title, nil)
      extension = File.extname(file_name).downcase
      all_exts = DocumentHelper.convert_exts + DocumentHelper.edited_exts + DocumentHelper.viewed_exts + DocumentHelper.fill_forms_exts

      unless all_exts.include?(extension)
        render plain: '{"error": "File type is not supported"}'
        return
      end

      uri = URI.parse(file_url)  # create the request url
      http = Net::HTTP.new(uri.host, uri.port)  # create a connection to the http server

      if file_url.start_with?('https')
        http.use_ssl = true
        http.verify_mode = OpenSSL::SSL::VERIFY_NONE  # set the flags for the server certificate verification at the beginning of SSL session
      end
      req = Net::HTTP::Get.new(uri.request_uri)  # create the get requets
      res = http.request(req)
      data = res.body

      if data.size <= 0 || data.size > Rails.configuration.fileSizeMax
        render plain: '{"error": "File size is incorrect"}'
        return
      end

      File.open(DocumentHelper.storage_path(file_name, nil), 'wb') do |file|
        file.write(data)
      end
      user = Users.get_user(cookies[:uid])
      DocumentHelper.create_meta(file_name, user.id, user.name, nil)  # create meta data of the new file

      render plain: '{"file" : "' + file_name + '"}'
      return
    rescue => ex
      render plain: '{"error":1, "message": "' + ex.message + '"}'
      return
    end
  end
end