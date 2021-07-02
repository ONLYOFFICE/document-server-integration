using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using OnlineEditorsExampleNetCore.Helpers;
using OnlineEditorsExampleNetCore.Models;
using OnlineEditorsExampleNetCore.Services;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading.Tasks;

namespace OnlineEditorsExampleNetCore.Controllers
{
    public class HomeController : Controller
    {
        private readonly IWebHostEnvironment _environment;
        private readonly IHttpContextAccessor _httpContextAccessor;

        public HomeController(IWebHostEnvironment environment, IHttpContextAccessor httpContextAccessor)
        {
            _environment = environment;
            _httpContextAccessor = httpContextAccessor;
        }

        public IActionResult Index()
        {
            DocManagerHelper.ContentPath = _environment.ContentRootPath;
            DocManagerHelper.Context = _httpContextAccessor.HttpContext;
            return View();
        }

        public IActionResult Editor(string fileName, string editorsMode, string editorsType)
        {
            DocManagerHelper.ContentPath = _environment.ContentRootPath;
            DocManagerHelper.Context = _httpContextAccessor.HttpContext;
            var file = new FileModel
                {
                    Mode = editorsMode,  // editor mode: edit or view
                    Type = editorsType,  // editor type: desktop, mobile, embedded
                    FileName = Path.GetFileName(fileName), // file name
                    
            };
            return View("Editor", file);
        }

        public IActionResult Sample(string fileExt, bool? sample) ///Editor?fileName=new%20%287%29.docx
        {
            DocManagerHelper.ContentPath = _environment.ContentRootPath;
            DocManagerHelper.Context = _httpContextAccessor.HttpContext;
            if (ModelState.IsValid)
            {
                var fileName = DocManagerHelper.CreateDemo(fileExt, false);  // create a sample document
                var id = Request.Cookies.GetOrDefault("uid", null);
                var user = Users.getUser(id);
                DocManagerHelper.CreateMeta(fileName, user.id, user.name);  // create meta information for the sample document
                Response.Redirect(Url.Action("Editor", "Home", new { fileName = fileName }));
            }
            return new EmptyResult();
        }

        public IActionResult Upload()
        {
            DocManagerHelper.ContentPath = _environment.ContentRootPath;
            DocManagerHelper.Context = _httpContextAccessor.HttpContext;
            Response.Redirect(Url.Content("~/?type=upload"));
            return new EmptyResult();
        }
    }
}
