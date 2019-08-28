/*
 *
 * (c) Copyright Ascensio System SIA 2019
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
*/

using OnlineEditorsExampleMVC.Helpers;
using System;
using System.Collections.Generic;
using System.IO;
using System.Web;
using System.Web.Mvc;
using System.Web.Script.Serialization;

namespace OnlineEditorsExampleMVC.Models
{
    public class FileModel
    {
        public bool TypeDesktop { get; set; }

        public string FileUri
        {
            get { return DocManagerHelper.GetFileUri(FileName); }
        }

        public string FileName { get; set; }

        public string DocumentType
        {
            get { return FileUtility.GetFileType(FileName).ToString().ToLower(); }
        }

        public string Key
        {
            get { return ServiceConverter.GenerateRevisionId(DocManagerHelper.CurUserHostAddress() + "/" + FileName); }
        }

        public string CallbackUrl
        {
            get
            {
                return DocManagerHelper.GetCallback(FileName);
            }
        }

        public string GetDocConfig(HttpRequest request, UrlHelper url)
        {
            var ext = Path.GetExtension(FileName);
            var config = new Dictionary<string, object>()
            {
                { "type", request["mode"] != "embedded" ? "desktop" : "embedded" },
                { "documentType", DocumentType },
                { "document", new Dictionary<string, object>()
                {
                    { "title", FileName },
                    { "url", FileUri },
                    { "fileType", ext.Trim('.') },
                    { "key", Key },
                    { "info", new Dictionary<string,object>()
                    {
                        { "author", "Me" },
                        { "created", DateTime.Now.ToShortDateString() }
                    } },
                    { "permissions", new Dictionary<string, object>
                    {
                        { "edit", DocManagerHelper.EditedExts.Contains(Path.GetExtension(FileName)) },
                        { "download", true }
                    } }
                } },
                { "editorConfig", new Dictionary<string, object>()
                {
                    { "mode", DocManagerHelper.EditedExts.Contains(Path.GetExtension(FileName)) && request["mode"] != "view" ? "edit" : "view" },
                    { "lang", "en" },
                    { "callbackUrl", CallbackUrl },
                    { "user", new Dictionary<string, object>()
                    {
                        { "id", DocManagerHelper.CurUserHostAddress() },
                        { "name", "John Smith" }
                    } },
                    { "embedded", new Dictionary<string, object>()
                    {
                        { "saveUrl", FileUri },
                        { "embedUrl", FileUri },
                        { "shareUrl", FileUri },
                        { "toolbarDocked", "top" }
                    } },
                    { "customization", new Dictionary<string, object>()
                    {
                        { "about", true },
                        { "feedback", true },
                        { "goback", new Dictionary<string, object>()
                        {
                            { "url", url.Action("Index", "Home") }
                        } }
                    } }
                } }
            };

            if (JwtManager.Enabled)
            {
                var token = JwtManager.Encode(config);
                config.Add("token", token);
            }

            return new JavaScriptSerializer().Serialize(config);
        }
    }
}