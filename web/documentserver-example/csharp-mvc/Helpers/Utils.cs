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