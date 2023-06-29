/**
 *
 * (c) Copyright Ascensio System SIA 2023
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

using JWT;
using JWT.Algorithms;
using JWT.Builder;
using JWT.Serializers;
using System.Collections.Generic;
using System.Web.Configuration;

namespace OnlineEditorsExampleMVC.Helpers
{
    public static class JwtManager
    {
        private static readonly string Secret;
        public static readonly bool Enabled;
        public static readonly bool SignatureUseForRequest;

        static JwtManager()
        {
            Secret = WebConfigurationManager.AppSettings["files.docservice.secret"] ?? "";  // get token secret from the config parameters
            Enabled = !string.IsNullOrEmpty(Secret);  // check if the token is enabled
            SignatureUseForRequest = bool.Parse(WebConfigurationManager.AppSettings["files.docservice.token.useforrequest"]);
        }

        // encode a payload object into a token using a secret key
        public static string Encode(IDictionary<string, object> payload)
        {
            var encoder = new JwtEncoder(new HMACSHA256Algorithm(),
                                         new JsonNetSerializer(),
                                         new JwtBase64UrlEncoder());
            return encoder.Encode(payload, Secret);
        }

        // decode a token into a payload object using a secret key
        public static string Decode(string token)
        {
            if (!Enabled || string.IsNullOrEmpty(token)) return "";

            return JwtBuilder.Create()
                    .WithAlgorithm(new HMACSHA256Algorithm())
                    .WithSecret(Secret)
                    .MustVerifySignature()
                    .Decode(token);
        }
    }
}