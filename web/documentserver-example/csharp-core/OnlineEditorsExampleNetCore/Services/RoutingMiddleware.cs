using Microsoft.AspNetCore.Http;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace OnlineEditorsExampleNetCore.Services
{
    public class RoutingMiddleware
    {
        private readonly RequestDelegate _next;
        public RoutingMiddleware(RequestDelegate next)
        {
            _next = next;
        }
        public Task Invoke(HttpContext context)
        {
            switch (context.Request.Query["type"])
            {
                //case "upload":
                //    WebEditor.Upload(context);
                //    break;
                //case "download":
                //    WebEditor.Download(context);
                //    break;
                //case "convert":
                //    WebEditor.Convert(context);
                //    break;
                case "track":
                    WebEditorController.Track(context);
                    break;
                //case "remove":
                //    WebEditor.Remove(context);
                //    break;
                //case "assets":
                //    WebEditor.Assets(context);
                //    break;
                //case "csv":
                //    WebEditor.GetCsv(context);
                //    break;
                case "files":
                    WebEditorController.Files(context);
                    break;
            }
            return _next.Invoke(context);
        }
    }
}
