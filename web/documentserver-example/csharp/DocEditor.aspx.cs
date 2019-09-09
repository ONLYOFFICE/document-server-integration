/*
 *
 * (c) Copyright Ascensio System SIA 2019
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
*/

using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Web;
using System.Web.Configuration;
using System.Web.Script.Serialization;
using System.Web.UI;
using ASC.Api.DocumentConverter;

namespace OnlineEditorsExample
{
    public partial class DocEditor : Page
    {
        public static string FileName;

        public static string FileUri
        {
            get { return _Default.FileUri(FileName); }
        }

        protected string Key
        {
            get
            {
                return ServiceConverter.GenerateRevisionId(_Default.CurUserHostAddress(null)
                                                           + "/" + Path.GetFileName(FileUri)
                                                           + "/" + File.GetLastWriteTime(_Default.StoragePath(FileName, null)).GetHashCode());
            }
        }

        protected string DocServiceApiUri
        {
            get { return WebConfigurationManager.AppSettings["files.docservice.url.api"] ?? string.Empty; }
        }

        protected string DocConfig { get; private set; }
        protected string History { get; private set; }
        protected string HistoryData { get; private set; }

        public static string CallbackUrl
        {
            get
            {
                var callbackUrl = _Default.Host;
                callbackUrl.Path =
                    HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "webeditor.ashx";
                callbackUrl.Query = "type=track"
                                    + "&fileName=" + HttpUtility.UrlEncode(FileName)
                                    + "&userAddress=" + HttpUtility.UrlEncode(HttpContext.Current.Request.UserHostAddress);
                return callbackUrl.ToString();
            }
        }

        protected void Page_Load(object sender, EventArgs e)
        {
            var externalUrl = Request["fileUrl"];
            if (!string.IsNullOrEmpty(externalUrl))
            {
                FileName = _Default.DoUpload(externalUrl, Request);
            }
            else
            {
                FileName = Request["fileID"];
            }

            var type = Request["type"];
            if (!string.IsNullOrEmpty(type))
            {
                Try(type, Request["sample"], Request);
                Response.Redirect("doceditor.aspx?fileID=" + HttpUtility.UrlEncode(FileName));
            }

            var ext = Path.GetExtension(FileName);

            var editorsMode = Request["editorsMode"] ?? "edit";

            var canEdit = _Default.EditedExts.Contains(ext);
            var mode = canEdit && editorsMode != "view" ? "edit" : "view";

            var config = new Dictionary<string, object>
                {
                    { "type", Request["editorsType"] ?? "desktop" },
                    { "documentType", _Default.DocumentType(FileName) },
                    {
                        "document", new Dictionary<string, object>
                            {
                                { "title", FileName },
                                { "url", FileUri },
                                { "fileType", ext.Trim('.') },
                                { "key", Key },
                                {
                                    "info", new Dictionary<string, object>
                                        {
                                            { "author", "Me" },
                                            { "created", DateTime.Now.ToShortDateString() }
                                        }
                                },
                                {
                                    "permissions", new Dictionary<string, object>
                                        {
                                            { "comment", editorsMode != "view" && editorsMode != "fillForms" && editorsMode != "embedded" },
                                            { "download", true },
                                            { "edit", canEdit && (editorsMode == "edit" || editorsMode == "filter") },
                                            { "fillForms", editorsMode != "view" && editorsMode != "comment" && editorsMode != "embedded" },
                                            { "modifyFilter", editorsMode != "filter" },
                                            { "review", editorsMode == "edit" || editorsMode == "review" }
                                        }
                                }
                            }
                    },
                    {
                        "editorConfig", new Dictionary<string, object>
                            {
                                { "mode", mode },
                                { "lang", Request.Cookies["ulang"]?.Value ?? "en" },
                                { "callbackUrl", CallbackUrl },
                                {
                                    "user", new Dictionary<string, object>
                                        {
                                            { "id", Request.Cookies["uid"]?.Value ?? "uid-1" },
                                            { "name", Request.Cookies["uname"]?.Value ?? "John Smith" }
                                        }
                                },
                                {
                                    "embedded", new Dictionary<string, object>
                                        {
                                            { "saveUrl", FileUri },
                                            { "embedUrl", FileUri },
                                            { "shareUrl", FileUri },
                                            { "toolbarDocked", "top" }
                                        }
                                },
                                {
                                    "customization", new Dictionary<string, object>
                                        {
                                            { "about", true },
                                            { "feedback", true },
                                            {
                                                "goback", new Dictionary<string, object>
                                                    {
                                                        { "url", _Default.Host + "default.aspx" }
                                                    }
                                            }
                                        }
                                }
                            }
                    }
                };

            if (JwtManager.Enabled)
            {
                var token = JwtManager.Encode(config);
                config.Add("token", token);
            }

            var jss = new JavaScriptSerializer();

            DocConfig = jss.Serialize(config);

            try
            {
                GetHistory(out var hist, out var histData);
                if (hist != null && histData != null)
                {
                    History = jss.Serialize(hist);
                    HistoryData = jss.Serialize(histData);
                }
            }
            catch { }
        }

        private void GetHistory(out Dictionary<string, object> history, out Dictionary<string, object> historyData)
        {
            var jss = new JavaScriptSerializer();
            var histDir = _Default.HistoryDir(_Default.StoragePath(FileName, null));

            history = null;
            historyData = null;

            if (_Default.GetFileVersion(histDir) > 0)
            {
                var currentVersion = _Default.GetFileVersion(histDir);
                var hist = new List<Dictionary<string, object>>();
                var histData = new Dictionary<string, object>();

                for (var i = 0; i <= currentVersion; i++)
                {
                    var obj = new Dictionary<string, object>();
                    var dataObj = new Dictionary<string, object>();
                    var verDir = _Default.VersionDir(histDir, i + 1);

                    var key = i == currentVersion ? Key : File.ReadAllText(Path.Combine(verDir, "key.txt"));

                    obj.Add("key", key);
                    obj.Add("version", i);

                    if (i == 0)
                    {
                        var infoPath = Path.Combine(histDir, "createdInfo.json");

                        if (File.Exists(infoPath)) {
                            var info = jss.Deserialize<Dictionary<string, object>>(File.ReadAllText(infoPath));
                            obj.Add("created", info["created"]);
                            obj.Add("user", new Dictionary<string, object>() {
                                { "id", info["id"] },
                                { "name", info["name"] },
                            });
                        }
                    }

                    dataObj.Add("key", key);
                    dataObj.Add("url", i == currentVersion ? FileUri : MakePublicUrl(Directory.GetFiles(verDir, "prev.*")[0]));
                    dataObj.Add("version", i);
                    if (i > 0)
                    {
                        var changes = jss.Deserialize<Dictionary<string, object>>(File.ReadAllText(Path.Combine(_Default.VersionDir(histDir, i), "changes.json")));
                        var change = ((Dictionary<string, object>)((ArrayList)changes["changes"])[0]);

                        obj.Add("changes", changes["changes"]);
                        obj.Add("serverVersion", changes["serverVersion"]);
                        obj.Add("created", change["created"]);
                        obj.Add("user", change["user"]);

                        var prev = (Dictionary<string, object>)histData[(i - 1).ToString()];
                        dataObj.Add("previous", new Dictionary<string, object>() {
                            { "key", prev["key"] },
                            { "url", prev["url"] },
                        });
                        dataObj.Add("changesUrl", MakePublicUrl(Path.Combine(_Default.VersionDir(histDir, i), "diff.zip")));
                    }

                    hist.Add(obj);
                    histData.Add(i.ToString(), dataObj);
                }

                history = new Dictionary<string, object>()
                {
                    { "currentVersion", currentVersion },
                    { "history", hist }
                };
                historyData = histData;
            }
        }

        private string MakePublicUrl(string fullPath)
        {
            var root = HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"];
            return _Default.Host + fullPath.Substring(root.Length).Replace(Path.DirectorySeparatorChar, '/');
        }

        private static void Try(string type, string sample, HttpRequest request)
        {
            string ext;
            switch (type)
            {
                case "document":
                    ext = ".docx";
                    break;
                case "spreadsheet":
                    ext = ".xlsx";
                    break;
                case "presentation":
                    ext = ".pptx";
                    break;
                default:
                    return;
            }
            var demoName = (string.IsNullOrEmpty(sample) ? "new" : "demo") + ext;
            FileName = _Default.GetCorrectName(demoName);

            var filePath = _Default.StoragePath(FileName, null);
            File.Copy(HttpRuntime.AppDomainAppPath + "app_data/" + demoName, filePath);

            var histDir = _Default.HistoryDir(filePath);
            Directory.CreateDirectory(histDir);
            File.WriteAllText(Path.Combine(histDir, "createdInfo.json"), new JavaScriptSerializer().Serialize(new Dictionary<string, object> {
                { "created", DateTime.Now.ToString() },
                { "id", request.Cookies["uid"]?.Value ?? "uid-1" },
                { "name", request.Cookies["uname"]?.Value ?? "John Smith" }
            }));
        }
    }
}