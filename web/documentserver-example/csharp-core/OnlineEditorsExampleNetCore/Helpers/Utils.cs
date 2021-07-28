/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

using Microsoft.AspNetCore.Http;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace OnlineEditorsExampleNetCore.Helpers
{
    public static class Utils
    {
        // get the request header or return the default one
        public static string GetOrDefault(this HttpContext context, string header, string def)
        {
            var value = context.Request.Headers[header];
            if (value != String.Empty) return value.ToString();
            return def;
        }

        // get a cookie from the cookie collection or return the default one
        public static string GetOrDefault(this IRequestCookieCollection cookies, string cookie, string def)
        {
            var cook = cookies[cookie];
            if (cook != null && !string.IsNullOrEmpty(cook)) return cook;
            return def;
        }
    }
}
