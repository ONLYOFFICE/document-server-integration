#
# (c) Copyright Ascensio System SIA 2023
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

require 'jwt'

class JwtHelper

    @jwt_secret = Rails.configuration.jwtSecret
  
    class << self
        # check if a secret key to generate token exists or not
        def is_enabled
            return @jwt_secret && !@jwt_secret.empty? ? true : false
        end

        # encode a payload object into a token using a secret key
        def encode(payload)
            return JWT.encode payload, @jwt_secret, 'HS256' # define the hashing algorithm and get token
        end

        # decode a token into a payload object using a secret key
        def decode(token)
            begin
                decoded = JWT.decode token, @jwt_secret, true, { algorithm: 'HS256' }
            rescue
                return ""
            end
            # decoded = Array [ {"data"=>"test"}, # payload
            #                   {"alg"=>"HS256"} # header   ]
            return decoded[0].to_json   #   get json payload
        end
    end
end