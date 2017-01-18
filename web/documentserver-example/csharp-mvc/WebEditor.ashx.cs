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
using System.Net;
using System.Web;
using System.Web.Script.Serialization;
using System.Web.Services;
using OnlineEditorsExampleMVC.Helpers;
using OnlineEditorsExampleMVC.Models;

namespace OnlineEditorsExampleMVC
{
    [WebService(Namespace = "http://tempuri.org/")]
    [WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
    public class WebEditor : IHttpHandler
    {
        public void ProcessRequest(HttpContext context)
        {
            switch (context.Request["type"])
            {
                case "upload":
                    Upload(context);
                    break;
                case "convert":
                    Convert(context);
                    break;
                case "track":
                    Track(context);
                    break;
            }
        }

        private static void Upload(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var httpPostedFile = context.Request.Files[0];
                string fileName;

                if (HttpContext.Current.Request.Browser.Browser.ToUpper() == "IE")
                {
                    var files = httpPostedFile.FileName.Split(new char[] { '\\' });
                    fileName = files[files.Length - 1];
                }
                else
                {
                    fileName = httpPostedFile.FileName;
                }

                var curSize = httpPostedFile.ContentLength;
                if (DocManagerHelper.MaxFileSize < curSize || curSize <= 0)
                {
                    throw new Exception("File size is incorrect");
                }

                var curExt = (Path.GetExtension(fileName) ?? "").ToLower();
                if (!DocManagerHelper.FileExts.Contains(curExt))
                {
                    throw new Exception("File type is not supported");
                }

                fileName = DocManagerHelper.GetCorrectName(fileName);

                var savedFileName = DocManagerHelper.StoragePath(fileName);
                httpPostedFile.SaveAs(savedFileName);

                context.Response.Write("{ \"filename\": \"" + fileName + "\"}");
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        private static void Convert(HttpContext context)
        {
            context.Response.ContentType = "text/plain";
            try
            {
                var fileName = context.Request["filename"];
                var fileUri = DocManagerHelper.GetFileUri(fileName);

                var extension = (Path.GetExtension(fileUri) ?? "").Trim('.');
                var internalExtension = DocManagerHelper.GetInternalExtension(FileUtility.GetFileType(fileName)).Trim('.');

                if (DocManagerHelper.ConvertExts.Contains("." + extension)
                    && !string.IsNullOrEmpty(internalExtension))
                {
                    var key = ServiceConverter.GenerateRevisionId(fileUri);

                    string newFileUri;
                    var result = ServiceConverter.GetConvertedUri(fileUri, extension, internalExtension, key, true, out newFileUri);
                    if (result != 100)
                    {
                        context.Response.Write("{ \"step\" : \"" + result + "\", \"filename\" : \"" + fileName + "\"}");
                        return;
                    }

                    var correctName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + "." + internalExtension);

                    var req = (HttpWebRequest)WebRequest.Create(newFileUri);

                    using (var stream = req.GetResponse().GetResponseStream())
                    {
                        if (stream == null) throw new Exception("Stream is null");
                        const int bufferSize = 4096;

                        using (var fs = File.Open(DocManagerHelper.StoragePath(correctName), FileMode.Create))
                        {
                            var buffer = new byte[bufferSize];
                            int readed;
                            while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                            {
                                fs.Write(buffer, 0, readed);
                            }
                        }
                    }

                    File.Delete(DocManagerHelper.StoragePath(fileName));
                    fileName = correctName;
                }

                context.Response.Write("{ \"filename\" : \"" + fileName + "\"}");
            }
            catch (Exception e)
            {
                context.Response.Write("{ \"error\": \"" + e.Message + "\"}");
            }
        }

        private enum TrackerStatus
        {
            NotFound = 0,
            Editing = 1,
            MustSave = 2,
            Corrupted = 3,
            Closed = 4,
        }

        private static void Track(HttpContext context)
        {
            var userAddress = context.Request["userAddress"];
            var fileName = context.Request["fileName"];

            var storagePath = DocManagerHelper.StoragePath(fileName, userAddress);

            string body;
            try
            {
                using (var receiveStream = context.Request.InputStream)
                using (var readStream = new StreamReader(receiveStream))
                {
                    body = readStream.ReadToEnd();
                }
            }
            catch (Exception e)
            {
                throw new HttpException((int) HttpStatusCode.BadRequest, e.Message);
            }

            var jss = new JavaScriptSerializer();
            if (string.IsNullOrEmpty(body)) return;
            var fileData = jss.Deserialize<Dictionary<string, object>>(body);
            var status = (TrackerStatus) (int) fileData["status"];

            switch (status)
            {
                case TrackerStatus.MustSave:
                case TrackerStatus.Corrupted:
                    var downloadUri = (string) fileData["url"];

                    var req = (HttpWebRequest) WebRequest.Create(downloadUri);

                    var saved = 1;
                    try
                    {
                        using (var stream = req.GetResponse().GetResponseStream())
                        {
                            if (stream == null) throw new Exception("stream is null");
                            const int bufferSize = 4096;

                            using (var fs = File.Open(storagePath, FileMode.Create))
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
                        saved = 0;
                    }

                    break;
            }
            context.Response.Write("{\"error\":0}");
        }

        public bool IsReusable
        {
            get { return false; }
        }
    }
}