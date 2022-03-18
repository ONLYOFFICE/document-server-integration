/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

        // get url to the original file
        public static string FileUri
        {
            get { return _Default.FileUri(FileName, true); }
        }

        // get url to the original file for Document Server
        public static string FileUriUser
        {
            get { return Path.IsPathRooted(WebConfigurationManager.AppSettings["storage-path"]) ? getDownloadUrl(FileName) + "&dmode=emb" : _Default.FileUri(FileName, false); }
        }

        protected string Key
        {
            get
            {
                // generate document key
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
        protected string CompareFileData { get; private set; }
        protected string DataMailMergeRecipients { get; private set; }
        protected string UsersForMentions { get; private set; }
        protected string DocumentType { get { return _Default.DocumentType(FileName); } }

        // get callback url
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

        // get url to the created file
        public static string getCreateUrl(String documentType, String editorsType)
        {
            var createUrl = new UriBuilder(_Default.GetServerUrl(false));
            createUrl.Path =
                HttpRuntime.AppDomainAppVirtualPath
                + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                + "doceditor.aspx";
            createUrl.Query = "type=" + documentType
                                + "&editorsType=" + editorsType;
            return createUrl.ToString();
        }

        // get url to download a file
        public static string getDownloadUrl(string fileName)
        {
            var downloadUrl = new UriBuilder(_Default.GetServerUrl(true));
                downloadUrl.Path =
                    HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "webeditor.ashx";
                downloadUrl.Query = "type=download"
                                    + "&fileName=" + HttpUtility.UrlEncode(fileName)
                                    + "&userAddress=" + HttpUtility.UrlEncode(HttpContext.Current.Request.UserHostAddress);
                return downloadUrl.ToString();
        }

        // loading a page
        protected void Page_Load(object sender, EventArgs e)
        {
            // get file url
            var externalUrl = Request["fileUrl"];
            if (!string.IsNullOrEmpty(externalUrl))
            {
                // and upload the file by the file url and the request
                FileName = _Default.DoUpload(externalUrl, Request);
            }
            else  // if it doesn't exist
            {
                // get file name
                FileName = Path.GetFileName(Request["fileID"]);
            }

            // get file type
            var type = Request["type"];
            if (!string.IsNullOrEmpty(type))
            {
                // create demo document of a specified file type
                Try(type, Request["sample"], Request);
                Response.Redirect("doceditor.aspx?fileID=" + HttpUtility.UrlEncode(FileName));
            }

            // get file extension
            var ext = Path.GetExtension(FileName).ToLower();

            // get editor mode or set the default one (edit)
            var editorsMode = Request.GetOrDefault("editorsMode", "edit");

            var canEdit = _Default.EditedExts.Contains(ext);  // check if this file can be edited
            var editorsType = Request.GetOrDefault("editorsType", "desktop");

            var id = Request.Cookies.GetOrDefault("uid", null);
            var user = Users.getUser(id);  // get the user
            
            if ((!canEdit && editorsMode.Equals("edit") || editorsMode.Equals("fillForms")) && _Default.FillFormsExts.Contains(ext)) {
                editorsMode = "fillForms";
                canEdit = true;
            }            
            var submitForm = editorsMode.Equals("fillForms") && id.Equals("uid-1") && false;  // check if the Submit form button is displayed or hidden
            var mode = canEdit && editorsMode != "view" ? "edit" : "view";  // get the editor opening mode (edit or view)

            var jss = new JavaScriptSerializer();

            // favorite icon state
            bool? favorite = user.favorite;

            var actionLink = Request.GetOrDefault("actionLink", null);  // get the action link (comment or bookmark) if it exists
            var actionData = string.IsNullOrEmpty(actionLink) ? null : jss.DeserializeObject(actionLink);  // get action data for the action link

            var createUrl = getCreateUrl(DocumentType, editorsType);
            var templatesImageUrl = GetTemplateImageUrl(ext); // image url for templates
            var templates = new List<Dictionary<string, string>>
            {
                new Dictionary<string, string>()
                {
                    { "image", "" },
                    { "title", "Blank" },
                    { "url", createUrl }
                },
                new Dictionary<string, string>()
                {
                    { "image", templatesImageUrl },
                    { "title", "With sample content" },
                    { "url", createUrl + "&sample=true" }
                }
            };

            // specify the document config
            var config = new Dictionary<string, object>
                {
                    { "type", editorsType },
                    { "documentType", DocumentType },
                    {
                        "document", new Dictionary<string, object>
                            {
                                { "title", FileName },
                                { "url", getDownloadUrl(FileName) },
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
                                    // the permission for the document to be edited and downloaded or not
                                    "permissions", new Dictionary<string, object>
                                        {
                                            { "comment", editorsMode != "view" && editorsMode != "fillForms" && editorsMode != "embedded" && editorsMode != "blockcontent"},
                                            { "copy", !user.deniedPermissions.Contains("copy") },
                                            { "download", !user.deniedPermissions.Contains("download") },
                                            { "edit", canEdit && (editorsMode == "edit" || editorsMode =="view" || editorsMode == "filter" || editorsMode == "blockcontent") },
                                            { "print", !user.deniedPermissions.Contains("print") },
                                            { "fillForms", editorsMode != "view" && editorsMode != "comment" && editorsMode != "embedded" && editorsMode != "blockcontent" },
                                            { "modifyFilter", editorsMode != "filter" },
                                            { "modifyContentControl", editorsMode != "blockcontent" },
                                            { "review", canEdit && (editorsMode == "edit" || editorsMode == "review") },
                                            { "reviewGroups", user.reviewGroups },
                                            { "commentGroups", user.commentGroups },
                                            { "userInfoGroups", user.userInfoGroups }
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
                                { "callbackUrl", CallbackUrl },  // absolute URL to the document storage service
                                { "createUrl", !user.id.Equals("uid-0") ? createUrl : null },
                                { "templates", user.templates ? templates : null },
                                {
                                    // the user currently viewing or editing the document
                                    "user", new Dictionary<string, object>
                                        {
                                            { "id", !user.id.Equals("uid-0") ? user.id : null },
                                            { "name",  user.name },
                                            { "group", user.group }
                                        }
                                },
                                {
                                    // the parameters for the embedded document type
                                    "embedded", new Dictionary<string, object>
                                        {
                                            { "saveUrl", FileUriUser },  // the absolute URL that will allow the document to be saved onto the user personal computer
                                            { "embedUrl", FileUriUser },  // the absolute URL to the document serving as a source file for the document embedded into the web page
                                            { "shareUrl", FileUriUser },  // the absolute URL that will allow other users to share this document
                                            { "toolbarDocked", "top" }  // the place for the embedded viewer toolbar (top or bottom)
                                        }
                                },
                                {
                                    // the parameters for the editor interface
                                    "customization", new Dictionary<string, object>
                                        {
                                            { "about", true },  // the About section display
                                            { "comments", true },
                                            { "feedback", true },  // the Feedback & Support menu button display
                                            { "forcesave", false },  // adds the request for the forced file saving to the callback handler
                                            { "submitForm", submitForm },  // if the Submit form button is displayed or not
                                            {
                                                "goback", new Dictionary<string, object>  // settings for the Open file location menu button and upper right corner button
                                                    {
                                                        { "url", _Default.GetServerUrl(false) + "default.aspx" }  // the absolute URL to the website address which will be opened when clicking the Open file location menu button
                                                    }
                                            }
                                        }
                                }
                            }
                    }
                };

            // if the secret key to generate token exists
            if (JwtManager.Enabled)
            {
                // encode the document config into a token
                var token = JwtManager.Encode(config);
                config.Add("token", token);
            }

            DocConfig = jss.Serialize(config);

            try
            {
                // a logo which will be inserted into the document
                Dictionary<string, object> logoConfig = GetLogoConfig();
                InsertImageConfig = jss.Serialize(logoConfig).Replace("{", "").Replace("}", "");

                // a document which will be compared with the current document
                Dictionary<string, object> compareFile = GetCompareFile();
                CompareFileData = jss.Serialize(compareFile);

                // recipient data for mail merging
                Dictionary<string, object> mailMergeConfig = GetMailMergeConfig();
                DataMailMergeRecipients = jss.Serialize(mailMergeConfig);

                // get users for mentions
                List<Dictionary<string, object>> usersData = Users.getUsersForMentions(user.id);
                UsersForMentions = !user.id.Equals("uid-0") ? jss.Serialize(usersData) : null;

                Dictionary<string, object> hist;
                Dictionary<string, object> histData;
  
                // get the document history
                GetHistory(out hist, out histData);
                if (hist != null && histData != null)
                {
                    History = jss.Serialize(hist);
                    HistoryData = jss.Serialize(histData);
                }
            }
            catch { }
        }

        // get the document history
        private void GetHistory(out Dictionary<string, object> history, out Dictionary<string, object> historyData)
        {
            var storagePath = WebConfigurationManager.AppSettings["storage-path"];
            var jss = new JavaScriptSerializer();
            var histDir = _Default.HistoryDir(_Default.StoragePath(FileName, null));

            history = null;
            historyData = null;

            if (_Default.GetFileVersion(histDir) > 0)  // if the file was modified (the file version is greater than 0)
            {
                var currentVersion = _Default.GetFileVersion(histDir);
                var hist = new List<Dictionary<string, object>>();
                var histData = new Dictionary<string, object>();

                for (var i = 1; i <= currentVersion; i++)  // run through all the file versions
                {
                    var obj = new Dictionary<string, object>();
                    var dataObj = new Dictionary<string, object>();
                    var verDir = _Default.VersionDir(histDir, i);  // get the path to the given file version

                    var key = i == currentVersion ? Key : File.ReadAllText(Path.Combine(verDir, "key.txt"));  // get document key

                    obj.Add("key", key);
                    obj.Add("version", i);

                    if (i == 1)  // check if the version number is equal to 1
                    {
                        var infoPath = Path.Combine(histDir, "createdInfo.json");  // get meta data of this file

                        if (File.Exists(infoPath)) {
                            var info = jss.Deserialize<Dictionary<string, object>>(File.ReadAllText(infoPath));
                            obj.Add("created", info["created"]);  // write meta information to the object (user information and creation date)
                            obj.Add("user", new Dictionary<string, object>() {
                                { "id", info["id"] },
                                { "name", info["name"] },
                            });
                        }
                    }

                    var ext = Path.GetExtension(FileName).ToLower();
                    dataObj.Add("fileType", ext.Replace(".", ""));
                    dataObj.Add("key", key);
                    // write file url to the data object
                    var prevFileUrl = i == currentVersion ? FileUri : MakePublicHistoryUrl(FileName, i.ToString(), "prev" + ext);
                    if (Path.IsPathRooted(storagePath))
                    {
                        prevFileUrl = i == currentVersion ? getDownloadUrl(FileName) : getDownloadUrl(Directory.GetFiles(verDir, "prev.*")[0].Replace(storagePath + "\\", ""));
                    }

                    dataObj.Add("url", prevFileUrl);  // write file url to the data object
                    dataObj.Add("version", i);
                    if (i > 1)  // check if the version number is greater than 1 (the file was modified)
                    {
                        // get the path to the changes.json file 
                        var changes = jss.Deserialize<Dictionary<string, object>>(File.ReadAllText(Path.Combine(_Default.VersionDir(histDir, i - 1), "changes.json")));
                        var changesArray = (ArrayList)changes["changes"];
                        var change = changesArray.Count > 0
                            ? (Dictionary<string, object>)changesArray[0]
                            : new Dictionary<string, object>();

                        // write information about changes to the object
                        obj.Add("changes", change.Count > 0 ? changes["changes"] : null);
                        obj.Add("serverVersion", changes["serverVersion"]);
                        obj.Add("created", change.Count > 0 ? change["created"] : null);
                        obj.Add("user", change.Count > 0 ? change["user"] : null);

                        var prev = (Dictionary<string, object>)histData[(i - 2).ToString()];  // get the history data from the previous file version
                        dataObj.Add("previous", new Dictionary<string, object>() {  // write information about previous file version to the data object
                            { "fileType", prev["fileType"] },
                            { "key", prev["key"] },  // write key and url information about previous file version
                            { "url", prev["url"] },
                        });
                        // write the path to the diff.zip archive with differences in this file version
                        var changesUrl = MakePublicHistoryUrl(FileName, (i - 1).ToString(), "diff.zip");
                        dataObj.Add("changesUrl", changesUrl);
                    }
                    if (JwtManager.Enabled)
                    {
                        var token = JwtManager.Encode(dataObj);
                        dataObj.Add("token", token);
                    }
                    hist.Add(obj);  // add object dictionary to the hist list
                    histData.Add((i - 1).ToString(), dataObj);  // write data object information to the history data
                }

                // write history information about the current file version to the history object
                history = new Dictionary<string, object>()
                {
                    { "currentVersion", currentVersion },
                    { "history", hist }
                };
                historyData = histData;
            }
        }

        // get a logo config
        private Dictionary<string, object> GetLogoConfig()
        {
            // get the path to the logo image
            var InsertImageUrl = new UriBuilder(_Default.GetServerUrl(true));
            InsertImageUrl.Path = HttpRuntime.AppDomainAppVirtualPath
                + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                + "App_Themes\\images\\logo.png";

            // create a logo config
            Dictionary<string, object> logoConfig = new Dictionary<string, object>
                {
                    { "fileType", "png"},
                    { "url", InsertImageUrl.ToString()}
                };

            if (JwtManager.Enabled)  // if the secret key to generate token exists
            {
                var insImageToken = JwtManager.Encode(logoConfig);  // encode logoConfig into the token
                logoConfig.Add("token", insImageToken);  // and add it to the logo config
            }

            return logoConfig;
        }

        // get a document which will be compared with the current document
        private Dictionary<string, object> GetCompareFile()
        {
            // get the path to the compared file
            var compareFileUrl = new UriBuilder(_Default.GetServerUrl(true));
            compareFileUrl.Path = HttpRuntime.AppDomainAppVirtualPath
                + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                + "webeditor.ashx";
            compareFileUrl.Query = "type=assets&fileName=" + HttpUtility.UrlEncode("sample.docx");

            // create an object with the information about the compared file
            Dictionary<string, object> dataCompareFile = new Dictionary<string, object>
                {
                    { "fileType", "docx" },
                    { "url", compareFileUrl.ToString() }
                };

            if (JwtManager.Enabled)  // if the secret key to generate token exists
            {
                var compareFileToken = JwtManager.Encode(dataCompareFile);  // encode the dataCompareFile object into the token
                dataCompareFile.Add("token", compareFileToken);  // and add it to the dataCompareFile object
            }

            return dataCompareFile;
        }

        // get a mail merge config
        private Dictionary<string, object> GetMailMergeConfig()
        {
            // get the path to the recipients data for mail merging
            var mailmergeUrl = new UriBuilder(_Default.GetServerUrl(true));
            mailmergeUrl.Path =
                    HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "webeditor.ashx";
            mailmergeUrl.Query = "type=csv";

            // create a mail merge config
            Dictionary<string, object> mailMergeConfig = new Dictionary<string, object>
                {
                    { "fileType", "csv" },
                    { "url", mailmergeUrl.ToString() }
                };

            if (JwtManager.Enabled)  // if the secret key to generate token exists
            {
                var mailmergeToken = JwtManager.Encode(mailMergeConfig);  // encode mailMergeConfig into the token
                mailMergeConfig.Add("token", mailmergeToken);  // and add it to the mail merge config
            }

            return mailMergeConfig;
        }

        // get image url for templates
        private string GetTemplateImageUrl (string ext)
        {
            var path = new UriBuilder(_Default.GetServerUrl(true)) // templates image url in the "From Template" section
            {
                Path = HttpRuntime.AppDomainAppVirtualPath
                + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                + "App_Themes\\images\\"
            };
            switch (ext)
            {
                case ".docx":
                    return path + "file_docx.svg"; // for word document type
                case ".xlsx":
                    return path + "file_xlsx.svg"; // .xlsx for cell document type
                case ".pptx":
                    return path + "file_pptx.svg"; // .pptx for slide document type
                default:
                    return path + "file_docx.svg"; // the default value
            }
        }

        // create the public url
        private string MakePublicUrl(string fullPath)
        {
            var root = Path.IsPathRooted(WebConfigurationManager.AppSettings["storage-path"]) ? WebConfigurationManager.AppSettings["storage-path"] 
                : HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"];
            return _Default.GetServerUrl(true) + fullPath.Substring(root.Length).Replace(Path.DirectorySeparatorChar, '/');
        }


        // create the public history url
        private string MakePublicHistoryUrl(string filename, string version, string file)
        {
            var fileUrl = new UriBuilder(_Default.GetServerUrl(true));
            fileUrl.Path = HttpRuntime.AppDomainAppVirtualPath
                + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                + "webeditor.ashx";
            fileUrl.Query = "type=downloadhistory&fileName=" + HttpUtility.UrlEncode(filename)
                + "&ver=" + version + "&file=" + file
                + "&userAddress=" + HttpUtility.UrlEncode(HttpContext.Current.Request.UserHostAddress);
            return fileUrl.ToString();
        }

        // create demo document
        private static void Try(string type, string sample, HttpRequest request)
        {
            string ext;
            switch (type)
            {
                case "word":
                    ext = ".docx";  // .docx for word document type
                    break;
                case "cell":
                    ext = ".xlsx";  // .xlsx for cell document type
                    break;
                case "slide":
                    ext = ".pptx";  // .pptx for slide document type
                    break;
                case "docxf":
                    ext = ".docxf";
                    break;
                default:
                    return;
            }
            var demoName = (string.IsNullOrEmpty(sample) ? "new" : "sample") + ext;  // create demo document name with the necessary extension
            var demoPath = "assets\\" + (string.IsNullOrEmpty(sample) ? "new\\" : "sample\\");  // and put this file into the assets directory

            FileName = _Default.GetCorrectName(demoName);  // get file name with an index if such a file name already exists

            var filePath = _Default.StoragePath(FileName, null);
            File.Copy(HttpRuntime.AppDomainAppPath + demoPath + demoName, filePath);  // copy this file to the storage directory

            // create a json file with file meta data
            var id = request.Cookies.GetOrDefault("uid", null);
            var user = Users.getUser(id);  // get the user
            CreateMeta(FileName, user.id, user.name, null);
        }

        // create a json file with file meta data
        public static void CreateMeta(string fileName, string uid, string uname, string userAddress)
        {
            var histDir = _Default.HistoryDir(_Default.StoragePath(fileName, userAddress)); 
            Directory.CreateDirectory(histDir);
            // create the meta data object and write the information into the createdInfo.json file
            File.WriteAllText(Path.Combine(histDir, "createdInfo.json"), new JavaScriptSerializer().Serialize(new Dictionary<string, object> {
                { "created", DateTime.Now.ToString("yyyy'-'MM'-'dd HH':'mm':'ss") },
                { "id", uid },
                { "name", uname }
            }));
        }
    }
}