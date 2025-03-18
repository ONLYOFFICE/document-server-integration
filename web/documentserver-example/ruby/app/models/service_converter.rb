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

# Class responsible for converting documents using a document conversion service.
class ServiceConverter
  @config_manager = ConfigurationManager.new

  class << self
    attr_reader :config_manager
  end

  @convert_timeout = ServiceConverter.config_manager.convertation_timeout
  @document_converter_url = ServiceConverter.config_manager.document_server_converter_uri.to_s

  # get the url of the converted file
  def self.get_converted_data(
    document_uri,
    from_ext,
    to_ext,
    document_revision_id,
    is_async,
    file_pass,
    lang = nil,
    title = nil
  )
    from_ext = File.extname(document_uri).downcase if from_ext.nil? # get the current document extension

    # get the current document name or uuid
    title = File.basename(URI.parse(document_uri).path) if title.nil?
    title = UUID.generate.to_s if title.nil?

    # get the document key
    document_revision_id = document_uri if document_revision_id.empty?
    document_revision_id = generate_revision_id(document_revision_id)

    payload = { # write all the conversion parameters to the payload
      async: is_async ? true : false,
      url: document_uri,
      outputtype: to_ext.delete('.'),
      filetype: from_ext.delete('.'),
      title:,
      key: document_revision_id,
      password: file_pass,
      region: lang
    }

    data = nil
    begin
      uri = URI.parse(@document_converter_url) # create the request url
      http = Net::HTTP.new(uri.host, uri.port) # create a connection to the http server

      DocumentHelper.verify_ssl(@document_converter_url, http)

      http.read_timeout = @convert_timeout
      http.open_timeout = 5
      req = Net::HTTP::Post.new(uri.request_uri) # create the post request
      req.add_field('Accept', 'application/json') # set headers
      req.add_field('Content-Type', 'application/json')

      if JwtHelper.enabled? && JwtHelper.use_for_request # if the signature is enabled
        payload['token'] = JwtHelper.encode(payload) # get token and save it to the payload
        jwt_header = ServiceConverter.config_manager.jwt_header; # get signature authorization header
        # set it to the request with the Bearer prefix
        req.add_field(jwt_header, "Bearer #{JwtHelper.encode({ payload: })}")
      end

      req.body = payload.to_json
      res = http.request(req) # get the response

      status_code = Integer(res.code, 10)
      raise("Conversion service returned status: #{status_code}") if status_code != 200 # checking status code

      data = res.body # and take its body
    rescue Timeout::Error
      # try again
    rescue StandardError => e
      raise(e.message)
    end

    json_data = JSON.parse(data) # parse response body
    get_response_data(json_data) # get response url
  end

  # generate the document key value
  def self.generate_revision_id(expected_key)
    require('zlib')

    if expected_key.length > 20 # check if the expected key length is greater than 20
      # calculate 32-bit crc value from the expected key and turn it into the string
      expected_key = Zlib.crc32(expected_key).to_s
    end

    key = expected_key.gsub(/[^0-9a-zA-Z.=]/, '_')
    key[(key.length - [key.length, 20].min)..key.length] # the resulting key is of the length 20 or less
  end

  # create an error message for the error code
  def self.process_convert_service_responce_error(error_code)
    error_message = 'unknown error'

    # add an error message to the error message template depending on the error code
    case error_code
    when -9
      error_message = 'Error occurred in the ConvertService: Error conversion output format'
    when -8
      error_message = 'Error occurred in the ConvertService: Error document VKey'
    when -7
      error_message = 'Error occurred in the ConvertService: Error document request'
    when -6
      error_message = 'Error occurred in the ConvertService: Error database'
    when -5
      error_message = 'Error occurred in the ConvertService: Incorrect password'
    when -4
      error_message = 'Error occurred in the ConvertService: Error download error'
    when -3
      error_message = 'Error occurred in the ConvertService: Error convertation error'
    when -2
      error_message = 'Error occurred in the ConvertService: Error convertation timeout'
    when -1
      error_message = 'Error occurred in the ConvertService: Error convertation unknown'
    when 0
    # public const int c_nErrorNo = 0
    else
      error_message = "ErrorCode = #{error_code}" # default value for the error message
    end

    raise(error_message)
  end

  # get the response url
  def self.get_response_data(json_data)
    file_result = json_data

    error_element = file_result['error']
    unless error_element.nil? # if an error occurs
      process_convert_service_responce_error(Integer(error_element)) # get an error message
    end

    is_end_convert = file_result['endConvert'] # check if the conversion is completed

    result_percent = 0 # the conversion percentage
    response_uri = ''
    response_file_type = ''

    if is_end_convert # if the conversion is completed

      file_url_element = file_result['fileUrl']
      file_type_element = file_result['fileType']

      if file_url_element.nil? # and the file url doesn't exist
        raise('Invalid answer format') # get ann error message
      end

      response_uri = file_url_element # otherwise, get the file url
      response_file_type = file_type_element # get the file type
      result_percent = 100

    else # if the conversion isn't completed

      percent_element = file_result['percent'] # get the percentage value

      result_percent = unless percent_element.nil?
                         if percent_element.is_a?(String)
                           Integer(percent_element, 10)
                         else
                           Integer(percent_element)
                         end
                       end

      result_percent = 99 if result_percent >= 100

    end

    [result_percent, response_uri, response_file_type]
  end
end
