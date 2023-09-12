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
using System.Linq;
using System.Net;
using System.Text.RegularExpressions;
using System.Web;
using System.Web.Configuration;
using System.Web.Script.Serialization;
using System.Web.UI;
using ASC.Api.DocumentConverter;

namespace OnlineEditorsExample
{
    internal static class FileType
    {
        // the spreadsheet extension list
        public static readonly List<string> ExtsSpreadsheet = new List<string>
            {
                ".xls", ".xlsx", ".xlsm", ".xlsb",
                ".xlt", ".xltx", ".xltm",
                ".ods", ".fods", ".ots", ".csv"
            };

        // the presentation extension list
        public static readonly List<string> ExtsPresentation = new List<string>
            {
                ".pps", ".ppsx", ".ppsm",
                ".ppt", ".pptx", ".pptm",
                ".pot", ".potx", ".potm",
                ".odp", ".fodp", ".otp"
            };

        // the document extension list
        public static readonly List<string> ExtsDocument = new List<string>
            {
                ".doc", ".docx", ".docm",
                ".dot", ".dotx", ".dotm",
                ".odt", ".fodt", ".ott", ".rtf", ".txt",
                ".html", ".htm", ".mht", ".xml",
                ".pdf", ".djvu", ".fb2", ".epub", ".xps", ".oxps", ".oform"
            };

        // get an internal file extension
        public static string GetInternalExtension(string extension)
        {
            extension = Path.GetExtension(extension).ToLower();  // get file extension
            if (ExtsDocument.Contains(extension)) return ".docx";  // .docx for text document extensions
            if (ExtsSpreadsheet.Contains(extension)) return ".xlsx";  // .xlsx for spreadsheet extensions
            if (ExtsPresentation.Contains(extension)) return ".pptx";  // .pptx for presentation extensions
            return string.Empty;
        }
    }

    public partial class _Default : Page
    {

        // get the virtual path
        public static string VirtualPath
        {
            get
            {
                return Path.IsPathRooted(WebConfigurationManager.AppSettings["storage-path"]) ? 
                    WebConfigurationManager.AppSettings["storage-path"] + "/"
                    :
                    HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + WebConfigurationManager.AppSettings["storage-path"]
                    + CurUserHostAddress(null) + "/";
            }
        }

        private static bool? _ismono;

        public static bool IsMono
        {
            get { return _ismono.HasValue ? _ismono.Value : (_ismono = (bool?)(Type.GetType("Mono.Runtime") != null)).Value; }
        }

        // get maximum file size
        private static long MaxFileSize
        {
            get
            {
                long size;
                long.TryParse(WebConfigurationManager.AppSettings["filesize-max"], out size);
                return size > 0 ? size : 5*1024*1024;
            }
        }

        // get all the supported file extensions
        private static List<string> FileExts
        {
            get { return ViewedExts.Concat(EditedExts).Concat(ConvertExts).Concat(FillFormsExts).ToList(); }
        }

        // file extensions that can be viewed
        private static List<string> ViewedExts
        {
            get { return (WebConfigurationManager.AppSettings["files.docservice.viewed-docs"] ?? "").Split(new char[] { '|', ',' }, StringSplitOptions.RemoveEmptyEntries).ToList(); }
        }
        
        public static List<string> FillFormsExts
        {
            get { return (WebConfigurationManager.AppSettings["files.docservice.fillform-docs"] ?? "").Split(new char[] { '|', ',' }, StringSplitOptions.RemoveEmptyEntries).ToList(); }
        }

        // file extensions that can be edited
        public static List<string> EditedExts
        {
            get { return (WebConfigurationManager.AppSettings["files.docservice.edited-docs"] ?? "").Split(new char[] { '|', ',' }, StringSplitOptions.RemoveEmptyEntries).ToList(); }
        }

        // file extensions that can be converted
        public static List<string> ConvertExts
        {
            get { return (WebConfigurationManager.AppSettings["files.docservice.convert-docs"] ?? "").Split(new char[] { '|', ',' }, StringSplitOptions.RemoveEmptyEntries).ToList(); }
        }

        private static string _fileName;

        // get current user host address
        public static string CurUserHostAddress(string userAddress)
        {
            return Regex.Replace(userAddress ?? HttpContext.Current.Request.UserHostAddress, "[^0-9a-zA-Z.=]", "_");
        }

        // get the storage path of the given file
        public static string StoragePath(string fileName, string userAddress)
        {
            var directory = "";
            if (Path.IsPathRooted(WebConfigurationManager.AppSettings["storage-path"]))
            {
                directory = WebConfigurationManager.AppSettings["storage-path"] + "\\";
            }
            else
            {
                directory = HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"] + CurUserHostAddress(userAddress) + "\\";
            }

            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);  // if the file directory doesn't exist, make it
            }
            return directory + (fileName.Contains("\\") ? fileName : Path.GetFileName(fileName));
        }

        // get the path to the history file version
        public static string HistoryPath(string fileName, string userAddress, string version, string file)
        {
            var directory = "";
            if (Path.IsPathRooted(WebConfigurationManager.AppSettings["storage-path"]))
            {
                directory = WebConfigurationManager.AppSettings["storage-path"] + "\\";
            }
            else
            {
                directory = HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"] + CurUserHostAddress(userAddress) + "\\";
            }
            var filepath = directory + Path.GetFileName(fileName) + "-hist" + "\\" + version + "\\" + file;
            return filepath;
        }

        // get the path to the forcesaved file version
        public static string ForcesavePath(string fileName, string userAddress, Boolean create)
        {
            var directory = "";
            if (Path.IsPathRooted(WebConfigurationManager.AppSettings["storage-path"]))
            {
                directory = WebConfigurationManager.AppSettings["storage-path"] + "\\";
            }
            else
            {
                directory = HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"] + CurUserHostAddress(userAddress) + "\\";
            }
            
            if (!Directory.Exists(directory))  // the directory with host address doesn't exist
            {
                return "";
            }

            directory = directory + Path.GetFileName(fileName) + "-hist" + "\\";  // get the path to the history of the given file
            if (!Directory.Exists(directory))
            {
                if (create)  // create history directory if it doesn't exist
                {
                    Directory.CreateDirectory(directory);
                }
                else  // the history directory doesn't exist and we are not supposed to create it
                {
                    return "";
                }
            }

            directory = directory + Path.GetFileName(fileName);  // get the path to the given file
            if (!File.Exists(directory))
            {
                if (!create)
                {
                    return "";
                }
            }

            return directory;
        }

        // create the path to the file history
        public static string HistoryDir(string storagePath)
        {
            return storagePath += "-hist";
        }

        // get the path to the specified file version by its history directory
        public static string VersionDir(string histPath, int version)
        {
            return Path.Combine(histPath, version.ToString());
        }

        // get the path to the specified file version by the file name and the user address
        public static string VersionDir(string fileName, string userAddress, int version)
        {
            return VersionDir(HistoryDir(StoragePath(fileName, userAddress)), version);
        }

        // get the last file version by its history directory
        public static int GetFileVersion(string historyPath)
        {
            if (!Directory.Exists(historyPath)) return 1;
            return Directory.EnumerateDirectories(historyPath).Count() + 1;  // run through all the file versions and count them
        }

        // get the last file version by the file name and the user address
        public static int GetFileVersion(string fileName, string userAddress)
        {
            return GetFileVersion(HistoryDir(StoragePath(fileName, userAddress)));
        }

        // get url to the original file
        public static string FileUri(string fileName, Boolean forDocumentServer)
        {
            var uri = new UriBuilder(GetServerUrl(forDocumentServer));
            uri.Path = VirtualPath + fileName;  // get full url address to the file
            return uri.ToString();
        }

        // get server url
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

        // get the document type
        public static string DocumentType(string fileName)
        {
            var ext = Path.GetExtension(fileName).ToLower();

            if (FileType.ExtsDocument.Contains(ext)) return "word";  // word for text document extensions
            if (FileType.ExtsSpreadsheet.Contains(ext)) return "cell";  // cell for spreadsheet extensions
            if (FileType.ExtsPresentation.Contains(ext)) return "slide";  // slide for presentation extensions

            return "word";  // the default document type is word
        }

        protected string UrlPreloadScripts = WebConfigurationManager.AppSettings["files.docservice.url.site"] + WebConfigurationManager.AppSettings["files.docservice.url.preloader"];


        protected void Page_Load(object sender, EventArgs e)
        {
        }

        // uploading a file by the HtthContext object
        public static string DoUpload(HttpContext context)
        {
            var httpPostedFile = context.Request.Files[0];

            if (HttpContext.Current.Request.Browser.Browser.ToUpper() == "IE")  // check from which browser the request came for
            {
                var files = httpPostedFile.FileName.Split(new char[] { '\\' });
                _fileName = files[files.Length - 1];  // get file name
            }
            else
            {
                _fileName = httpPostedFile.FileName;
            }

            var curSize = httpPostedFile.ContentLength;
            if (MaxFileSize < curSize || curSize <= 0)  // check if the file size exceeds the maximum file size
            {
                throw new Exception("File size is incorrect");
            }

            var curExt = (Path.GetExtension(_fileName) ?? "").ToLower();
            if (!FileExts.Contains(curExt))  // check if the file extension is supported by the editor
            {
                throw new Exception("File type is not supported");
            }

            _fileName = GetCorrectName(_fileName);  // get the correct file name if such a name already exists

            var savedFileName = StoragePath(_fileName, null);  // get the storage path to the uploading file
            httpPostedFile.SaveAs(savedFileName);  // and save it

            // get file meta information or create the default one
            var id = context.Request.Cookies.GetOrDefault("uid", null);
            var user = Users.getUser(id);  // get the user
            DocEditor.CreateMeta(_fileName, user.id, user.name, null);

            return _fileName;
        }

        // uploading a file by the file url and the request
        public static string DoUpload(string fileUri, HttpRequest request)
        {
            _fileName = GetCorrectName(Path.GetFileName(fileUri));  // get the correct file name if such a name already exists

            var curExt = (Path.GetExtension(_fileName) ?? "").ToLower();
            if (!FileExts.Contains(curExt))  // check if the file extension is supported by the editor
            {
                throw new Exception("File type is not supported");
            }

            var req = (HttpWebRequest)WebRequest.Create(fileUri);

            try
            {
                VerifySSL();

                using (var stream = req.GetResponse().GetResponseStream())  // get response stream of the uploading file
                {
                    if (stream == null) throw new Exception("stream is null");
                    const int bufferSize = 4096;

                    using (var fs = File.Open(StoragePath(_fileName, null), FileMode.Create))
                    {
                        var buffer = new byte[bufferSize];
                        int readed;
                        while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                        {
                            fs.Write(buffer, 0, readed);  // write bytes to the output stream
                        }
                    }
                }

                // get file meta information or create the default one
                var id = request.Cookies.GetOrDefault("uid", null);
                var user = Users.getUser(id);  // get the user
                DocEditor.CreateMeta(_fileName, user.id, user.name, null);
            }
            catch (Exception)
            {

            }
            return _fileName;
        }

        public static string DoSaveAs(HttpContext context)
        {
            string fileData;
            try
            {
                using (var receiveStream = context.Request.InputStream)
                using (var readStream = new StreamReader(receiveStream))
                {
                    fileData = readStream.ReadToEnd();
                    if (string.IsNullOrEmpty(fileData)) return "{\"error\":\"Request stream is empty\"}";
                }
            }
            catch (Exception e)
            {
                throw new HttpException((int)HttpStatusCode.BadRequest, e.Message);
            }

            var jss = new JavaScriptSerializer();
            var body = jss.Deserialize<Dictionary<string, object>>(fileData);
            var fileUrl = (string) body["url"];
            var title = (string) body["title"];
            var fileName = GetCorrectName(title);
            var extension = "." + (Path.GetExtension(fileName).ToLower() ?? "").Trim('.');

            var allExt = ConvertExts.Concat(EditedExts).Concat(ViewedExts).Concat(FillFormsExts).ToArray();

            if (!allExt.Contains(extension))
            {
                return "{\"error\":\"File type is not supported\"}";
            }
            
            var req = (HttpWebRequest)WebRequest.Create(fileUrl);
            
            VerifySSL();
            
            using (var stream = req.GetResponse().GetResponseStream())
            {
                
                if (stream == null || req.GetResponse().ContentLength <= 0 || req.GetResponse().ContentLength > MaxFileSize)
                {
                    return "{\"error\": \"File size is incorrect\"}";
                }
                const int bufferSize = 4096;
            
                using (var fs = File.Open(StoragePath(fileName, null), FileMode.Create))
                {
                    var buffer = new byte[bufferSize];
                    int readed;
                    while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                    {
                        fs.Write(buffer, 0, readed);  // write bytes to the output stream
                    }
                }
            }
                
            var id = context.Request.Cookies.GetOrDefault("uid", null);
            var user = Users.getUser(id);  // get the user
            DocEditor.CreateMeta(fileName, user.id, user.name, null);

            return "{\"file\": \"" + fileName + "\"}";
        }

        // converting a file
        public static string DoConvert(HttpContext context)
        {
            string fileData;
            try
            {
                using (var receiveStream = context.Request.InputStream)
                using (var readStream = new StreamReader(receiveStream))
                {
                    fileData = readStream.ReadToEnd();
                    if (string.IsNullOrEmpty(fileData)) context.Response.Write("{\"error\":1,\"message\":\"Request stream is empty\"}");
                }
            }
            catch (Exception e)
            {
                throw new HttpException((int)HttpStatusCode.BadRequest, e.Message);
            }

            var jss = new JavaScriptSerializer();
            var body = jss.Deserialize<Dictionary<string, object>>(fileData);

            _fileName = Path.GetFileName(body["filename"].ToString());
            var filePass = body["filePass"] != null ? body["filePass"].ToString() : null;
            var lang = context.Request.Cookies.GetOrDefault("ulang", null);

            var extension = (Path.GetExtension(_fileName).ToLower() ?? "").Trim('.');
            var internalExtension = "ooxml";

            // check if the file with such an extension can be converted
            if (ConvertExts.Contains("." + extension))
            {
                // generate document key
                var key = ServiceConverter.GenerateRevisionId(FileUri(_fileName, true));

                var fileUrl = new UriBuilder(_Default.GetServerUrl(true));
                fileUrl.Path = HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + "webeditor.ashx";
                fileUrl.Query = "type=download&fileName=" + HttpUtility.UrlEncode(_fileName)
                + "&userAddress=" + HttpUtility.UrlEncode(CurUserHostAddress(HttpContext.Current.Request.UserHostAddress));

                // get the url and file type of the converted file
                Dictionary<string, string> newFileData;
                var result = ServiceConverter.GetConvertedData(fileUrl.ToString() , extension, internalExtension, key, true, out newFileData, filePass, lang);
                if (result != 100)
                {
                    return "{ \"step\" : \"" + result + "\", \"filename\" : \"" + _fileName + "\"}";
                }

                var newFileUri = newFileData["fileUrl"];
                var newFileType = "." + newFileData["fileType"];
                // get a file name of an internal file extension with an index if the file with such a name already exists
                var fileName = GetCorrectName(Path.GetFileNameWithoutExtension(_fileName) + newFileType);

                var req = (HttpWebRequest)WebRequest.Create(newFileUri);

                VerifySSL();

                using (var stream = req.GetResponse().GetResponseStream())  // get response stream of the converting file
                {
                    if (stream == null) throw new Exception("Stream is null");
                    const int bufferSize = 4096;

                    using (var fs = File.Open(StoragePath(fileName, null), FileMode.Create))
                    {
                        var buffer = new byte[bufferSize];
                        int readed;
                        while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                        {
                            fs.Write(buffer, 0, readed);  // write bytes to the output stream
                        }
                    }
                }

                // remove the original file and its history if it exists
                var storagePath = StoragePath(_fileName, null);
                var histDir = HistoryDir(storagePath);
                File.Delete(storagePath);
                if (Directory.Exists(histDir)) Directory.Delete(histDir, true);

                // create meta information about the converted file with user id and name specified
                _fileName = fileName;
                var id = context.Request.Cookies.GetOrDefault("uid", null);
                var user = Users.getUser(id);  // get the user
                DocEditor.CreateMeta(_fileName, user.id, user.name, null);
            }

            return "{ \"filename\" : \"" + _fileName + "\"}";
        }

        // get the correct file name if such a name already exists
        public static string GetCorrectName(string fileName, string userAddress = null)
        {
            var baseName = Path.GetFileNameWithoutExtension(fileName);  // get file name without extension
            var ext = Path.GetExtension(fileName).ToLower();  // get file extension
            var name = baseName + ext;  // get full file name

            // if the file with such a name already exists in this directory
            for (var i = 1; File.Exists(StoragePath(name, userAddress)); i++)
            {
                name = baseName + " (" + i + ")" + ext;  // add an index after its base name
            }
            return name;
        }

        // get all the stored files from the folder
        protected static List<FileInfo> GetStoredFiles()
        {
            var directory = "";
            if (Path.IsPathRooted(WebConfigurationManager.AppSettings["storage-path"]))
            {
                directory = WebConfigurationManager.AppSettings["storage-path"] + "\\";
            }
            else
            {
                directory = HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"] + CurUserHostAddress(null) + "\\";
            }
            
            if (!Directory.Exists(directory)) return new List<FileInfo>();

            var directoryInfo = new DirectoryInfo(directory);  // read the user host directory contents

            // get the list of stored files from the host directory
            List<FileInfo> storedFiles = directoryInfo.GetFiles("*.*", SearchOption.TopDirectoryOnly).ToList();
            return storedFiles;
        }

        // get files information
        public static List<Dictionary<string, object>> GetFilesInfo(string fileId = null)
        {
            var files = new List<Dictionary<string, object>>();

            // run through all the files from the directory
            foreach (var file in GetStoredFiles())
            {
                // write file parameters to the file object
                var dictionary = new Dictionary<string, object>();
                dictionary.Add("version", GetFileVersion(file.Name, null));
                dictionary.Add("id", ServiceConverter.GenerateRevisionId(_Default.CurUserHostAddress(null) + "/" + file.Name + "/" + File.GetLastWriteTime(_Default.StoragePath(file.Name, null)).GetHashCode()));
                dictionary.Add("contentLength", Math.Round(file.Length / 1024.0, 2) + " KB");
                dictionary.Add("pureContentLength", file.Length);
                dictionary.Add("title", file.Name);
                dictionary.Add("updated", file.LastWriteTime.ToString());
                if (fileId != null)
                {
                    // if file id is defined and it is equal to the document key value
                    if (fileId.Equals(dictionary["id"]))
                    {
                        files.Add(dictionary);  // add file object to the files
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

        // enable certificate ignore
        public static void VerifySSL()
        {
            // hack. http://ubuntuforums.org/showthread.php?t=1841740
            if(WebConfigurationManager.AppSettings["files.docservice.verify-peer-off"].Equals("true")) {
                ServicePointManager.ServerCertificateValidationCallback += (s, ce, ca, p) => true;
                ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12 | SecurityProtocolType.Tls11 | SecurityProtocolType.Tls;
            }
        }
        
        public static Dictionary<string, string> GetLanguages()
        {
            var languages = new Dictionary<string, string>();
            String[] couples = (WebConfigurationManager.AppSettings["files.docservice.languages"] ?? "").Split('|');
            foreach (string couple in couples)
            {   
                String[] tmp = couple.Split(':');
                languages.Add(tmp[0],tmp[1]);
            }
            return languages;
        }

        public static string GetDirectUrlParam()
        {
            string isEnabledDirectUrl = HttpUtility.ParseQueryString(HttpContext.Current.Request.Url.Query).Get("directUrl");
            return "&directUrl=" + (isEnabledDirectUrl != null ? isEnabledDirectUrl : "false");
        }

        // get direct url flag
        public static bool IsEnabledDirectUrl()
        {
            string isEnabledDirectUrl = HttpUtility.ParseQueryString(HttpContext.Current.Request.Url.Query).Get("directUrl");
            return isEnabledDirectUrl != null ? Convert.ToBoolean(isEnabledDirectUrl) : false;
        }
    }
}