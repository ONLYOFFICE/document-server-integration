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
        private readonly IConfiguration _configuration;

        public HomeController(IWebHostEnvironment environment, IHttpContextAccessor httpContextAccessor, IConfiguration configuration)
        {
            _environment = environment;
            _httpContextAccessor = httpContextAccessor;
            _configuration = configuration;
        }
        public IActionResult Index()
        {
            return View();
        }

        public IActionResult Editor(string fileName, string editorsMode, string editorsType)
        {
            DocManagerHelper.wwwPath = _environment.WebRootPath;
            DocManagerHelper.ContentPath = _environment.ContentRootPath;
            DocManagerHelper.Configuration = _configuration;
            var test = Environment.GetEnvironmentVariable("applicationUrl");
            DocManagerHelper.HttpContext = _httpContextAccessor.HttpContext;

            var file = new FileModel
                {
                    Mode = editorsMode,  // editor mode: edit or view
                    Type = editorsType,  // editor type: desktop, mobile, embedded
                    FileName = Path.GetFileName(fileName), // file name
                    
            };
            return View("Editor", file);
        }

        public ActionResult Sample(string fileExt, bool? sample) ///Editor?fileName=new%20%287%29.docx
        {
            DocManagerHelper.Host = _httpContextAccessor.HttpContext.Request.Host.ToString();
            if (ModelState.IsValid)
            {
                var fileName = DocManagerHelper.CreateDemo(fileExt, false);  // create a sample document

                //var id = Request.Cookies.GetOrDefault("uid", null);
                //var user = Users.getUser(id);
                //DocManagerHelper.CreateMeta(fileName, user.id, user.name);  // create meta information for the sample document
                Response.Redirect(Url.Action("Editor", "Home", new { fileName = fileName }));
            }
            return new EmptyResult();
        }
    }
}
