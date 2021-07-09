using System;
using System.IO;
using System.Net;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Microsoft.AspNetCore.Http;
using Newtonsoft.Json;

namespace OnlineEditorsExampleNetCore.Helpers
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
                using (var receiveStream = context.Request.Body)
                using (var readStream = new StreamReader(receiveStream))
                {
                    body = readStream.ReadToEnd();
                    if (string.IsNullOrEmpty(body)) context.Response.WriteAsync("{\"error\":1,\"message\":\"Request stream is empty\"}");
                }
            }
            catch (Exception e)
            {
                throw new Exception(HttpStatusCode.BadRequest.ToString(), e);
            }

            var fileData = JsonConvert.DeserializeObject<Dictionary<string, object>>(body);

            // check if the document token is enabled
            if (JwtManager.Enabled)
            {
                string JWTheader = Startup.AppSettings["files.docservice.header"].Equals("") ? "Authorization" : Startup.AppSettings["files.docservice.header"];

                string token = null;

                // if the document token is in the data
                if (fileData.ContainsKey("token"))
                {
                    token = JwtManager.Decode(fileData["token"].ToString());  // decode it
                }
                else if (context.Request.Headers.Keys.Contains(JWTheader, StringComparer.InvariantCultureIgnoreCase))  // if the Authorization header exists
                {
                    var headerToken = context.Request.Headers[JWTheader].ToString().Substring("Bearer ".Length);
                    token = JwtManager.Decode(headerToken);  // decode its part after Authorization prefix
                }
                else  // otherwise, an error occurs
                {
                    context.Response.WriteAsync("{\"error\":1,\"message\":\"JWT expected\"}");
                }

                if (token != null && !token.Equals(""))  // invalid signature error
                {
                    fileData = (Dictionary<string, object>)JsonConvert.DeserializeObject<Dictionary<string, object>>(token)["payload"];
                }
                else
                {
                    context.Response.WriteAsync("{\"error\":1,\"message\":\"JWT validation failed\"}");
                }
            }
            return fileData;
        }

        // file saving process
        public static int processSave(Dictionary<string, object> fileData, string fileName, string userAddress)
        {
            var downloadUri = (string)fileData["url"];
            string curExt = Path.GetExtension(fileName).ToLower();  // get current file extension
            string downloadExt = Path.GetExtension(downloadUri).ToLower() ?? "";  // get the extension of the downloaded file
            var newFileName = fileName;

            // convert downloaded file to the file with the current extension if these extensions aren't equal
            if (!curExt.Equals(downloadExt, StringComparison.InvariantCultureIgnoreCase))
            {
                try
                {
                    // convert file and give url to a new file
                    string newFileUri;
                    var result = ServiceConverter.GetConvertedUri(downloadUri, downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), false, out newFileUri);
                    if (string.IsNullOrEmpty(newFileUri))
                    {
                        // get the correct file name if it already exists
                        newFileName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
                    }
                    else 
                    {
                        downloadUri = newFileUri;
                    }
                } 
                catch (Exception)
                {
                    newFileName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
                }
            }

            var storagePath = DocManagerHelper.StoragePath(newFileName, userAddress);  // get the file path
            var histDir = DocManagerHelper.HistoryDir(storagePath);  // get the path to the history directory
            if (!Directory.Exists(histDir)) Directory.CreateDirectory(histDir);

            var versionDir = DocManagerHelper.VersionDir(histDir, DocManagerHelper.GetFileVersion(histDir));  // get the path to the file version
            if (!Directory.Exists(versionDir)) Directory.CreateDirectory(versionDir);  // if the path doesn't exist, create it

            // get the path to the previous file version and move it to the storage directory
            File.Move(DocManagerHelper.StoragePath(fileName, userAddress), Path.Combine(versionDir, "prev" + curExt));

            DownloadToFile(downloadUri, storagePath);  // save file to the storage directory
            DownloadToFile((string)fileData["changesurl"], Path.Combine(versionDir, "diff.zip"));  // save file changes to the diff.zip archive

            var hist = fileData.ContainsKey("changeshistory") ? (string)fileData["changeshistory"] : null;
            if (string.IsNullOrEmpty(hist) && fileData.ContainsKey("history"))
            {
                hist = JsonConvert.SerializeObject(fileData["history"]);
            }

            if (!string.IsNullOrEmpty(hist))
            {
                File.WriteAllText(Path.Combine(versionDir, "changes.json"), hist);  // write the history changes to the changes.json file
            }

            File.WriteAllText(Path.Combine(versionDir, "key.txt"), (string)fileData["key"]);  // write the key value to the key.txt file

            string forcesavePath = DocManagerHelper.ForcesavePath(newFileName, userAddress, false);  // get the path to the forcesaved file version
            if (!forcesavePath.Equals(""))  // if the forcesaved file version exists
            {
                File.Delete(forcesavePath);  // remove it
            }

            return 0;
        }

        // file force saving process
        public static int processForceSave(Dictionary<string, object> fileData, string fileName, string userAddress)
        {
            var downloadUri = (string)fileData["url"];

            string curExt = Path.GetExtension(fileName).ToLower();  // get current file extension
            string downloadExt = Path.GetExtension(downloadUri).ToLower();  // get the extension of the downloaded file
            Boolean newFileName = false;

            // convert downloaded file to the file with the current extension if these extensions aren't equal
            if (!curExt.Equals(downloadExt))
            {
                try
                {
                    // convert file and give url to a new file
                    string newFileUri;
                    var result = ServiceConverter.GetConvertedUri(downloadUri, downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), false, out newFileUri);
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

            string forcesavePath = "";
            Boolean isSubmitForm = fileData["forcesavetype"].ToString().Equals("3");  // SubmitForm

            if (isSubmitForm)  // if the form is submitted
            {
                if (newFileName)
                {
                    fileName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + "-form" + downloadExt, userAddress);  // get the correct file name if it already exists
                } else
                {
                    fileName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + "-form" + curExt, userAddress);
                }
                forcesavePath = DocManagerHelper.StoragePath(fileName, userAddress);
            }
            else
            {
                if (newFileName)
                {
                    fileName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
                }
                forcesavePath = DocManagerHelper.ForcesavePath(fileName, userAddress, false);
                if (forcesavePath.Equals(""))  // create forcesave path if it doesn't exist
                {
                    forcesavePath = DocManagerHelper.ForcesavePath(fileName, userAddress, true);
                }
            }

            DownloadToFile(downloadUri, forcesavePath);

            if (isSubmitForm)
            {
                var actions = JsonConvert.DeserializeObject<List<object>>(JsonConvert.SerializeObject(fileData["actions"]));
                var action = JsonConvert.DeserializeObject<Dictionary<string, object>>(JsonConvert.SerializeObject(actions[0]));
                var user = action["userid"].ToString();  // get the user id
                DocManagerHelper.CreateMeta(fileName, user, "Filling Form", userAddress);  // create meta data for the forcesaved file
            }

            return 0;
        }

        // create a command request
        public static void commandRequest(string method, string key)
        {
            string documentCommandUrl = Startup.AppSettings["files.docservice.url.site"] + Startup.AppSettings["files.docservice.url.command"];

            var request = (HttpWebRequest)WebRequest.Create(documentCommandUrl);
            request.Method = "POST";
            request.ContentType = "application/json";

            var body = new Dictionary<string, object>() {
                { "c", method },
                { "key", key }
            };

            // check if a secret key to generate token exists or not
            if (JwtManager.Enabled)
            {
                var payload = new Dictionary<string, object>
                    {
                        { "payload", body }
                    };

                var payloadToken = JwtManager.Encode(payload);  // encode a payload object into a header token
                var bodyToken = JwtManager.Encode(body);  // encode body into a body token
                string JWTheader = Startup.AppSettings["files.docservice.header"].Equals("") ? "Authorization" : Startup.AppSettings["files.docservice.header"];
                request.Headers.Add(JWTheader, "Bearer " + payloadToken);  // add a header Authorization with a header token and Authorization prefix in it

                body.Add("token", bodyToken);
            }

            var bytes = Encoding.UTF8.GetBytes(JsonConvert.SerializeObject(body));
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
            var responseObj = JsonConvert.DeserializeObject<Dictionary<string, object>>(dataResponse);
            if (!responseObj["error"].ToString().Equals("0"))
            {
                throw new Exception(dataResponse);
            }
        }

        // save file information from the url to the file specified
        public static void DownloadToFile(string url, string path)
        {
            if (string.IsNullOrEmpty(url)) throw new ArgumentException("url");  // url isn't specified
            if (string.IsNullOrEmpty(path)) throw new ArgumentException("path");  // file isn't specified

            var req = (HttpWebRequest)WebRequest.Create(url);
            using (var stream = req.GetResponse().GetResponseStream())  // get input stream of the file information from the url
            {
                if (stream == null) throw new Exception("stream is null");
                const int bufferSize = 4096;

                using (var fs = File.Open(path, FileMode.Create))
                {
                    var buffer = new byte[bufferSize];
                    int readed;
                    while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                    {
                        fs.Write(buffer, 0, readed);  // write bytes to the output stream
                    }
                }
            }
        }
    }
}