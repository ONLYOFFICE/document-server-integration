/**
 *
 * (c) Copyright Ascensio System SIA 2020
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Web;
using System.Web.Configuration;
using System.Web.Script.Serialization;
using System.Web.Services;
using OnlineEditorsExampleMVC.Helpers;
using OnlineEditorsExampleMVC.Models;

namespace OnlineEditorsExampleMVC
{
    [WebService(Namespace = "http://tempuri.org/")]
    [WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
    public class WebEditor : IHttpHandler
    {
        public void ProcessRequest(HttpContext context)
        {
            switch (context.Request["type"])
            {
                case "upload":
                    Upload(context);
                    break;
                case "convert":
                    Convert(context);
                    break;
                case "track":
                    Track(context);
                    break;
                case "remove":
                    Remove(context);
                    break;
            }
        }

        private static void Upload(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var httpPostedFile = context.Request.Files[0];
                string fileName;

                if (HttpContext.Current.Request.Browser.Browser.ToUpper() == "IE")
                {
                    var files = httpPostedFile.FileName.Split(new char[] { '\\' });
                    fileName = files[files.Length - 1];
                }
                else
                {
                    fileName = httpPostedFile.FileName;
                }

                var curSize = httpPostedFile.ContentLength;
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
                httpPostedFile.SaveAs(savedFileName);
                DocManagerHelper.CreateMeta(fileName, context.Request.Cookies.GetOrDefault("uid", ""), context.Request.Cookies.GetOrDefault("uname", ""));

                context.Response.Write("{ \"filename\": \"" + fileName + "\"}");
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        private static void Convert(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var fileName = context.Request["filename"];
                var fileUri = DocManagerHelper.GetFileUri(fileName);

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
                        context.Response.Write("{ \"step\" : \"" + result + "\", \"filename\" : \"" + fileName + "\"}");
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

                context.Response.Write("{ \"filename\" : \"" + fileName + "\"}");
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        private enum TrackerStatus
        {
            NotFound = 0,
            Editing = 1,
            MustSave = 2,
            Corrupted = 3,
            Closed = 4,
        }

        private static void Track(HttpContext context)
        {
            var userAddress = context.Request["userAddress"];
            var fileName = context.Request["fileName"];

            string body;
            try
            {
                using (var receiveStream = context.Request.InputStream)
                using (var readStream = new StreamReader(receiveStream))
                {
                    body = readStream.ReadToEnd();
                }
            }
            catch (Exception e)
            {
                throw new HttpException((int) HttpStatusCode.BadRequest, e.Message);
            }

            var jss = new JavaScriptSerializer();
            if (string.IsNullOrEmpty(body)) return;
            var fileData = jss.Deserialize<Dictionary<string, object>>(body);

            if (JwtManager.Enabled)
            {
                if (fileData.ContainsKey("token"))
                {
                    fileData = jss.Deserialize<Dictionary<string, object>>(JwtManager.Decode(fileData["token"].ToString()));
                }
                else if (context.Request.Headers.AllKeys.Contains("Authorization", StringComparer.InvariantCultureIgnoreCase))
                {
                    var headerToken = context.Request.Headers.Get("Authorization").Substring("Bearer ".Length);
                    fileData = (Dictionary<string, object>)jss.Deserialize<Dictionary<string, object>>(JwtManager.Decode(headerToken))["payload"];
                }
                else
                {
                    throw new Exception("Expected JWT");
                }
            }

            var status = (TrackerStatus) (int) fileData["status"];

            switch (status)
            {
                case TrackerStatus.MustSave:
                case TrackerStatus.Corrupted:
                    var downloadUri = (string) fileData["url"];

                    var saved = 1;
                    try
                    {
                        var storagePath = DocManagerHelper.StoragePath(fileName, userAddress);
                        var histDir = DocManagerHelper.HistoryDir(storagePath);
                        var versionDir = DocManagerHelper.VersionDir(histDir, DocManagerHelper.GetFileVersion(histDir) + 1);

                        if (!Directory.Exists(versionDir)) Directory.CreateDirectory(versionDir);

                        File.Copy(storagePath, Path.Combine(versionDir, "prev" + Path.GetExtension(fileName)));

                        DownloadToFile(downloadUri, DocManagerHelper.StoragePath(fileName, userAddress));
                        DownloadToFile((string)fileData["changesurl"], Path.Combine(versionDir, "diff.zip"));

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
            context.Response.Write("{\"error\":0}");
        }

        private static void Remove(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var fileName = context.Request["fileName"];
                Remove(fileName);

                context.Response.Write("{ \"success\": true }");
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        private static void Remove(string fileName)
        {
            var path = DocManagerHelper.StoragePath(fileName, null);
            var histDir = DocManagerHelper.HistoryDir(path);

            if (File.Exists(path)) File.Delete(path);
            if (Directory.Exists(histDir)) Directory.Delete(histDir, true);
        }

        private static void DownloadToFile(string url, string path)
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

        public bool IsReusable
        {
            get { return false; }
        }
    }
}