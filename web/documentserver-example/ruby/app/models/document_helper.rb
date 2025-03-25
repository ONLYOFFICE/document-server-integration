# frozen_string_literal: true

#
# (c) Copyright Ascensio System SIA 2025
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

# Class that provides various utility methods related to documents.
class DocumentHelper
  @config_manager = ConfigurationManager.new
  @format_manager = FormatManager.new

  class << self
    attr_reader :config_manager
    attr_reader :format_manager
  end

  @runtime_cache = {}
  @remote_ip = nil
  @base_url = nil

  def self.init(ip, url)
    @remote_ip = ip
    @base_url = url
  end

  # define max file size
  def self.file_size_max
    DocumentHelper.config_manager.maximum_file_size
  end

  # all the supported file extensions
  def self.file_exts
    DocumentHelper.format_manager.all_extensions
  end

  def self.fill_forms_exts
    DocumentHelper.format_manager.fillable_extensions
  end

  # file extensions that can be viewed
  def self.viewed_exts
    DocumentHelper.format_manager.viewable_extensions
  end

  # file extensions that can be edited
  def self.edited_exts
    DocumentHelper.format_manager.editable_extensions
  end

  # file extensions that can be converted
  def self.convert_exts
    DocumentHelper.format_manager.convertible_extensions
  end

  # get current user host address
  def self.cur_user_host_address(user_address)
    (user_address.nil? ? @remote_ip : user_address).gsub(/[^0-9\-.a-zA-Z_=]/, '_')
  end

  # get the storage path of the given file
  def self.storage_path(file_name, user_address)
    directory = DocumentHelper.config_manager.storage_path.join(cur_user_host_address(user_address))

    # create a new directory if it doesn't exist
    FileUtils.mkdir_p(directory) unless File.directory?(directory)

    # put the given file to this directory
    File.join(directory, File.basename(file_name))
  end

  # get the path to the forcesaved file version
  def self.forcesave_path(file_name, user_address, create)
    directory = DocumentHelper.config_manager.storage_path.join(cur_user_host_address(user_address))

    # the directory with host address doesn't exist
    return '' unless File.directory?(directory)

    # get the path to the history of the given file
    directory = File.join(directory, "#{File.basename(file_name)}-hist")
    unless File.directory?(directory)
      return '' unless create

      FileUtils.mkdir_p(directory) # create history directory if it doesn't exist

      # the history directory doesn't exist and we are not supposed to create it

    end

    directory = File.join(directory, File.basename(file_name)) # get the path to the given file
    return '' if !File.file?(directory) && !create

    directory.to_s
  end

  # get the path to the file history
  def self.history_dir(storage_path)
    directory = "#{storage_path}-hist"

    # create history directory if it doesn't exist
    FileUtils.mkdir_p(directory) unless File.directory?(directory)

    directory
  end

  # get the path to the specified file version
  def self.version_dir(hist_dir, ver)
    File.join(hist_dir, ver.to_s)
  end

  # get the last file version
  def self.get_file_version(hist_dir)
    return 1 unless Dir.exist?(hist_dir)

    ver = 1
    Dir.foreach(hist_dir) do |e| # run through all the file versions
      next if e.eql?('.')
      next if e.eql?('..')

      if File.directory?(File.join(hist_dir, e))
        ver += 1 # and count them
      end
    end

    ver
  end

  # get the correct file name if such a name already exists
  def self.get_correct_name(file_name, user_address)
    max_name = 50
    ext = File.extname(file_name) # get file extension
    # get file name without extension
    base_name = File.basename(file_name, ext)[0...max_name] + (file_name.length > max_name ? '[...]' : '')
    name = base_name + ext.downcase # get full file name
    index = 1

    # if the file with such a name already exists in this directory
    while File.exist?(storage_path(name, user_address))
      name = "#{base_name} (#{index})#{ext.downcase}" # add an index after its base name
      index += 1
    end

    name
  end

  # get all the stored files from the folder
  def self.get_stored_files(user_address)
    directory = DocumentHelper.config_manager.storage_path.join(cur_user_host_address(user_address))

    arr = []

    if Dir.exist?(directory)
      Dir.foreach(directory) do |e| # run through all the elements from the folder
        next if e.eql?('.')
        next if e.eql?('..')
        next if File.directory?(File.join(directory, e)) # if the element is a directory, skip it

        file_path = File.join(directory, e)
        arr.push([e, File.mtime(file_path)]) # push the file name and its modification time to the array
      end
    end

    arr.sort_by { |_, mtime| -mtime.to_i } # rubocop:disable Lint/NumberConversion
       .map(&:first)
  end

  # create file meta information
  def self.create_meta(file_name, uid, uname, user_address)
    hist_dir = history_dir(storage_path(file_name, user_address)) # get the path to the file history

    # write user name, user uid and the creation time to the json object
    json = {
      created: Time.zone.now.to_fs(:db),
      uid:,
      uname:
    }

    # write file meta information to the createdInfo.json file
    File.binwrite(File.join(hist_dir, 'createdInfo.json'), json.to_json)
  end

  # create demo document
  def self.create_demo(file_ext, sample, user)
    demo_name = (sample == 'true' ? 'sample.' : 'new.') + file_ext
    file_name = get_correct_name(demo_name, nil) # get the correct file name if such a name already exists

    # save sample document of a necessary extension to the storage directory
    src = Rails.root.join('assets', 'document-templates', sample == 'true' ? 'sample' : 'new', demo_name)
    dest = storage_path(file_name, nil)

    FileUtils.cp(src, dest)

    # save file meta data to the file

    create_meta(file_name, user.id, user.name, nil)

    file_name
  end

  # get file url
  def self.get_file_uri(file_name, for_document_server)
    "#{get_server_url(for_document_server)}/" \
      "#{DocumentHelper.config_manager.storage_path}/" \
      "#{cur_user_host_address(nil)}/" \
      "#{ERB::Util.url_encode(file_name)}"
  end

  # get history path url
  def self.get_historypath_uri(file_name, version, file, is_server_url: true)
    # for redirection to my link
    user_host = is_server_url ? "&userAddress=#{cur_user_host_address(nil)}" : ''
    "#{get_server_url(is_server_url)}/downloadhistory/?" \
      "fileName=#{ERB::Util.url_encode(file_name)}&ver=#{version}" \
      "&file=#{ERB::Util.url_encode(file)}#{user_host}"
  end

  # get server url
  def self.get_server_url(for_document_server)
    return DocumentHelper.config_manager.example_uri.to_s if
      for_document_server &&
      DocumentHelper.config_manager.example_uri

    @base_url
  end

  # get callback url
  def self.get_callback(file_name)
    "#{get_server_url(true)}/track?" \
      "fileName=#{ERB::Util.url_encode(file_name)}&" \
      "userAddress=#{cur_user_host_address(nil)}"
  end

  # get url to the created file
  def self.get_create_url(document_type)
    "#{get_server_url(false)}/sample?fileExt=#{get_internal_extension(document_type).delete('.')}"
  end

  # get url to download a file
  def self.get_download_url(file_name, is_server_url: true)
    user_host = is_server_url ? "&userAddress=#{cur_user_host_address(nil)}" : ''
    "#{get_server_url(is_server_url)}/download?fileName=#{ERB::Util.url_encode(file_name)}#{user_host}"
  end

  # get internal file extension by its type
  def self.get_internal_extension(file_type)
    case file_type
    when 'word'  # .docx for word type
      '.docx'
    when 'cell'  # .xlsx for cell type
      '.xlsx'
    when 'slide' # .pptx for slide type
      '.pptx'
    else
      '.docx' # the default value is .docx
    end
  end

  # get image url for templates
  def self.get_template_image_url(file_type)
    path = "#{get_server_url(false)}/assets/"
    case file_type
    when 'word'  # for word type
      "#{path}file_docx.svg"
    when 'cell'  # .xlsx for cell type
      "#{path}file_xlsx.svg"
    when 'slide' # .pptx for slide type
      "#{path}file_pptx.svg"
    else
      "#{path}file_docx.svg" # the default value is .docx
    end
  end

  # get files information
  def self.get_files_info(file_id)
    result = []

    get_stored_files(nil).each do |file_name| # run through all the stored files from the folder
      directory = storage_path(file_name, nil)
      uri = "#{cur_user_host_address(nil)}/#{file_name}"

      # write file parameters to the info object
      info = {
        version: get_file_version(history_dir(directory)),
        id: ServiceConverter.generate_revision_id("#{uri}.#{File.mtime(directory)}"),
        contentLength: "#{(File.size(directory) / 1024.0).round(2)} KB",
        pureContentLength: File.size(directory),
        title: file_name,
        updated: File.mtime(directory)
      }

      if file_id.nil? # if file id is undefined
        result.push(info) # push info object to the response array
      elsif file_id.eql?(info['id']) # if file id is defined
        result.push(info) # response object will be equal to the info object
        return result # and it is equal to the document key value
      end
    end

    return '"File not found"' unless file_id.nil?

    result
  end

  # enable ignore certificate
  def self.verify_ssl(file_uri, http)
    return unless file_uri.start_with?('https') && DocumentHelper.config_manager.ssl_verify_peer_mode_enabled

    http.use_ssl = true
    # set the flags for the server certificate verification at the beginning of SSL session
    http.verify_mode = OpenSSL::SSL::VERIFY_NONE
  end
end
