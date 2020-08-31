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
using System.Linq;
using System.Text.RegularExpressions;
using System.Web;
using System.Web.Configuration;
using System.Web.Script.Serialization;
using OnlineEditorsExampleMVC.Models;

namespace OnlineEditorsExampleMVC.Helpers
{
    public class DocManagerHelper
    {
        public static long MaxFileSize
        {
            get
            {
                long size;
                long.TryParse(WebConfigurationManager.AppSettings["filesize-max"], out size);
                return size > 0 ? size : 5 * 1024 * 1024;
            }
        }

        public static List<string> FileExts
        {
            get { return ViewedExts.Concat(EditedExts).Concat(ConvertExts).ToList(); }
        }

        public static List<string> ViewedExts
        {
            get { return (WebConfigurationManager.AppSettings["files.docservice.viewed-docs"] ?? "").Split(new char[] { '|', ',' }, StringSplitOptions.RemoveEmptyEntries).ToList(); }
        }

        public static List<string> EditedExts
        {
            get { return (WebConfigurationManager.AppSettings["files.docservice.edited-docs"] ?? "").Split(new char[] { '|', ',' }, StringSplitOptions.RemoveEmptyEntries).ToList(); }
        }

        public static List<string> ConvertExts
        {
            get { return (WebConfigurationManager.AppSettings["files.docservice.convert-docs"] ?? "").Split(new char[] { '|', ',' }, StringSplitOptions.RemoveEmptyEntries).ToList(); }
        }

        public static string CurUserHostAddress(string userAddress = null)
        {
            return Regex.Replace(userAddress ?? HttpContext.Current.Request.UserHostAddress, "[^0-9a-zA-Z.=]", "_");
        }

        public static string StoragePath(string fileName, string userAddress = null)
        {
            var directory = HttpRuntime.AppDomainAppPath + CurUserHostAddress(userAddress) + "\\";
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }
            return directory + fileName;
        }

        public static string HistoryDir(string storagePath)
        {
            return storagePath += "-hist";
        }

        public static string VersionDir(string histPath, int version)
        {
            return Path.Combine(histPath, version.ToString());
        }

        public static string VersionDir(string fileName, string userAddress, int version)
        {
            return VersionDir(HistoryDir(StoragePath(fileName, userAddress)), version);
        }

        public static int GetFileVersion(string historyPath)
        {
            if (!Directory.Exists(historyPath)) return 0;
            return Directory.EnumerateDirectories(historyPath).Count();
        }

        public static int GetFileVersion(string fileName, string userAddress)
        {
            return GetFileVersion(HistoryDir(StoragePath(fileName, userAddress)));
        }

        public static string GetCorrectName(string fileName)
        {
            var baseName = Path.GetFileNameWithoutExtension(fileName);
            var ext = Path.GetExtension(fileName);
            var name = baseName + ext;

            for (var i = 1; File.Exists(StoragePath(name)); i++)
            {
                name = baseName + " (" + i + ")" + ext;
            }
            return name;
        }

        public static List<string> GetStoredFiles()
        {
            var directory = HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"] + CurUserHostAddress(null) + "\\";
            if (!Directory.Exists(directory)) return new List<string>();

            var directoryInfo = new DirectoryInfo(directory);

            var storedFiles = directoryInfo.GetFiles("*.*", SearchOption.TopDirectoryOnly).Select(fileInfo => fileInfo.Name).ToList();
            return storedFiles;
        }

        public static string CreateDemo(string fileExt, bool withContent)
        {
            var demoName = (withContent ? "sample." : "new.") + fileExt;

            var fileName = GetCorrectName(demoName);

            File.Copy(HttpRuntime.AppDomainAppPath + "app_data\\" + demoName, StoragePath(fileName));

            return fileName;
        }

        public static void CreateMeta(string fileName, string uid, string uname)
        {
            var histDir = HistoryDir(StoragePath(fileName, null));
            Directory.CreateDirectory(histDir);
            File.WriteAllText(Path.Combine(histDir, "createdInfo.json"), new JavaScriptSerializer().Serialize(new Dictionary<string, object> {
                { "created", DateTime.Now.ToString() },
                { "id", string.IsNullOrEmpty(uid) ? "uid-1" : uid },
                { "name", string.IsNullOrEmpty(uname) ? "John Smith" : uname }
            }));
        }

        public static string GetFileUri(string fileName)
        {
            var uri = new UriBuilder(HttpContext.Current.Request.Url)
                {
                    Path = HttpRuntime.AppDomainAppVirtualPath + "/"
                           + CurUserHostAddress() + "/"
                           + fileName,
                    Query = ""
                };

            return uri.ToString();
        }

        public static string GetPathUri(string path)
        {
            var uri = new UriBuilder(HttpContext.Current.Request.Url)
            {
                Path = HttpRuntime.AppDomainAppVirtualPath + "/"
                           + path,
                Query = ""
            };

            return uri.ToString();
        }

        public static string GetCallback(string fileName)
        {
            var callbackUrl = new UriBuilder(HttpContext.Current.Request.Url)
            {
                Path =
                    HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "webeditor.ashx",
                Query = "type=track"
                        + "&fileName=" + HttpUtility.UrlEncode(fileName)
                        + "&userAddress=" + HttpUtility.UrlEncode(HttpContext.Current.Request.UserHostAddress)
            };
            return callbackUrl.ToString();
        }

        public static string GetInternalExtension(FileUtility.FileType fileType)
        {
            switch (fileType)
            {
                case FileUtility.FileType.Text:
                    return ".docx";
                case FileUtility.FileType.Spreadsheet:
                    return ".xlsx";
                case FileUtility.FileType.Presentation:
                    return ".pptx";
                default:
                    return ".docx";
            }
        }
    }
}