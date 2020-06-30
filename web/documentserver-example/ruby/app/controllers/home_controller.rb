require 'net/http'

class HomeController < ApplicationController
  def index
  end

  def editor

    DocumentHelper.init(request.remote_ip, request.base_url)

    @file = FileModel.new(:file_name => params[:fileName], :mode => params[:editorsMode], :type => params[:editorsType], :user_ip => request.remote_ip, :lang => cookies[:ulang], :uid => cookies[:uid], :uname => cookies[:uname], :action_data => params[:actionLink])

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
      file_name = params[:filename]
      file_uri = DocumentHelper.get_file_uri(file_name)
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

    user_address = params[:userAddress]
    file_name = params[:fileName]

    storage_path = DocumentHelper.storage_path(file_name, user_address)
    body = request.body.read

    if body == nil || body.empty?
      return
    end

    file_data = JSON.parse(body)

    if JwtHelper.is_enabled
      inHeader = false
      token = nil
      if file_data["token"]
        token = JwtHelper.decode(file_data["token"])
      elsif request.headers["Authorization"]
        hdr = request.headers["Authorization"]
        hdr.slice!(0, "Bearer ".length)
        token = JwtHelper.decode(hdr)
        inHeader = true
      else
        raise "Expected JWT"
      end
      if !token
        raise "Invalid JWT signature"
      end

      file_data = JSON.parse(token)
      if inHeader
        file_data = file_data["payload"]
      end
    end

    status = file_data['status'].to_i

    if status == 2 || status == 3 #MustSave, Corrupted

      saved = 0

      begin

        def save_from_uri(path, uristr)
          uri = URI.parse(uristr)
          http = Net::HTTP.new(uri.host, uri.port)

          if uristr.start_with?('https')
            http.use_ssl = true
            http.verify_mode = OpenSSL::SSL::VERIFY_NONE
          end

          req = Net::HTTP::Get.new(uri)
          res = http.request(req)
          data = res.body

          if data == nil
            raise 'stream is null'
          end

          File.open(path, 'wb') do |file|
            file.write(data)
          end
        end

        hist_dir = DocumentHelper.history_dir(storage_path)
        ver_dir = DocumentHelper.version_dir(hist_dir, DocumentHelper.get_file_version(hist_dir) + 1)

        FileUtils.mkdir_p(ver_dir)

        FileUtils.move(storage_path, File.join(ver_dir, "prev#{File.extname(file_name)}"))
        save_from_uri(storage_path, file_data['url'])

        if (file_data["changesurl"])
          save_from_uri(File.join(ver_dir, "diff.zip"), file_data["changesurl"])
        end

        hist_data = file_data["changeshistory"]
        if (!hist_data)
          hist_data = file_data["history"].to_json
        end
        if (hist_data)
          File.open(File.join(ver_dir, "changes.json"), 'wb') do |file|
            file.write(hist_data)
          end
        end

        File.open(File.join(ver_dir, "key.txt"), 'wb') do |file|
          file.write(file_data["key"])
        end

      rescue StandardError => msg
        saved = 1
      end

      render plain: '{"error":' + saved.to_s + '}'
      return
    end

    render plain: '{"error":0}'
    return

  end

  def remove
    file_name = params[:filename]
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
end