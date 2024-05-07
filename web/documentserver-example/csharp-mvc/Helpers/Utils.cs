/**
 *
 * (c) Copyright Ascensio System SIA 2024
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

using System.Web;

namespace OnlineEditorsExampleMVC.Helpers
{
    public static class Utils
    {
        // get the request header or return the default one
        public static string GetOrDefault(this HttpRequest request, string header, string def)
        {
            var value = request[header];
            if (value != null) return value;
            return def;
        }

        // get a cookie from the cookie collection or return the default one
        public static string GetOrDefault(this HttpCookieCollection cookies, string cookie, string def)
        {
            var cook = cookies[cookie];
            if (cook != null && !string.IsNullOrEmpty(cook.Value)) return cook.Value;
            return def;
        }
    }
}