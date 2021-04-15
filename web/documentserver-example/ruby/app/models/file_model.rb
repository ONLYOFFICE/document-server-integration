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

class FileModel

  attr_accessor :file_name, :mode, :type, :user_ip, :lang, :uid, :uname, :action_data

  # set file parameters
  def initialize(attributes = {})
    @file_name = attributes[:file_name]
    @mode = attributes[:mode]
    @type = attributes[:type]
    @user_ip = attributes[:user_ip]
    @lang = attributes[:lang]
    @user_id = attributes[:uid]
    @user_name = attributes[:uname]
    @action_data = attributes[:action_data]
  end

  def type
    @type ? @type : "desktop"  # the default platform type is desktop
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
    DocumentHelper.get_file_uri(@file_name, false)
  end

  # get document type from its name (word, cell or slide)
  def document_type
    FileUtility.get_file_type(@file_name)
  end

  # generate the document key value
  def key
    uri = DocumentHelper.cur_user_host_address(nil) + '/' + @file_name  # get current user host address
    stat = File.mtime(DocumentHelper.storage_path(@file_name, nil))  # get the modification time of the given file
    return ServiceConverter.generate_revision_id("#{uri}.#{stat.to_s}")
  end

  # get callback url
  def callback_url
    DocumentHelper.get_callback(@file_name)
  end

  def create_url
    DocumentHelper.get_create_url(document_type)
  end

  # get current user host address
  def cur_user_host_address
    DocumentHelper.cur_user_host_address(nil)
  end

  # get config parameters
  def get_config
    editorsmode = @mode ? @mode : "edit"  # mode: view/edit/review/comment/fillForms/embedded
    canEdit = DocumentHelper.edited_exts.include?(file_ext)  # check if the document can be edited
    submitForm = canEdit && (editorsmode.eql?("edit") || editorsmode.eql?("fillForms"))  # the Submit form button state
    mode = canEdit && editorsmode.eql?("view") ? "view" : "edit"
    userId = @user_id ? @user_id : "uid-1"
    user_name = (userId.eql?("uid-0") ? nil : (@user_name ? @user_name : "John Smith"))
    userGroup = nil
    reviewGroups = nil
    if (userId == "uid-2")
        userGroup = "group-2"
        reviewGroups = ["group-2", ""]
    end 
    if (userId == "uid-3") 
        userGroup = "group-3"
        reviewGroups = ["group-2"]
    end

    config = {
      :type => type(),
      :documentType => document_type,
      :document => {
        :title => @file_name,
        :url => file_uri,
        :fileType => file_ext.delete("."),
        :key => key,
        :info => {
          :owner => "Me",
          :uploaded => Time.now.to_s,
          :favorite => @user_id ? @user_id.eql?("uid-2") : nil
        },
        :permissions => {  # the permission for the document to be edited and downloaded or not
          :comment => !editorsmode.eql?("view") && !editorsmode.eql?("fillForms") && !editorsmode.eql?("embedded") && !editorsmode.eql?("blockcontent"),
          :download => true,
          :edit => canEdit && (editorsmode.eql?("edit") || editorsmode.eql?("view") ||  editorsmode.eql?("filter") || editorsmode.eql?("blockcontent")),
          :fillForms => !editorsmode.eql?("view") && !editorsmode.eql?("comment") && !editorsmode.eql?("embedded") && !editorsmode.eql?("blockcontent"),
          :modifyFilter => !editorsmode.eql?("filter"),
          :modifyContentControl => !editorsmode.eql?("blockcontent"),
          :review => editorsmode.eql?("edit") || editorsmode.eql?("review"),
          :reviewGroups => reviewGroups
        }
      },
      :editorConfig => {
        :actionLink => @action_data ? JSON.parse(@action_data) : nil,
        :mode => mode,
        :lang => @lang ? @lang : "en",
        :callbackUrl => callback_url,  # absolute URL to the document storage service
        :createUrl => create_url,
        :user => {  # the user currently viewing or editing the document
          :id => userId,
          :name => user_name,
          :group => userGroup
        },
        :embedded => {  # the parameters for the embedded document type
          :saveUrl => file_uri_user,  # the absolute URL that will allow the document to be saved onto the user personal computer
          :embedUrl => file_uri_user,  # the absolute URL to the document serving as a source file for the document embedded into the web page
          :shareUrl => file_uri_user,  # the absolute URL that will allow other users to share this document
          :toolbarDocked => "top"  # the place for the embedded viewer toolbar (top or bottom)
        },
        :customization => {  # the parameters for the editor interface
          :forcesave => false,  # adding the request for the forced file saving to the callback handler
          :submitForm => submitForm  # the Submit form button state
        }
      }
    }

    if JwtHelper.is_enabled  # check if a secret key to generate token exists or not
      config["token"] = JwtHelper.encode(config)  # encode a payload object into a token and write it to the config
    end

    return config
  end

  # get document history
  def get_history
    file_name = @file_name
    file_ext = File.extname(file_name).downcase
    doc_key = key()
    doc_uri = file_uri()

    hist_dir = DocumentHelper.history_dir(DocumentHelper.storage_path(@file_name, nil))  # get the path to the file history
    cur_ver = DocumentHelper.get_file_version(hist_dir)  # get the file version

    if (cur_ver > 0)  # if file was modified
      hist = []
      histData = {}

      for i in 1..cur_ver  # run through all the file versions
        obj = {}
        dataObj = {}
        ver_dir = DocumentHelper.version_dir(hist_dir, i)  # get the path to the given file version

        # get document key
        cur_key = doc_key
        if (i != cur_ver)
          File.open(File.join(ver_dir, "key.txt"), 'r') do |file|
            cur_key = file.read()
          end
        end
        obj["key"] = cur_key
        obj["version"] = i

        if (i == 1)  # check if the version number is equal to 1
          if File.file?(File.join(hist_dir, "createdInfo.json"))  # check if the createdInfo.json file with meta data exists
            File.open(File.join(hist_dir, "createdInfo.json"), 'r') do |file|  # open it
              cr_info = JSON.parse(file.read())  # parse the file content

              # write information about changes to the object
              obj["created"] = cr_info["created"]
              obj["user"] = {
                :id => cr_info["uid"],
                :name => cr_info["uname"]
              }
            end
          end
        end

        # get the history data from the previous file version and write key and url information about it
        dataObj["key"] = cur_key
        dataObj["url"] = i == cur_ver ? doc_uri : DocumentHelper.get_path_uri(File.join("#{file_name}-hist", i.to_s, "prev#{file_ext}"))
        dataObj["version"] = i

        if (i > 1)  # check if the version number is greater than 1
          changes = nil
          File.open(File.join(DocumentHelper.version_dir(hist_dir, i - 1), "changes.json"), 'r') do |file|  # get the path to the changes.json file
            changes = JSON.parse(file.read())  # and parse its content
          end

          change = changes["changes"][0]

          # write information about changes to the object
          obj["changes"] = changes["changes"]
          obj["serverVersion"] = changes["serverVersion"]
          obj["created"] = change["created"]
          obj["user"] = change["user"]

          prev = histData[(i - 2).to_s]  # get the history data from the previous file version
          dataObj["previous"] = {  # write key and url information about previous file version
            :key => prev["key"],
            :url => prev["url"]
          }

          # write the path to the diff.zip archive with differences in this file version
          dataObj["changesUrl"] = DocumentHelper.get_path_uri(File.join("#{file_name}-hist", (i - 1).to_s, "diff.zip"))
        end

        if JwtHelper.is_enabled  # check if a secret key to generate token exists or not
          dataObj["token"] = JwtHelper.encode(dataObj)  # encode a payload object into a token and write it to the data object
        end

        hist.push(obj)  # add object dictionary to the hist list
        histData[(i - 1).to_s] = dataObj  # write data object information to the history data
      end

      return {
        :hist => {  # write history information about the current file version to the hist
          :currentVersion => cur_ver,
          :history => hist
        },
        :histData => histData
      }
    end

    return nil

  end

  # get image information
  def get_insert_image 
    insert_image = {
      :fileType => "png",  # image file type
      :url => DocumentHelper.get_server_url(true) + "/assets/logo.png"  # server url to the image
    }

    if JwtHelper.is_enabled  # check if a secret key to generate token exists or not
      insert_image["token"] = JwtHelper.encode(insert_image)  # encode a payload object into a token and write it to the insert_image object
    end

    return insert_image.to_json.tr("{", "").tr("}","")
  end

  # get compared file information
  def get_compare_file
    compare_file = {
      :fileType => "docx",  # file type
      :url => DocumentHelper.get_server_url(true) + "/assets/sample/sample.docx"  # server url to the compared file
    }

    if JwtHelper.is_enabled  # check if a secret key to generate token exists or not
      compare_file["token"] = JwtHelper.encode(compare_file)  # encode a payload object into a token and write it to the compare_file object
    end
    
    return compare_file
  end

  # get mail merge recipients information
  def dataMailMergeRecipients
    dataMailMergeRecipients = {
      :fileType => "csv",  # file type
      :url => DocumentHelper.get_server_url(true) + "/csv"  # server url to the mail merge recipients file
    }

    if JwtHelper.is_enabled  # check if a secret key to generate token exists or not
      dataMailMergeRecipients["token"] = JwtHelper.encode(dataMailMergeRecipients)  # encode a payload object into a token and write it to the dataMailMergeRecipients object
    end

    return dataMailMergeRecipients
  end

end