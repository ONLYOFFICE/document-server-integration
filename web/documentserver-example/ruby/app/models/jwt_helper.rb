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

class JwtHelper

    @jwt_secret = Rails.configuration.jwtSecret
  
    class << self
        def is_enabled
            return @jwt_secret && !@jwt_secret.empty? ? true : false
        end

        def encode(payload)
            header = { :alg => "HS256", :typ => "JWT" }
            enc_header = Base64.urlsafe_encode64(header.to_json).remove("=")
            enc_payload = Base64.urlsafe_encode64(payload.to_json).remove("=")
            hash = Base64.urlsafe_encode64(calc_hash(enc_header, enc_payload)).remove("=")

            return "#{enc_header}.#{enc_payload}.#{hash}"
        end

        def decode(token)
            if !is_enabled
                return ""
            end

            split = token.split(".")

            hash = Base64.urlsafe_encode64(calc_hash(split[0], split[1])).remove("=")

            if !hash.eql?(split[2])
                return ""
            end

            return Base64.urlsafe_decode64(split[1])
        end

        private

        def calc_hash(header, payload)
            return OpenSSL::HMAC.digest("SHA256", @jwt_secret, "#{header}.#{payload}")
        end
    end
end