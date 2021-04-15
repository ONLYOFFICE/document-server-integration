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

class TrackHelper

    class << self

        # read the request body
        def read_body(request)
            body = request.body.read

            if body == nil || body.empty?
                return ""
            end

            file_data = JSON.parse(body)  # parse file data

            # check if a secret key to generate token exists or not
            if JwtHelper.is_enabled
                inHeader = false
                token = nil
                jwtHeader = Rails.configuration.header.empty? ? "Authorization" : Rails.configuration.header;  # get the authorization header from the config
                if file_data["token"]  # if the token is in the body
                    token = JwtHelper.decode(file_data["token"])  # decode a token into a payload object using a secret key
                elsif request.headers[jwtHeader]  # if the token is in the header
                    hdr = request.headers[jwtHeader]
                    hdr.slice!(0, "Bearer ".length)  # get token from it (after Bearer prefix)
                    token = JwtHelper.decode(hdr)  # decode a token into a payload object using a secret key
                    inHeader = true
                else
                    raise "Expected JWT"  # token missing error message
                end

                if !token
                    raise "Invalid JWT signature"
                end

                file_data = JSON.parse(token)

                if inHeader
                    file_data = file_data["payload"]
                end
            end

            return file_data
        end

        # file saving process
        def process_save(file_data, file_name, user_address)
            download_uri = file_data['url']
            new_file_name = file_name

            cur_ext = File.extname(file_name)  # get current file extension
            download_ext = File.extname(download_uri)  # get the extension of the downloaded file

            # convert downloaded file to the file with the current extension if these extensions aren't equal
            if (!cur_ext.eql?(download_ext))
                key = ServiceConverter.generate_revision_id(download_uri)  # get the document key
                begin
                    percent, new_file_uri = ServiceConverter.get_converted_uri(download_uri, download_ext.delete('.'), cur_ext.delete('.'), key, false)  # get the url of the converted file
                    if (new_file_uri == nil || new_file_uri.empty?)
                        new_file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + download_ext, user_address)  # get the correct file name if it already exists
                    else
                        download_uri = new_file_uri
                    end
                rescue StandardError => msg
                    new_file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + download_ext, user_address)
                end
            end

            saved = 1
            begin
                storage_path = DocumentHelper.storage_path(new_file_name, user_address)  # get the storage directory of the new file

                hist_dir = DocumentHelper.history_dir(storage_path)  # get the history directory of the new file
                ver_dir = DocumentHelper.version_dir(hist_dir, DocumentHelper.get_file_version(hist_dir))  # get the path to the specified file version

                FileUtils.mkdir_p(ver_dir)  # create the version directory if doesn't exist

                FileUtils.move(DocumentHelper.storage_path(file_name, user_address), File.join(ver_dir, "prev#{cur_ext}"))  # move the file from the storage directory to the previous file version directory
                save_from_uri(storage_path, download_uri)  # save the downloaded file to the storage directory

                if (file_data["changesurl"])  # if the changesurl is in the body
                    save_from_uri(File.join(ver_dir, "diff.zip"), file_data["changesurl"])  # get the information from this url to the file with document versions differences
                end

                hist_data = file_data["changeshistory"]
                if (!hist_data)  # if there are no changes in the history
                    hist_data = file_data["history"].to_json  # write the original history information to the history data
                end
                if (hist_data)
                    File.open(File.join(ver_dir, "changes.json"), 'wb') do |file|  # open the file with document changes
                        file.write(hist_data)  # and write history data to this file
                    end
                end

                # write the key value to the key.txt file
                File.open(File.join(ver_dir, "key.txt"), 'wb') do |file|
                    file.write(file_data["key"])
                end

                forcesave_path = DocumentHelper.forcesave_path(new_file_name, user_address, false)  # get the path to the forcesaved file
                if (!forcesave_path.eql?(""))  # if this path is empty
                    File.delete(forcesave_path)  # remove it
                end

                saved = 0
            rescue StandardError => msg
                saved = 1
            end

            return saved
        end

        # file force saving process
        def process_force_save(file_data, file_name, user_address)  
            download_uri = file_data['url'] 

            cur_ext = File.extname(file_name)  # get current file extension
            download_ext = File.extname(download_uri)  # get the extension of the downloaded file
            new_file_name = file_name

            # convert downloaded file to the file with the current extension if these extensions aren't equal
            if (!cur_ext.eql?(download_ext))
                key = ServiceConverter.generate_revision_id(download_uri)  # get the document key
                begin
                    percent, new_file_uri = ServiceConverter.get_converted_uri(download_uri, download_ext.delete('.'), cur_ext.delete('.'), key, false)  # get the url of the converted file
                    if (new_file_uri == nil || new_file_uri.empty?)
                        new_file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + download_ext, user_address)  # get the correct file name if it already exists
                    else
                        download_uri = new_file_uri
                    end
                rescue StandardError => msg
                    new_file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + download_ext, user_address)
                end
            end

            saved = 1
            begin
                is_submit_form = file_data["forcesavetype"].to_i == 3  # check if the forcesave type is equal to 3 (the form was submitted)

                if (is_submit_form)
                    if (new_file_name.eql?(file_name))
                        new_file_name = DocumentHelper.get_correct_name(file_name, user_address)  # get the correct file name if it already exists
                    end
                    forcesave_path = DocumentHelper.storage_path(new_file_name, user_address)  # get the path to the new file
                else
                    forcesave_path = DocumentHelper.forcesave_path(new_file_name, user_address, false)
                    if (forcesave_path.eql?(""))
                        forcesave_path = DocumentHelper.forcesave_path(new_file_name, user_address, true)  # if the path to the new file doesn't exist, create it
                    end
                end

                save_from_uri(forcesave_path, download_uri)  # download the form

                if (is_submit_form)
                    uid = file_data['actions'][0]['userid']
                    DocumentHelper.create_meta(new_file_name, uid, "Filling Form", user_address)  # create file meta information with the Filling form tag instead of user name 
                end

                saved = 0
            rescue StandardError => msg
                saved = 1
            end

            return saved
        end

        # send the command request
        def command_request(method, key)
            document_command_url = Rails.configuration.urlSite + Rails.configuration.commandUrl  # get the document command url

            # create a payload object with the method and key
            payload = {
                :c => method,
                :key => key
              }

            data = nil
            begin

                uri = URI.parse(document_command_url)  # parse the document command url
                http = Net::HTTP.new(uri.host, uri.port)  # create a connection to the http server

                if document_command_url.start_with?('https')  # check if the documnent command url starts with https
                    http.use_ssl = true
                    http.verify_mode = OpenSSL::SSL::VERIFY_NONE  # set the flags for the server certificate verification at the beginning of SSL session
                end

                req = Net::HTTP::Post.new(uri.request_uri)  # create the post request
                req.add_field("Content-Type", "application/json")  # set headers

                if JwtHelper.is_enabled  # if the signature is enabled
                    payload["token"] = JwtHelper.encode(payload)  # get token and save it to the payload
                    jwtHeader = Rails.configuration.header.empty? ? "Authorization" : Rails.configuration.header;  # get signature authorization header
                    req.add_field(jwtHeader, "Bearer #{JwtHelper.encode({ :payload => payload })}")  # set it to the request with the Bearer prefix
                end

                req.body = payload.to_json   # convert the payload object into the json format
                res = http.request(req)  # get the response
                data = res.body  # and take its body
            rescue => ex
                raise ex.message
            end

            json_data = JSON.parse(data)  # convert the response body into the json format
            return json_data
        end

        # save file from the url
        def save_from_uri(path, uristr)
            uri = URI.parse(uristr)  # parse the url string
            http = Net::HTTP.new(uri.host, uri.port)  # create a connection to the http server

            if uristr.start_with?('https')  # check if the documnent command url starts with https
              http.use_ssl = true
              http.verify_mode = OpenSSL::SSL::VERIFY_NONE  # set the flags for the server certificate verification at the beginning of SSL session
            end

            req = Net::HTTP::Get.new(uri)
            res = http.request(req)  # get the response
            data = res.body  # and take its body

            if data == nil
              raise 'stream is null'
            end

            File.open(path, 'wb') do |file|  # open the file from the path specified
              file.write(data)  # and write the response data to it
            end
        end
    end
end