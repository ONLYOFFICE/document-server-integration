/*
 *
 * (c) Copyright Ascensio System Limited 2010-2017
 *
 * This program is freeware. You can redistribute it and/or modify it under the terms of the GNU 
 * General Public License (GPL) version 3 as published by the Free Software Foundation (https://www.gnu.org/copyleft/gpl.html). 
 * In accordance with Section 7(a) of the GNU GPL its Section 15 shall be amended to the effect that 
 * Ascensio System SIA expressly excludes the warranty of non-infringement of any third-party rights.
 *
 * THIS PROGRAM IS DISTRIBUTED WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. For more details, see GNU GPL at https://www.gnu.org/copyleft/gpl.html
 *
 * You can contact Ascensio System SIA by email at sales@onlyoffice.com
 *
 * The interactive user interfaces in modified source and object code versions of ONLYOFFICE must display 
 * Appropriate Legal Notices, as required under Section 5 of the GNU GPL version 3.
 *
 * Pursuant to Section 7 § 3(b) of the GNU GPL you must retain the original ONLYOFFICE logo which contains 
 * relevant author attributions when distributing the software. If the display of the logo in its graphic 
 * form is not reasonably feasible for technical reasons, you must include the words "Powered by ONLYOFFICE" 
 * in every copy of the program you distribute. 
 * Pursuant to Section 7 § 3(e) we decline to grant you any rights under trademark law for use of our trademarks.
 *
*/

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text.RegularExpressions;
using System.Web;
using System.Web.Caching;
using System.Web.Configuration;
using System.Web.UI;
using ASC.Api.DocumentConverter;

namespace OnlineEditorsExample
{
    internal static class FileType
    {
        public static readonly List<string> ExtsSpreadsheet = new List<string>
            {
                ".xls", ".xlsx",
                ".ods", ".csv"
            };

        public static readonly List<string> ExtsPresentation = new List<string>
            {
                ".pps", ".ppsx",
                ".ppt", ".pptx",
                ".odp"
            };

        public static readonly List<string> ExtsDocument = new List<string>
            {
                ".docx", ".doc", ".odt", ".rtf", ".txt",
                ".html", ".htm", ".mht", ".pdf", ".djvu",
                ".fb2", ".epub", ".xps"
            };

        public static string GetInternalExtension(string extension)
        {
            extension = Path.GetExtension(extension).ToLower();
            if (ExtsDocument.Contains(extension)) return ".docx";
            if (ExtsSpreadsheet.Contains(extension)) return ".xlsx";
            if (ExtsPresentation.Contains(extension)) return ".pptx";
            return string.Empty;
        }
    }

    public partial class _Default : Page
    {
        public static UriBuilder Host
        {
            get
            {
                var uri = new UriBuilder(HttpContext.Current.Request.Url) {Query = ""};
                var requestHost = HttpContext.Current.Request.Headers["Host"];
                if (!string.IsNullOrEmpty(requestHost))
                    uri = new UriBuilder(uri.Scheme + "://" + requestHost);

                return uri;
            }
        }

        public static string VirtualPath
        {
            get
            {
                return
                    HttpRuntime.AppDomainAppVirtualPath
                    + (HttpRuntime.AppDomainAppVirtualPath.EndsWith("/") ? "" : "/")
                    + WebConfigurationManager.AppSettings["storage-path"]
                    + CurUserHostAddress(null) + "/";
            }
        }

        private static bool? _ismono;
        public static bool IsMono
        {
            get
            {
                return _ismono.HasValue ? _ismono.Value : (_ismono = (bool?)(Type.GetType("Mono.Runtime") != null)).Value;
            }
        }

        private static long MaxFileSize
        {
            get
            {
                long size;
                long.TryParse(WebConfigurationManager.AppSettings["filesize-max"], out size);
                return size > 0 ? size : 5*1024*1024;
            }
        }

        private static List<string> FileExts
        {
            get { return ViewedExts.Concat(EditedExts).Concat(ConvertExts).ToList(); }
        }

        private static List<string> ViewedExts
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

        public static bool EditMode
        {
            get { return (WebConfigurationManager.AppSettings["mode"] ?? "") != "view"; }
        }

        private static string _fileName;

        public static string CurUserHostAddress(string userAddress)
        {
            return Regex.Replace(userAddress ?? HttpContext.Current.Request.UserHostAddress, "[^0-9a-zA-Z.=]", "_");
        }

        public static string StoragePath(string fileName, string userAddress)
        {
            var directory = HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"] + CurUserHostAddress(userAddress) + "\\";
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }
            return directory + fileName;
        }

        public static string FileUri(string fileName)
        {
            var uri = Host;
            uri.Path = VirtualPath + fileName;
            return uri.ToString();
        }

        public static string DocumentType(string fileName)
        {
            var ext = Path.GetExtension(fileName).ToLower();

            if (FileType.ExtsDocument.Contains(ext)) return "text";
            if (FileType.ExtsSpreadsheet.Contains(ext)) return "spreadsheet";
            if (FileType.ExtsPresentation.Contains(ext)) return "presentation";

            return string.Empty;
        }

        protected string UrlPreloadScripts = WebConfigurationManager.AppSettings["files.docservice.url.preloader"];


        protected void Page_Load(object sender, EventArgs e)
        {
        }

        public static string DoUpload(HttpContext context)
        {
            var httpPostedFile = context.Request.Files[0];

            if (HttpContext.Current.Request.Browser.Browser.ToUpper() == "IE")
            {
                var files = httpPostedFile.FileName.Split(new char[] { '\\' });
                _fileName = files[files.Length - 1];
            }
            else
            {
                _fileName = httpPostedFile.FileName;
            }

            var curSize = httpPostedFile.ContentLength;
            if (MaxFileSize < curSize || curSize <= 0)
            {
                throw new Exception("File size is incorrect");
            }

            var curExt = (Path.GetExtension(_fileName) ?? "").ToLower();
            if (!FileExts.Contains(curExt))
            {
                throw new Exception("File type is not supported");
            }

            _fileName = GetCorrectName(_fileName);

            var savedFileName = StoragePath(_fileName, null);
            httpPostedFile.SaveAs(savedFileName);

            return _fileName;
        }

        public static string DoUpload(string fileUri)
        {
            _fileName = GetCorrectName(Path.GetFileName(fileUri));

            var curExt = (Path.GetExtension(_fileName) ?? "").ToLower();
            if (!FileExts.Contains(curExt))
            {
                throw new Exception("File type is not supported");
            }

            var req = (HttpWebRequest)WebRequest.Create(fileUri);

            try
            {
                // hack. http://ubuntuforums.org/showthread.php?t=1841740
                if (IsMono)
                {
                    ServicePointManager.ServerCertificateValidationCallback += (s, ce, ca, p) => true;
                }

                using (var stream = req.GetResponse().GetResponseStream())
                {
                    if (stream == null) throw new Exception("stream is null");
                    const int bufferSize = 4096;

                    using (var fs = File.Open(StoragePath(_fileName, null), FileMode.Create))
                    {
                        var buffer = new byte[bufferSize];
                        int readed;
                        while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                        {
                            fs.Write(buffer, 0, readed);
                        }
                    }
                }
            }
            catch (Exception)
            {

            }
            return _fileName;
        }

        public static string DoConvert(HttpContext context)
        {
            _fileName = context.Request["filename"];

            var extension = (Path.GetExtension(_fileName) ?? "").Trim('.');
            var internalExtension = FileType.GetInternalExtension(_fileName).Trim('.');

            if (ConvertExts.Contains("." + extension)
                && !string.IsNullOrEmpty(internalExtension))
            {
                var key = ServiceConverter.GenerateRevisionId(FileUri(_fileName));

                string newFileUri;
                var result = ServiceConverter.GetConvertedUri(FileUri(_fileName), extension, internalExtension, key, true, out newFileUri);
                if (result != 100)
                {
                    return "{ \"step\" : \"" + result + "\", \"filename\" : \"" + _fileName + "\"}";
                }

                var fileName = GetCorrectName(Path.GetFileNameWithoutExtension(_fileName) + "." + internalExtension);

                var req = (HttpWebRequest)WebRequest.Create(newFileUri);

                // hack. http://ubuntuforums.org/showthread.php?t=1841740
                if (IsMono)
                {
                    ServicePointManager.ServerCertificateValidationCallback += (s, ce, ca, p) => true;
                }

                using (var stream = req.GetResponse().GetResponseStream())
                {
                    if (stream == null) throw new Exception("Stream is null");
                    const int bufferSize = 4096;

                    using (var fs = File.Open(StoragePath(fileName, null), FileMode.Create))
                    {
                        var buffer = new byte[bufferSize];
                        int readed;
                        while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                        {
                            fs.Write(buffer, 0, readed);
                        }
                    }
                }

                File.Delete(StoragePath(_fileName, null));
                _fileName = fileName;
            }

            return "{ \"filename\" : \"" + _fileName + "\"}";
        }

        public static string GetCorrectName(string fileName, string userAddress = null)
        {
            var baseName = Path.GetFileNameWithoutExtension(fileName);
            var ext = Path.GetExtension(fileName);
            var name = baseName + ext;

            for (var i = 1; File.Exists(StoragePath(name, userAddress)); i++)
            {
                name = baseName + " (" + i + ")" + ext;
            }
            return name;
        }

        protected static List<string> GetStoredFiles()
        {
            var directory = HttpRuntime.AppDomainAppPath + WebConfigurationManager.AppSettings["storage-path"] + CurUserHostAddress(null) + "\\";
            if (!Directory.Exists(directory)) return new List<string>();

            var directoryInfo = new DirectoryInfo(directory);

            var storedFiles = directoryInfo.GetFiles("*.*", SearchOption.TopDirectoryOnly).Select(fileInfo => fileInfo.Name).ToList();
            return storedFiles;
        }
    }
}