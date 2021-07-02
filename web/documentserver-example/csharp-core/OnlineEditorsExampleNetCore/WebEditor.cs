using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.StaticFiles;
using Nancy.Json;
using Newtonsoft.Json;
using OnlineEditorsExampleNetCore.Helpers;
using OnlineEditorsExampleNetCore.Models;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using System.Web;

namespace OnlineEditorsExampleNetCore
{
    public class WebEditor
    {

        // define tracker status
        private enum TrackerStatus
        {
            NotFound = 0,
            Editing = 1,
            MustSave = 2,
            Corrupted = 3,
            Closed = 4,
            MustForceSave = 6,
            CorruptedForceSave = 7
        }

        // upload a file
        public static async Task Upload(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var httpPostedFile = context.Request.Form.Files[0];
                string fileName;

                if (context.Request.Headers["User-Agent"] == "IE")
                {
                    var files = httpPostedFile.FileName.Split(new char[] { '\\' });
                    fileName = files[files.Length - 1];
                }
                else
                {
                    fileName = httpPostedFile.FileName;
                }

                var curSize = httpPostedFile.Length;
                if (DocManagerHelper.MaxFileSize < curSize || curSize <= 0)
                {
                    throw new Exception("File size is incorrect");
                }

                var curExt = (Path.GetExtension(fileName) ?? "").ToLower();
                if (!DocManagerHelper.FileExts.Contains(curExt))
                {
                    throw new Exception("File type is not supported");
                }

                fileName = DocManagerHelper.GetCorrectName(fileName);

                fileName = DocManagerHelper.GetCorrectName(fileName);  // get the correct file name if such a name already exists
                var documentType = FileUtility.GetFileType(fileName).ToString().ToLower();

                var savedFileName = DocManagerHelper.StoragePath(fileName);  // get the storage path to the uploading file
                //httpPostedFile.SaveAs(savedFileName);  // and save it
                // get file meta information or create the default one
                var id = context.Request.Cookies.GetOrDefault("uid", null);
                var user = Users.getUser(id);
                DocManagerHelper.CreateMeta(fileName, user.id, user.name);

                await context.Response.WriteAsync("{ \"filename\": \"" + fileName + "\", \"documentType\": \"" + documentType + "\"}");

            }
            catch (Exception e)
            {
                await context.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        public static async Task Convert(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var fileName = context.Request.Query["filename"];
                var fileUri = DocManagerHelper.GetFileUri(fileName, true);

                var extension = (Path.GetExtension(fileUri) ?? "").Trim('.');
                var internalExtension = DocManagerHelper.GetInternalExtension(FileUtility.GetFileType(fileName)).Trim('.');

                if (DocManagerHelper.ConvertExts.Contains("." + extension)
                    && !string.IsNullOrEmpty(internalExtension))
                {
                    var key = ServiceConverter.GenerateRevisionId(fileUri);

                    string newFileUri;
                    var result = ServiceConverter.GetConvertedUri(fileUri, extension, internalExtension, key, true, out newFileUri);
                    if (result != 100)
                    {
                        await context.Response.WriteAsync("{ \"step\" : \"" + result + "\", \"filename\" : \"" + fileName + "\"}");
                        return;
                    }

                    var correctName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + "." + internalExtension);

                    var req = (HttpWebRequest)WebRequest.Create(newFileUri);

                    using (var stream = req.GetResponse().GetResponseStream())
                    {
                        if (stream == null) throw new Exception("Stream is null");
                        const int bufferSize = 4096;

                        using (var fs = File.Open(DocManagerHelper.StoragePath(correctName), FileMode.Create))
                        {
                            var buffer = new byte[bufferSize];
                            int readed;
                            while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                            {
                                fs.Write(buffer, 0, readed);
                            }
                        }
                    }

                    Remove(fileName);
                    fileName = correctName;
                    DocManagerHelper.CreateMeta(fileName, context.Request.Cookies.GetOrDefault("uid", ""), context.Request.Cookies.GetOrDefault("uname", ""));
                }

                await context .Response.WriteAsync("{ \"filename\" : \"" + fileName + "\"}");
            }
            catch (Exception e)
            {
                await context .Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        // track file changes
        public static async Task Track(HttpContext context)
        {
            // read request body
            var fileData = TrackManager.readBody(context);

            var userAddress = context.Request.Query["userAddress"];
            var fileName = Path.GetFileName(context.Request.Query["fileName"]);
            var status = (TrackerStatus)(Int64)fileData["status"];  // get status from the request body
            var saved = 1;  // editing
            switch (status)
            {
                case TrackerStatus.Editing:
                    try
                    {
                        var actions = JsonConvert.DeserializeObject<List<object>>(JsonConvert.SerializeObject(fileData["actions"]));
                        var action = JsonConvert.DeserializeObject<Dictionary<string, object>>(JsonConvert.SerializeObject(actions[0]));
                        if (action != null && action["type"].ToString().Equals("0"))  // finished edit
                        {
                            var user = action["userid"].ToString();  // the user who finished editing
                            var users = JsonConvert.DeserializeObject<List<object>>(JsonConvert.SerializeObject(fileData["users"]));
                            if (!users.Contains(user))
                            {
                                TrackManager.commandRequest("forcesave", fileData["key"].ToString());  // create a command request with the forcesave method
                            }

                        }
                    }
                    catch (Exception e)
                    {
                        Debug.Print(e.StackTrace);
                    }
                    return;

                // MustSave, Corrupted
                case TrackerStatus.MustSave:
                case TrackerStatus.Corrupted:
                    try
                    {
                        // saving a document
                        saved = TrackManager.processSave(fileData, fileName, userAddress);
                    }
                    catch (Exception)
                    {
                        saved = 1;
                    }
                    await context.Response.WriteAsync("{\"error\":" + saved + "}");
                    return;

                // MustForceSave, CorruptedForceSave
                case TrackerStatus.MustForceSave:
                case TrackerStatus.CorruptedForceSave:
                    try
                    {
                        // force saving a document
                        saved = TrackManager.processForceSave(fileData, fileName, userAddress);
                    }
                    catch (Exception)
                    {
                        saved = 1;
                    }
                    await context.Response.WriteAsync("{\"error\":" + saved + "}");
                    return;
            }
        }

        // remove a file
        public static async Task Remove(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var fileName = context.Request.Query["fileName"];
                Remove(fileName);

                await context.Response.WriteAsync("{ \"success\": true }");
            }
            catch (Exception e)
            {
                await context.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        public static void Remove(string fileName)
        {
            var path = DocManagerHelper.StoragePath(fileName, null);
            var histDir = DocManagerHelper.HistoryDir(path);

            if (File.Exists(path)) File.Delete(path);
            if (Directory.Exists(histDir)) Directory.Delete(histDir, true);
        }

        // get files information
        public static async Task Files(HttpContext context)
        {
            List<Dictionary<string, object>> files = null;

            try
            {
                var jss = new JavaScriptSerializer();
                context.Response.ContentType = "application/json";

                if (context.Request.Query["fileId"].ToString() == null)
                {
                    files = DocManagerHelper.GetFilesInfo();  // get the information about the files from the storage path
                    await context.Response.WriteAsync(jss.Serialize(files));
                }
                else
                {
                    var fileId = context.Request.Query["fileId"];  // get file id from the request
                    files = DocManagerHelper.GetFilesInfo(fileId);
                    if (files.Count == 0)
                    {
                        await context.Response.WriteAsync("\"File not found\"");
                    }
                    else
                    {
                        await context.Response.WriteAsync(jss.Serialize(files));
                    }
                }
            }
            catch (Exception e)
            {
                await context.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
            }
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

        // get sample files from the assests
        public static async Task Assets(HttpContext context)
        {
            var fileName = Path.GetFileName(context.Request.Query["filename"]);
            var filePath = "assets/sample/" + fileName;
            await download(filePath, context);
        }

        // download a csv file
        public static async Task GetCsv(HttpContext context)
        {
            var fileName = "csv.csv";
            var filePath = "assets/sample/" + fileName;
            await download(filePath, context);
        }

        // download a file
        public static async Task Download(HttpContext context)
        {
            try
            {
                var fileName = Path.GetFileName(context.Request.Query["fileName"]);
                var userAddress = context.Request.Query["userAddress"];

                if (JwtManager.Enabled)
                {
                    string JWTheader = Startup.AppSettings["files.docservice.header"].Equals("") ? "Authorization" : Startup.AppSettings["files.docservice.header"];

                    if (context.Request.Headers.Keys.Contains(JWTheader, StringComparer.InvariantCultureIgnoreCase))
                    {
                        var headerToken = context.Request.Headers[JWTheader].ToString().Substring("Bearer ".Length);
                        string token = JwtManager.Decode(headerToken);
                        if (token == null || token.Equals(""))
                        {
                            context.Response.StatusCode = (int)HttpStatusCode.Forbidden;
                            await context.Response.WriteAsync("JWT validation failed");
                            return;
                        }
                    }
                }

                var filePath = DocManagerHelper.ForcesavePath(fileName, userAddress, false);  // get the path to the force saved document version
                if (filePath.Equals(""))
                {
                    filePath = DocManagerHelper.StoragePath(fileName, userAddress);  // or to the original document
                }
                await download (filePath, context);
            }
            catch (Exception)
            {
                await context.Response.WriteAsync("{ \"error\": \"File not found!\"}");
            }
        }

        // download data from the url to the file
        private static async Task download(string filePath, HttpContext context)
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
