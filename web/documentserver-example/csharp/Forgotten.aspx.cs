/**
 *
 * (c) Copyright Ascensio System SIA 2025
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
using System.Web.Configuration;
using System.Web.UI;

namespace OnlineEditorsExample
{
    public partial class Forgotten : Page
    {

        //get server version
        public static string GetVersion()
        {
            return WebConfigurationManager.AppSettings["version"];
        }

        private static bool? _ismono;

        public static bool IsMono
        {
            get { return _ismono.HasValue ? _ismono.Value : (_ismono = (bool?)(Type.GetType("Mono.Runtime") != null)).Value; }
        }

        // get the document type
        public static string DocumentType(string fileName)
        {
            var ext = Path.GetExtension(fileName).ToLower();

            if (FormatManager.PdfExtensions().Contains(ext)) return "pdf";  // pdf for pdf extensions
            if (FormatManager.DocumentExtensions().Contains(ext)) return "word";  // word for text document extensions
            if (FormatManager.SpreadsheetExtensions().Contains(ext)) return "cell";  // cell for spreadsheet extensions
            if (FormatManager.PresentationExtensions().Contains(ext)) return "slide";  // slide for presentation extensions

            return "word";  // the default document type is word
        }

        protected void Page_Load(object sender, EventArgs e)
        {
            if (!bool.Parse(WebConfigurationManager.AppSettings["enable-forgotten"]))
            {
                Response.Clear();
                Response.StatusCode = 403;
                Response.End();
            }
        }

        // fetch forgotten files from the document server
        public static List<Dictionary<string, string>> GetForgottenFiles()
        {
            var files = new List<Dictionary<string, string>>();

            try
            {
                var response = TrackManager.commandRequest("getForgottenList", null);
                ArrayList keys = (ArrayList) response["keys"];

                // fetch all the forgotten files from the document server
                foreach (string key in keys)
                {
                    var file = new Dictionary<string, string>();
                    var fileResult = TrackManager.commandRequest("getForgotten", key);
                    file.Add("key", fileResult["key"].ToString());
                    file.Add("url", fileResult["url"].ToString());
                    file.Add("type", DocumentType(fileResult["url"].ToString()));

                    files.Add(file);
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine(ex.Message);
            }

            return files;
        }
    }
}