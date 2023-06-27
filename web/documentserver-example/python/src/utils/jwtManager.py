"""

 (c) Copyright Ascensio System SIA 2023

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

"""

import jwt
from src.configuration import ConfigurationManager

# check if a secret key to generate token exists or not
def isEnabled():
    config = ConfigurationManager()
    return bool(config.jwt_secret())

# check if a secret key to generate token exists or not
def useForRequest():
    config = ConfigurationManager()
    return config.jwt_use_for_request()

# encode a payload object into a token using a secret key and decodes it into the utf-8 format
def encode(payload):
    config = ConfigurationManager()
    return jwt.encode(payload, config.jwt_secret(), algorithm='HS256')

# decode a token into a payload object using a secret key
def decode(string):
    config = ConfigurationManager()
    return jwt.decode(string, config.jwt_secret(), algorithms=['HS256'])
