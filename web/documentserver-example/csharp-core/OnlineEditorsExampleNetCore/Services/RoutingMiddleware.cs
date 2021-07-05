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
        //private readonly RequestDelegate _next;
        //public RoutingMiddleware(RequestDelegate next)
        //{
        //    _next = next;
        //}
        //public async Task Invoke(HttpContext context)
        //{
        //    try
        //    {
        //        switch (context.Request.Query["type"])
        //        {
        //            case "upload":
        //                await WebEditorExtenstion.Upload(context);
        //                break;
        //            case "download":
        //                await WebEditorExtenstion.Download(context);
        //                break;
        //            case "convert":
        //                await WebEditorExtenstion.Convert(context);
        //                break;
        //            case "track":
        //                await WebEditorExtenstion.Track(context);
        //                break;
        //            case "remove":
        //                await WebEditorExtenstion.Remove(context);
        //                break;
        //            case "assets":
        //                await WebEditorExtenstion.Assets(context);
        //                break;
        //            case "csv":
        //                await WebEditorExtenstion.GetCsv(context);
        //                break;
        //            case "files":
        //                await WebEditorExtenstion.Files(context);
        //                break;
        //        }
        //    }
        //    catch (Exception e)
        //    {
        //        await context.Response.WriteAsync(e.Message);
        //    }
        //    finally
        //    {
        //        if (!context.Response.HasStarted)
        //            await _next(context);
        //    }
        //}
    }
    //    public static class RoutingMiddlewareExtensions
    //{
    //        public static IApplicationBuilder UseMyMiddleware(this IApplicationBuilder builder)
    //        {
    //            return builder.UseMiddleware<RoutingMiddleware>();
    //        }
    //}
}
