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

using ASC.Api.DocumentConverter;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Web;
using System.Web.Script.Serialization;
using System.Web.Services;

namespace OnlineEditorsExample
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
                context.Response.Write("{ \"filename\": \"" + _Default.DoUpload(context) + "\"}");
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
                context.Response.Write(_Default.DoConvert(context));
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

                    var curExt = Path.GetExtension(fileName);
                    var downloadExt = Path.GetExtension(downloadUri) ?? "";
                    if (!downloadExt.Equals(curExt, StringComparison.InvariantCultureIgnoreCase))
                    {
                        var key = ServiceConverter.GenerateRevisionId(downloadUri);

                        try
                        {
                            string newFileUri;
                            ServiceConverter.GetConvertedUri(downloadUri, downloadExt, curExt, key, false, out newFileUri);
                            downloadUri = newFileUri;
                        }
                        catch (Exception ex)
                        {
                            fileName = _Default.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
                        }
                    }

                    // hack. http://ubuntuforums.org/showthread.php?t=1841740
                    if (_Default.IsMono)
                    {
                        ServicePointManager.ServerCertificateValidationCallback += (s, ce, ca, p) => true;
                    }

                    var saved = 1;
                    try
                    {
                        var storagePath = _Default.StoragePath(fileName, userAddress);
                        var histDir = _Default.HistoryDir(storagePath);
                        var versionDir = _Default.VersionDir(histDir, _Default.GetFileVersion(histDir) + 1);

                        if (!Directory.Exists(versionDir)) Directory.CreateDirectory(versionDir);

                        File.Copy(storagePath, Path.Combine(versionDir, "prev" + curExt));

                        DownloadToFile(downloadUri, _Default.StoragePath(fileName, userAddress));
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
                var path = _Default.StoragePath(fileName, HttpUtility.UrlEncode(HttpContext.Current.Request.UserHostAddress));
                var histDir = _Default.HistoryDir(path);

                if (File.Exists(path)) File.Delete(path);
                if (Directory.Exists(histDir)) Directory.Delete(histDir, true);

                context.Response.Write("{ \"success\": true }");
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
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