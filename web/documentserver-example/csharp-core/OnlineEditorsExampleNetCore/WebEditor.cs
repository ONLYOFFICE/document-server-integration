using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
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
        public static void Upload(HttpContext context)
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

                var savedFileName = DocManagerHelper.StoragePath(fileName);
                //httpPostedFile.SaveAs(savedFileName);
                if (httpPostedFile.Length > 0)
                {
                    string filePath = Path.Combine(savedFileName, httpPostedFile.FileName);
                    using (Stream fileStream = new FileStream(filePath, FileMode.Create))
                    {
                        httpPostedFile.CopyToAsync(fileStream);
                    }
                }

                DocManagerHelper.CreateMeta(fileName, context.Request.Cookies.GetOrDefault("uid", ""), context.Request.Cookies.GetOrDefault("uname", ""));

                context.Response.WriteAsync("{ \"filename\": \"" + fileName + "\"}");
            }
            catch (Exception e)
            {
                context.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        public static void Convert(HttpContext context)
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
                        context.Response.WriteAsync("{ \"step\" : \"" + result + "\", \"filename\" : \"" + fileName + "\"}");
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

                context.Response.WriteAsync("{ \"filename\" : \"" + fileName + "\"}");
            }
            catch (Exception e)
            {
                context.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        // track file changes
        public static void Track(HttpContext context)
        {
            var userAddress = context.Request.Query["userAddress"];
            var fileName = context.Request.Query["fileName"];

            string body;
            try
            {
                using (var receiveStream = context.Request.Body)
                using (var readStream = new StreamReader(receiveStream))
                {
                    body = readStream.ReadToEnd();
                }
            }
            catch (Exception e)
            {
                throw new Exception(HttpStatusCode.BadRequest.ToString(), e);
            }

            var jss = new JavaScriptSerializer();
            if (string.IsNullOrEmpty(body)) return;
            var fileData = JsonConvert.DeserializeObject<Dictionary<string, object>>(body);

            //if (JwtManager.Enabled)
            //{
            //    if (fileData.ContainsKey("token"))
            //    {
            //        fileData = jss.Deserialize<Dictionary<string, object>>(JwtManager.Decode(fileData["token"].ToString()));
            //    }
            //    else if (context.Request.Headers.AllKeys.Contains("Authorization", StringComparer.InvariantCultureIgnoreCase))
            //    {
            //        var headerToken = context.Request.Headers.Get("Authorization").Substring("Bearer ".Length);
            //        fileData = (Dictionary<string, object>)jss.Deserialize<Dictionary<string, object>>(JwtManager.Decode(headerToken))["payload"];
            //    }
            //    else
            //    {
            //        throw new Exception("Expected JWT");
            //    }
            //}

            var status = (TrackerStatus)long.Parse(fileData["status"].ToString());

            switch (status)
            {
                case TrackerStatus.MustSave:
                case TrackerStatus.Corrupted:
                    var downloadUri = (string)fileData["url"];

                    var saved = 1;
                    try
                    {
                        var storagePath = DocManagerHelper.StoragePath(fileName, userAddress);
                        var histDir = DocManagerHelper.HistoryDir(storagePath);
                        var versionDir = DocManagerHelper.VersionDir(histDir, DocManagerHelper.GetFileVersion(histDir) + 1);

                        if (!Directory.Exists(versionDir)) Directory.CreateDirectory(versionDir);

                        File.Copy(storagePath, Path.Combine(versionDir, "prev" + Path.GetExtension(fileName)));

                        TrackManager.DownloadToFile(downloadUri, DocManagerHelper.StoragePath(fileName, userAddress));
                        TrackManager.DownloadToFile((string)fileData["changesurl"], Path.Combine(versionDir, "diff.zip"));

                        var hist = fileData.ContainsKey("changeshistory") ? (string)fileData["changeshistory"] : null;
                        if (string.IsNullOrEmpty(hist) && fileData.ContainsKey("history"))
                        {
                            hist = jss.Serialize(fileData["history"]);
                        }

                        if (!string.IsNullOrEmpty(hist))
                        {
                            File.WriteAllText(Path.Combine(versionDir, "changes.json"), hist);
                        }

                        File.WriteAllText(Path.Combine(versionDir, "key.txt"), (string)fileData["key"]);
                    }
                    catch (Exception)
                    {
                        saved = 0;
                    }

                    break;
            }
            context.Response.WriteAsync("{\"error\":0}");
        }

        // remove a file
        public static void Remove(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var fileName = context.Request.Query["fileName"];
                Remove(fileName);

                context.Response.WriteAsync("{ \"success\": true }");
            }
            catch (Exception e)
            {
                context.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
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
        public static void Files(HttpContext context)
        {
            List<Dictionary<string, object>> files = null;

            try
            {
                var jss = new JavaScriptSerializer();
                context.Response.ContentType = "application/json";

                if (context.Request.Query["fileId"].ToString() == null)
                {
                    files = DocManagerHelper.GetFilesInfo();  // get the information about the files from the storage path
                    context.Response.WriteAsync(jss.Serialize(files));
                }
                else
                {
                    var fileId = context.Request.Query["fileId"];  // get file id from the request
                    files = DocManagerHelper.GetFilesInfo(fileId);
                    if (files.Count == 0)
                    {
                        context.Response.WriteAsync("\"File not found\"");
                    }
                    else
                    {
                        context.Response.WriteAsync(jss.Serialize(files));
                    }
                }
            }
            catch (Exception e)
            {
                context.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
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
        public static void Assets(HttpContext context)
        {
            var fileName = Path.GetFileName(context.Request.Query["filename"]);
            var filePath = "assets/sample/" + fileName;
            download(filePath, context);
        }

        // download a csv file
        public static void GetCsv(HttpContext context)
        {
            var fileName = "csv.csv";
            var filePath = "assets/sample/" + fileName;
            download(filePath, context);
        }

        // download a file
        public static void Download(HttpContext context)
        {
            try
            {
                var fileName = Path.GetFileName(context.Request.Query["fileName"]);
                var userAddress = context.Request.Query["userAddress"];

                //if (JwtManager.Enabled)
                //{
                //    string JWTheader = WebConfigurationManager.AppSettings["files.docservice.header"].Equals("") ? "Authorization" : WebConfigurationManager.AppSettings["files.docservice.header"];

                //    if (context.Request.Headers.AllKeys.Contains(JWTheader, StringComparer.InvariantCultureIgnoreCase))
                //    {
                //        var headerToken = context.Request.Headers.Get(JWTheader).Substring("Bearer ".Length);
                //        string token = JwtManager.Decode(headerToken);
                //        if (token == null || token.Equals(""))
                //        {
                //            context.Response.StatusCode = (int)HttpStatusCode.Forbidden;
                //            context.Response.Write("JWT validation failed");
                //            return;
                //        }
                //    }
                //}

                var filePath = DocManagerHelper.ForcesavePath(fileName, userAddress, false);  // get the path to the force saved document version
                if (filePath.Equals(""))
                {
                    filePath = DocManagerHelper.StoragePath(fileName, userAddress);  // or to the original document
                }
                download(filePath, context);
            }
            catch (Exception)
            {
                context.Response.WriteAsync("{ \"error\": \"File not found!\"}");
            }
        }

        // download data from the url to the file
        public static void download(string filePath, HttpContext context)
        {
            var fileinf = new FileInfo(filePath);
            context.Response.Headers.Add("Content-Length", fileinf.Length.ToString());  // set headers to the response
            //context.Response.Headers.Add("Content-Type", MimeMapping.GetMimeMapping(filePath));
            var tmp = HttpUtility.UrlEncode(Path.GetFileName(filePath));
            tmp = tmp.Replace("+", "%20");
            context.Response.Headers.Add("Content-Disposition", "attachment; filename*=UTF-8\'\'" + tmp);
            //context.Response.TransmitFile(filePath);
        }

    }
}
