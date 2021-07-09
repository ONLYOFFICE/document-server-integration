using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Newtonsoft.Json;
using OnlineEditorsExampleNetCore.Helpers;
using OnlineEditorsExampleNetCore.Models;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
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
            DocManagerHelper.Context = HttpContext;
            return View();
        }

        public IActionResult Editor(string fileName, string editorsMode, string editorsType)
        {
            DocManagerHelper.ContentPath = _environment.ContentRootPath;
            DocManagerHelper.Context = HttpContext;
            var file = new FileModel
                {
                    Mode = editorsMode,  // editor mode: edit or view
                    Type = editorsType,  // editor type: desktop, mobile, embedded
                    FileName = Path.GetFileName(fileName), // file name
                    
            };
            return View("Editor", file);
        }

        public IActionResult Sample(string fileExt, bool? sample)
        {
            DocManagerHelper.ContentPath = _environment.ContentRootPath;
            DocManagerHelper.Context = HttpContext;
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

        [Route("/upload")]
        public IActionResult Upload()
        {
            HttpContext.Response.ContentType = "text/plain";
            try
            {
                var httpPostedFile = HttpContext.Request.Form.Files[0];
                string fileName;

                if (HttpContext.Request.Headers["User-Agent"] == "IE")
                {
                    var files = httpPostedFile.FileName.Split(new char[] { '\\' });
                    fileName = files[files.Length - 1];
                }
                else
                {
                    fileName = httpPostedFile.FileName;
                }

                var curSize = httpPostedFile.Length;
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

                fileName = DocManagerHelper.GetCorrectName(fileName);  // get the correct file name if such a name already exists
                var documentType = FileUtility.GetFileType(fileName).ToString().ToLower();

                var savedFileName = DocManagerHelper.StoragePath(fileName);  // get the storage path to the uploading file
                //httpPostedFile.SaveAs(savedFileName);  // and save it
                // get file meta information or create the default one
                var id = HttpContext.Request.Cookies.GetOrDefault("uid", null);
                var user = Users.getUser(id);
                DocManagerHelper.CreateMeta(fileName, user.id, user.name);
                Json(new Dictionary<string, object>() {
                    { "filename", fileName },
                    { "documentType", documentType }
                });
            }
            catch (Exception e)
            {
                Json(new Dictionary<string, object>() { { "error", e.Message } });
            }
            return new EmptyResult();
        }

        [Route("/convert")]
        public IActionResult Convert([FromQuery] string fileName)
        {
            HttpContext.Response.ContentType = "text/plain";
            try
            {
                var fileUri = DocManagerHelper.GetFileUri(fileName, true);
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
                        Json(new Dictionary<string, object>() {
                            { "step", result },
                            { "filename", fileName }
                        });
                    }

                    var correctName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + "." + internalExtension);

                    var req = (HttpWebRequest)WebRequest.Create(newFileUri);

                    using (var stream = req.GetResponse().GetResponseStream())
                    {
                        if (stream == null) throw new Exception("Stream is null");
                        const int bufferSize = 4096;

                        using (var fs = System.IO.File.Open(DocManagerHelper.StoragePath(correctName), FileMode.Create))
                        {
                            var buffer = new byte[bufferSize];
                            int readed;
                            while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                            {
                                fs.Write(buffer, 0, readed);
                            }
                        }
                    }

                    WebEditorExtenstion.Remove(fileName);
                    fileName = correctName;
                    DocManagerHelper.CreateMeta(fileName, HttpContext.Request.Cookies.GetOrDefault("uid", ""), HttpContext.Request.Cookies.GetOrDefault("uname", ""));

                }
                Json(new Dictionary<string, object>() { { "filename", fileName } });
            }
            catch (Exception e)
            {
                Json(new Dictionary<string, object>() { { "error", e.Message } });
            }
            return new EmptyResult();
        }

        // track file changes
        [Route("/track")]
        public IActionResult Track([FromQuery] string userAddress, [FromQuery] string fileName)
        {
            // read request body
            var fileData = TrackManager.readBody(HttpContext);

            var status = (WebEditorExtenstion.TrackerStatus)(Int64)fileData["status"];  // get status from the request body
            var saved = 1;  // editing
            switch (status)
            {
                case WebEditorExtenstion.TrackerStatus.Editing:
                    try
                    {
                        var actions = JsonConvert.DeserializeObject<List<object>>(JsonConvert.SerializeObject(fileData["actions"]));
                        var action = JsonConvert.DeserializeObject<Dictionary<string, object>>(JsonConvert.SerializeObject(actions[0]));
                        if (action != null && action["type"].ToString().Equals("0"))  // finished edit
                        {
                            var user = action["userid"].ToString();  // the user who finished editing
                            var users = JsonConvert.DeserializeObject<List<object>>(JsonConvert.SerializeObject(fileData["users"]));
                            if (!users.Contains(user))
                            {
                                TrackManager.commandRequest("forcesave", fileData["key"].ToString());  // create a command request with the forcesave method
                            }

                        }
                    }
                    catch (Exception e)
                    {
                        Debug.Print(e.StackTrace);
                    }
                    return Json(new Dictionary<string, object>() { { "error", 0 } });

                // MustSave, Corrupted
                case WebEditorExtenstion.TrackerStatus.MustSave:
                case WebEditorExtenstion.TrackerStatus.Corrupted:
                    try
                    {
                        // saving a document
                        saved = TrackManager.processSave(fileData, fileName, userAddress);
                    }
                    catch (Exception)
                    {
                        saved = 1;
                    }
                    return Json(new Dictionary<string, object>() { { "error", saved } });

                // MustForceSave, CorruptedForceSave
                case WebEditorExtenstion.TrackerStatus.MustForceSave:
                case WebEditorExtenstion.TrackerStatus.CorruptedForceSave:
                    try
                    {
                        // force saving a document
                        saved = TrackManager.processForceSave(fileData, fileName, userAddress);
                    }
                    catch (Exception)
                    {
                        saved = 1;
                    }
                    return Json(new Dictionary<string, object>() { { "error", saved } });
            }

            return BadRequest();
        }

        // remove a file
        [Route("/remove")]
        public IActionResult Remove([FromQuery] string fileName)
        {
            DocManagerHelper.Context = _httpContextAccessor.HttpContext;
            HttpContext.Response.ContentType = "text/plain";
            try
            {
                WebEditorExtenstion.Remove(fileName);
                Json(new Dictionary<string, object>() { { "success", true } });
            }
            catch (Exception e)
            {
                Json(new Dictionary<string, object>() { { "error", e.Message } });
            }
            return Redirect(Url.Action("Index", "Home"));
        }

        // get files information
        [Route("/files")]
        public IActionResult Files([FromQuery] string fileId)
        {
            List<Dictionary<string, object>> files = null;

            try
            {
                HttpContext.Response.ContentType = "application/json";

                if (fileId.ToString() == null)
                {
                    files = DocManagerHelper.GetFilesInfo();  // get the information about the files from the storage path
                    Json(JsonConvert.SerializeObject(files));
                }
                else
                {
                    files = DocManagerHelper.GetFilesInfo(fileId);
                    if (files.Count == 0)
                    {
                        Json("File not found");
                    }
                    else
                    {
                        Json(JsonConvert.SerializeObject(files));
                    }
                }
            }
            catch (Exception e)
            {
                Json(new Dictionary<string, object>() { { "error", e.Message } });
            }
            return new EmptyResult();
        }

        // get sample files from the assests
        [Route("/assets")]
        public IActionResult Assets([FromQuery] string fileName)
        {
            var filePath = "assets/sample/" + fileName;
            WebEditorExtenstion.download(filePath, HttpContext);
            return new EmptyResult();
        }

        // download a csv file
        [Route("/csv")]
        public IActionResult GetCsv()
        {
            var fileName = "csv.csv";
            var filePath = "assets/sample/" + fileName;
            WebEditorExtenstion.download(filePath, HttpContext);
            return new EmptyResult();
        }

        // download a file
        [Route("/download")]
        public IActionResult Download([FromQuery] string fileName, [FromQuery] string userAddress)
        {
            try
            {
                if (JwtManager.Enabled)
                {
                    string JWTheader = Startup.AppSettings["files.docservice.header"].Equals("") ? "Authorization" : Startup.AppSettings["files.docservice.header"];

                    if (HttpContext.Request.Headers.Keys.Contains(JWTheader, StringComparer.InvariantCultureIgnoreCase))
                    {
                        var headerToken = HttpContext.Request.Headers[JWTheader].ToString().Substring("Bearer ".Length);
                        string token = JwtManager.Decode(headerToken);
                        if (token == null || token.Equals(""))
                        {
                            HttpContext.Response.StatusCode = (int)HttpStatusCode.Forbidden;
                            Json("JWT validation failed");
                        }
                    }
                }

                var filePath = DocManagerHelper.ForcesavePath(fileName, userAddress, false);  // get the path to the force saved document version
                if (filePath.Equals(""))
                {
                    filePath = DocManagerHelper.StoragePath(fileName, userAddress);  // or to the original document
                }
                WebEditorExtenstion.download(filePath, HttpContext);
            }
            catch (Exception)
            {
                Json(new Dictionary<string, object>() { { "error", "File not found!" } });
            }
            return new EmptyResult();
        }
    }
}
