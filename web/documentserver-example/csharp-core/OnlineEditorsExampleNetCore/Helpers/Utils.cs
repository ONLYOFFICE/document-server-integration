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
