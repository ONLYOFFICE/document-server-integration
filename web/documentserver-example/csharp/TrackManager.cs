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
using System.IO;
using System.Net;
using System.Collections.Generic;
using System.Web.Script.Serialization;
using System.Web.Configuration;
using System.Linq;
using System.Web;
using System.Text;
using ASC.Api.DocumentConverter;


namespace OnlineEditorsExample
{
    public class TrackManager
    {
        public static Dictionary<string, object> readBody(HttpContext context)
        {
            string body;
            try
            {
                using (var receiveStream = context.Request.InputStream)
                using (var readStream = new StreamReader(receiveStream))
                {
                    body = readStream.ReadToEnd();
                    if (string.IsNullOrEmpty(body)) context.Response.Write("{\"error\":1,\"message\":\"Request stream is empty\"}");
                }
            }
            catch (Exception e)
            {
                throw new HttpException((int)HttpStatusCode.BadRequest, e.Message);
            }

            var jss = new JavaScriptSerializer();
            var fileData = jss.Deserialize<Dictionary<string, object>>(body);

            if (JwtManager.Enabled)
            {
                string JWTheader = WebConfigurationManager.AppSettings["files.docservice.header"].Equals("") ? "Authorization" : WebConfigurationManager.AppSettings["files.docservice.header"];

                string token = null;

                if (fileData.ContainsKey("token"))
                {
                    token = JwtManager.Decode(fileData["token"].ToString());
                }
                else if (context.Request.Headers.AllKeys.Contains(JWTheader, StringComparer.InvariantCultureIgnoreCase))
                {
                    var headerToken = context.Request.Headers.Get(JWTheader).Substring("Bearer ".Length);
                    token = JwtManager.Decode(headerToken);
                }
                else
                {
                    context.Response.Write("{\"error\":1,\"message\":\"JWT expected\"}");
                }

                if (token != null && !token.Equals(""))
                {
                    fileData = (Dictionary<string, object>)jss.Deserialize<Dictionary<string, object>>(token)["payload"];
                }
                else
                {
                    context.Response.Write("{\"error\":1,\"message\":\"JWT validation failed\"}");
                }
            }

            return fileData;
        }

        public static int processSave(Dictionary<string, object> fileData, string fileName, string userAddress)
        {
            var downloadUri = (string)fileData["url"];
            var curExt = Path.GetExtension(fileName);
            var downloadExt = Path.GetExtension(downloadUri) ?? "";
            var newFileName = fileName;

            if (!downloadExt.Equals(curExt, StringComparison.InvariantCultureIgnoreCase))
            {
                try
                {
                    string newFileUri;
                    ServiceConverter.GetConvertedUri("", downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), false, out newFileUri);
                    if (string.IsNullOrEmpty(newFileUri))
                    {
                        newFileName = _Default.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
                    }
                    else
                    { 
                        downloadUri = newFileUri;
                    }
                }
                catch (Exception)
                {
                    newFileName = _Default.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
                }
            }

            // hack. http://ubuntuforums.org/showthread.php?t=1841740
            if (_Default.IsMono)
            {
                ServicePointManager.ServerCertificateValidationCallback += (s, ce, ca, p) => true;
            }

            var storagePath = _Default.StoragePath(newFileName, userAddress);
            var histDir = _Default.HistoryDir(storagePath);
            if (!Directory.Exists(histDir)) Directory.CreateDirectory(histDir);

            var versionDir = _Default.VersionDir(histDir, _Default.GetFileVersion(histDir));
            if (!Directory.Exists(versionDir)) Directory.CreateDirectory(versionDir);

            File.Copy(_Default.StoragePath(fileName, userAddress), Path.Combine(versionDir, "prev" + curExt));

            DownloadToFile(downloadUri, storagePath);
            DownloadToFile((string)fileData["changesurl"], Path.Combine(versionDir, "diff.zip"));

            var hist = fileData.ContainsKey("changeshistory") ? (string)fileData["changeshistory"] : null;
            if (string.IsNullOrEmpty(hist) && fileData.ContainsKey("history"))
            {
                var jss = new JavaScriptSerializer();
                hist = jss.Serialize(fileData["history"]);
            }

            if (!string.IsNullOrEmpty(hist))
            {
                File.WriteAllText(Path.Combine(versionDir, "changes.json"), hist);
            }

            File.WriteAllText(Path.Combine(versionDir, "key.txt"), (string)fileData["key"]);

            string forcesavePath = _Default.ForcesavePath(newFileName, userAddress, false);
            if (!forcesavePath.Equals(""))
            {
                File.Delete(forcesavePath);
            }

            return 0;
        }

        public static int processForceSave(Dictionary<string, object> fileData, string fileName, string userAddress)
        {
            var downloadUri = (string)fileData["url"];

            string curExt = Path.GetExtension(fileName);
            string downloadExt = Path.GetExtension(downloadUri);

            if (!curExt.Equals(downloadExt))
            {
                try
                {
                    string newFileUri;
                    var result = ServiceConverter.GetConvertedUri("", downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), false, out newFileUri);
                    if (string.IsNullOrEmpty(newFileUri))
                    {
                        fileName = _Default.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
                    }
                    else
                    {
                        downloadUri = newFileUri;
                    }
                }
                catch (Exception)
                {
                    fileName = _Default.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
                }
            }

            // hack. http://ubuntuforums.org/showthread.php?t=1841740
            if (_Default.IsMono)
            {
                ServicePointManager.ServerCertificateValidationCallback += (s, ce, ca, p) => true;
            }

            string forcesavePath = _Default.ForcesavePath(fileName, userAddress, false);
            if (forcesavePath.Equals(""))
            {
                forcesavePath = _Default.ForcesavePath(fileName, userAddress, true);
            }

            DownloadToFile(downloadUri, forcesavePath);

            return 0;
        }

        public static void commandRequest(string method, string key)
        {
            string documentCommandUrl = WebConfigurationManager.AppSettings["files.docservice.url.site"] + WebConfigurationManager.AppSettings["files.docservice.url.command"];

            var request = (HttpWebRequest)WebRequest.Create(documentCommandUrl);
            request.Method = "POST";
            request.ContentType = "application/json";

            var body = new Dictionary<string, object>() {
                { "c", method },
                { "key", key }
            };

            if (JwtManager.Enabled)
            {
                var payload = new Dictionary<string, object>
                    {
                        { "payload", body }
                    };

                var payloadToken = JwtManager.Encode(payload);
                var bodyToken = JwtManager.Encode(body);
                string JWTheader = WebConfigurationManager.AppSettings["files.docservice.header"].Equals("") ? "Authorization" : WebConfigurationManager.AppSettings["files.docservice.header"];
                request.Headers.Add(JWTheader, "Bearer " + payloadToken);

                body.Add("token", bodyToken);
            }

            var bytes = Encoding.UTF8.GetBytes(new JavaScriptSerializer().Serialize(body));
            request.ContentLength = bytes.Length;
            using (var requestStream = request.GetRequestStream())
            {
                requestStream.Write(bytes, 0, bytes.Length);
            }

            string dataResponse;
            using (var response = request.GetResponse())
            using (var stream = response.GetResponseStream())
            {
                if (stream == null) throw new Exception("Response is null");

                using (var reader = new StreamReader(stream))
                {
                    dataResponse = reader.ReadToEnd();
                }
            }

            var jss = new JavaScriptSerializer();
            var responseObj = jss.Deserialize<Dictionary<string, object>>(dataResponse);
            if (!responseObj["error"].ToString().Equals("0"))
            {
                throw new Exception(dataResponse);
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
    }
}
