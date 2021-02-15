#
# (c) Copyright Ascensio System SIA 2020
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

    @file = FileModel.new(:file_name => File.basename(params[:fileName]), :mode => params[:editorsMode], :type => params[:editorsType], :user_ip => request.remote_ip, :lang => cookies[:ulang], :uid => cookies[:uid], :uname => cookies[:uname], :action_data => params[:actionLink])

  end

  def sample

    DocumentHelper.init(request.remote_ip, request.base_url)

    file_name = DocumentHelper.create_demo(params[:fileExt], params[:sample], cookies[:uid], cookies[:uname])
    redirect_to :controller => 'home', :action => 'editor', :fileName => file_name

  end

  def upload

    DocumentHelper.init(request.remote_ip, request.base_url)

    begin
      http_posted_file = params[:file]
      file_name = http_posted_file.original_filename
      cur_size = http_posted_file.size

      if DocumentHelper.file_size_max < cur_size || cur_size <= 0
        raise 'File size is incorrect'
      end

      cur_ext = File.extname(file_name).downcase

      unless DocumentHelper.file_exts.include? cur_ext
        raise 'File type is not supported'
      end

      file_name = DocumentHelper.get_correct_name(file_name)

      File.open(DocumentHelper.storage_path(file_name, nil), 'wb') do |file|
        file.write(http_posted_file.read)
      end

      DocumentHelper.create_meta(file_name, cookies[:uid], cookies[:uname])

      render plain: '{ "filename": "' + file_name + '"}'
    rescue => ex
      render plain: '{ "error": "' + ex.message + '"}'
    end

  end

  def convert

    begin
      file_name = File.basename(params[:filename])
      file_uri = DocumentHelper.get_file_uri(file_name, true)
      extension = File.extname(file_name)
      internal_extension = DocumentHelper.get_internal_extension(FileUtility.get_file_type(file_name))

      if DocumentHelper.convert_exts.include? (extension)
        key = ServiceConverter.generate_revision_id(file_uri)
        percent, new_file_uri  = ServiceConverter.get_converted_uri(file_uri, extension.delete('.'), internal_extension.delete('.'), key, true)

        if percent != 100
          render plain: '{ "step" : "' + percent.to_s + '", "filename" : "' + file_name + '"}'
          return
        end

        correct_name = DocumentHelper.get_correct_name(File.basename(file_name, extension) + internal_extension)

        uri = URI.parse(new_file_uri)
        http = Net::HTTP.new(uri.host, uri.port)

        if new_file_uri.start_with?('https')
          http.use_ssl = true
          http.verify_mode = OpenSSL::SSL::VERIFY_NONE
        end

        req = Net::HTTP::Get.new(uri.request_uri)
        res = http.request(req)
        data = res.body

        if data == nil
          raise 'stream is null'
        end

        File.open(DocumentHelper.storage_path(correct_name, nil), 'wb') do |file|
          file.write(data)
        end

        file_name = correct_name

        DocumentHelper.create_meta(file_name, cookies[:uid], cookies[:uname])
      end

      render plain: '{ "filename" : "' + file_name + '"}'
    rescue => ex
      render plain: '{ "error": "' + ex.message + '"}'
    end

  end

  def track
    file_data = TrackHelper.read_body(request)
    if file_data == nil || file_data.empty?
      render plain: '{"error":1}'
      return
    end

    status = file_data['status'].to_i

    user_address = params[:userAddress]
    file_name = File.basename(params[:fileName])

    if status == 1 #Editing
      if file_data['actions'][0]['type'] == 0 #Finished edit
        user = file_data['actions'][0]['userid']
         if !file_data['users'].index(user)
          json_data = TrackHelper.command_request("forcesave", file_data['key'])
         end
      end
    end

    if status == 2 || status == 3 #MustSave, Corrupted
      saved = TrackHelper.process_save(file_data, file_name, user_address)
      render plain: '{"error":' + saved.to_s + '}'
      return
    end

    if status == 6 || status == 7 # MustForceave, CorruptedForcesave
      saved = TrackHelper.process_force_save(file_data, file_name, user_address)
      render plain: '{"error":' + saved.to_s + '}'
      return
    end

    render plain: '{"error":0}'
    return
  end

  def remove
    file_name = File.basename(params[:filename])
    if !file_name
      render plain: '{"success":false}'
      return
    end

    DocumentHelper.init(request.remote_ip, request.base_url)
    storage_path = DocumentHelper.storage_path(file_name, nil)
    hist_dir = DocumentHelper.history_dir(storage_path)

    if File.exist?(storage_path)
      File.delete(storage_path)
    end

    if Dir.exist?(hist_dir)
      FileUtils.remove_entry_secure(hist_dir)
    end

    render plain: '{"success":true}'
    return
  end

  def files
    file_id = params[:fileId]
    filesInfo = DocumentHelper.get_files_info(file_id)
    render json: filesInfo
  end

  def csv
    file_name = "csv.csv"
    csvPath = Rails.root.join('public', 'assets', 'sample', file_name)

    response.headers['Content-Length'] = File.size(csvPath).to_s
    response.headers['Content-Type'] = MimeMagic.by_path(csvPath).type
    response.headers['Content-Disposition'] = "attachment;filename*=UTF-8\'\'" + URI.escape(file_name, Regexp.new("[^#{URI::PATTERN::UNRESERVED}]"))

    send_file csvPath, :x_sendfile => true
  end
end