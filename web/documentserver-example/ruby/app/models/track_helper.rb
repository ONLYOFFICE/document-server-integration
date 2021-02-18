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

        def read_body(request)
            body = request.body.read

            if body == nil || body.empty?
                return ""
            end

            file_data = JSON.parse(body)

            if JwtHelper.is_enabled
                inHeader = false
                token = nil
                jwtHeader = Rails.configuration.header.empty? ? "Authorization" : Rails.configuration.header;
                if file_data["token"]
                    token = JwtHelper.decode(file_data["token"])
                elsif request.headers[jwtHeader]
                    hdr = request.headers[jwtHeader]
                    hdr.slice!(0, "Bearer ".length)
                    token = JwtHelper.decode(hdr)
                    inHeader = true
                else
                    raise "Expected JWT"
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

        def process_save(file_data, file_name, user_address)
            download_uri = file_data['url']
            new_file_name = file_name

            cur_ext = File.extname(file_name)
            download_ext = File.extname(download_uri)

            if (!cur_ext.eql?(download_ext))
                key = ServiceConverter.generate_revision_id(download_uri)
                begin
                    percent, new_file_uri = ServiceConverter.get_converted_uri(download_uri, download_ext.delete('.'), cur_ext.delete('.'), key, false)
                    if (new_file_uri == nil || new_file_uri.empty?)
                        new_file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + download_ext)
                    else
                        download_uri = new_file_uri
                    end
                rescue StandardError => msg
                    new_file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + download_ext)
                end
            end

            saved = 1
            begin
                storage_path = DocumentHelper.storage_path(new_file_name, user_address)

                hist_dir = DocumentHelper.history_dir(storage_path)
                ver_dir = DocumentHelper.version_dir(hist_dir, DocumentHelper.get_file_version(hist_dir))

                FileUtils.mkdir_p(ver_dir)

                FileUtils.move(DocumentHelper.storage_path(file_name, user_address), File.join(ver_dir, "prev#{cur_ext}"))
                save_from_uri(storage_path, download_uri)

                if (file_data["changesurl"])
                    save_from_uri(File.join(ver_dir, "diff.zip"), file_data["changesurl"])
                end

                hist_data = file_data["changeshistory"]
                if (!hist_data)
                    hist_data = file_data["history"].to_json
                end
                if (hist_data)
                    File.open(File.join(ver_dir, "changes.json"), 'wb') do |file|
                        file.write(hist_data)
                    end
                end

                File.open(File.join(ver_dir, "key.txt"), 'wb') do |file|
                    file.write(file_data["key"])
                end

                forcesave_path = DocumentHelper.forcesave_path(new_file_name, user_address, false)
                if (!forcesave_path.eql?(""))
                    File.delete(forcesave_path)
                end

                saved = 0
            rescue StandardError => msg
                saved = 1
            end

            return saved
        end

        def process_force_save(file_data, file_name, user_address)  
            download_uri = file_data['url'] 

            cur_ext = File.extname(file_name)
            download_ext = File.extname(download_uri)

            if (!cur_ext.eql?(download_ext))
                key = ServiceConverter.generate_revision_id(download_uri)
                begin
                    percent, new_file_uri = ServiceConverter.get_converted_uri(download_uri, download_ext.delete('.'), cur_ext.delete('.'), key, false)
                    if (new_file_uri == nil || new_file_uri.empty?)
                        file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + download_ext)
                    else
                        download_uri = new_file_uri
                    end
                rescue StandardError => msg
                    file_name = DocumentHelper.get_correct_name(File.basename(file_name, cur_ext) + download_ext)
                end
            end

            saved = 1
            begin
                forcesave_path = DocumentHelper.forcesave_path(file_name, user_address, false)
                if (forcesave_path.eql?(""))
                    forcesave_path = DocumentHelper.forcesave_path(file_name, user_address, true)
                end

                save_from_uri(forcesave_path, download_uri)

                saved = 0
            rescue StandardError => msg
                saved = 1
            end

            return saved
        end

        def command_request(method, key)
            document_command_url = Rails.configuration.urlSite + Rails.configuration.commandUrl

            payload = {
                :c => method,
                :key => key
              }

            data = nil
            begin

                uri = URI.parse(document_command_url)
                http = Net::HTTP.new(uri.host, uri.port)

                if document_command_url.start_with?('https')
                    http.use_ssl = true
                    http.verify_mode = OpenSSL::SSL::VERIFY_NONE
                end

                req = Net::HTTP::Post.new(uri.request_uri)
                req.add_field("Content-Type", "application/json")

                if JwtHelper.is_enabled
                    payload["token"] = JwtHelper.encode(payload)
                    jwtHeader = Rails.configuration.header.empty? ? "Authorization" : Rails.configuration.header;
                    req.add_field(jwtHeader, "Bearer #{JwtHelper.encode({ :payload => payload })}")
                end

                req.body = payload.to_json
                res = http.request(req)
                data = res.body
            rescue => ex
                raise ex.message
            end

            json_data = JSON.parse(data)
            return json_data
        end

        def save_from_uri(path, uristr)
            uri = URI.parse(uristr)
            http = Net::HTTP.new(uri.host, uri.port)

            if uristr.start_with?('https')
              http.use_ssl = true
              http.verify_mode = OpenSSL::SSL::VERIFY_NONE
            end

            req = Net::HTTP::Get.new(uri)
            res = http.request(req)
            data = res.body

            if data == nil
              raise 'stream is null'
            end

            File.open(path, 'wb') do |file|
              file.write(data)
            end
        end
    end
end