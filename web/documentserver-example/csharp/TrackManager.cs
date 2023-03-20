/**
 *
 * (c) Copyright Ascensio System SIA 2023
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
        // read request body
        public static Dictionary<string, object> readBody(HttpContext context)
        {
            string body;
            try
            {
                // read request body by streams and check if it is correct
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

            // check if the document token is enabled
            if (JwtManager.Enabled && JwtManager.SignatureUseForRequest)
            {
                string JWTheader = string.IsNullOrEmpty(WebConfigurationManager.AppSettings["files.docservice.header"]) ? "Authorization" : WebConfigurationManager.AppSettings["files.docservice.header"];

                string token = null;

                // if the document token is in the data
                if (fileData.ContainsKey("token"))
                {
                    token = JwtManager.Decode(fileData["token"].ToString());  // decode it
                }
                else if (context.Request.Headers.AllKeys.Contains(JWTheader, StringComparer.InvariantCultureIgnoreCase))  // if the Authorization header exists
                {
                    var headerToken = context.Request.Headers.Get(JWTheader).Substring("Bearer ".Length);
                    token = JwtManager.Decode(headerToken);  // decode its part after Authorization prefix
                }
                else  // otherwise, an error occurs
                {
                    context.Response.Write("{\"error\":1,\"message\":\"JWT expected\"}");
                }

                if (!string.IsNullOrEmpty(token))  // invalid signature error
                {
                    fileData = jss.Deserialize<Dictionary<string, object>>(token);
                    if (fileData.ContainsKey("payload"))
                        fileData = (Dictionary<string, object>)fileData["payload"];
                }
                else
                {
                    context.Response.Write("{\"error\":1,\"message\":\"JWT validation failed\"}");
                }
            }

            return fileData;
        }

        // file saving process
        public static int processSave(Dictionary<string, object> fileData, string fileName, string userAddress)
        {
            if (fileData["url"].Equals(null)) {
                throw new Exception("DownloadUrl is null");
            }
            var downloadUri = (string)fileData["url"];
            var curExt = Path.GetExtension(fileName).ToLower();  // get current file extension

            var downloadExt = "." + (string)fileData["filetype"];  // get the extension of the downloaded file

            var newFileName = fileName;

            // convert downloaded file to the file with the current extension if these extensions aren't equal
            if (!downloadExt.Equals(curExt, StringComparison.InvariantCultureIgnoreCase))
            {
                try
                {
                    // convert file and give url to a new file
                    Dictionary<string, string> newFileData;
                    ServiceConverter.GetConvertedData(downloadUri, downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), false, out newFileData);
                    var newFileUri = newFileData["fileUrl"];
                    if (string.IsNullOrEmpty(newFileUri))
                    {
                        // get the correct file name if it already exists
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

            _Default.VerifySSL();

            try
            {
                var bytesFile = DownloadFile(downloadUri); // download document file
                var storagePath = _Default.StoragePath(newFileName, userAddress);  // get the file path

                var histDir = _Default.HistoryDir(storagePath);  // get the path to the history directory
                if (!Directory.Exists(histDir)) Directory.CreateDirectory(histDir);

                var versionDir = _Default.VersionDir(histDir, _Default.GetFileVersion(histDir));  // get the path to the file version
                if (!Directory.Exists(versionDir)) Directory.CreateDirectory(versionDir);  // if the path doesn't exist, create it

                // get the path to the previous file version and rename the storage path with it
                File.Copy(_Default.StoragePath(fileName, userAddress), Path.Combine(versionDir, "prev" + curExt));

                SaveFile(bytesFile, storagePath);// save document file

                var bytesChanges = DownloadFile((string)fileData["changesurl"]); // download changes file
                SaveFile(bytesChanges, Path.Combine(versionDir, "diff.zip")); // save file changes to the diff.zip archive

                var hist = fileData.ContainsKey("changeshistory") ? (string)fileData["changeshistory"] : null;
                if (string.IsNullOrEmpty(hist) && fileData.ContainsKey("history"))
                {
                    var jss = new JavaScriptSerializer();
                    hist = jss.Serialize(fileData["history"]);
                }

                if (!string.IsNullOrEmpty(hist))
                {
                    File.WriteAllText(Path.Combine(versionDir, "changes.json"), hist);  // write the history changes to the changes.json file
                }

                File.WriteAllText(Path.Combine(versionDir, "key.txt"), (string)fileData["key"]);  // write the key value to the key.txt file

                string forcesavePath = _Default.ForcesavePath(newFileName, userAddress, false);  // get the path to the forcesaved file version
            if (!string.IsNullOrEmpty(forcesavePath))  // if the forcesaved file version exists
                {
                    File.Delete(forcesavePath);  // remove it
                }
            }
            catch (Exception)
            {
                return 1;
            }

            return 0;
        }

        // file force saving process
        public static int processForceSave(Dictionary<string, object> fileData, string fileName, string userAddress)
        {
            if (fileData["url"].Equals(null)) {
                throw new Exception("DownloadUrl is null");
            }
            var downloadUri = (string)fileData["url"];

            string curExt = Path.GetExtension(fileName).ToLower();  // get current file extension

            var downloadExt = "." + (string)fileData["filetype"];  // get the extension of the downloaded file

            Boolean newFileName = false;

            // convert downloaded file to the file with the current extension if these extensions aren't equal
            if (!curExt.Equals(downloadExt))
            {
                try
                {
                    // convert file and give url to a new file
                    Dictionary<string, string> newFileData;
                    var result = ServiceConverter.GetConvertedData(downloadUri, downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), false, out newFileData);
                    var newFileUri = newFileData["fileUrl"];
                    if (string.IsNullOrEmpty(newFileUri))
                    {
                        newFileName = true;
                    }
                    else
                    {
                        downloadUri = newFileUri;
                    }
                }
                catch (Exception)
                {
                    newFileName = true;
                }
            }

            _Default.VerifySSL();

            try
            {
                var bytesFile = DownloadFile(downloadUri); // download document file
                string forcesavePath = "";
                Boolean isSubmitForm = fileData["forcesavetype"].ToString().Equals("3");  // SubmitForm

                if (isSubmitForm)  // if the form is submitted
                {
                    if (newFileName)
                    {
                        fileName = _Default.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + "-form" + downloadExt, userAddress);  // get the correct file name if it already exists
                } else
                    {
                        fileName = _Default.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + "-form" + curExt, userAddress);
                    }
                    forcesavePath = _Default.StoragePath(fileName, userAddress);
                }
                else
                {
                    if (newFileName)
                    {
                        fileName = _Default.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
                    }

                    forcesavePath = _Default.ForcesavePath(fileName, userAddress, false);
                if (string.IsNullOrEmpty(forcesavePath))  // create forcesave path if it doesn't exist
                    {
                        forcesavePath = _Default.ForcesavePath(fileName, userAddress, true);
                    }
                }

                SaveFile(bytesFile, forcesavePath);// save document file

                if (isSubmitForm)
                {
                    var jss = new JavaScriptSerializer();
                    var actions = jss.Deserialize<List<object>>(jss.Serialize(fileData["actions"]));
                    var action = jss.Deserialize<Dictionary<string, object>>(jss.Serialize(actions[0]));
                    var user = action["userid"].ToString();  // get the user id
                    DocEditor.CreateMeta(fileName, user, "Filling Form", userAddress);  // create meta data for the forcesaved file
                }
            }
            catch (Exception)
            {
                return 1;
            }

            return 0;
        }

        // create a command request
        public static void commandRequest(string method, string key, object meta = null)
        {
            _Default.VerifySSL();
            
            string documentCommandUrl = WebConfigurationManager.AppSettings["files.docservice.url.site"] + WebConfigurationManager.AppSettings["files.docservice.url.command"];

            var request = (HttpWebRequest)WebRequest.Create(documentCommandUrl);
            request.Method = "POST";
            request.ContentType = "application/json";

            var body = new Dictionary<string, object>() {
                { "c", method },
                { "key", key }
            };

            if (meta != null)
            {
                body.Add("meta", meta);
            }

            // check if a secret key to generate token exists or not
            if (JwtManager.Enabled && JwtManager.SignatureUseForRequest)
            {
                var payload = new Dictionary<string, object>
                {
                    { "payload", body }
                };

                var payloadToken = JwtManager.Encode(payload);  // encode a payload object into a header token
                var bodyToken = JwtManager.Encode(body);  // encode body into a body token
                string JWTheader = string.IsNullOrEmpty(WebConfigurationManager.AppSettings["files.docservice.header"]) ? "Authorization" : WebConfigurationManager.AppSettings["files.docservice.header"];
                request.Headers.Add(JWTheader, "Bearer " + payloadToken);  // add a header Authorization with a header token and Authorization prefix in it

                body.Add("token", bodyToken);
            }

            var bytes = Encoding.UTF8.GetBytes(new JavaScriptSerializer().Serialize(body));
            request.ContentLength = bytes.Length;
            using (var requestStream = request.GetRequestStream())
            {
                // write bytes to the output stream
                requestStream.Write(bytes, 0, bytes.Length);
            }

            string dataResponse;
            using (var response = request.GetResponse())  // get the response
            using (var stream = response.GetResponseStream())
            {
                if (stream == null) throw new Exception("Response is null");

                using (var reader = new StreamReader(stream))
                {
                    dataResponse = reader.ReadToEnd();  // and read it
                }
            }

            // convert stream to json string
            var jss = new JavaScriptSerializer();
            var responseObj = jss.Deserialize<Dictionary<string, object>>(dataResponse);
            if (!responseObj["error"].ToString().Equals("0"))
            {
                throw new Exception(dataResponse);
            }
        }

        private static void SaveFile(byte[] data, string path)
        {
            using (var fs = File.Open(path, FileMode.Create))
            {
                fs.Write(data, 0, data.Length);
            }
        }

        // save file information from the url to the file specified
        private static byte[] DownloadFile(string url)
        {
            if (string.IsNullOrEmpty(url)) throw new ArgumentException("url");  // url isn't specified

            var req = (HttpWebRequest)WebRequest.Create(url);
            req.Timeout = 5000;
            using (var stream = req.GetResponse().GetResponseStream())  // get input stream of the file information from the url
            {
                if (stream == null) throw new Exception("stream is null");

                using (MemoryStream memoryStream = new MemoryStream())
                {
                    stream.CopyTo(memoryStream);
                    return memoryStream.ToArray();
                }
            }
        }
    }
}
