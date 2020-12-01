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

        public ActionResult Editor(string fileName, string editorsMode, string editorsType)
        {
            var file = new FileModel
            {
                Mode = editorsMode,
                Type = editorsType,
                FileName = fileName
            };

            return View("Editor", file);
        }

        public ActionResult Sample(string fileExt, bool? sample)
        {
            var fileName = DocManagerHelper.CreateDemo(fileExt, sample ?? false);
            DocManagerHelper.CreateMeta(fileName, Request.Cookies.GetOrDefault("uid", ""), Request.Cookies.GetOrDefault("uname", ""));
            Response.Redirect(Url.Action("Editor", "Home", new { fileName = fileName }));
            return null;
        }
    }
}