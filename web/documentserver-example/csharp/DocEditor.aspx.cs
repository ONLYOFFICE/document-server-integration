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
            get { return _Default.FileUri(FileName, true); }
        }

        public static string FileUriUser
        {
            get { return _Default.FileUri(FileName, false); }
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
            get { return (WebConfigurationManager.AppSettings["files.docservice.url.site"] ?? string.Empty) + (WebConfigurationManager.AppSettings["files.docservice.url.api"] ?? string.Empty); }
        }

        protected string DocConfig { get; private set; }
        protected string History { get; private set; }
        protected string HistoryData { get; private set; }
        protected string InsertImageConfig { get; private set; }
        protected string compareFileData { get; private set; }
        protected string dataMailMergeRecipients { get; private set; }

        public static string CallbackUrl
        {
            get
            {
                var callbackUrl = new UriBuilder(_Default.GetServerUrl(true));
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
                FileName = Path.GetFileName(Request["fileID"]);
            }

            var type = Request["type"];
            if (!string.IsNullOrEmpty(type))
            {
                Try(type, Request["sample"], Request);
                Response.Redirect("doceditor.aspx?fileID=" + HttpUtility.UrlEncode(FileName));
            }

            var ext = Path.GetExtension(FileName);

            var editorsMode = Request.GetOrDefault("editorsMode", "edit");

            var canEdit = _Default.EditedExts.Contains(ext);
            var mode = canEdit && editorsMode != "view" ? "edit" : "view";

            var jss = new JavaScriptSerializer();

            object favorite = null;
            if (!string.IsNullOrEmpty(Request.Cookies.GetOrDefault("uid", null)))
            {
                favorite = Request.Cookies.GetOrDefault("uid", null).Equals("uid-2");
            }

            var actionLink = Request.GetOrDefault("actionLink", null);
            var actionData = string.IsNullOrEmpty(actionLink) ? null : jss.DeserializeObject(actionLink);

            var config = new Dictionary<string, object>
                {
                    { "type", Request.GetOrDefault("editorsType", "desktop") },
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
                                            { "owner", "Me" },
                                            { "uploaded", DateTime.Now.ToShortDateString() },
                                            { "favorite", favorite }
                                        }
                                },
                                {
                                    "permissions", new Dictionary<string, object>
                                        {
                                            { "comment", editorsMode != "view" && editorsMode != "fillForms" && editorsMode != "embedded" && editorsMode != "blockcontent"},
                                            { "download", true },
                                            { "edit", canEdit && (editorsMode == "edit" || editorsMode == "filter") || editorsMode == "blockcontent" },
                                            { "fillForms", editorsMode != "view" && editorsMode != "comment" && editorsMode != "embedded" && editorsMode != "blockcontent" },
                                            { "modifyFilter", editorsMode != "filter" },
                                            { "modifyContentControl", editorsMode != "blockcontent" },
                                            { "review", editorsMode == "edit" || editorsMode == "review" }
                                        }
                                }
                            }
                    },
                    {
                        "editorConfig", new Dictionary<string, object>
                            {
                                { "actionLink", actionData },
                                { "mode", mode },
                                { "lang", Request.Cookies.GetOrDefault("ulang", "en") },
                                { "callbackUrl", CallbackUrl },
                                {
                                    "user", new Dictionary<string, object>
                                        {
                                            { "id", Request.Cookies.GetOrDefault("uid", "uid-1") },
                                            { "name", Request.Cookies.GetOrDefault("uname", "John Smith") }
                                        }
                                },
                                {
                                    "embedded", new Dictionary<string, object>
                                        {
                                            { "saveUrl", FileUriUser },
                                            { "embedUrl", FileUriUser },
                                            { "shareUrl", FileUriUser },
                                            { "toolbarDocked", "top" }
                                        }
                                },
                                {
                                    "customization", new Dictionary<string, object>
                                        {
                                            { "about", true },
                                            { "feedback", true },
                                            { "forcesave", false },
                                            {
                                                "goback", new Dictionary<string, object>
                                                    {
                                                        { "url", _Default.GetServerUrl(false) + "default.aspx" }
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

            DocConfig = jss.Serialize(config);

            try
            {
                Dictionary<string, object> logoConfig = GetLogoConfig();
                InsertImageConfig = jss.Serialize(logoConfig).Replace("{", "").Replace("}", "");

                Dictionary<string, object> compareFile = GetCompareFile();
                compareFileData = jss.Serialize(compareFile);

                Dictionary<string, object> mailMergeConfig = GetMailMergeConfig();
                dataMailMergeRecipients = jss.Serialize(mailMergeConfig);


                Dictionary<string, object> hist;
                Dictionary<string, object> histData;
  
                GetHistory(out hist, out histData);
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

                for (var i = 1; i <= currentVersion; i++)
                {
                    var obj = new Dictionary<string, object>();
                    var dataObj = new Dictionary<string, object>();
                    var verDir = _Default.VersionDir(histDir, i);

                    var key = i == currentVersion ? Key : File.ReadAllText(Path.Combine(verDir, "key.txt"));

                    obj.Add("key", key);
                    obj.Add("version", i);

                    if (i == 1)
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
                    if (i > 1)
                    {
                        var changes = jss.Deserialize<Dictionary<string, object>>(File.ReadAllText(Path.Combine(_Default.VersionDir(histDir, i - 1), "changes.json")));
                        var change = ((Dictionary<string, object>)((ArrayList)changes["changes"])[0]);

                        obj.Add("changes", changes["changes"]);
                        obj.Add("serverVersion", changes["serverVersion"]);
                        obj.Add("created", change["created"]);
                        obj.Add("user", change["user"]);

                        var prev = (Dictionary<string, object>)histData[(i - 2).ToString()];
                        dataObj.Add("previous", new Dictionary<string, object>() {
                            { "key", prev["key"] },
                            { "url", prev["url"] },
                        });
                        dataObj.Add("changesUrl", MakePublicUrl(Path.Combine(_Default.VersionDir(histDir, i - 1), "diff.zip")));
                    }
                    if (JwtManager.Enabled)
                    {
                        var token = JwtManager.Encode(dataObj);
                        dataObj.Add("token", token);
                    }
                    hist.Add(obj);
                    histData.Add((i - 1).ToString(), dataObj);
                }

                history = new Dictionary<string, object>()
                {
                    { "currentVersion", currentVersion },
                    { "history", hist }
                };
                historyData = histData;
            }
        }

        private Dictionary<string, object> GetLogoConfig()
        {
            var InsertImageUrl = new UriBuilder(_Default.GetServerUrl(true));
            InsertImageUrl.Path = HttpRuntime.AppDomainAppVirtualPath
                + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                + "App_Themes\\images\\logo.png";

            Dictionary<string, object> logoConfig = new Dictionary<string, object>
                {
                    { "fileType", "png"},
                    { "url", InsertImageUrl.ToString()}
                };

            if (JwtManager.Enabled)
            {
                var insImageToken = JwtManager.Encode(logoConfig);
                logoConfig.Add("token", insImageToken);
            }

            return logoConfig;
        }

        private Dictionary<string, object> GetCompareFile()
        {
            var compareFileUrl = new UriBuilder(_Default.GetServerUrl(true));
            compareFileUrl.Path = HttpRuntime.AppDomainAppVirtualPath
                + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                + "webeditor.ashx";
            compareFileUrl.Query = "type=download&fileName=" + HttpUtility.UrlEncode("sample.docx");

            Dictionary<string, object> dataCompareFile = new Dictionary<string, object>
                {
                    { "fileType", "docx" },
                    { "url", compareFileUrl.ToString() }
                };

            if (JwtManager.Enabled)
            {
                var compareFileToken = JwtManager.Encode(dataCompareFile);
                dataCompareFile.Add("token", compareFileToken);
            }

            return dataCompareFile;
        }

        private Dictionary<string, object> GetMailMergeConfig()
        {
            var mailmergeUrl = new UriBuilder(_Default.GetServerUrl(true));
            mailmergeUrl.Path =
                    HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "webeditor.ashx";
            mailmergeUrl.Query = "type=csv";

            Dictionary<string, object> mailMergeConfig = new Dictionary<string, object>
                {
                    { "fileType", "csv" },
                    { "url", mailmergeUrl.ToString() }
                };

            if (JwtManager.Enabled)
            {
                var mailmergeToken = JwtManager.Encode(mailMergeConfig);
                mailMergeConfig.Add("token", mailmergeToken);
            }

            return mailMergeConfig;
        }

        private string MakePublicUrl(string fullPath)
        {
            var root = HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"];
            return _Default.GetServerUrl(true) + fullPath.Substring(root.Length).Replace(Path.DirectorySeparatorChar, '/');
        }

        private static void Try(string type, string sample, HttpRequest request)
        {
            string ext;
            switch (type)
            {
                case "word":
                    ext = ".docx";
                    break;
                case "cell":
                    ext = ".xlsx";
                    break;
                case "slide":
                    ext = ".pptx";
                    break;
                default:
                    return;
            }
            var demoName = (string.IsNullOrEmpty(sample) ? "new" : "sample") + ext;
            var demoPath = "assets\\" + (string.IsNullOrEmpty(sample) ? "new\\" : "sample\\");

            FileName = _Default.GetCorrectName(demoName);

            var filePath = _Default.StoragePath(FileName, null);
            File.Copy(HttpRuntime.AppDomainAppPath + demoPath + demoName, filePath);

            var histDir = _Default.HistoryDir(filePath);
            Directory.CreateDirectory(histDir);
            File.WriteAllText(Path.Combine(histDir, "createdInfo.json"), new JavaScriptSerializer().Serialize(new Dictionary<string, object> {
                { "created", DateTime.Now.ToString("yyyy'-'MM'-'dd HH':'mm':'ss") },
                { "id", request.Cookies.GetOrDefault("uid", "uid-1") },
                { "name", request.Cookies.GetOrDefault("uname", "John Smith") }
            }));
        }
    }
}