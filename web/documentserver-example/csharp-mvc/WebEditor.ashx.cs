﻿/**
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
using System.Web.Script.Serialization;
using System.Web.Services;
using System.Web.Configuration;
using OnlineEditorsExampleMVC.Helpers;
using OnlineEditorsExampleMVC.Models;
using System.Diagnostics;

namespace OnlineEditorsExampleMVC
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
            }
        }

        // upload a file
        private static void Upload(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var httpPostedFile = context.Request.Files[0];
                string fileName;

                // check from which browser the request came for
                if (HttpContext.Current.Request.Browser.Browser.ToUpper() == "IE")
                {
                    var files = httpPostedFile.FileName.Split(new char[] { '\\' });
                    fileName = files[files.Length - 1];  // get file name
                }
                else
                {
                    fileName = httpPostedFile.FileName;
                }

                var curSize = httpPostedFile.ContentLength;
                if (DocManagerHelper.MaxFileSize < curSize || curSize <= 0)  // check if the file size exceeds the maximum file size
                {
                    throw new Exception("File size is incorrect");
                }

                var curExt = (Path.GetExtension(fileName) ?? "").ToLower();
                if (!DocManagerHelper.FileExts.Contains(curExt))  // check if the file extension is supported by the editor
                {
                    throw new Exception("File type is not supported");
                }

                fileName = DocManagerHelper.GetCorrectName(fileName);  // get the correct file name if such a name already exists

                var savedFileName = DocManagerHelper.StoragePath(fileName);  // get the storage path to the uploading file
                httpPostedFile.SaveAs(savedFileName);  // and save it
                // get file meta information or create the default one
                DocManagerHelper.CreateMeta(fileName, context.Request.Cookies.GetOrDefault("uid", ""), context.Request.Cookies.GetOrDefault("uname", ""));

                context.Response.Write("{ \"filename\": \"" + fileName + "\"}");
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
                var fileName = Path.GetFileName(context.Request["filename"]);
                var fileUri = DocManagerHelper.GetFileUri(fileName, true);

                var extension = (Path.GetExtension(fileUri) ?? "").Trim('.');
                var internalExtension = DocManagerHelper.GetInternalExtension(FileUtility.GetFileType(fileName)).Trim('.');

                // check if the file with such an extension can be converted
                if (DocManagerHelper.ConvertExts.Contains("." + extension)
                    && !string.IsNullOrEmpty(internalExtension))
                {
                    // generate document key
                    var key = ServiceConverter.GenerateRevisionId(fileUri);

                    // get the url to the converted file
                    string newFileUri;
                    var result = ServiceConverter.GetConvertedUri(fileUri, extension, internalExtension, key, true, out newFileUri);
                    if (result != 100)
                    {
                        context.Response.Write("{ \"step\" : \"" + result + "\", \"filename\" : \"" + fileName + "\"}");
                        return;
                    }

                    // get a file name of an internal file extension with an index if the file with such a name already exists
                    var correctName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + "." + internalExtension);

                    var req = (HttpWebRequest)WebRequest.Create(newFileUri);

                    using (var stream = req.GetResponse().GetResponseStream())  // get response stream of the converting file
                    {
                        if (stream == null) throw new Exception("Stream is null");
                        const int bufferSize = 4096;

                        using (var fs = File.Open(DocManagerHelper.StoragePath(correctName), FileMode.Create))
                        {
                            var buffer = new byte[bufferSize];
                            int readed;
                            while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                            {
                                fs.Write(buffer, 0, readed);  // write bytes to the output stream
                            }
                        }
                    }

                    Remove(fileName);  // remove the original file and its history if it exists
                    fileName = correctName;  // create meta information about the converted file with user id and name specified
                    DocManagerHelper.CreateMeta(fileName, context.Request.Cookies.GetOrDefault("uid", ""), context.Request.Cookies.GetOrDefault("uname", ""));
                }

                context.Response.Write("{ \"filename\" : \"" + fileName + "\"}");
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
            var saved = 1;  // editing
            switch (status)
            {
                case TrackerStatus.Editing:
                    try
                    {
                        var jss = new JavaScriptSerializer();
                        var actions = jss.Deserialize <List<object>> (jss.Serialize(fileData["actions"]));
                        var action = jss.Deserialize <Dictionary<string, object>> (jss.Serialize(actions[0]));
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

            context.Response.Write("{\"error\":0}");
        }

        // remove a file
        private static void Remove(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var fileName = Path.GetFileName(context.Request["fileName"]);
                Remove(fileName);  // remove a file and its history if it exists

                context.Response.Write("{ \"success\": true }");
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        // remove a file by its name
        private static void Remove(string fileName)
        {
            var path = DocManagerHelper.StoragePath(fileName, null);  // delete file
            var histDir = DocManagerHelper.HistoryDir(path);  // delete file history

            if (File.Exists(path)) File.Delete(path);
            if (Directory.Exists(histDir)) Directory.Delete(histDir, true);
        }

        // get files information
        private static void Files(HttpContext context)
        {
            List<Dictionary<string, object>> files = null;

            try
            {
                var jss = new JavaScriptSerializer();
                context.Response.ContentType = "application/json";

                if (context.Request["fileId"] == null)
                {
                    files = DocManagerHelper.GetFilesInfo();  // get the information about the files from the storage path
                    context.Response.Write(jss.Serialize(files));
                }
                else
                {
                    var fileId = context.Request["fileId"];  // get file id from the request
                    files = DocManagerHelper.GetFilesInfo(fileId);
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
                var fileName = Path.GetFileName(context.Request["filename"]);

                var filePath = DocManagerHelper.ForcesavePath(fileName, null, false);  // get the path to the force saved document version
                if (filePath.Equals(""))
                {
                    filePath = DocManagerHelper.StoragePath(fileName, null);  // or to the original document
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
            var fileinf = new FileInfo(filePath);
            context.Response.AddHeader("Content-Length", fileinf.Length.ToString());  // set headers to the response
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
    }
}