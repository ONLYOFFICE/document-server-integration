class FileModel

  attr_accessor :file_name, :mode, :type, :user_ip, :lang, :uid, :uname

  def initialize(attributes = {})
    @file_name = attributes[:file_name]
    @mode = attributes[:mode]
    @type = attributes[:type]
    @user_ip = attributes[:user_ip]
    @lang = attributes[:lang]
    @user_id = attributes[:uid]
    @user_name = attributes[:uname]
  end

  def type
    @type ? @type : "desktop"
  end

  def file_ext
    File.extname(@file_name)
  end

  def file_uri
    DocumentHelper.get_file_uri(@file_name)
  end

  def document_type
    FileUtility.get_file_type(@file_name)
  end

  def key
    uri = DocumentHelper.cur_user_host_address(nil) + '/' + @file_name
    stat = File.mtime(DocumentHelper.storage_path(@file_name, nil))
    return ServiceConverter.generate_revision_id("#{uri}.#{stat.to_s}")
  end

  def callback_url
    DocumentHelper.get_callback(@file_name)
  end

  def cur_user_host_address
    DocumentHelper.cur_user_host_address(nil)
  end

  def get_config
    editorsmode = @mode ? @mode : "edit"
    canEdit = DocumentHelper.edited_exts.include?(file_ext)
    mode = canEdit && editorsmode.eql?("view") ? "view" : "edit"

    config = {
      :type => type(),
      :documentType => document_type,
      :document => {
        :title => @file_name,
        :url => file_uri,
        :fileType => file_ext.delete("."),
        :key => key,
        :info => {
          :author => "Me",
          :created => Time.now.to_s,
        },
        :permissions => {
          :comment => !editorsmode.eql?("view") && !editorsmode.eql?("fillForms") && !editorsmode.eql?("embedded"),
          :download => true,
          :edit => canEdit && (editorsmode.eql?("edit") || editorsmode.eql?("filter")),
          :fillForms => !editorsmode.eql?("view") && !editorsmode.eql?("comment") && !editorsmode.eql?("embedded"),
          :modifyFilter => !editorsmode.eql?("filter"),
          :review => editorsmode.eql?("edit") || editorsmode.eql?("review")
        }
      },
      :editorConfig => {
        :mode => mode,
        :lang => @lang ? @lang : "en",
        :callbackUrl => callback_url,
        :user => {
          :id => @user_id ? @user_id : "uid-0",
          :name => @user_name ? @user_name : "Jonn Smith"
        },
        :embedded => {
          :saveUrl => file_uri,
          :embedUrl => file_uri,
          :shareUrl => file_uri,
          :toolbarDocked => "top"
        },
      }
    }

    if JwtHelper.is_enabled
      config["token"] = JwtHelper.encode(config)
    end

    return config
  end

  def get_history
    file_name = @file_name
    file_ext = File.extname(file_name)
    doc_key = key()
    doc_uri = file_uri()

    hist_dir = DocumentHelper.history_dir(DocumentHelper.storage_path(@file_name, nil))
    cur_ver = DocumentHelper.get_file_version(hist_dir)

    if (cur_ver > 0)
      hist = []
      histData = {}

      for i in 0..cur_ver
        obj = {}
        dataObj = {}
        ver_dir = DocumentHelper.version_dir(hist_dir, i + 1)

        cur_key = doc_key
        if (i != cur_ver)
          File.open(File.join(ver_dir, "key.txt"), 'r') do |file|
            cur_key = file.read()
          end
        end
        obj["key"] = cur_key
        obj["version"] = i

        if (i == 0)
          File.open(File.join(hist_dir, "createdInfo.json"), 'r') do |file|
            cr_info = JSON.parse(file.read())

            obj["created"] = cr_info["created"]
            obj["user"] = {
              :id => cr_info["created"],
              :name => cr_info["name"]
            }
          end
        end

        dataObj["key"] = cur_key
        dataObj["url"] = i == cur_ver ? doc_uri : DocumentHelper.get_path_uri(File.join("#{file_name}-hist", i.to_s, "prev#{file_ext}"))
        dataObj["version"] = i

        if (i > 0)
          changes = nil
          File.open(File.join(DocumentHelper.version_dir(hist_dir, i), "changes.json"), 'r') do |file|
            changes = JSON.parse(file.read())
          end

          change = changes["changes"][0]

          obj["changes"] = changes["changes"]
          obj["serverVersion"] = changes["serverVersion"]
          obj["created"] = change["created"]
          obj["user"] = change["user"]

          prev = histData[(i-1).to_s]
          dataObj["previous"] = {
            :key => prev["key"],
            :url => prev["url"]
          }

          dataObj["changesUrl"] = DocumentHelper.get_path_uri(File.join("#{file_name}-hist", i.to_s, "diff.zip"))
        end

        hist.push(obj)
        histData[i.to_s] = dataObj
      end

      return {
        :hist => {
          :currentVersion => cur_ver,
          :history => hist
        },
        :histData => histData
      }
    end

    return nil

  end

end