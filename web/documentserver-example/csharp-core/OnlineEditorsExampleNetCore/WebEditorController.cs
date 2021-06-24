using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Nancy.Json;
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
    [Route("api/[controller]")]
    [ApiController]
    public class WebEditorController : ControllerBase
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

        //// upload a file
        //[HttpGet]
        //public static void Upload(HttpContext context)
        //{
        //    context.Response.ContentType = "text/plain";
        //    try
        //    {
        //        var httpPostedFile = context.Request.Files[0];
        //        string fileName;

        //        // check from which browser the request came for
        //        if (context.Request.Browser.Browser.ToUpper() == "IE")
        //        {
        //            var files = httpPostedFile.FileName.Split(new char[] { '\\' });
        //            fileName = files[files.Length - 1];  // get file name
        //        }
        //        else
        //        {
        //            fileName = httpPostedFile.FileName;
        //        }

        //        var curSize = httpPostedFile.ContentLength;
        //        if (DocManagerHelper.MaxFileSize < curSize || curSize <= 0)  // check if the file size exceeds the maximum file size
        //        {
        //            throw new Exception("File size is incorrect");
        //        }

        //        var curExt = (Path.GetExtension(fileName) ?? "").ToLower();
        //        if (!DocManagerHelper.FileExts.Contains(curExt))  // check if the file extension is supported by the editor
        //        {
        //            throw new Exception("File type is not supported");
        //        }

        //        fileName = DocManagerHelper.GetCorrectName(fileName);  // get the correct file name if such a name already exists
        //        var documentType = FileUtility.GetFileType(fileName).ToString().ToLower();

        //        var savedFileName = DocManagerHelper.StoragePath(fileName);  // get the storage path to the uploading file
        //        httpPostedFile.SaveAs(savedFileName);  // and save it
        //        // get file meta information or create the default one
        //        var id = context.Request.Cookies.GetOrDefault("uid", null);
        //        var user = Users.getUser(id);
        //        DocManagerHelper.CreateMeta(fileName, user.id, user.name);

        //        context.Response.WriteAsync("{ \"filename\": \"" + fileName + "\", \"documentType\": \"" + documentType + "\"}");
        //    }
        //    catch (Exception e)
        //    {
        //        context.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
        //    }
        //}
        //[HttpPost]
        //public static void Convert(HttpContext context)
        //{
        //    context.Response.ContentType = "text/plain";
        //    try
        //    {
        //        string fileData;

        //        using (var receiveStream = context.Request.InputStream)
        //        using (var readStream = new StreamReader(receiveStream))
        //        {
        //            fileData = readStream.ReadToEnd();
        //            if (string.IsNullOrEmpty(fileData)) context.Response.WriteAsync("{\"error\":1,\"message\":\"Request stream is empty\"}");
        //        }

        //        var jss = new JavaScriptSerializer();
        //        var body = jss.Deserialize<Dictionary<string, object>>(fileData);

        //        var fileName = Path.GetFileName(body["filename"].ToString());
        //        var filePass = body["filePass"] != null ? body["filePass"].ToString() : null;
        //        var fileUri = DocManagerHelper.GetFileUri(fileName, true);

        //        var extension = (Path.GetExtension(fileUri).ToLower() ?? "").Trim('.');
        //        var internalExtension = DocManagerHelper.GetInternalExtension(FileUtility.GetFileType(fileName)).Trim('.');

        //        // check if the file with such an extension can be converted
        //        if (DocManagerHelper.ConvertExts.Contains("." + extension)
        //            && !string.IsNullOrEmpty(internalExtension))
        //        {
        //            // generate document key
        //            var key = ServiceConverter.GenerateRevisionId(fileUri);

        //            var downloadUri = new UriBuilder(DocManagerHelper.GetServerUrl(true))
        //            {
        //                Path = "webeditor.ashx",
        //                Query = "type=download&fileName=" + HttpUtility.UrlEncode(fileName)
        //            };

        //            // get the url to the converted file
        //            string newFileUri;
        //            var result = ServiceConverter.GetConvertedUri(downloadUri.ToString(), extension, internalExtension, key, true, out newFileUri, filePass);
        //            if (result != 100)
        //            {
        //                context.Response.WriteAsync("{ \"step\" : \"" + result + "\", \"filename\" : \"" + fileName + "\"}");
        //                return;
        //            }

        //            // get a file name of an internal file extension with an index if the file with such a name already exists
        //            var correctName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + "." + internalExtension);

        //            var req = (HttpWebRequest)WebRequest.Create(newFileUri);

        //            using (var stream = req.GetResponse().GetResponseStream())  // get response stream of the converting file
        //            {
        //                if (stream == null) throw new Exception("Stream is null");
        //                const int bufferSize = 4096;

        //                using (var fs = File.Open(DocManagerHelper.StoragePath(correctName), FileMode.Create))
        //                {
        //                    var buffer = new byte[bufferSize];
        //                    int readed;
        //                    while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
        //                    {
        //                        fs.Write(buffer, 0, readed);  // write bytes to the output stream
        //                    }
        //                }
        //            }

        //            Remove(fileName);  // remove the original file and its history if it exists
        //            fileName = correctName;  // create meta information about the converted file with user id and name specified
        //            var id = context.Request.Cookies.GetOrDefault("uid", null);
        //            var user = Users.getUser(id);
        //            DocManagerHelper.CreateMeta(fileName, user.id, user.name);
        //        }

        //        var documentType = FileUtility.GetFileType(fileName).ToString().ToLower();
        //        context.Response.WriteAsync("{ \"filename\" : \"" + fileName + "\", \"documentType\": \"" + documentType + "\" }");
        //    }
        //    catch (Exception e)
        //    {
        //        context.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
        //    }
        //}

        // track file changes
        [HttpPost]
        public static void Track(HttpContext context)
        {
            // read request body
            var fileData = TrackManager.readBody(context);

            var userAddress = context.Request.Query["userAddress"];
            var fileName = Path.GetFileName(context.Request.Query["fileName"]);
            var status = (TrackerStatus)(int)fileData["status"];  // get status from the request body
            var saved = 1;  // editing
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
                    catch (Exception)
                    {
                        saved = 1;
                    }
                    context.Response.WriteAsync("{\"error\":" + saved + "}");
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
                    context.Response.WriteAsync("{\"error\":" + saved + "}");
                    return; 
                    
            }
            context.Response.WriteAsync("{\"error\":0}");
            return;
        }


        //// remove a file
        //[HttpDelete]
        //public static void Remove(HttpContext context)
        //{
        //    context.Response.ContentType = "text/plain";
        //    try
        //    {
        //        var fileName = Path.GetFileName(context.Request.Query["fileName"]);
        //        Remove(fileName);  // remove a file and its history if it exists

        //        context.Response.WriteAsync("{ \"success\": true }");
        //    }
        //    catch (Exception e)
        //    {
        //        context.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
        //    }
        //}

        //// remove a file by its name
        //[HttpDelete]
        //public static void Remove(string fileName)
        //{
        //    var path = DocManagerHelper.StoragePath(fileName, null);  // delete file
        //    var histDir = DocManagerHelper.HistoryDir(path);  // delete file history

        //    if (File.Exists(path)) File.Delete(path);
        //    if (Directory.Exists(histDir)) Directory.Delete(histDir, true);
        //}

        // get files information

        [HttpGet]
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

        //// get sample files from the assests
        //public static void Assets(HttpContext context)
        //{
        //    var fileName = Path.GetFileName(context.Request.Query["filename"]);
        //    var filePath = "assets/sample/" + fileName;
        //    download(filePath, context);
        //}

        //// download a csv file
        //public static void GetCsv(HttpContext context)
        //{
        //    var fileName = "csv.csv";
        //    var filePath = "assets/sample/" + fileName;
        //    download(filePath, context);
        //}

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
