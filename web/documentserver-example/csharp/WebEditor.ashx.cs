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
using System.Collections.Generic;
using System.IO;
using System.Web;
using System.Web.Script.Serialization;
using System.Web.Services;
using System.Diagnostics;
using System.Web.Configuration;
using System.Linq;
using System.Net;

namespace OnlineEditorsExample
{
    [WebService(Namespace = "http://tempuri.org/")]
    [WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
    public class WebEditor : IHttpHandler
    {
        public void ProcessRequest(HttpContext context)
        {
            // define functions for each type of operation
            switch (context.Request["type"])
            {
                case "upload":
                    Upload(context);
                    break;
                case "download":
                    Download(context);
                    break;
                case "downloadhistory":
                    DownloadHistory(context);
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
                case "assets":
                    Assets(context);
                    break;
                case "csv":
                    GetCsv(context);
                    break;
                case "files":
                    Files(context);
                    break;
                case "saveas":
                    SaveAs(context);
                    break;
                case "rename":
                    Rename(context);
                    break;
            }
        }

        private static void SaveAs(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var result = _Default.DoSaveAs(context);
                context.Response.Write(result);
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + 1 + "\", \"message\": \"" + e.Message + "\"}");
            }
        }

        // upload a file
        private static void Upload(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var filename = _Default.DoUpload(context);
                var documentType = _Default.DocumentType(filename);
                // get file name of the uploading file and write it to the response
                context.Response.Write("{ \"filename\": \"" + filename + "\", \"documentType\": \"" + documentType + "\"}");
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        // convert a file
        private static void Convert(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                // get file name of the converting file and write it to the response
                context.Response.Write(_Default.DoConvert(context));
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
        }

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

        // track file changes
        private static void Track(HttpContext context)
        {
            // read request body
            var fileData = TrackManager.readBody(context);

            var userAddress = context.Request["userAddress"];
            var fileName = Path.GetFileName(context.Request["fileName"]);
            var status = (TrackerStatus) (int) fileData["status"];  // get status from the request body
            var saved = 0;
            switch (status)
            {
                case TrackerStatus.Editing:
                    try
                    {
                        var jss = new JavaScriptSerializer();
                        var actions = jss.Deserialize<List<object>>(jss.Serialize(fileData["actions"]));
                        var action = jss.Deserialize<Dictionary<string, object>>(jss.Serialize(actions[0]));
                        if (action != null && action["type"].ToString().Equals("0"))  // finished edit
                        {
                            var user = action["userid"].ToString();  // the user who finished editing
                            var users = jss.Deserialize<List<object>>(jss.Serialize(fileData["users"]));
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
                    break;

                // MustSave, Corrupted
                case TrackerStatus.MustSave:
                case TrackerStatus.Corrupted:
                    try
                    {
                        // saving a document
                        saved = TrackManager.processSave(fileData, fileName, userAddress);
                    }
                    catch (Exception e)
                    {
                        saved = 1;
                    }
                    context.Response.Write("{\"error\":" + saved + "}");
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
                    context.Response.Write("{\"error\":" + saved + "}");
                    return;
            }
            context.Response.Write("{\"error\":" + saved + "}");
        }

        // remove a file
        private static void Remove(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var fileName = Path.GetFileName(context.Request["fileName"]);
                var path = _Default.StoragePath(fileName, HttpUtility.UrlEncode(_Default.CurUserHostAddress(HttpContext.Current.Request.UserHostAddress)));
                var histDir = _Default.HistoryDir(path);

                if (File.Exists(path)) File.Delete(path);  // delete file
                if (Directory.Exists(histDir)) Directory.Delete(histDir, true);  // delete file history

                context.Response.Write("{ \"success\": true }");
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        // get files information
        private static void Files(HttpContext context)
        {
            List<Dictionary<string, object>> files = null;

            try
            {
                context.Response.ContentType = "application/json";
                var jss = new JavaScriptSerializer();

                if (context.Request["fileId"] == null)
                {
                    files = _Default.GetFilesInfo();  // get the information about the files from the storage path
                    context.Response.Write(jss.Serialize(files));
                }
                else
                {
                    var fileId = context.Request["fileId"];  // get file id from the request
                    files = _Default.GetFilesInfo(fileId);
                    if (files.Count == 0)
                    {
                        context.Response.Write("\"File not found\"");
                    }
                    else
                    {
                        context.Response.Write(jss.Serialize(files));
                    }
                }
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        // get sample files from the assests
        private static void Assets(HttpContext context)
        {
            var fileName = Path.GetFileName(context.Request["filename"]);
            var filePath = HttpRuntime.AppDomainAppPath + "assets/sample/" + fileName;
            download(filePath, context);
        }

        // download a csv file
        private static void GetCsv(HttpContext context)
        {
            var fileName = "csv.csv";
            var filePath = HttpRuntime.AppDomainAppPath + "assets/sample/" + fileName;
            download(filePath, context);
        }

        // download a file
        private static void Download(HttpContext context)
        {
            try
            {
                var fileName = Path.IsPathRooted(WebConfigurationManager.AppSettings["storage-path"]) ? context.Request["fileName"] : Path.GetFileName(context.Request["fileName"]);
                var userAddress = Path.GetFileName(context.Request["userAddress"]);
                var isEmbedded = context.Request["dmode"];

                if (JwtManager.Enabled && isEmbedded == null)
                {
                    string JWTheader = string.IsNullOrEmpty(WebConfigurationManager.AppSettings["files.docservice.header"]) ? "Authorization" : WebConfigurationManager.AppSettings["files.docservice.header"];

                    if (context.Request.Headers.AllKeys.Contains(JWTheader, StringComparer.InvariantCultureIgnoreCase))
                    {
                        var headerToken = context.Request.Headers.Get(JWTheader).Substring("Bearer ".Length);
                        string token = JwtManager.Decode(headerToken);

                        if (string.IsNullOrEmpty(token))
                        {
                            context.Response.StatusCode = (int)HttpStatusCode.Forbidden;
                            context.Response.Write("JWT validation failed");
                            return;
                        }
                    }
                }

                var filePath = _Default.ForcesavePath(fileName, userAddress, false);  // get the path to the force saved document version
                if (string.IsNullOrEmpty(filePath))
                {
                    filePath = _Default.StoragePath(fileName, userAddress);  // or to the original document
                }
                download(filePath, context);
            }
            catch (Exception)
            {
                context.Response.Write("{ \"error\": \"File not found!\"}");
            }
        }

        // download data from the url to the file
        private static void download(string filePath, HttpContext context)
        {
            FileInfo fileinf = new FileInfo(filePath);
            context.Response.AddHeader("Content-Length", "" + fileinf.Length);  // set headers to the response
            context.Response.AddHeader("Content-Type", MimeMapping.GetMimeMapping(filePath));
            var tmp = HttpUtility.UrlEncode(Path.GetFileName(filePath));
            tmp = tmp.Replace("+", "%20");
            context.Response.AddHeader("Content-Disposition", "attachment; filename*=UTF-8\'\'" + tmp);
            context.Response.TransmitFile(filePath);
        }

        public bool IsReusable
        {
            get { return false; }
        }

        private static void DownloadHistory(HttpContext context)
        {
            try
            {
                var fileName = Path.GetFileName(context.Request["fileName"]);
                var userAddress = Path.GetFileName(context.Request["userAddress"]);
                var version = Path.GetFileName(context.Request["ver"]);
                var file = Path.GetFileName(context.Request["file"]);

                if (JwtManager.Enabled)
                {
                    string JWTheader = string.IsNullOrEmpty(WebConfigurationManager.AppSettings["files.docservice.header"]) ? "Authorization" : WebConfigurationManager.AppSettings["files.docservice.header"];

                    if (context.Request.Headers.AllKeys.Contains(JWTheader, StringComparer.InvariantCultureIgnoreCase))
                    {
                        var headerToken = context.Request.Headers.Get(JWTheader).Substring("Bearer ".Length);
                        string token = JwtManager.Decode(headerToken);

                        if (string.IsNullOrEmpty(token))
                        {
                            context.Response.StatusCode = (int)HttpStatusCode.Forbidden;
                            context.Response.Write("JWT validation failed");
                            return;
                        }
                    }
                    else
                    {
                        context.Response.StatusCode = (int)HttpStatusCode.Forbidden;
                        context.Response.Write("JWT validation failed");
                        return;
                    }
                }

                var filePath = _Default.HistoryPath(fileName, userAddress, version, file);  // get the path to the force saved document version

                download(filePath, context);
            }
            catch (Exception)
            {
                context.Response.Write("{ \"error\": \"File not found!\"}");
            }
        }

        // rename a file
        private static void Rename(HttpContext context)
        {
           string fileData;
            try
            {
                using (var receiveStream = context.Request.InputStream)
                using (var readStream = new StreamReader(receiveStream))
                {
                    fileData = readStream.ReadToEnd();
                    if (string.IsNullOrEmpty(fileData)) return;
                }
            }
            catch (Exception e)
            {
                throw new HttpException((int)HttpStatusCode.BadRequest, e.Message);
            }

            var jss = new JavaScriptSerializer();
            var body = jss.Deserialize<Dictionary<string, object>>(fileData);
            var newFileName = (string) body["newfilename"];
            var docKey = (string) body["dockey"];

            var origExt = '.' + (string) body["ext"];
            var curExt = Path.GetExtension(newFileName).ToLower();

            if (string.Compare(origExt, curExt, true) != 0)
            {
                newFileName += origExt;
            }

            var meta =  new Dictionary<string, object>() {
                { "title", newFileName }
            };
            TrackManager.commandRequest("meta", docKey, meta);
            context.Response.Write("{ \"result\": \"OK\"}");
        }
    }
}