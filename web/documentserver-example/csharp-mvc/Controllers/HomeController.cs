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
using System.IO;
using System.Web.Mvc;
using OnlineEditorsExampleMVC.Helpers;
using OnlineEditorsExampleMVC.Models;

namespace OnlineEditorsExampleMVC.Controllers
{
    public class HomeController : Controller
    {
        public ActionResult Index()
        {
            return View();
        }

        // viewing file in the editor
        public ActionResult Editor(string fileName, string editorsMode, string editorsType, string directUrl)
        {
            var file = new FileModel
            {
                Mode = editorsMode,  // editor mode: edit or view
                Type = editorsType,  // editor type: desktop, mobile, embedded
                FileName = Path.GetFileName(fileName),  // file name
                IsEnabledDirectUrl = directUrl != null ? Convert.ToBoolean(directUrl) : false
            };

            return View("Editor", file);
        }

        // creating a sample document
        public ActionResult Sample(string fileExt, bool? sample)
        {
            var fileName = DocManagerHelper.CreateDemo(fileExt, sample ?? false);  // create a sample document
            var id = Request.Cookies.GetOrDefault("uid", null);
            var user = Users.getUser(id);
            DocManagerHelper.CreateMeta(fileName, user.id, user.name);  // create meta information for the sample document
            Response.Redirect(Url.Action("Editor", "Home", new { fileName = fileName }));
            return null;
        }
    }
}