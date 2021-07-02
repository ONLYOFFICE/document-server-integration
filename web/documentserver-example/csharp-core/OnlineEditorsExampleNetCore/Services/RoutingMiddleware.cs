using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
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
        public async Task Invoke(HttpContext context)
        {
            try
            {
                    switch (context.Request.Query["type"])
                    {
                        case "upload":
                            await WebEditor.Upload(context);
                            break;
                        case "download":
                            await WebEditor.Download(context);
                            break;
                        case "convert":
                            await WebEditor.Convert(context);
                            break;
                        case "track":
                            await WebEditor.Track(context);
                            break;
                        case "remove":
                            await WebEditor.Remove(context);
                            break;
                        case "assets":
                            await WebEditor.Assets(context);
                            break;
                        case "csv":
                            await WebEditor.GetCsv(context);
                            break;
                        case "files":
                            await WebEditor.Files(context);
                            break;
                    }
            }
            catch (Exception e)
            {
                await context.Response.WriteAsync(e.Message);
            }

            finally
            {
                if (!context.Response.HasStarted)
                    await _next(context);
            }
        }
    }
        public static class RoutingMiddlewareExtensions
    {
            public static IApplicationBuilder UseMyMiddleware(this IApplicationBuilder builder)
            {
                return builder.UseMiddleware<RoutingMiddleware>();
            }
    }
}
