﻿/**
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
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Web;
using System.Web.Configuration;
using System.Web.Mvc;
using System.Web.Script.Serialization;
using OnlineEditorsExampleMVC.Helpers;

namespace OnlineEditorsExampleMVC.Models
{
    // create file model
    public class FileModel
    {
        public string Mode { get; set; }  // editor mode
        public string Type { get; set; }  // editor type
        public bool IsEnabledDirectUrl { get; set; }  // is enabled direct url

        // get file url for Document Server
        public string FileUri
        {
            get { return DocManagerHelper.GetFileUri(FileName, true); }
        }

        // get file url for user
        public string FileUriUser
        {
            get { return Path.IsPathRooted(WebConfigurationManager.AppSettings["storage-path"]) ? DownloadUrl + "&dmode=emb" : DocManagerHelper.GetFileUri(FileName, false); }
        }

        public string FileName { get; set; }  // file name

        // get document type
        public string DocumentType
        {
            get { return FileUtility.GetFileType(FileName).ToString().ToLower(); }
        }

        // get document key
        public string Key
        {
            get { return ServiceConverter.GenerateRevisionId(DocManagerHelper.CurUserHostAddress() + "/" + FileName + "/" + File.GetLastWriteTime(DocManagerHelper.StoragePath(FileName, null)).GetHashCode()); }
        }

        // get the callback url
        public string CallbackUrl
        {
            get { return DocManagerHelper.GetCallback(FileName); }
        }

        public string DownloadUrl
        {
            get { return DocManagerHelper.GetDownloadUrl(FileName); }
        }

        // get the document config
        public string GetDocConfig(HttpRequest request, UrlHelper url)
        {
            var jss = new JavaScriptSerializer();

            var ext = Path.GetExtension(FileName).ToLower();  // get file extension
            var editorsMode = Mode ?? "edit";  // get editor mode

            var canEdit = DocManagerHelper.EditedExts.Contains(ext);  // check if the file with such an extension can be edited

            var id = request.Cookies.GetOrDefault("uid", null);
            var user = Users.getUser(id);  // get the user

            if ((!canEdit && editorsMode.Equals("edit") || editorsMode.Equals("fillForms")) && DocManagerHelper.FillFormExts.Contains(ext)) {
                editorsMode = "fillForms";
                canEdit = true;
            }
            if (user.id.Equals("uid-0") && editorsMode.Equals("view"))
            {
                canEdit = false;
            }
            var submitForm = editorsMode.Equals("fillForms") && id.Equals("uid-1") && false;  // check if the Submit form button is displayed or not
            var mode = canEdit && editorsMode != "view" ? "edit" : "view";  // set the mode parameter: change it to view if the document can't be edited

            // favorite icon state
            bool? favorite = user.favorite;

            var actionLink = request.GetOrDefault("actionLink", null);  // get the action link (comment or bookmark) if it exists
            var actionData = string.IsNullOrEmpty(actionLink) ? null : jss.DeserializeObject(actionLink);  // get action data for the action link

            var directUrl = DocManagerHelper.GetDownloadUrl(FileName, false);
            var createUrl = DocManagerHelper.GetCreateUrl(FileUtility.GetFileType(FileName));
            var templatesImageUrl = DocManagerHelper.GetTemplateImageUrl(FileUtility.GetFileType(FileName)); // image url for templates
            var templates = new List<Dictionary<string, string>>
            {
                new Dictionary<string, string>()
                {
                    { "image", "" },
                    { "title", "Blank" },
                    { "url", createUrl },
                },
                new Dictionary<string, string>()
                {
                    { "image", templatesImageUrl },
                    { "title", "With sample content" },
                    { "url", createUrl + "&sample=true" },
                }
            };

            // specify the document config
            var config = new Dictionary<string, object>
                {
                    { "type", Type ?? "desktop" },
                    { "documentType", DocumentType },
                    {
                        "document", new Dictionary<string, object>
                            {
                                { "title", FileName },
                                { "url", DownloadUrl },
                                { "directUrl", IsEnabledDirectUrl ? directUrl : "" },
                                { "fileType", ext.Trim('.') },
                                { "key", Key },
                                {
                                    "info", new Dictionary<string, object>
                                        {
                                            { "owner", "Me" },
                                            { "uploaded", DateTime.Now.ToShortDateString() },
                                            { "favorite", favorite}
                                        }
                                },
                                {
                                    "referenceData", new Dictionary<string, string>()
                                    {
                                        { "fileKey", !user.id.Equals("uid-0") ?
                                            jss.Serialize(new Dictionary<string, object>{
                                                {"fileName", FileName},
                                                {"userAddress", HttpUtility.UrlEncode(DocManagerHelper.CurUserHostAddress(HttpContext.Current.Request.UserHostAddress))}
                                        }) : null },
                                        {"instanceId", DocManagerHelper.GetServerUrl(false) }
                                    }
                                },
                                {
                                    // the permission for the document to be edited and downloaded or not
                                    "permissions", new Dictionary<string, object>
                                        {
                                            { "comment", editorsMode != "view" && editorsMode != "fillForms" && editorsMode != "embedded" && editorsMode != "blockcontent" },
                                            { "copy", !user.deniedPermissions.Contains("copy") },
                                            { "download", !user.deniedPermissions.Contains("download") },
                                            { "edit", canEdit && (editorsMode == "edit" || editorsMode == "view" || editorsMode == "filter" || editorsMode == "blockcontent") },
                                            { "print", !user.deniedPermissions.Contains("print") },
                                            { "fillForms", editorsMode != "view" && editorsMode != "comment" && editorsMode != "embedded" && editorsMode != "blockcontent" },
                                            { "modifyFilter", editorsMode != "filter" },
                                            { "modifyContentControl", editorsMode != "blockcontent" },
                                            { "review", canEdit && (editorsMode == "edit" || editorsMode == "review") },
                                            { "chat", !user.id.Equals("uid-0") },
                                            { "reviewGroups", user.reviewGroups },
                                            { "commentGroups", user.commentGroups },
                                            { "userInfoGroups", user.userInfoGroups },
                                            { "protect", !user.deniedPermissions.Contains("protect") }
                                        }
                                }
                            }
                    },
                    {
                        "editorConfig", new Dictionary<string, object>
                            {
                                { "actionLink", actionData },
                                { "mode", mode },
                                { "lang", request.Cookies.GetOrDefault("ulang", "en") },
                                { "callbackUrl", CallbackUrl },  // absolute URL to the document storage service
                                { "coEditing", editorsMode == "view" && user.id.Equals("uid-0") ?
                                    new Dictionary<string, object>{
                                        {"mode", "strict"},
                                        {"change", false}
                                    } : null },
                                { "createUrl", !user.id.Equals("uid-0") ? createUrl : null },
                                { "templates", user.templates ? templates : null },
                                {
                                    // the user currently viewing or editing the document
                                    "user", new Dictionary<string, object>
                                        {
                                            { "id", !user.id.Equals("uid-0") ? user.id : null  },
                                            { "name", user.name },
                                            { "group", user.group }
                                        }
                                },
                                {
                                    // the parameters for the embedded document type
                                    "embedded", new Dictionary<string, object>
                                        {
                                            { "saveUrl", directUrl },  // the absolute URL that will allow the document to be saved onto the user personal computer
                                            { "embedUrl", directUrl },  // the absolute URL to the document serving as a source file for the document embedded into the web page
                                            { "shareUrl", directUrl },  // the absolute URL that will allow other users to share this document
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
                                                        { "url", DocManagerHelper.GetServerUrl(false) }  // the absolute URL to the website address which will be opened when clicking the Open file location menu button
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

            return jss.Serialize(config);
        }

        // get the document history
        public void GetHistory(out string history, out string historyData)
        {
            var storagePath = WebConfigurationManager.AppSettings["storage-path"];
            var jss = new JavaScriptSerializer();
            var histDir = DocManagerHelper.HistoryDir(DocManagerHelper.StoragePath(FileName, null));

            history = null;
            historyData = null;

            if (DocManagerHelper.GetFileVersion(histDir) > 0)  // if the file was modified (the file version is greater than 0)
            {
                var currentVersion = DocManagerHelper.GetFileVersion(histDir);
                var hist = new List<Dictionary<string, object>>();
                var histData = new Dictionary<string, object>();

                for (var i = 1; i <= currentVersion; i++)  // run through all the file versions
                {
                    var obj = new Dictionary<string, object>();
                    var dataObj = new Dictionary<string, object>();
                    var verDir = DocManagerHelper.VersionDir(histDir, i);  // get the path to the given file version

                    var key = i == currentVersion ? Key : File.ReadAllText(Path.Combine(verDir, "key.txt"));  // get document key

                    obj.Add("key", key);
                    obj.Add("version", i);

                    if (i == 1)  // check if the version number is equal to 1
                    {
                        var infoPath = Path.Combine(histDir, "createdInfo.json");  // get meta data of this file

                        if (File.Exists(infoPath))
                        {
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
                    string prevFileUrl;
                    string directPrevFileUrl;
                    if (Path.IsPathRooted(storagePath) && !string.IsNullOrEmpty(storagePath))
                    {
                        prevFileUrl = i == currentVersion ? DocManagerHelper.GetHistoryDownloadUrl(FileName, i.ToString(), "prev" + ext)
                            : DocManagerHelper.GetDownloadUrl(Directory.GetFiles(verDir, "prev.*")[0].Replace(storagePath + "\\", ""));
                        directPrevFileUrl = i == currentVersion ? DocManagerHelper.GetHistoryDownloadUrl(FileName, i.ToString(), "prev" + ext, false)
                            : DocManagerHelper.GetDownloadUrl(Directory.GetFiles(verDir, "prev.*")[0].Replace(storagePath + "\\", ""), false);
                    }
                    else {
                        prevFileUrl = i == currentVersion ? FileUri
                            : DocManagerHelper.GetHistoryDownloadUrl(FileName, i.ToString(), "prev" + ext);
                        directPrevFileUrl = i == currentVersion ? DocManagerHelper.GetHistoryDownloadUrl(FileName, i.ToString(), "prev" + ext, false)
                            : DocManagerHelper.GetDownloadUrl(Directory.GetFiles(verDir, "prev.*")[0].Replace(storagePath + "\\", ""), false);
                    }

                    dataObj.Add("url", prevFileUrl);
                    if (IsEnabledDirectUrl)
                    {
                        dataObj.Add("directUrl", directPrevFileUrl);
                    }
                    dataObj.Add("version", i);
                    if (i > 1)  // check if the version number is greater than 1 (the file was modified)
                    {
                        // get the path to the changes.json file
                        var changes = jss.Deserialize<Dictionary<string, object>>(File.ReadAllText(Path.Combine(DocManagerHelper.VersionDir(histDir, i - 1), "changes.json")));
                        var changesArray = (ArrayList)changes["changes"];
                        var change = changesArray.Count > 0
                            ? (Dictionary<string, object>)changesArray[0]
                            : new Dictionary<string, object>();

                        // write information about changes to the object
                        obj.Add("changes", change.Count > 0 ? changes["changes"] : null);
                        obj.Add("serverVersion", changes["serverVersion"]);
                        obj.Add("created", change.Count > 0  ? change["created"] : null);
                        obj.Add("user", change.Count > 0 ? change["user"] : null);

                        var prev = (Dictionary<string, object>)histData[(i - 2).ToString()];  // get the history data from the previous file version
                        dataObj.Add("previous", IsEnabledDirectUrl ? new Dictionary<string, object>() {  // write information about previous file version to the data object with direct url
                            { "fileType", prev["fileType"] },
                            { "key", prev["key"] },  // write key and url information about previous file version
                            { "url", prev["url"] },
                            { "directUrl", prev["directUrl"] },
                        } : new Dictionary<string, object>() {  // write information about previous file version to the data object without direct url
                            { "fileType", prev["fileType"] },
                            { "key", prev["key"] },  // write key and url information about previous file version
                            { "url", prev["url"] },
                        });
                        // write the path to the diff.zip archive with differences in this file version
                        var changesUrl = DocManagerHelper.GetHistoryDownloadUrl(FileName, (i - 1).ToString(), "diff.zip");
                        dataObj.Add("changesUrl", changesUrl);
                    }
                    if(JwtManager.Enabled)
                    {
                        var token = JwtManager.Encode(dataObj);
                        dataObj.Add("token", token);
                    }
                    hist.Add(obj);  // add object dictionary to the hist list
                    histData.Add((i - 1).ToString(), dataObj);  // write data object information to the history data
                }

                // write history information about the current file version to the history object
                history = jss.Serialize(new Dictionary<string, object>()
                {
                    { "currentVersion", currentVersion },
                    { "history", hist }
                });
                historyData = jss.Serialize(histData);
            }
        }

        // get a document which will be compared with the current document
        public void GetCompareFileData(out string compareConfig)
        {
            var jss = new JavaScriptSerializer();

            // get the path to the compared file
            var compareFileUrl = new UriBuilder(DocManagerHelper.GetServerUrl(true))
            {
                Path = HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "webeditor.ashx",
                Query = "type=assets&fileName=" + HttpUtility.UrlEncode("sample.docx")
            };

            var directCompareFileUrl = new UriBuilder(DocManagerHelper.GetServerUrl(false))
            {
                Path = HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "webeditor.ashx",
                Query = "type=assets&fileName=" + HttpUtility.UrlEncode("sample.docx")
            };

            // create an object with the information about the compared file
            var dataCompareFile = new Dictionary<string, object>
            {
                { "fileType", "docx" },
                { "url", compareFileUrl.ToString() }
            };

            if (IsEnabledDirectUrl)
            {
                dataCompareFile.Add("directUrl", directCompareFileUrl.ToString());
            }

            if (JwtManager.Enabled)  // if the secret key to generate token exists
            {
                var compareFileToken = JwtManager.Encode(dataCompareFile);  // encode the dataCompareFile object into the token
                dataCompareFile.Add("token", compareFileToken);  // and add it to the dataCompareFile object
            }

            compareConfig = jss.Serialize(dataCompareFile);
        }

        // get a logo config
        public void GetLogoConfig(out string logoUrl)
        {
            var jss = new JavaScriptSerializer();

            // get the path to the logo image
            var mailMergeUrl = new UriBuilder(DocManagerHelper.GetServerUrl(true))
            {
                Path = HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "Content\\images\\logo.png"
            };

            var directMailMergeUrl = new UriBuilder(DocManagerHelper.GetServerUrl(false))
            {
                Path = HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "Content\\images\\logo.png"
            };

            // create a logo config
            var logoConfig = new Dictionary<string, object>
            {
                { "fileType", "png"},
                { "url", mailMergeUrl.ToString()}
            };

            if (IsEnabledDirectUrl)
            {
                logoConfig.Add("directUrl", directMailMergeUrl.ToString());
            }

            if (JwtManager.Enabled)  // if the secret key to generate token exists
            {
                var token = JwtManager.Encode(logoConfig);  // encode logoConfig into the token
                logoConfig.Add("token", token);  // and add it to the logo config
            }

            logoUrl = jss.Serialize(logoConfig).Replace("{", "").Replace("}", "");
        }

        // get a mail merge config
        public void GetMailMergeConfig(out string dataMailMergeRecipients)
        {
            var jss = new JavaScriptSerializer();

            // get the path to the recipients data for mail merging
            var mailMergeUrl = new UriBuilder(DocManagerHelper.GetServerUrl(true))
            {
                Path =
                    HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "webeditor.ashx",
                Query = "type=csv"
            };

            var directMailMergeUrl = new UriBuilder(DocManagerHelper.GetServerUrl(false))
            {
                Path =
                    HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "webeditor.ashx",
                Query = "type=csv"
            };

            // create a mail merge config
            var mailMergeConfig = new Dictionary<string, object>
            {
                { "fileType", "csv" },
                { "url", mailMergeUrl.ToString()}
            };

            if (IsEnabledDirectUrl)
            {
                mailMergeConfig.Add("directUrl", directMailMergeUrl.ToString());
            }

            if (JwtManager.Enabled)  // if the secret key to generate token exists
            {
                var mailmergeToken = JwtManager.Encode(mailMergeConfig);  // encode mailMergeConfig into the token
                mailMergeConfig.Add("token", mailmergeToken);  // and add it to the mail merge config
            }

            dataMailMergeRecipients = jss.Serialize(mailMergeConfig);
        }

        //get a users for mentions
        public void GetUsersMentions(HttpRequest request, out string usersForMentions)
        {
            var jss = new JavaScriptSerializer();
            var id = request.Cookies.GetOrDefault("uid", null);
            var user = Users.getUser(id);
            usersForMentions = !user.id.Equals("uid-0") ? jss.Serialize(Users.getUsersForMentions(user.id)) : null;
        }
    }
}
