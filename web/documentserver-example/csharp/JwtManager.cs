/*
 *
 * (c) Copyright Ascensio System SIA 2020
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
*/

using System;
using System.Collections.Generic;
using System.Security.Cryptography;
using System.Text;
using System.Web.Configuration;
using System.Web.Script.Serialization;

namespace OnlineEditorsExample
{
    public static class JwtManager
    {
        private static readonly string Secret;
        public static readonly bool Enabled;

        private static readonly JavaScriptSerializer Serializer;

        static JwtManager()
        {
            Secret = WebConfigurationManager.AppSettings["files.docservice.secret"] ?? "";
            Enabled = !string.IsNullOrEmpty(Secret);
            Serializer = new JavaScriptSerializer();
        }

        public static string Encode(IDictionary<string, object> payload)
        {
            var header = new Dictionary<string, object>
                {
                    { "alg", "HS256" },
                    { "typ", "JWT" }
                };

            var encHeader = Base64UrlEncode(Serializer.Serialize(header));
            var encPayload = Base64UrlEncode(Serializer.Serialize(payload));
            var hashSum = Base64UrlEncode(CalculateHash(encHeader, encPayload));

            return string.Format("{0}.{1}.{2}", encHeader, encPayload, hashSum);
        }

        public static string Decode(string token)
        {
            if (!Enabled || string.IsNullOrEmpty(token)) return "";

            var split = token.Split('.');
            if (split.Length != 3) return "";

            var hashSum = Base64UrlEncode(CalculateHash(split[0], split[1]));
            if (hashSum != split[2]) return "";
            return Base64UrlDecode(split[1]);
        }

        private static byte[] CalculateHash(string encHeader, string encPayload)
        {
            using (var hasher = new HMACSHA256(Encoding.UTF8.GetBytes(Secret)))
            {
                var bytes = Encoding.UTF8.GetBytes(string.Format("{0}.{1}", encHeader, encPayload));
                return hasher.ComputeHash(bytes);
            }
        }

        private static string Base64UrlEncode(string str)
        {
            return Base64UrlEncode(Encoding.UTF8.GetBytes(str));
        }

        private static string Base64UrlEncode(byte[] bytes)
        {
            return Convert.ToBase64String(bytes)
                          .TrimEnd('=').Replace('+', '-').Replace('/', '_');
        }

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