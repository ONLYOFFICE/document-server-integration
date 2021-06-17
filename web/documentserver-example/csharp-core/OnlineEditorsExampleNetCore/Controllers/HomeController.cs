using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using OnlineEditorsExampleNetCore.Models;
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
        public IActionResult Index()
        {
            return View();
        }

        public ActionResult Editor(string fileName, string editorsMode, string editorsType, IWebHostEnvironment environment, IHttpContextAccessor httpContextAccessor, IConfiguration configuration)
        {

        var file = new FileModel
            {
                Mode = editorsMode,  // editor mode: edit or view
                Type = editorsType,  // editor type: desktop, mobile, embedded
                FileName = Path.GetFileName(fileName), // file name
                _environment = environment,
                _httpContextAccessor = httpContextAccessor,
                _configuration = configuration,
        };

            return View("Editor", file);
        }
    }
}
