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
using System.Web;
using System.Web.Script.Serialization;
using System.Web.Services;
using System.Diagnostics;

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
                case "download":
                    Download(context);
                    break;
                case "csv":
                    GetCsv(context);
                    break;
                case "files":
                    Files(context);
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
            MustForceSave = 6,
            CorruptedForceSave = 7
        }

        private static void Track(HttpContext context)
        {
            var fileData = TrackManager.readBody(context);

            var userAddress = context.Request["userAddress"];
            var fileName = Path.GetFileName(context.Request["fileName"]);
            var status = (TrackerStatus) (int) fileData["status"];
            var saved = 1;
            switch (status)
            {
                case TrackerStatus.Editing:
                    try
                    {
                        var jss = new JavaScriptSerializer();
                        var actions = jss.Deserialize<List<object>>(jss.Serialize(fileData["actions"]));
                        var action = jss.Deserialize<Dictionary<string, object>>(jss.Serialize(actions[0]));
                        if (action != null && action["type"].ToString().Equals("0"))
                        {
                            var user = action["userid"].ToString();
                            var users = jss.Deserialize<List<object>>(jss.Serialize(fileData["users"]));
                            if (!users.Contains(user))
                            {
                                TrackManager.commandRequest("forcesave", fileData["key"].ToString());
                            }

                        }
                    }
                    catch (Exception e)
                    {
                        Debug.Print(e.StackTrace);
                    }
                    break;

                case TrackerStatus.MustSave:
                case TrackerStatus.Corrupted:
                    try
                    {
                        saved = TrackManager.processSave(fileData, fileName, userAddress);
                    }
                    catch (Exception e)
                    {
                        saved = 1;
                    }
                    context.Response.Write("{\"error\":" + saved + "}");
                    return;

                case TrackerStatus.MustForceSave:
                case TrackerStatus.CorruptedForceSave:
                    try
                    {
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

        private static void Remove(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var fileName = Path.GetFileName(context.Request["fileName"]);
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

        private static void Files(HttpContext context)
        {
            List<Dictionary<string, object>> files = null;

            try
            {
                context.Response.ContentType = "application/json";
                var jss = new JavaScriptSerializer();

                if (context.Request["fileId"] == null)
                {
                    files = _Default.GetFilesInfo();
                    context.Response.Write(jss.Serialize(files));
                }
                else
                {
                    var fileId = context.Request["fileId"];
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

        private static void Download(HttpContext context)
        {
            var fileName = "sample/" + Path.GetFileName(context.Request["filename"]);
            download(fileName, context);
        }

        private static void GetCsv(HttpContext context)
        {
            var fileName = "sample/" + "csv.csv";
            download(fileName, context);
        }

        private static void download(string fileName, HttpContext context)
        {
            var csvPath = HttpRuntime.AppDomainAppPath + "assets/" + fileName;
            FileInfo fileinf = new FileInfo(csvPath);
            context.Response.AddHeader("Content-Length", "" + fileinf.Length);
            context.Response.AddHeader("Content-Type", MimeMapping.GetMimeMapping(csvPath));
            var tmp = HttpUtility.UrlEncode(Path.GetFileName(csvPath));
            tmp = tmp.Replace("+", "%20");
            context.Response.AddHeader("Content-Disposition", "attachment; filename*=UTF-8\'\'" + tmp);
            context.Response.TransmitFile(csvPath);
        }

        public bool IsReusable
        {
            get { return false; }
        }
    }
}