using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.StaticFiles;
using OnlineEditorsExampleNetCore.Helpers;
using System;
using System.IO;
using System.Net;
using System.Threading.Tasks;
using System.Web;

namespace OnlineEditorsExampleNetCore
{
    public class WebEditorExtenstions
    {
        // define tracker status
        public enum TrackerStatus
        {
            NotFound = 0,
            Editing = 1,
            MustSave = 2,
            Corrupted = 3,
            Closed = 4,
            MustForceSave = 6,
            CorruptedForceSave = 7
        }

        public static void Remove(string fileName)
        {
            var path = DocManagerHelper.StoragePath(fileName, null);
            var histDir = DocManagerHelper.HistoryDir(path);

            if (File.Exists(path)) File.Delete(path);
            if (Directory.Exists(histDir)) Directory.Delete(histDir, true);
        }

        public static void DownloadToFile(string url, string path)
        {
            if (string.IsNullOrEmpty(url)) throw new ArgumentException("url");
            if (string.IsNullOrEmpty(path)) throw new ArgumentException("path");

            var req = (HttpWebRequest)WebRequest.Create(url);
            using (var stream = req.GetResponse().GetResponseStream())
            {
                if (stream == null) throw new Exception("stream is null");
                const int bufferSize = 4096;

                using (var fs = File.Open(path, FileMode.Create))
                {
                    var buffer = new byte[bufferSize];
                    int readed;
                    while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                    {
                        fs.Write(buffer, 0, readed);
                    }
                }
            }
        }

        // download data from the url to the file
        public static async Task DownloadAsync(string filePath, HttpContext context)
        {
            var fileinf = new FileInfo(filePath);
            context.Response.Headers.Add("Content-Length", fileinf.Length.ToString());  // set headers to the response
            new FileExtensionContentTypeProvider().TryGetContentType(filePath, out string contentType);
            context.Response.Headers.Add("Content-Type", contentType);
            var tmp = HttpUtility.UrlEncode(Path.GetFileName(filePath));
            tmp = tmp.Replace("+", "%20");
            context.Response.Headers.Add("Content-Disposition", "attachment; filename*=UTF-8\'\'" + tmp);
            await context.Response.SendFileAsync(filePath);
        }
    }
}
