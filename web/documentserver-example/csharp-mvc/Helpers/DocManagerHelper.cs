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
using System.Text.RegularExpressions;
using System.Web;
using System.Web.Configuration;
using System.Web.Script.Serialization;
using OnlineEditorsExampleMVC.Models;

namespace OnlineEditorsExampleMVC.Helpers
{
    public class DocManagerHelper
    {
        // get max file size
        public static long MaxFileSize
        {
            get
            {
                long size;
                long.TryParse(WebConfigurationManager.AppSettings["filesize-max"], out size);
                return size > 0 ? size : 5 * 1024 * 1024;
            }
        }

        // get all the supported file extensions
        public static List<string> FileExts
        {
            get { return ViewedExts.Concat(EditedExts).Concat(ConvertExts).ToList(); }
        }

        // get file extensions that can be viewed
        public static List<string> ViewedExts
        {
            get { return (WebConfigurationManager.AppSettings["files.docservice.viewed-docs"] ?? "").Split(new char[] { '|', ',' }, StringSplitOptions.RemoveEmptyEntries).ToList(); }
        }

        // get file extensions that can be edited
        public static List<string> EditedExts
        {
            get { return (WebConfigurationManager.AppSettings["files.docservice.edited-docs"] ?? "").Split(new char[] { '|', ',' }, StringSplitOptions.RemoveEmptyEntries).ToList(); }
        }

        // get file extensions that can be converted
        public static List<string> ConvertExts
        {
            get { return (WebConfigurationManager.AppSettings["files.docservice.convert-docs"] ?? "").Split(new char[] { '|', ',' }, StringSplitOptions.RemoveEmptyEntries).ToList(); }
        }

        // get current user host address
        public static string CurUserHostAddress(string userAddress = null)
        {
            return Regex.Replace(userAddress ?? HttpContext.Current.Request.UserHostAddress, "[^0-9a-zA-Z.=]", "_");
        }

        // get the storage path of the file
        public static string StoragePath(string fileName, string userAddress = null)
        {
            var directory = HttpRuntime.AppDomainAppPath + CurUserHostAddress(userAddress) + "\\";
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }
            return directory + Path.GetFileName(fileName);
        }

        // get the path to the forcesaved file version
        public static string ForcesavePath(string fileName, string userAddress, Boolean create)
        {
            // create the directory to this file version
            var directory = HttpRuntime.AppDomainAppPath + CurUserHostAddress(userAddress) + "\\";
            if (!Directory.Exists(directory))
            {
                return "";
            }

            // create the directory to the history of this file version
            directory = directory + Path.GetFileName(fileName) + "-hist" + "\\";
            if (!Directory.Exists(directory))
            {
                if (create)
                {
                    Directory.CreateDirectory(directory);
                }
                else
                {
                    return "";
                }
            }

            directory = directory + Path.GetFileName(fileName);
            if (!File.Exists(directory))
            {
                if (!create)
                {
                    return "";
                }
            }

            return directory;
        }

        // get the history directory
        public static string HistoryDir(string storagePath)
        {
            return storagePath += "-hist";
        }

        // get the path to the file version by the history path and file version
        public static string VersionDir(string histPath, int version)
        {
            return Path.Combine(histPath, version.ToString());
        }

        // get the path to the file version by the file name, user address and file version
        public static string VersionDir(string fileName, string userAddress, int version)
        {
            return VersionDir(HistoryDir(StoragePath(fileName, userAddress)), version);
        }

        // get the file version by the history path
        public static int GetFileVersion(string historyPath)
        {
            if (!Directory.Exists(historyPath)) return 0;  // if the history path doesn't exist, then the file version is 0
            return Directory.EnumerateDirectories(historyPath).Count() + 1;  // take only directories from the history folder and count them
        }

        // get the file version by the file name and user address
        public static int GetFileVersion(string fileName, string userAddress)
        {
            return GetFileVersion(HistoryDir(StoragePath(fileName, userAddress)));
        }

        // get a file name with an index if the file with such a name already exists
        public static string GetCorrectName(string fileName, string userAddress = null)
        {
            var baseName = Path.GetFileNameWithoutExtension(fileName);
            var ext = Path.GetExtension(fileName);
            var name = baseName + ext;

            for (var i = 1; File.Exists(StoragePath(name, userAddress)); i++)  // run through all the files with such a name in the storage directory
            {
                name = baseName + " (" + i + ")" + ext;  // and add an index to the base name
            }
            return name;
        }

        // get all the stored files from the user host address
        public static List<FileInfo> GetStoredFiles()
        {
            var directory = HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"] + CurUserHostAddress(null) + "\\";
            if (!Directory.Exists(directory)) return new List<FileInfo>();

            var directoryInfo = new DirectoryInfo(directory);

            // take files from the root directory
            List<FileInfo> storedFiles = directoryInfo.GetFiles("*.*", SearchOption.TopDirectoryOnly).ToList();

            return storedFiles;
        }

        // create demo document
        public static string CreateDemo(string fileExt, bool withContent)
        {
            var demoName = (withContent ? "sample." : "new.") + fileExt;  // create sample or new template file with the necessary extension
            var demoPath = "assets\\" + (withContent ? "sample\\" : "new\\");  // get the path to the sample document

            var fileName = GetCorrectName(demoName);  // get a file name with an index if the file with such a name already exists

            File.Copy(HttpRuntime.AppDomainAppPath + demoPath + demoName, StoragePath(fileName));  // copy file to the storage directory

            return fileName;
        }

        // create meta information
        public static void CreateMeta(string fileName, string uid, string uname, string userAddress = null)
        {
            var histDir = HistoryDir(StoragePath(fileName, userAddress));  // create history directory
            Directory.CreateDirectory(histDir);
            // create createdInfo.json file with meta information in the history directory (creation time, user id and name)
            File.WriteAllText(Path.Combine(histDir, "createdInfo.json"), new JavaScriptSerializer().Serialize(new Dictionary<string, object> {
                { "created", DateTime.Now.ToString("yyyy'-'MM'-'dd HH':'mm':'ss") },
                { "id", string.IsNullOrEmpty(uid) ? "uid-1" : uid },
                { "name", string.IsNullOrEmpty(uname) ? "John Smith" : uname }
            }));
        }

        // get file url
        public static string GetFileUri(string fileName, Boolean forDocumentServer)
        {
            var uri = new UriBuilder(GetServerUrl(forDocumentServer))
                {
                    Path = HttpRuntime.AppDomainAppVirtualPath + "/"
                           + CurUserHostAddress() + "/"
                           + fileName,
                    Query = ""
                };

            return uri.ToString();
        }

        // get the path url
        public static string GetPathUri(string path)
        {
            var uri = new UriBuilder(GetServerUrl(true))
            {
                Path = HttpRuntime.AppDomainAppVirtualPath + "/"
                           + path,
                Query = ""
            };

            return uri.ToString();
        }

        // get the server url
        public static string GetServerUrl(Boolean forDocumentServer)
        {
            if (forDocumentServer && !WebConfigurationManager.AppSettings["files.docservice.url.example"].Equals(""))
            {
                return WebConfigurationManager.AppSettings["files.docservice.url.example"];
            }
            else
            {
                var uri = new UriBuilder(HttpContext.Current.Request.Url) { Query = "" };
                var requestHost = HttpContext.Current.Request.Headers["Host"];
                if (!string.IsNullOrEmpty(requestHost))
                    uri = new UriBuilder(uri.Scheme + "://" + requestHost);

                return uri.ToString();
            }
        }

        // get the callback url
        public static string GetCallback(string fileName)
        {
            var callbackUrl = new UriBuilder(GetServerUrl(true))
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

        // get an editor internal extension
        public static string GetInternalExtension(FileUtility.FileType fileType)
        {
            switch (fileType)
            {
                case FileUtility.FileType.Word:  // .docx for word file type
                    return ".docx";
                case FileUtility.FileType.Cell:  // .xlsx for cell file type
                    return ".xlsx";
                case FileUtility.FileType.Slide:  // .pptx for slide file type
                    return ".pptx";
                default:
                    return ".docx";  // the default file type is .docx
            }
        }

        // get file information
        public static List<Dictionary<string, object>> GetFilesInfo(string fileId = null)
        {
            var files = new List<Dictionary<string, object>>();

            // run through all the stored files
            foreach (var file in GetStoredFiles())
            {
                // write all the parameters to the map
                var dictionary = new Dictionary<string, object>();
                dictionary.Add("version", GetFileVersion(file.Name, null));
                dictionary.Add("id", ServiceConverter.GenerateRevisionId(DocManagerHelper.CurUserHostAddress() + "/" + file.Name + "/" + File.GetLastWriteTime(DocManagerHelper.StoragePath(file.Name, null)).GetHashCode()));
                dictionary.Add("contentLength", Math.Round(file.Length / 1024.0, 2) + " KB");
                dictionary.Add("pureContentLength", file.Length);
                dictionary.Add("title", file.Name);
                dictionary.Add("updated", file.LastWriteTime.ToString());

                // get file information by its id
                if (fileId != null) 
                {
                    if (fileId.Equals(dictionary["id"]))
                    {
                        files.Add(dictionary);
                        break;
                    }
                }
                else
                {
                    files.Add(dictionary);
                }
            }

            return files;
        }
    }
}