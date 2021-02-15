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

class DocumentHelper

  @@runtime_cache = {}
  @@remote_ip = nil
  @@base_url = nil

  class << self

    def init (ip, url)
      @@remote_ip = ip
      @@base_url = url
    end

    def file_size_max
      if Rails.configuration.fileSizeMax == nil
        5 * 1024 * 1024
      else
        Rails.configuration.fileSizeMax
      end
    end

    def file_exts
      [].concat(viewed_exts).concat(edited_exts).concat(convert_exts)
    end

    def viewed_exts
      if Rails.configuration.viewedDocs.empty?
        []
      else
        Rails.configuration.viewedDocs.split("|")
      end
    end

    def edited_exts
      if Rails.configuration.editedDocs.empty?
        []
      else
        Rails.configuration.editedDocs.split("|")
      end
    end

    def convert_exts
      if Rails.configuration.convertDocs.empty?
        []
      else
        Rails.configuration.convertDocs.split("|")
      end
    end

    def cur_user_host_address(user_address)
      (user_address == nil ? @@remote_ip : user_address).gsub(/[^0-9-.a-zA-Z_=]/, '_');
    end

    def storage_path(file_name, user_address)
      directory = Rails.root.join('public', Rails.configuration.storagePath, cur_user_host_address(user_address))

      unless File.directory?(directory)
        FileUtils.mkdir_p(directory)
      end

      directory.join(File.basename(file_name)).to_s
    end

    def forcesave_path(file_name, user_address, create)
      directory = Rails.root.join('public', Rails.configuration.storagePath, cur_user_host_address(user_address))

      unless File.directory?(directory)
        return ""
      end

      directory = directory.join("#{File.basename(file_name)}-hist")
      unless File.directory?(directory)
        if create
          FileUtils.mkdir_p(directory)
        else
          return ""
        end
      end

      directory = directory.join(File.basename(file_name))
      unless File.file?(directory)
        if !create
          return ""
        end
      end

      return directory.to_s
    end

    def history_dir(storage_path)
      directory = "#{storage_path}-hist"

      unless File.directory?(directory)
        FileUtils.mkdir_p(directory)
      end

      return directory
    end

    def version_dir(hist_dir, ver)
      return File.join(hist_dir, ver.to_s)
    end

    def get_file_version(hist_dir)
      if !Dir.exist?(hist_dir)
        return 0
      end

      ver = 1
      Dir.foreach(hist_dir) {|e|
        next if e.eql?(".")
        next if e.eql?("..")
        if File.directory?(File.join(hist_dir, e))
          ver += 1
        end
      }

      return ver
    end

    def get_correct_name(file_name)
      ext = File.extname(file_name)
      base_name = File.basename(file_name, ext)
      name = base_name + ext
      index = 1

      while File.exist?(storage_path(name, nil))
          name = base_name + ' (' + index.to_s + ')' + ext
          index = index + 1
      end

      name
    end

    def get_stored_files(user_address)
      directory = Rails.root.join('public', Rails.configuration.storagePath, cur_user_host_address(user_address))

      arr = [];

      if Dir.exist?(directory)
        Dir.foreach(directory) {|e|
          next if e.eql?(".")
          next if e.eql?("..")
          next if File.directory?(File.join(directory, e))

          arr.push(e)
        }
      end

      return arr
    end

    def create_meta(file_name, uid, uname)
      hist_dir = history_dir(storage_path(file_name, nil))

      json = {
        :created => Time.now.to_formatted_s(:db),
        :uid => uid ? uid : "uid-0",
        :uname => uname ? uname : "John Smith"
      }

      File.open(File.join(hist_dir, "createdInfo.json"), 'wb') do |file|
        file.write(json.to_json)
      end
    end

    def create_demo(file_ext, sample, uid, uname)
      demo_name = (sample == 'true' ? 'sample.' : 'new.') + file_ext
      file_name = get_correct_name demo_name

      src = Rails.root.join('public', 'assets', sample == 'true' ? 'sample' : 'new', demo_name)
      dest = storage_path file_name, nil

      FileUtils.cp src, dest

      create_meta(file_name, uid, uname)

      file_name
    end

    def get_file_uri(file_name, for_document_server)
      uri = get_server_url(for_document_server) + '/' + Rails.configuration.storagePath + '/' + cur_user_host_address(nil) + '/' + URI::encode(file_name)

      return uri
    end

    def get_path_uri(path)
      uri = get_server_url(true) + '/' + Rails.configuration.storagePath + '/' + cur_user_host_address(nil) + '/' + path

      return uri
    end
    
    def get_server_url(for_document_server)
      if for_document_server && !Rails.configuration.urlExample.empty?
        return Rails.configuration.urlExample
      else
        return @@base_url
      end 
    end

    def get_callback(file_name)

      get_server_url(true) + '/track?type=track&fileName=' + URI::encode(file_name)  + '&userAddress=' + cur_user_host_address(nil)

    end

    def get_internal_extension(file_type)

      case file_type
        when 'word'
          ext = '.docx'
        when 'cell'
          ext = '.xlsx'
        when 'slide'
          ext = '.pptx'
        else
          ext = '.docx'
      end

      ext
    end

    def get_files_info(file_id)
      result = [];

      for fileName in get_stored_files(nil)
        directory = storage_path(fileName, nil)
        uri = cur_user_host_address(nil) + '/' + fileName

        info = {
          "version" => get_file_version(history_dir(directory)),
          "id" => ServiceConverter.generate_revision_id("#{uri}.#{File.mtime(directory).to_s}"),
          "contentLength" => "#{(File.size(directory)/ 1024.0).round(2)} KB",
          "pureContentLength" => File.size(directory),
          "title" => fileName,
          "updated" => File.mtime(directory) 
        }

        if file_id == nil
          result.push(info)
        else
          if file_id.eql?(info["id"])
            result.push(info)
            return result
          end
        end
      end

      if file_id != nil
        return "\"File not found\""
      else
        return result
      end
    end

  end

end