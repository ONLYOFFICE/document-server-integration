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

require_relative '../configuration/configuration'
require_relative '../format/format'

class DocumentHelper
  @config_manager = ConfigurationManager.new
  @format_manager = FormatManager.new

  class << self
    attr_reader :config_manager
    attr_reader :format_manager
  end

  @@runtime_cache = {}
  @@remote_ip = nil
  @@base_url = nil

  class << self

    def init (ip, url)
      @@remote_ip = ip
      @@base_url = url
    end

    # define max file size
    def file_size_max
      DocumentHelper.config_manager.maximum_file_size
    end

    # all the supported file extensions
    def file_exts
      DocumentHelper.format_manager.all_extensions
    end

    def fill_forms_exts
      DocumentHelper.format_manager.fillable_extensions
    end

    # file extensions that can be viewed
    def viewed_exts
      DocumentHelper.format_manager.viewable_extensions
    end

    # file extensions that can be edited
    def edited_exts
      DocumentHelper.format_manager.editable_extensions
    end

    # file extensions that can be converted
    def convert_exts
      DocumentHelper.format_manager.convertible_extensions
    end

    # get current user host address
    def cur_user_host_address(user_address)
      (user_address == nil ? @@remote_ip : user_address).gsub(/[^0-9\-.a-zA-Z_=]/, '_');
    end

    # get the storage path of the given file
    def storage_path(file_name, user_address)
      directory = DocumentHelper.config_manager.storage_path.join(cur_user_host_address(user_address))

      # create a new directory if it doesn't exist
      unless File.directory?(directory)
        FileUtils.mkdir_p(directory)
      end

      # put the given file to this directory
      File.join(directory, File.basename(file_name))
    end

    # get the path to the forcesaved file version
    def forcesave_path(file_name, user_address, create)
      directory = DocumentHelper.config_manager.storage_path.join(cur_user_host_address(user_address))

      # the directory with host address doesn't exist
      unless File.directory?(directory)
        return ""
      end

      directory = File.join(directory,"#{File.basename(file_name)}-hist")  # get the path to the history of the given file
      unless File.directory?(directory)
        if create
          FileUtils.mkdir_p(directory)  # create history directory if it doesn't exist
        else
          return ""  # the history directory doesn't exist and we are not supposed to create it
        end
      end

      directory = File.join(directory, File.basename(file_name))  # get the path to the given file
      unless File.file?(directory)
        if !create
          return ""
        end
      end

      return directory.to_s
    end

    # get the path to the file history
    def history_dir(storage_path)
      directory = "#{storage_path}-hist"

      # create history directory if it doesn't exist
      unless File.directory?(directory)
        FileUtils.mkdir_p(directory)
      end

      return directory
    end

    # get the path to the specified file version
    def version_dir(hist_dir, ver)
      return File.join(hist_dir, ver.to_s)
    end

    # get the last file version
    def get_file_version(hist_dir)
      if !Dir.exist?(hist_dir)
        return 1
      end

      ver = 1
      Dir.foreach(hist_dir) {|e|  # run through all the file versions
        next if e.eql?(".")
        next if e.eql?("..")
        if File.directory?(File.join(hist_dir, e))
          ver += 1  # and count them
        end
      }

      return ver
    end

    # get the correct file name if such a name already exists
    def get_correct_name(file_name, user_address)
      ext = File.extname(file_name)  # get file extension
      base_name = File.basename(file_name, ext)  # get file name without extension
      name = base_name + ext.downcase  # get full file name
      index = 1

      while File.exist?(storage_path(name, user_address))  # if the file with such a name already exists in this directory
          name = base_name + ' (' + index.to_s + ')' + ext.downcase  # add an index after its base name
          index = index + 1
      end

      name
    end

    # get all the stored files from the folder
    def get_stored_files(user_address)
      directory = DocumentHelper.config_manager.storage_path.join(cur_user_host_address(user_address))

      arr = [];

      if Dir.exist?(directory)
        Dir.foreach(directory) {|e|  # run through all the elements from the folder
          next if e.eql?(".")
          next if e.eql?("..")
          next if File.directory?(File.join(directory, e))  # if the element is a directory, skip it

          arr.push(e)  # push the file to the array
        }
      end

      return arr
    end

    # create file meta information
    def create_meta(file_name, uid, uname, user_address)
      hist_dir = history_dir(storage_path(file_name, user_address))  # get the path to the file history

      # write user name, user uid and the creation time to the json object
      json = {
        :created => Time.now.to_formatted_s(:db),
        :uid => uid,
        :uname => uname
      }

      # write file meta information to the createdInfo.json file
      File.open(File.join(hist_dir, "createdInfo.json"), 'wb') do |file|
        file.write(json.to_json)
      end
    end

    # create demo document
    def create_demo(file_ext, sample, user)
      demo_name = (sample == 'true' ? 'sample.' : 'new.') + file_ext
      file_name = get_correct_name(demo_name, nil)  # get the correct file name if such a name already exists

      src = Rails.root.join('assets', 'document-templates', sample == 'true' ? 'sample' : 'new', demo_name)  # save sample document of a necessary extension to the storage directory
      dest = storage_path file_name, nil

      FileUtils.cp src, dest

      # save file meta data to the file

      create_meta(file_name, user.id, user.name, nil)

      file_name
    end

    # get file url
    def get_file_uri(file_name, for_document_server)
      uri = get_server_url(for_document_server) + '/' + DocumentHelper.config_manager.storage_path.to_s + '/' + cur_user_host_address(nil) + '/' + ERB::Util.url_encode(file_name)

      return uri
    end

    # get history path url
    def get_historypath_uri(file_name,version,file,is_serverUrl=true)
      # for redirection to my link
      user_host = is_serverUrl ? '&userAddress=' + cur_user_host_address(nil) : ""
      uri = get_server_url(is_serverUrl) + '/downloadhistory/?fileName=' + ERB::Util.url_encode(file_name) + '&ver='+ version.to_s + '&file='+ ERB::Util.url_encode(file) + user_host
      return uri
    end

    # get server url
    def get_server_url(for_document_server)
      if for_document_server && DocumentHelper.config_manager.example_uri
        return DocumentHelper.config_manager.example_uri.to_s
      else
        return @@base_url
      end
    end

    # get callback url
    def get_callback(file_name)

      get_server_url(true) + '/track?fileName=' + ERB::Util.url_encode(file_name)  + '&userAddress=' + cur_user_host_address(nil)

    end

    # get url to the created file
    def get_create_url(document_type)

      get_server_url(false) + '/sample?fileExt=' + get_internal_extension(document_type).delete('.')

    end

    # get url to download a file
    def get_download_url(file_name, is_serverUrl=true)

      user_host = is_serverUrl ? '&userAddress=' + cur_user_host_address(nil) : ""
      get_server_url(is_serverUrl) + '/download?fileName=' + ERB::Util.url_encode(file_name)  + user_host

    end

    # get internal file extension by its type
    def get_internal_extension(file_type)

      case file_type
        when 'word'  # .docx for word type
          ext = '.docx'
        when 'cell'  # .xlsx for cell type
          ext = '.xlsx'
        when 'slide'  # .pptx for slide type
          ext = '.pptx'
        else
          ext = '.docx'  # the default value is .docx
      end

      ext
    end

    # get image url for templates
    def get_template_image_url(file_type)
      path = get_server_url(true) + "/assets/"
      case file_type
        when 'word'  # for word type
          full_path = path + 'file_docx.svg'
        when 'cell'  # .xlsx for cell type
          full_path = path + 'file_xlsx.svg'
        when 'slide'  # .pptx for slide type
          full_path = path + 'file_pptx.svg'
        else
          full_path = path + 'file_docx.svg'  # the default value is .docx
      end

      full_path
    end

    # get files information
    def get_files_info(file_id)
      result = [];

      for fileName in get_stored_files(nil)  # run through all the stored files from the folder
        directory = storage_path(fileName, nil)
        uri = cur_user_host_address(nil) + '/' + fileName

        # write file parameters to the info object
        info = {
          "version" => get_file_version(history_dir(directory)),
          "id" => ServiceConverter.generate_revision_id("#{uri}.#{File.mtime(directory).to_s}"),
          "contentLength" => "#{(File.size(directory)/ 1024.0).round(2)} KB",
          "pureContentLength" => File.size(directory),
          "title" => fileName,
          "updated" => File.mtime(directory) 
        }

        if file_id == nil  # if file id is undefined
          result.push(info)  # push info object to the response array
        else  # if file id is defined
          if file_id.eql?(info["id"])  # and it is equal to the document key value
            result.push(info)  # response object will be equal to the info object
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
    # enable ignore certificate
    def verify_ssl(file_uri, http)
      if file_uri.start_with?('https') && DocumentHelper.config_manager.ssl_verify_peer_mode_enabled
        http.use_ssl = true
        http.verify_mode = OpenSSL::SSL::VERIFY_NONE  # set the flags for the server certificate verification at the beginning of SSL session
      end
    end
  end

end