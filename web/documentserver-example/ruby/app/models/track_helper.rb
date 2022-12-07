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

                if !token || token.eql?("")
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
            if download_uri.eql?(nil)
                saved = 1
                return saved
            end

            data = download_file(download_uri) # download document file
            if data.eql?(nil)
                saved = 1
                return saved
            end

            new_file_name = file_name
            download_ext = "."+file_data['filetype']  # get the extension of the downloaded file

            # TODO [Delete in version 7.0 or higher]
            if (download_ext == ".")
                download_ext = File.extname(download_uri).downcase; # Support for versions below 7.0
            end

            cur_ext = File.extname(file_name).downcase  # get current file extension

            # convert downloaded file to the file with the current extension if these extensions aren't equal
            unless cur_ext.eql?(download_ext)
                key = ServiceConverter.generate_revision_id(download_uri) # get the document key
                begin
                    percent, new_file_uri = ServiceConverter.get_converted_uri(download_uri, download_ext.delete('.'), cur_ext.delete('.'), key, false, nil) # get the url of the converted file
                    if new_file_uri == nil || new_file_uri.empty?
                        new_file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + download_ext, user_address) # get the correct file name if it already exists
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

                FileUtils.copy(DocumentHelper.storage_path(file_name, user_address), File.join(ver_dir, "prev#{cur_ext}"))  # copy the file from the storage directory to the previous file version directory
                is_saved = save_file(data, storage_path)  # save the downloaded file to the storage directory
                if is_saved == 1
                    return is_saved
                end

                if file_data["changesurl"] # if the changesurl is in the body
                    change_data = download_file(file_data["changesurl"]) # download file with document versions differences
                    save_file(change_data, File.join(ver_dir, "diff.zip")) # save file with document versions differences
                end

                hist_data = file_data["changeshistory"]
                unless hist_data # if there are no changes in the history
                    hist_data = file_data["history"].to_json # write the original history information to the history data
                end
                if hist_data
                    File.open(File.join(ver_dir, "changes.json"), 'wb') do |file|  # open the file with document changes
                        file.write(hist_data)  # and write history data to this file
                    end
                end

                # write the key value to the key.txt file
                File.open(File.join(ver_dir, "key.txt"), 'wb') do |file|
                    file.write(file_data["key"])
                end

                forcesave_path = DocumentHelper.forcesave_path(new_file_name, user_address, false)  # get the path to the forcesaved file
                unless forcesave_path.eql?("") # if this path is empty
                    File.delete(forcesave_path) # remove it
                end

                saved = 0
            rescue StandardError => msg
                saved = 1
            end

            saved
        end

        # file force saving process
        def process_force_save(file_data, file_name, user_address)  
            download_uri = file_data['url']
            if download_uri.eql?(nil)
                saved = 1
                return saved
            end

            data = download_file(download_uri) # download document file
            if data.eql?(nil)
                saved = 1
                return saved
            end
            download_ext = "."+file_data['filetype']  # get the extension of the downloaded file

            # TODO [Delete in version 7.0 or higher]
            if download_ext == "."
                download_ext = File.extname(download_uri).downcase; # Support for versions below 7.0
            end

            cur_ext = File.extname(file_name).downcase  # get current file extension

            new_file_name = false

            # convert downloaded file to the file with the current extension if these extensions aren't equal
            unless cur_ext.eql?(download_ext)
                key = ServiceConverter.generate_revision_id(download_uri) # get the document key
                begin
                    percent, new_file_uri = ServiceConverter.get_converted_uri(download_uri, download_ext.delete('.'), cur_ext.delete('.'), key, false, nil) # get the url of the converted file
                    if new_file_uri == nil || new_file_uri.empty?
                        new_file_name = true
                    else
                        download_uri = new_file_uri
                    end
                rescue StandardError => msg
                    new_file_name = true
                end
            end

            saved = 1
            begin
                is_submit_form = file_data["forcesavetype"].to_i == 3  # check if the forcesave type is equal to 3 (the form was submitted)

                if is_submit_form
                    if new_file_name
                        file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + "-form" + download_ext, user_address)  # get the correct file name if it already exists
                    else
                        file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + "-form" + cur_ext, user_address)
                    end
                    forcesave_path = DocumentHelper.storage_path(file_name, user_address)  # get the path to the new file
                else
                    if new_file_name
                        file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + download_ext, user_address)
                    end
                    forcesave_path = DocumentHelper.forcesave_path(file_name, user_address, false)
                    if forcesave_path.eql?("")
                        forcesave_path = DocumentHelper.forcesave_path(file_name, user_address, true)  # if the path to the new file doesn't exist, create it
                    end
                end

                is_saved = save_file(data, forcesave_path)  # save the downloaded file to the storage directory
                if is_saved == 1
                    return is_saved
                end

                if is_submit_form
                    uid = file_data['actions'][0]['userid']
                    DocumentHelper.create_meta(file_name, uid, "Filling Form", user_address)  # create file meta information with the Filling form tag instead of user name 
                end

                saved = 0
            rescue StandardError => msg
                saved = 1
            end

            saved
        end

        # send the command request
        def command_request(method, key, meta = nil)
            document_command_url = Rails.configuration.urlSite + Rails.configuration.commandUrl  # get the document command url

            # create a payload object with the method and key
            payload = {
                :c => method,
                :key => key
            }

            if (meta != nil)
                payload.merge!({:meta => meta})
            end

            data = nil
            begin

                uri = URI.parse(document_command_url)  # parse the document command url
                http = Net::HTTP.new(uri.host, uri.port)  # create a connection to the http server

                DocumentHelper.verify_ssl(document_command_url, http)

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
        def download_file(uristr)
            uri = URI.parse(uristr)  # parse the url string
            http = Net::HTTP.new(uri.host, uri.port)  # create a connection to the http server
            http.open_timeout = 5

            DocumentHelper.verify_ssl(uristr, http)

            req = Net::HTTP::Get.new(uri)
            res = http.request(req)  # get the response

            status_code = res.code
            if status_code != '200'  # checking status code
                raise "Document editing service returned status: #{status_code}"
            end
            data = res.body  # and take its body

            if data == nil
                raise 'stream is null'
            end
            data
        end

        def save_file(data, path)
            begin
                File.open(path, 'wb') do |file|  # open the file from the path specified
                    file.write(data)  # and write the response data to it
                end
                return 0
            rescue
                return 1
            end
        end
    end
end