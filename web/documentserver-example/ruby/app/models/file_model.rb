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

# Class for handling file-related operations and information.
class FileModel
  attr_accessor :file_name
  attr_accessor :mode
  attr_accessor :type
  attr_accessor :user_ip
  attr_accessor :lang
  attr_accessor :user
  attr_accessor :action_data
  attr_accessor :direct_url
  attr_reader :config_manager

  # set file parameters
  def initialize(attributes = {})
    @file_name = attributes[:file_name]
    @mode = attributes[:mode]
    @type = attributes[:type] || 'desktop' # the default platform type is desktop
    @user_ip = attributes[:user_ip]
    @lang = attributes[:lang]
    @user = attributes[:user]
    @action_data = attributes[:action_data]
    @direct_url = attributes[:direct_url]
    @config_manager = ConfigurationManager.new
  end

  # get file extension from its name
  def file_ext
    File.extname(@file_name).downcase
  end

  # get file url
  def file_uri
    DocumentHelper.get_file_uri(@file_name, true)
  end

  # get file uri for document server
  def file_uri_user
    if @config_manager.storage_path.absolute?
      "#{download_url}&dmode=emb"
    else
      DocumentHelper.get_file_uri(
        @file_name,
        false
      )
    end
  end

  # get document type from its name (word, cell or slide)
  def document_type
    FileUtility.get_file_type(@file_name)
  end

  # generate the document key value
  def key
    uri = "#{DocumentHelper.cur_user_host_address(nil)}/#{@file_name}" # get current user host address
    stat = File.mtime(DocumentHelper.storage_path(@file_name, nil)) # get the modification time of the given file
    ServiceConverter.generate_revision_id("#{uri}.#{stat}")
  end

  # get callback url
  def callback_url
    DocumentHelper.get_callback(@file_name)
  end

  # get url to the created file
  def create_url
    DocumentHelper.get_create_url(document_type)
  end

  # get url to download a file
  def download_url(is_server_url: true)
    DocumentHelper.get_download_url(@file_name, is_server_url:)
  end

  # get current user host address
  def cur_user_host_address
    DocumentHelper.cur_user_host_address(nil)
  end

  # get config parameters
  def config
    editors_mode = @mode || 'edit' # mode: view/edit/review/comment/fillForms/embedded
    can_edit = DocumentHelper.edited_exts.include?(file_ext) # check if the document can be edited
    if ((!can_edit && editors_mode.eql?('edit')) || editors_mode.eql?('fillForms')) &&
       DocumentHelper.fill_forms_exts.include?(file_ext)
      editors_mode = 'fillForms'
      can_edit = true
    end
    submit_form = !editors_mode.eql?('view') && @user.id.eql?('uid-1') # Submit form button state
    mode = can_edit && !editors_mode.eql?('view') ? 'edit' : 'view'
    # templates image url in the "From Template" section
    templates_image_url = DocumentHelper.get_template_image_url(document_type)
    templates = [
      {
        image: '',
        title: 'Blank',
        url: create_url
      },
      {
        image: templates_image_url,
        title: 'With sample content',
        url: "#{create_url}&sample=true"
      }
    ]
    unless @user.goback.nil?
      @user.goback[:url] = DocumentHelper.get_server_url(false)
    end

    config = {
      type:,
      documentType: document_type,
      document: {
        title: @file_name,
        url: download_url,
        directUrl: enable_direct_url? ? download_url(is_server_url: false) : '',
        fileType: file_ext.delete('.'),
        key:,
        info: {
          owner: 'Me',
          uploaded: Time.zone.now.to_s,
          favorite: @user.favorite
        },
        permissions: { # the permission for the document to be edited and downloaded or not
          comment: ['view', 'fillForms', 'embedded', 'blockcontent'].exclude?(editors_mode),
          copy: @user.denied_permissions.exclude?('copy'),
          download: @user.denied_permissions.exclude?('download'),
          edit: can_edit && ['edit', 'view', 'filter', 'blockcontent'].include?(editors_mode),
          print: @user.denied_permissions.exclude?('print'),
          fillForms: ['view', 'comment', 'blockcontent'].exclude?(editors_mode),
          modifyFilter: !editors_mode.eql?('filter'),
          modifyContentControl: !editors_mode.eql?('blockcontent'),
          review: can_edit && (editors_mode.eql?('edit') || editors_mode.eql?('review')),
          chat: !@user.id.eql?('uid-0'),
          reviewGroups: @user.review_groups,
          commentGroups: @user.comment_groups,
          userInfoGroups: @user.user_info_groups,
          protect: @user.denied_permissions.exclude?('protect')
        },
        referenceData: {
          instanceId: DocumentHelper.get_server_url(false),
          fileKey: unless @user.id.eql?('uid-0')
                     {
                       fileName: @file_name,
                       userAddress: DocumentHelper.cur_user_host_address(nil)
                     }.to_json
                   end
        }
      },
      editorConfig: {
        actionLink: @action_data ? JSON.parse(@action_data) : nil,
        mode:,
        lang: @lang || 'en',
        callbackUrl: callback_url, # absolute URL to the document storage service
        coEditing: if editors_mode.eql?('view') && @user.id.eql?('uid-0')
                     {
                       mode: 'strict',
                       change: false
                     }
                   end,
        createUrl: @user.id.eql?('uid-0') ? nil : create_url,
        templates: @user.templates ? templates : nil,
        user: { # the user currently viewing or editing the document
          id: @user.id.eql?('uid-0') ? nil : @user.id,
          name: @user.name,
          group: @user.group,
          image: @user.avatar ? "#{DocumentHelper.get_server_url(false)}/assets/#{@user.id}.png" : nil
        },
        embedded: { # the parameters for the embedded document type
          # the absolute URL that will allow the document to be saved onto the user personal computer
          saveUrl: download_url(is_server_url: false),
          # the absolute URL to the document serving as a source file for the document embedded into the web page
          embedUrl: download_url(is_server_url: false),
          # the absolute URL that will allow other users to share this document
          shareUrl: download_url(is_server_url: false),
          toolbarDocked: 'top' # the place for the embedded viewer toolbar (top or bottom)
        },
        customization: { # the parameters for the editor interface
          about: true, # the About section display
          comments: true,
          feedback: true, # the Feedback & Support menu button display
          forcesave: false, # adding the request for the forced file saving to the callback handler
          submitForm: submit_form, # the Submit form button state
          goback: @user.goback.nil? ? '' : @user.goback,
          close: @user.close.nil? ? '' : @user.close
        }
      }
    }

    if JwtHelper.enabled? # check if a secret key to generate token exists or not
      config['token'] = JwtHelper.encode(config) # encode a payload object into a token and write it to the config
    end

    config
  end

  # get document history
  def history
    file_name = @file_name
    file_ext = File.extname(file_name).downcase
    doc_key = key
    file_uri

    # get the path to the file history
    hist_dir = DocumentHelper.history_dir(DocumentHelper.storage_path(@file_name, nil))
    cur_ver = DocumentHelper.get_file_version(hist_dir) # get the file version

    if cur_ver.positive? # if file was modified
      hist = []
      hist_data = {}

      (1..cur_ver).each do |i| # run through all the file versions
        obj = {}
        data_obj = {}
        ver_dir = DocumentHelper.version_dir(hist_dir, i) # get the path to the given file version

        # get document key
        cur_key = doc_key
        if i != cur_ver
          File.open(File.join(ver_dir, 'key.txt'), 'r') do |file|
            cur_key = file.read
          end
        end
        obj['key'] = cur_key
        obj['version'] = i

        # check if the createdInfo.json file with meta data exists
        if (i == 1) && File.file?(File.join(hist_dir, 'createdInfo.json'))
          File.open(File.join(hist_dir, 'createdInfo.json'), 'r') do |file| # open it
            cr_info = JSON.parse(file.read) # parse the file content

            # write information about changes to the object
            obj['created'] = cr_info['created']
            obj['user'] = {
              id: cr_info['uid'],
              name: cr_info['uname']
            }
          end
        end

        # get the history data from the previous file version and write key and url information about it
        data_obj['fileType'] = file_ext[1..file_ext.length]
        data_obj['key'] = cur_key
        data_obj['url'] =
          if i == cur_ver
            DocumentHelper.get_download_url(
              file_name
            )
          else
            DocumentHelper.get_historypath_uri(
              file_name,
              i,
              "prev#{file_ext}"
            )
          end
        if enable_direct_url? == true
          data_obj['directUrl'] =
            if i == cur_ver
              download_url(is_server_url: false)
            else
              DocumentHelper.get_historypath_uri(
                file_name,
                i,
                "prev#{file_ext}",
                false
              )
            end
        end
        data_obj['version'] = i

        if i > 1 # check if the version number is greater than 1
          changes = nil
          # get the path to the changes.json file
          File.open(File.join(DocumentHelper.version_dir(hist_dir, i - 1), 'changes.json'), 'r') do |file|
            changes = JSON.parse(file.read) # and parse its content
          end

          change = changes['changes'].last

          # write information about changes to the object
          obj['changes'] = change ? changes['changes'] : nil
          obj['serverVersion'] = changes['serverVersion']
          obj['created'] = change ? change['created'] : nil
          obj['user'] = change ? change['user'] : nil

          prev = hist_data[(i - 2).to_s] # get the history data from the previous file version
          # write key and url information about previous file version with optional direct url
          data_obj['previous'] = if enable_direct_url? == true
                                   { # write key and url information about previous file version with optional directUrl
                                     fileType: prev['fileType'],
                                     key: prev['key'],
                                     url: prev['url'],
                                     directUrl: prev['directUrl']
                                   }
                                 else
                                   {
                                     fileType: prev['fileType'],
                                     key: prev['key'],
                                     url: prev['url']
                                   }
                                 end

          diff_path = [hist_dir, (i - 1).to_s, 'diff.zip'].join(File::SEPARATOR)
          if File.exist?(diff_path)
            # write the path to the diff.zip archive with differences in this file version
            data_obj['changesUrl'] = DocumentHelper.get_historypath_uri(file_name, i - 1, 'diff.zip')
          end
        end

        if JwtHelper.enabled? # check if a secret key to generate token exists or not
          # encode a payload object into a token and write it to the data object
          data_obj['token'] = JwtHelper.encode(data_obj)
        end

        hist.push(obj) # add object dictionary to the hist list
        hist_data[(i - 1).to_s] = data_obj # write data object information to the history data
      end

      return {
        hist: { # write history information about the current file version to the hist
          currentVersion: cur_ver,
          history: hist
        },
        histData: hist_data
      }
    end

    nil
  end

  # get image information
  def insert_image
    # image file type
    # server url to the image
    # direct url to the image
    insert_image = if enable_direct_url? == true
                     {
                       fileType: 'svg', # image file type
                       url: "#{DocumentHelper.get_server_url(true)}/assets/logo.svg", # server url to the image
                       directUrl: "#{DocumentHelper.get_server_url(false)}/assets/logo.svg" # direct url to the image
                     }
                   else
                     {
                       fileType: 'svg', # image file type
                       url: "#{DocumentHelper.get_server_url(true)}/assets/logo.svg" # server url to the image
                     }
                   end

    if JwtHelper.enabled? # check if a secret key to generate token exists or not
      # encode a payload object into a token and write it to the insert_image object
      insert_image['token'] = JwtHelper.encode(insert_image)
    end

    insert_image.to_json.tr('{', '').tr('}', '')
  end

  # get compared file information
  def data_document
    # file type
    # server url to the compared file
    # direct url to the compared file
    compare_file = if enable_direct_url? == true
                     {
                       fileType: 'docx', # file type
                       # server url to the compared file
                       url: "#{DocumentHelper.get_server_url(true)}/asset?fileName=sample.docx",
                       # direct url to the compared file
                       directUrl: "#{DocumentHelper.get_server_url(false)}/asset?fileName=sample.docx"
                     }
                   else
                     {
                       fileType: 'docx', # file type
                       # server url to the compared file
                       url: "#{DocumentHelper.get_server_url(true)}/asset?fileName=sample.docx"
                     }
                   end

    if JwtHelper.enabled? # check if a secret key to generate token exists or not
      # encode a payload object into a token and write it to the compare_file object
      compare_file['token'] = JwtHelper.encode(compare_file)
    end

    compare_file
  end

  # get mail merge recipients information
  def data_spreadsheet
    # file type
    # server url to the mail merge recipients file
    # direct url to the mail merge recipients file
    data_spreadsheet = if enable_direct_url? == true
                         {
                           fileType: 'csv', # file type
                           # server url to the mail merge recipients file
                           url: "#{DocumentHelper.get_server_url(true)}/csv",
                           # direct url to the mail merge recipients file
                           directUrl: "#{DocumentHelper.get_server_url(false)}/csv"
                         }
                       else
                         {
                           fileType: 'csv', # file type
                           # server url to the mail merge recipients file
                           url: "#{DocumentHelper.get_server_url(true)}/csv"
                         }
                       end

    if JwtHelper.enabled? # check if a secret key to generate token exists or not
      # encode a payload object into a token and write it to the data_spreadsheet object
      data_spreadsheet['token'] = JwtHelper.encode(data_spreadsheet)
    end

    data_spreadsheet
  end

  # get users data for mentions
  def users_mentions
    @user.id.eql?('uid-0') ? nil : Users.get_users_for_mentions(@user.id)
  end

  def users_info
    users_info = []
    return if @user.id.eql?('uid-0')

    Users.all_users.each do |user_info|
      u = {
        id: user_info.id,
        name: user_info.name,
        email: user_info.email,
        group: user_info.group,
        reviewGroups: user_info.review_groups,
        commentGroups: user_info.comment_groups,
        userInfoGroups: user_info.user_info_groups,
        favorite: user_info.favorite,
        deniedPermissions: user_info.denied_permissions,
        descriptions: user_info.descriptions,
        templates: user_info.templates,
        avatar: user_info.avatar
      }
      u['image'] = user_info.avatar ? "#{DocumentHelper.get_server_url(false)}/assets/#{user_info.id}.png" : nil
      users_info.push(u)
    end
    users_info
  end

  # get users data for protect
  def users_protect
    @user.id.eql?('uid-0') ? nil : Users.get_users_for_protect(@user.id)
  end

  # get direct url existence flag
  def enable_direct_url?
    !@direct_url.nil? && @direct_url == 'true'
  end
end
