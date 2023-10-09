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
using System.Collections;
using System.Net.Sockets;
using ASC.Api.DocumentConverter;

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
                case "gethistory":
                    GetHistory(context);
                    break;
                case "getversiondata":
                    GetVersionData(context);
                    break;
                case "restore":
                    Restore(context);
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
                case "reference":
                    Reference(context);
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

                if (JwtManager.Enabled && isEmbedded == null && userAddress != null && JwtManager.SignatureUseForRequest)
                {
                    string JWTheader = string.IsNullOrEmpty(WebConfigurationManager.AppSettings["files.docservice.header"]) ? "Authorization" : WebConfigurationManager.AppSettings["files.docservice.header"];

                    string token = "";
                    if (context.Request.Headers.AllKeys.Contains(JWTheader, StringComparer.InvariantCultureIgnoreCase))
                    {
                        var headerToken = context.Request.Headers.Get(JWTheader).Substring("Bearer ".Length);
                        token = JwtManager.Decode(headerToken);
                    }
                    if (string.IsNullOrEmpty(token))
                    {
                        context.Response.StatusCode = (int)HttpStatusCode.Forbidden;
                        context.Response.Write("JWT validation failed");
                        return;
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

        private static void GetHistory(HttpContext context)
        {
            var jss = new JavaScriptSerializer();
            var fileName = context.Request["filename"];

            var history = GetHistory(fileName);

            context.Response.Write(jss.Serialize(history));
        }

        private static void GetVersionData(HttpContext context)
        {
            var storagePath = WebConfigurationManager.AppSettings["storage-path"];
            var jss = new JavaScriptSerializer();

            var fileName = context.Request["filename"];
            int version;

            if (!int.TryParse(context.Request["version"], out version))
            {
                context.Response.Write("{ \"error\": \"Version number invalid!\"}");
                return;
            }

            var versionData = new Dictionary<string, object>();

            var histDir = _Default.HistoryDir(_Default.StoragePath(fileName, null));
            var lastVersion = _Default.GetFileVersion(histDir);

            var verDir = _Default.VersionDir(histDir, version);

            var lastVersionUri = _Default.FileUri(fileName, true);
            var key = version == lastVersion
                ? ServiceConverter.GenerateRevisionId(_Default.CurUserHostAddress(null)
                                                           + "/" + Path.GetFileName(lastVersionUri)
                                                           + "/" + File.GetLastWriteTime(_Default.StoragePath(fileName, null)).GetHashCode())
                : File.ReadAllText(Path.Combine(verDir, "key.txt"));


            var ext = Path.GetExtension(fileName).ToLower();
            versionData.Add("fileType", ext.Replace(".", ""));
            versionData.Add("key", key);

            var directPrevFileUrl = version == lastVersion ? _Default.FileUri(fileName, false) : MakePublicHistoryUrl(fileName, version.ToString(), "prev" + ext, false);
            var prevFileUrl = version == lastVersion ? lastVersionUri : MakePublicHistoryUrl(fileName, version.ToString(), "prev" + ext);
            if (Path.IsPathRooted(storagePath))
            {
                prevFileUrl = version == lastVersion ? DocEditor.getDownloadUrl(fileName) : DocEditor.getDownloadUrl(Directory.GetFiles(verDir, "prev.*")[0].Replace(storagePath + "\\", ""));
                directPrevFileUrl = version == lastVersion ? DocEditor.getDownloadUrl(fileName, false) : DocEditor.getDownloadUrl(Directory.GetFiles(verDir, "prev.*")[0].Replace(storagePath + "\\", ""), false);
            }

            versionData.Add("url", prevFileUrl);

            if (_Default.IsEnabledDirectUrl())
            {
                versionData.Add("directUrl", directPrevFileUrl); // write direct url to the data object
            }

            versionData.Add("version", version);
            if (version > 1)
            {
                var prevVerDir = _Default.VersionDir(histDir, version - 1);

                var prevUrl = MakePublicHistoryUrl(fileName, (version - 1).ToString(), "prev" + ext);
                if (Path.IsPathRooted(storagePath))
                    prevUrl = DocEditor.getDownloadUrl(Directory.GetFiles(prevVerDir, "prev.*")[0].Replace(storagePath + "\\", ""));

                var prevKey = File.ReadAllText(Path.Combine(prevVerDir, "key.txt"));

                Dictionary<string, object> dataPrev = new Dictionary<string, object>() {  // write information about previous file version to the data object
                    { "fileType", ext.Replace(".", "") },
                    { "key", prevKey },  // write key and url information about previous file version
                    { "url", prevUrl }
                };

                string directPrevUrl;
                if (_Default.IsEnabledDirectUrl())
                {
                    directPrevUrl = Path.IsPathRooted(storagePath)
                        ? DocEditor.getDownloadUrl(Directory.GetFiles(prevVerDir, "prev.*")[0].Replace(storagePath + "\\", ""), false)
                        : MakePublicHistoryUrl(fileName, (version - 1).ToString(), "prev" + ext, false);

                    dataPrev.Add("directUrl", directPrevUrl); // write direct url to the data object
                }

                versionData.Add("previous", dataPrev);

                if (File.Exists(Path.Combine(prevVerDir, "diff.zip")))
                {
                    var changesUrl = MakePublicHistoryUrl(fileName, (version - 1).ToString(), "diff.zip");
                    versionData.Add("changesUrl", changesUrl);
                }
            }

            if (JwtManager.Enabled)
            {
                var token = JwtManager.Encode(versionData);
                versionData.Add("token", token);
            }

            context.Response.Write(jss.Serialize(versionData));
        }

        private void Restore(HttpContext context)
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

            var fileName = (string)body["fileName"];
            var version = (int)body["version"];

            var lastVersionUri = _Default.FileUri(fileName, true);
            var key = ServiceConverter.GenerateRevisionId(_Default.CurUserHostAddress(null)
                + "/" + Path.GetFileName(lastVersionUri)
                + "/" + File.GetLastWriteTime(_Default.StoragePath(fileName, null)).GetHashCode());

            var histDir = _Default.HistoryDir(_Default.StoragePath(fileName, null));
            var currentVersionDir = _Default.VersionDir(histDir, _Default.GetFileVersion(histDir));
            var verDir = _Default.VersionDir(histDir, version);

            if (!Directory.Exists(currentVersionDir)) Directory.CreateDirectory(currentVersionDir);

            var ext = Path.GetExtension(fileName).ToLower();
            File.Copy(_Default.StoragePath(fileName, null), Path.Combine(currentVersionDir, "prev" + ext));

            File.WriteAllText(Path.Combine(currentVersionDir, "key.txt"), key);

            var changesPath = Path.Combine(_Default.VersionDir(histDir, version - 1), "changes.json");
            if (File.Exists(changesPath))
            {
                File.Copy(changesPath, Path.Combine(currentVersionDir, "changes.json"));
            }

            File.Copy(Path.Combine(verDir, "prev" + ext), _Default.StoragePath(fileName, null), true);

            var fileInfo = new FileInfo(_Default.StoragePath(fileName, null));
            fileInfo.LastWriteTimeUtc = DateTime.UtcNow;

            var history = GetHistory(fileName);

            context.Response.Write(jss.Serialize(history));
        }

        private static void DownloadHistory(HttpContext context)
        {
            try
            {
                var fileName = Path.GetFileName(context.Request["fileName"]);
                var userAddress = Path.GetFileName(context.Request["userAddress"]);
                var version = Path.GetFileName(context.Request["ver"]);
                var file = Path.GetFileName(context.Request["file"]);

                if (JwtManager.Enabled && JwtManager.SignatureUseForRequest)
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

        private static void Reference(HttpContext context)
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
            Dictionary<string, object> referenceData = null;
            var fileName = "";
            var userAddress = "";

            if (body.ContainsKey("referenceData"))
            {
                referenceData = jss.Deserialize<Dictionary<string, object>>(jss.Serialize(body["referenceData"]));
                var instanceId = (string)referenceData["instanceId"];
                var fileKey = (string)referenceData["fileKey"];
                if (instanceId == _Default.GetServerUrl(false))
                {
                    var fileKeyObj = jss.Deserialize<Dictionary<string, object>>(fileKey);
                    userAddress = (string)fileKeyObj["userAddress"];
                    if (userAddress == HttpUtility.UrlEncode(_Default.CurUserHostAddress(HttpContext.Current.Request.UserHostAddress)))
                    {
                        fileName = (string)fileKeyObj["fileName"];
                    }
                }
            }

            if (fileName == "")
            {
                try
                {
                    var path = (string)body["path"];
                    path = Path.GetFileName(path);
                    if (File.Exists(_Default.StoragePath(path, null)))
                    {
                        fileName = path;
                    }
                }
                catch
                {
                    context.Response.Write("{ \"error\": \"Path not found!\"}");
                    return;
                }
            }

            if (fileName == "")
            {
                context.Response.Write("{ \"error\": \"File not found!\"}");
                return;
            }

            var directUrl = (bool) body["directUrl"];

            var data = new Dictionary<string, object>() {
            { "fileType", (Path.GetExtension(fileName) ?? "").ToLower().Trim('.') },
            { "key", ServiceConverter.GenerateRevisionId(_Default.CurUserHostAddress(null)
                        + "/" + Path.GetFileName(_Default.FileUri(fileName, true))
                        + "/" + File.GetLastWriteTime(_Default.StoragePath(fileName, null)).GetHashCode()) },
            { "url",  DocEditor.getDownloadUrl(fileName)},
            { "directUrl", directUrl ? DocEditor.getDownloadUrl(fileName, false) : null},
            { "referenceData", new Dictionary<string, string>()
                {
                    { "fileKey", jss.Serialize(new Dictionary<string, object>{
                            {"fileName", fileName},
                            {"userAddress", HttpUtility.UrlEncode(_Default.CurUserHostAddress(HttpContext.Current.Request.UserHostAddress))}
                    })
                    },
                    {"instanceId", _Default.GetServerUrl(false) }
                }
            },
            { "path", fileName }
            };

            if (JwtManager.Enabled)
            {
                var token = JwtManager.Encode(data);
                data.Add("token", token);
            }

            context.Response.Write(jss.Serialize(data));
        }

        // get the document history
        private static Dictionary<string, object> GetHistory(string fileName)
        {
            var jss = new JavaScriptSerializer();
            var histDir = _Default.HistoryDir(_Default.StoragePath(fileName, null));

            var history = new Dictionary<string, object>();

            var currentVersion = _Default.GetFileVersion(histDir);
            var currentFileUri = _Default.FileUri(fileName, true);
            var currentKey = ServiceConverter.GenerateRevisionId(_Default.CurUserHostAddress(null)
                                                           + "/" + Path.GetFileName(currentFileUri)
                                                           + "/" + File.GetLastWriteTime(_Default.StoragePath(fileName, null)).GetHashCode());

            var versionList = new List<Dictionary<string, object>>();
            for (var versionNum = 1; versionNum <= currentVersion; versionNum++)
            {
                var versionObj = new Dictionary<string, object>();
                var verDir = _Default.VersionDir(histDir, versionNum);  // get the path to the given file version

                var key = versionNum == currentVersion ? currentKey : File.ReadAllText(Path.Combine(verDir, "key.txt"));  // get document key

                versionObj.Add("key", key);
                versionObj.Add("version", versionNum);

                var changesPath = Path.Combine(_Default.VersionDir(histDir, versionNum - 1), "changes.json");
                if (versionNum == 1 || !File.Exists(changesPath))  // check if the version number is equal to 1
                {
                    var infoPath = Path.Combine(histDir, "createdInfo.json");  // get meta data of this file
                    if (File.Exists(infoPath))
                    {
                        var info = jss.Deserialize<Dictionary<string, object>>(File.ReadAllText(infoPath));
                        versionObj.Add("created", info["created"]);  // write meta information to the object (user information and creation date)
                        versionObj.Add("user", new Dictionary<string, object>()
                        {
                            { "id", info["id"] },
                            { "name", info["name"] },
                        });
                    }
                }
                else if (versionNum > 1)  // check if the version number is greater than 1 (the file was modified)
                {
                    // get the path to the changes.json file
                    var changes = jss.Deserialize<Dictionary<string, object>>(File.ReadAllText(changesPath));
                    var changesArray = (ArrayList)changes["changes"];
                    var change = changesArray.Count > 0
                        ? (Dictionary<string, object>)changesArray[0]
                        : new Dictionary<string, object>();

                    // write information about changes to the object
                    versionObj.Add("changes", change.Count > 0 ? changes["changes"] : null);
                    versionObj.Add("serverVersion", changes["serverVersion"]);
                    versionObj.Add("created", change.Count > 0 ? change["created"] : null);
                    versionObj.Add("user", change.Count > 0 ? change["user"] : null);
                }

                versionList.Add(versionObj);
            }

            history.Add("currentVersion", currentVersion);
            history.Add("history", versionList);

            return history;
        }

        // create the public history url
        private static string MakePublicHistoryUrl(string filename, string version, string file, Boolean isServer = true)
        {
            var userAddress = isServer ? "&userAddress=" + HttpUtility.UrlEncode(_Default.CurUserHostAddress(HttpContext.Current.Request.UserHostAddress)) : "";
            var fileUrl = new UriBuilder(_Default.GetServerUrl(isServer));
            fileUrl.Path = HttpRuntime.AppDomainAppVirtualPath
                + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                + "webeditor.ashx";
            fileUrl.Query = "type=downloadhistory&fileName=" + HttpUtility.UrlEncode(filename)
                + "&ver=" + version + "&file=" + file
                + userAddress;
            return fileUrl.ToString();
        }
    }
}