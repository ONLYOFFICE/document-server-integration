using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;

namespace OnlineEditorsExampleNetCore.Helpers
{
    public static class JwtManager
    {
        private static readonly string Secret;
        public static readonly bool Enabled;

        static JwtManager()
        {
            Secret = Startup.AppSettings["files.docservice.secret"] ?? "";  // get token secret from the config parameters
            Enabled = !string.IsNullOrEmpty(Secret);  // check if the token is enabled
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
            var encHeader = Base64UrlEncode(JsonConvert.SerializeObject(header));  // header
            var encPayload = Base64UrlEncode(JsonConvert.SerializeObject(payload));  // payload
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
            switch (b64.Length % 4)
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
