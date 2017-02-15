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

        public static string CreateDemo(string fileExt)
        {
            var demoName = "sample." + fileExt;

            var fileName = GetCorrectName(demoName);

            File.Copy(HttpRuntime.AppDomainAppPath + "app_data\\" + demoName, StoragePath(fileName));

            return fileName;
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