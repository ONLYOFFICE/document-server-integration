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
            DocManagerHelper.ContentPath = _environment.WebRootPath;
            DocManagerHelper.Context = HttpContext;
            return View();
        }

        public IActionResult Editor(string fileName, string editorsMode, string editorsType)
        {
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
            if (ModelState.IsValid)
            {
                var fileName = DocManagerHelper.CreateDemo(fileExt, sample ?? false);  // create a sample document
                var id = Request.Cookies.GetOrDefault("uid", null);
                var user = Users.getUser(id);
                DocManagerHelper.CreateMeta(fileName, user.id, user.name);  // create meta information for the sample document
                return Redirect(Url.Action("Editor", "Home", new { fileName = fileName }));
            }
            return new EmptyResult();
        }

        [Route("/upload")]
        public IActionResult Upload(IFormCollection form)
        {
            HttpContext.Response.ContentType = "text/plain";
            try
            {
                var httpPostedFile = form.Files[0];

                var fileName = httpPostedFile.FileName;
                if (HttpContext.Request.Headers["User-Agent"] == "IE")
                {
                    var files = httpPostedFile.FileName.Split(new char[] { '\\' });
                    fileName = files[files.Length - 1];
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
                using (var fs = System.IO.File.Open(savedFileName, FileMode.Create, FileAccess.ReadWrite))
                {
                    httpPostedFile.CopyTo(fs);  // and save it
                }

                // get file meta information or create the default one
                var id = HttpContext.Request.Cookies.GetOrDefault("uid", null);
                var user = Users.getUser(id);
                DocManagerHelper.CreateMeta(fileName, user.id, user.name);
                return Json(new Dictionary<string, object>() {
                    { "filename", fileName },
                    { "documentType", documentType }
                });
            }
            catch (Exception e)
            {
                return Json(new Dictionary<string, object>() { { "error", e.Message } });
            }
        }

        [Route("/convert")]
        public IActionResult Convert()
        {
            HttpContext.Response.ContentType = "text/plain";
            try
            {
                string fileData;

                using (var receiveStream = HttpContext.Request.Body)
                using (var readStream = new StreamReader(receiveStream))
                {
                    fileData = readStream.ReadToEnd();
                    if (string.IsNullOrEmpty(fileData)) return Json(new Dictionary<string, object>() { { "error", "1" }, { "message", "Request stream is empty" } });
                }

                var body = JsonConvert.DeserializeObject<Dictionary<string, object>>(fileData);

                var fileName = Path.GetFileName(body["filename"].ToString());
                var filePass = body["filePass"] != null ? body["filePass"].ToString() : null;
                var fileUri = DocManagerHelper.GetDownloadUrl(fileName);

                var extension = (Path.GetExtension(fileName).ToLower() ?? "").Trim('.');
                var internalExtension = DocManagerHelper.GetInternalExtension(FileUtility.GetFileType(fileName)).Trim('.');

                // check if the file with such an extension can be converted
                if (DocManagerHelper.ConvertExts.Contains("." + extension)
                    && !string.IsNullOrEmpty(internalExtension))
                {
                    // generate document key
                    var key = ServiceConverter.GenerateRevisionId(fileUri);

                    // get the url to the converted file
                    string newFileUri;
                    var result = ServiceConverter.GetConvertedUri(fileUri.ToString(), extension, internalExtension, key, true, out newFileUri, filePass);
                    if (result != 100)
                    {
                        return Json(new Dictionary<string, object>() { { "step", result }, { "filename", fileName } });
                    }

                    // get a file name of an internal file extension with an index if the file with such a name already exists
                    var correctName = DocManagerHelper.GetCorrectName(Path.GetFileNameWithoutExtension(fileName) + "." + internalExtension);

                    var req = (HttpWebRequest)WebRequest.Create(newFileUri);

                    using (var stream = req.GetResponse().GetResponseStream())  // get response stream of the converting file
                    {
                        if (stream == null) throw new Exception("Stream is null");
                        const int bufferSize = 4096;

                        using (var fs = System.IO.File.Open(DocManagerHelper.StoragePath(correctName), FileMode.Create))
                        {
                            var buffer = new byte[bufferSize];
                            int readed;
                            while ((readed = stream.Read(buffer, 0, bufferSize)) != 0)
                            {
                                fs.Write(buffer, 0, readed);  // write bytes to the output stream
                            }
                        }
                    }

                    Remove(fileName);  // remove the original file and its history if it exists
                    fileName = correctName;  // create meta information about the converted file with user id and name specified
                    var id = HttpContext.Request.Cookies.GetOrDefault("uid", null);
                    var user = Users.getUser(id);
                    DocManagerHelper.CreateMeta(fileName, user.id, user.name);
                }
                var documentType = FileUtility.GetFileType(fileName).ToString().ToLower();
                return Json(new Dictionary<string, object>() { { "filename", fileName }, { "documentType", documentType } });
            }
            catch (Exception e)
            {
                return Json(new Dictionary<string, object>() { { "error", e.Message } });
            }
        }

        // track file changes
        [Route("/track")]
        public IActionResult Track([FromQuery] string userAddress, [FromQuery] string fileName)
        {
            // read request body
            var fileData = TrackManager.readBody(HttpContext);

            var status = (WebEditorExtenstions.TrackerStatus)(Int64)fileData["status"];  // get status from the request body
            var saved = 1;  // editing
            switch (status)
            {
                case WebEditorExtenstions.TrackerStatus.Editing:
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
                    break;

                // MustSave, Corrupted
                case WebEditorExtenstions.TrackerStatus.MustSave:
                case WebEditorExtenstions.TrackerStatus.Corrupted:
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
                case WebEditorExtenstions.TrackerStatus.MustForceSave:
                case WebEditorExtenstions.TrackerStatus.CorruptedForceSave:
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
            return Json(new Dictionary<string, object>() { { "error", 0 } });
        }

        // remove a file
        [Route("/remove")]
        public IActionResult Remove([FromQuery] string fileName)
        {
            HttpContext.Response.ContentType = "text/plain";
            try
            {
                WebEditorExtenstions.Remove(fileName);
            }
            catch (Exception e)
            {
                return Json(new Dictionary<string, object>() { { "error", e.Message } });
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
                    return Json(JsonConvert.SerializeObject(files));
                }
                else
                {
                    files = DocManagerHelper.GetFilesInfo(fileId);
                    if (files.Count == 0)
                    {
                        return Json("File not found");
                    }
                    else
                    {
                        return Json(JsonConvert.SerializeObject(files));
                    }
                }
            }
            catch (Exception e)
            {
                return Json(new Dictionary<string, object>() { { "error", e.Message } });
            }
        }

        // get sample files from the assests
        [Route("/assets")]
        public async Task<IActionResult> Assets([FromQuery] string fileName)
        {
            var filePath = "assets/sample/" + fileName;
            await WebEditorExtenstions.DownloadAsync(filePath, HttpContext);
            return new EmptyResult();
        }

        // download a csv file
        [Route("/csv")]
        public async Task<IActionResult> GetCsv()
        {
            var fileName = "csv.csv";
            var filePath = "assets/sample/" + fileName;
            await WebEditorExtenstions.DownloadAsync(filePath, HttpContext);
            return new EmptyResult();
        }

        // download a file
        [Route("/download")]
        public async Task<IActionResult> Download([FromQuery] string fileName, [FromQuery] string userAddress)
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
                            return Json("JWT validation failed");
                        }
                    }
                }

                var filePath = DocManagerHelper.ForcesavePath(fileName, userAddress, false);  // get the path to the force saved document version
                if (filePath.Equals(""))
                {
                    filePath = DocManagerHelper.StoragePath(fileName, userAddress);  // or to the original document
                }
                await WebEditorExtenstions.DownloadAsync(filePath, HttpContext);
            }
            catch (Exception)
            {
                return Json(new Dictionary<string, object>() { { "error", "File not found!" } });
            }
            return new EmptyResult();
        }
    }
}
