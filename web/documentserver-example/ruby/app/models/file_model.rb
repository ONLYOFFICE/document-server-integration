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

end