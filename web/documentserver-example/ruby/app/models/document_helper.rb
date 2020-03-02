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

      directory.join(file_name).to_s
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

      ver = 0
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
        :created => Time.now.to_s,
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
      src = Rails.root.join('public', 'samples', demo_name)
      dest = storage_path file_name, nil

      FileUtils.cp src, dest

      create_meta(file_name, uid, uname)

      file_name
    end

    def get_file_uri(file_name)
      uri = @@base_url + '/' + Rails.configuration.storagePath + '/' + cur_user_host_address(nil) + '/' + URI::encode(file_name)

      return uri
    end

    def get_path_uri(path)
      uri = @@base_url + '/' + Rails.configuration.storagePath + '/' + cur_user_host_address(nil) + '/' + path

      return uri
    end

    def get_callback(file_name)

      @@base_url + '/track?type=track&fileName=' + URI::encode(file_name)  + '&userAddress=' + cur_user_host_address(nil)

    end

    def get_internal_extension(file_type)

      case file_type
        when 'text'
          ext = '.docx'
        when 'spreadsheet'
          ext = '.xlsx'
        when 'presentation'
          ext = '.pptx'
        else
          ext = '.docx'
      end

      ext
    end

  end

end