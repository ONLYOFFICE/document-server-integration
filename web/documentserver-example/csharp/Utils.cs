using System.Web;

namespace OnlineEditorsExample
{
    public static class Utils
    {
        public static string GetOrDefault(this HttpRequest request, string header, string def)
        {
            var value = request[header];
            if (value != null) return value;
            return def;
        }

        public static string GetOrDefault(this HttpCookieCollection cookies, string cookie, string def)
        {
            var cook = cookies[cookie];
            if (cook != null && !string.IsNullOrEmpty(cook.Value)) return cook.Value;
            return def;
        }
    }
}