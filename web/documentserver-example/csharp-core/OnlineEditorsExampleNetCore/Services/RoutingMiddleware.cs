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
            
            if (!context.Response.HasStarted)
            {
                //if (context.Request.Query["type"] == "upload")
                //{
                //    await WebEditor.Upload(context);
                //}
                   
                //else if (context.Request.Query["type"] == "download")
                //{
                //    await WebEditor.Download(context);
 
                //}
                    
                //else if (context.Request.Query["type"] == "convert")
                //{
                //    await WebEditor.Convert(context);

                //}
                    
                //else if (context.Request.Query["type"] == "track")
                //{
                //    await WebEditor.Track(context);
                  
                //}
                    
                //else if (context.Request.Query["type"] == "remove")
                //{
                //    await WebEditor.Remove(context);
            
                //}
                   
                //else if (context.Request.Query["type"] == "assets")
                //{
                //    await WebEditor.Assets(context);
                   
                //}
                    
                //else if (context.Request.Query["type"] == "csv")
                //{
                //    await WebEditor.GetCsv(context);
                   
                //}
                    
                //else if (context.Request.Query["type"] == "files")
                //{
                //    await WebEditor.Files(context);
              
                //}


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
                await _next.Invoke(context);

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
