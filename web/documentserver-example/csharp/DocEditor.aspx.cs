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
using System.IO;
using System.Web;
using System.Web.Configuration;
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
            get { return ServiceConverter.GenerateRevisionId(_Default.CurUserHostAddress(null) + "/" + Path.GetFileName(FileUri) + "/" + File.GetLastWriteTime(_Default.StoragePath(FileName, null)).GetHashCode()); }
        }

        protected string DocServiceApiUri
        {
            get { return WebConfigurationManager.AppSettings["files.docservice.url.api"] ?? string.Empty; }
        }

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
                                    + "&userAddress=" + HttpUtility.UrlEncode(HttpContext.Current.Request.UserHostAddress)
                                    + "&fileName=" + HttpUtility.UrlEncode(FileName);
                return callbackUrl.ToString();
            }
        }

        protected void Page_Load(object sender, EventArgs e)
        {
            var externalUrl = Request["fileUrl"];
            if (!string.IsNullOrEmpty(externalUrl))
            {
                FileName = _Default.DoUpload(externalUrl);
            }
            else
            {
                FileName = Request["fileID"];
            }

            var type = Request["type"];
            if (!string.IsNullOrEmpty(type))
            {
                Try(type);
                Response.Redirect("doceditor.aspx?fileID=" + HttpUtility.UrlEncode(FileName));
            }
        }

        private static void Try(string type)
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
            var demoName = "demo" + ext;
            FileName = _Default.GetCorrectName(demoName);

            File.Copy(HttpRuntime.AppDomainAppPath + "app_data/" + demoName, _Default.StoragePath(FileName, null));
        }
    }
}