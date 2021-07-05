using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;
using OnlineEditorsExampleNetCore.Helpers;
using OnlineEditorsExampleNetCore.Models;
using OnlineEditorsExampleNetCore.Services;
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

        public IActionResult Sample(string fileExt, bool? sample)
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

        [Route("/upload")]
        public IActionResult Upload()
        {
            _httpContextAccessor.HttpContext.Response.ContentType = "text/plain";
            try
            {
                var httpPostedFile = _httpContextAccessor.HttpContext.Request.Form.Files[0];
                string fileName;

                if (_httpContextAccessor.HttpContext.Request.Headers["User-Agent"] == "IE")
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
                var id = _httpContextAccessor.HttpContext.Request.Cookies.GetOrDefault("uid", null);
                var user = Users.getUser(id);
                DocManagerHelper.CreateMeta(fileName, user.id, user.name);
                _httpContextAccessor.HttpContext.Response.WriteAsync("{ \"filename\": \"" + fileName + "\", \"documentType\": \"" + documentType + "\"}");
            }
            catch (Exception e)
            {
                _httpContextAccessor.HttpContext.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
            }
            return new EmptyResult();
        }

        [Route("/convert")]
        public IActionResult Convert([FromQuery(Name = "filename")] string fileName)
        {
            _httpContextAccessor.HttpContext.Response.ContentType = "text/plain";
            try
            {
                //var fileName = _httpContextAccessor.HttpContext.Request.Query["filename"];
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
                        _httpContextAccessor.HttpContext.Response.WriteAsync("{ \"step\" : \"" + result + "\", \"filename\" : \"" + fileName + "\"}");
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
                    DocManagerHelper.CreateMeta(fileName, _httpContextAccessor.HttpContext.Request.Cookies.GetOrDefault("uid", ""), _httpContextAccessor.HttpContext.Request.Cookies.GetOrDefault("uname", ""));

                }
                _httpContextAccessor.HttpContext.Response.WriteAsync("{ \"filename\" : \"" + fileName + "\"}");
            }
            catch (Exception e)
            {
                _httpContextAccessor.HttpContext.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
            }
            return new EmptyResult();
        }

        // track file changes
        [Route("/track")]
        public IActionResult Track([FromQuery] string userAddress, [FromQuery] string fileName)
        {
            // read request body
            var fileData = TrackManager.readBody(HttpContext);

            //var userAddress = _httpContextAccessor.HttpContext.Request.Query["userAddress"];
            //var fileName = Path.GetFileName(_httpContextAccessor.HttpContext.Request.Query["fileName"]);
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
        public IActionResult Remove([FromQuery(Name = "fileName")] string fileName)
        {
            _httpContextAccessor.HttpContext.Response.ContentType = "text/plain";
            try
            {
                //var fileName = _httpContextAccessor.HttpContext.Request.Query["fileName"];
                WebEditorExtenstion.Remove(fileName);
                _httpContextAccessor.HttpContext.Response.WriteAsync("{ \"success\": true }");
            }
            catch (Exception e)
            {
                _httpContextAccessor.HttpContext.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
            }
            return new EmptyResult();
        }

        // get files information
        [Route("/files")]
        public IActionResult Files([FromQuery(Name = "fileId")] string fileId)
        {
            List<Dictionary<string, object>> files = null;

            try
            {
                _httpContextAccessor.HttpContext.Response.ContentType = "application/json";

                if (fileId.ToString() == null)
                {
                    files = DocManagerHelper.GetFilesInfo();  // get the information about the files from the storage path
                    _httpContextAccessor.HttpContext.Response.WriteAsync(JsonConvert.SerializeObject(files));
                }
                else
                {
                    //var fileId = _httpContextAccessor.HttpContext.Request.Query["fileId"];  // get file id from the request
                    files = DocManagerHelper.GetFilesInfo(fileId);
                    if (files.Count == 0)
                    {
                        _httpContextAccessor.HttpContext.Response.WriteAsync("\"File not found\"");
                    }
                    else
                    {
                        _httpContextAccessor.HttpContext.Response.WriteAsync(JsonConvert.SerializeObject(files));
                    }
                }
            }
            catch (Exception e)
            {
                _httpContextAccessor.HttpContext.Response.WriteAsync("{ \"error\": \"" + e.Message + "\"}");
            }
            return new EmptyResult();
        }

        // get sample files from the assests
        [Route("/assets")]
        public IActionResult Assets([FromQuery(Name = "fileName")] string fileName)
        {
            //var fileName = Path.GetFileName(_httpContextAccessor.HttpContext.Request.Query["filename"]);
            var filePath = "assets/sample/" + fileName;
            WebEditorExtenstion.download(filePath, _httpContextAccessor.HttpContext);
            return new EmptyResult();
        }

        // download a csv file
        [Route("/csv")]
        public IActionResult GetCsv()
        {
            var fileName = "csv.csv";
            var filePath = "assets/sample/" + fileName;
            WebEditorExtenstion.download(filePath, _httpContextAccessor.HttpContext);
            return new EmptyResult();
        }

        // download a file
        [Route("/download")]
        public IActionResult Download([FromQuery(Name = "fileName")] string fileName, [FromQuery(Name = "userAddress")] string userAddress)
        {
            try
            {
                //var fileName = Path.GetFileName(_httpContextAccessor.HttpContext.Request.Query["fileName"]);
                //var userAddress = _httpContextAccessor.HttpContext.Request.Query["userAddress"];

                if (JwtManager.Enabled)
                {
                    string JWTheader = Startup.AppSettings["files.docservice.header"].Equals("") ? "Authorization" : Startup.AppSettings["files.docservice.header"];

                    if (_httpContextAccessor.HttpContext.Request.Headers.Keys.Contains(JWTheader, StringComparer.InvariantCultureIgnoreCase))
                    {
                        var headerToken = _httpContextAccessor.HttpContext.Request.Headers[JWTheader].ToString().Substring("Bearer ".Length);
                        string token = JwtManager.Decode(headerToken);
                        if (token == null || token.Equals(""))
                        {
                            _httpContextAccessor.HttpContext.Response.StatusCode = (int)HttpStatusCode.Forbidden;
                            _httpContextAccessor.HttpContext.Response.WriteAsync("JWT validation failed");
                            
                        }
                    }
                }

                var filePath = DocManagerHelper.ForcesavePath(fileName, userAddress, false);  // get the path to the force saved document version
                if (filePath.Equals(""))
                {
                    filePath = DocManagerHelper.StoragePath(fileName, userAddress);  // or to the original document
                }
                WebEditorExtenstion.download(filePath, _httpContextAccessor.HttpContext);
            }
            catch (Exception)
            {
                _httpContextAccessor.HttpContext.Response.WriteAsync("{ \"error\": \"File not found!\"}");
            }
            return new EmptyResult();
        }
    }
}
