class ServiceConverter

  @@convert_timeout = Rails.configuration.timeout
  @@document_converter_url = Rails.configuration.urlConverter
  @@convert_params = '?url=%s&outputtype=%s&filetype=%s&title=%s&key=%s'

  class << self

    def get_converted_uri(document_uri, from_ext, to_ext, document_revision_id, is_async)

      from_ext = from_ext == nil ? File.extname(document_uri) : from_ext

      title = File.basename(URI.parse(document_uri).path)
      title = title == nil ? UUID.generate.to_s : title

      document_revision_id = document_revision_id.empty? ? document_uri : document_revision_id
      document_revision_id = generate_revision_id(document_revision_id)

      url_to_converter = @@document_converter_url +
          (@@convert_params % [URI::encode(document_uri), to_ext.delete('.'), from_ext.delete('.'), title, document_revision_id])

      if is_async
        url_to_converter += '&async=true'
      end

      data = nil
      begin

        uri = URI.parse(url_to_converter)
        http = Net::HTTP.new(uri.host, uri.port)

        if url_to_converter.start_with?('https')
          http.use_ssl = true
          http.verify_mode = OpenSSL::SSL::VERIFY_NONE
        end

        http.read_timeout = @@convert_timeout
        req = Net::HTTP::Get.new(uri.request_uri)
        req.add_field("Accept", "application/json")
        res = http.request(req)
        data = res.body
      rescue TimeoutError
        #try again
      rescue => ex
        raise ex.message
      end

      json_data = JSON.parse(data)
      return get_response_uri(json_data)
    end

    def generate_revision_id(expected_key)

      require 'zlib'

      if expected_key.length > 20
        expected_key = (Zlib.crc32 expected_key).to_s
      end

      key = expected_key.gsub(/[^0-9a-zA-Z.=]/, '_')
      key[(key.length - [key.length, 20].min)..key.length]

    end

    def process_convert_service_responce_error(error_code)

      error_message = 'unknown error'

      case error_code
        when -8
          error_message = 'Error occurred in the ConvertService.ashx: Error document VKey'
        when -7
          error_message = 'Error occurred in the ConvertService.ashx: Error document request'
        when -6
          error_message = 'Error occurred in the ConvertService.ashx: Error database'
        when -5
          error_message = 'Error occurred in the ConvertService.ashx: Error unexpected guid'
        when -4
          error_message = 'Error occurred in the ConvertService.ashx: Error download error'
        when -3
          error_message = 'Error occurred in the ConvertService.ashx: Error convertation error'
        when -2
          error_message = 'Error occurred in the ConvertService.ashx: Error convertation timeout'
        when -1
          error_message = 'Error occurred in the ConvertService.ashx: Error convertation unknown'
        when 0
          #public const int c_nErrorNo = 0
        else
          error_message = 'ErrorCode = ' + error_code.to_s
      end

      raise error_message

    end

    def get_response_uri(json_data)

      file_result = json_data

      error_element = file_result['error']
      if error_element != nil
        process_convert_service_responce_error(error_element.to_i)
      end

      is_end_convert = file_result['endConvert']

      result_percent = 0
      response_uri = ''

      if is_end_convert

        file_url_element = file_result['fileUrl']

        if file_url_element == nil
          raise 'Invalid answer format'
        end

        response_uri = file_url_element
        result_percent = 100

      else

        percent_element = file_result['percent']

        if percent_element != nil
          result_percent = percent_element.to_i
        end

        result_percent = result_percent >= 100 ? 99 : result_percent

      end

      return result_percent, response_uri
    end

  end

end