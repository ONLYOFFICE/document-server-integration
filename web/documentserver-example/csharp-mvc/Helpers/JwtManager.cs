﻿/**
 *
 * (c) Copyright Ascensio System SIA 2020
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

using System;
using System.Collections.Generic;
using System.Security.Cryptography;
using System.Text;
using System.Web.Configuration;
using System.Web.Script.Serialization;

namespace OnlineEditorsExampleMVC.Helpers
{
    public static class JwtManager
    {
        private static readonly string Secret;
        public static readonly bool Enabled;

        private static readonly JavaScriptSerializer Serializer;

        static JwtManager()
        {
            Secret = WebConfigurationManager.AppSettings["files.docservice.secret"] ?? "";  // get token secret from the config parameters
            Enabled = !string.IsNullOrEmpty(Secret);  // check if the token is enabled
            Serializer = new JavaScriptSerializer();  // define java script serializer
        }

        // encode a payload object into a token using a secret key
        public static string Encode(IDictionary<string, object> payload)
        {
            // define the hashing algorithm and the token type
            var header = new Dictionary<string, object>
                {
                    { "alg", "HS256" },
                    { "typ", "JWT" }
                };

            // three parts of token
            var encHeader = Base64UrlEncode(Serializer.Serialize(header));  // header
            var encPayload = Base64UrlEncode(Serializer.Serialize(payload));  // payload
            var hashSum = Base64UrlEncode(CalculateHash(encHeader, encPayload));  // signature

            return string.Format("{0}.{1}.{2}", encHeader, encPayload, hashSum);
        }

        // decode a token into a payload object using a secret key
        public static string Decode(string token)
        {
            if (!Enabled || string.IsNullOrEmpty(token)) return "";

            var split = token.Split('.');
            if (split.Length != 3) return "";

            var hashSum = Base64UrlEncode(CalculateHash(split[0], split[1]));  // get signature
            if (hashSum != split[2]) return "";  // and check if it is equal to the signature from the token
            return Base64UrlDecode(split[1]);  // decode payload
        }

        // generate a hash code based on a key using the HMAC method
        private static byte[] CalculateHash(string encHeader, string encPayload)
        {
            using (var hasher = new HMACSHA256(Encoding.UTF8.GetBytes(Secret)))
            {
                var bytes = Encoding.UTF8.GetBytes(string.Format("{0}.{1}", encHeader, encPayload));
                return hasher.ComputeHash(bytes);
            }
        }

        // encode a string into the base64 value
        private static string Base64UrlEncode(string str)
        {
            return Base64UrlEncode(Encoding.UTF8.GetBytes(str));
        }

        // encode bytes into the base64 value
        private static string Base64UrlEncode(byte[] bytes)
        {
            return Convert.ToBase64String(bytes)
                          .TrimEnd('=').Replace('+', '-').Replace('/', '_');
        }

        // decode a base64 value into the string
        private static string Base64UrlDecode(string payload)
        {
            var b64 = payload.Replace('_', '/').Replace('-', '+');
            switch (b64.Length%4)
            {
                case 2:
                    b64 += "==";
                    break;
                case 3:
                    b64 += "=";
                    break;
            }
            var bytes = Convert.FromBase64String(b64);
            return Encoding.UTF8.GetString(bytes);
        }
    }
}