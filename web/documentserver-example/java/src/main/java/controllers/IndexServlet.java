/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

package controllers;

import com.google.gson.Gson;
import entities.User;
import helpers.*;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import entities.FileType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.primeframework.jwt.Verifier;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACVerifier;

@WebServlet(name = "IndexServlet", urlPatterns = {"/IndexServlet"})
@MultipartConfig
public class IndexServlet extends HttpServlet
{
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // get the type parameter from the request
        String action = request.getParameter("type");

        if (action == null)
        {
            // forward the request and response objects to the index.jsp
            request.getRequestDispatcher("index.jsp").forward(request, response);
            return;
        }

        DocumentManager.Init(request, response);
        PrintWriter writer = response.getWriter();  // create a variable to display information about the application and error messages

        // define functions for each type of operation
        switch (action.toLowerCase())
        {
            case "upload":
                Upload(request, response, writer);
                break;
            case "download":
                String ver = request.getParameter("ver");

                if (ver == null | ver == "") {
                    Download(request, response, writer);
                } else {
                    DownloadHistory(request, response, writer);
                }
                break;
            case "convert":
                Convert(request, response, writer);
                break;
            case "track":
                Track(request, response, writer);
                break;
            case "remove":
                Remove(request, response, writer);
                break;
            case "assets":
                Assets(request, response, writer);
                break;
            case "csv":
                CSV(request, response, writer);
                break;
            case "files":
                Files(request, response, writer);
                break;
            case "saveas":
                SaveAs(request, response, writer);
                break;
        }
    }

    private static void SaveAs(HttpServletRequest request, HttpServletResponse response, PrintWriter writer) {
        response.setContentType("text/plain");
        try {
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            String bodyString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(bodyString);

            CookieManager cm = new CookieManager(request);
            User user = Users.getUser(cm.getCookie("uid"));

            String title = (String) body.get("title");
            String saveAsFileUrl = (String) body.get("url");
            int filesizeMax = Integer.parseInt(ConfigManager.GetProperty("filesize-max"));

            URL url = new URL(saveAsFileUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            InputStream stream = connection.getInputStream();

            if (filesizeMax < stream.available() || stream.available() <= 0) {
                writer.write( "{\"error\":\"File size is incorrect\"}");
            }

            String fileName = DocumentManager.GetCorrectName(title, null);
            DocumentManager.CreateFile(Paths.get(DocumentManager.StoragePath(fileName, null)), stream);

            DocumentManager.CreateMeta(fileName, user.id, user.name, null);

            writer.write( "{\"file\":  \"" + fileName + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writer.write("{ \"error\" : 1, \"message\" : \"" + e.getMessage() + "\"}");
        }
    }


    // upload a file
    private static void Upload(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        response.setContentType("text/plain");

        try
        {
            Part httpPostedFile = request.getPart("file");

            // get file name from the content-disposition response header
            String fileName = "";
            for (String content : httpPostedFile.getHeader("content-disposition").split(";"))
            {
                if (content.trim().startsWith("filename"))
                {
                    fileName = content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
                }
            }

            long curSize = httpPostedFile.getSize();  // get file size
            if (DocumentManager.GetMaxFileSize() < curSize || curSize <= 0)  // check if the file size exceeds the maximum file size or is less than 0
            {
                writer.write("{ \"error\": \"File size is incorrect\"}");  // if so, write the error status and message to the response
                return;
            }

            String curExt = FileUtility.GetFileExtension(fileName);  // get current file extension
            if (!DocumentManager.GetFileExts().contains(curExt))  // check if this extension is supported by the editor
            {
                writer.write("{ \"error\": \"File type is not supported\"}");  // if not, write the error status and message to the response
                return;
            }

            InputStream fileStream = httpPostedFile.getInputStream();  // get input file stream

            fileName = DocumentManager.GetCorrectName(fileName, null);  // get a file name with an index if the file with such a name already exists
            String fileStoragePath = DocumentManager.StoragePath(fileName, null);  // get the storage path of the file
            String documentType = FileUtility.GetFileType(fileName).toString().toLowerCase();

            File file = new File(fileStoragePath);

            try (FileOutputStream out = new FileOutputStream(file))
            {
                int read;
                final byte[] bytes = new byte[1024];
                while ((read = fileStream.read(bytes)) != -1)
                {
                    out.write(bytes, 0, read);  // write bytes to the output stream
                }

                // force write data to the output stream that can be cached in the current thread
                out.flush();
            }

            // create meta information with the user id and name specified
            CookieManager cm = new CookieManager(request);
            User user = Users.getUser(cm.getCookie("uid"));

            DocumentManager.CreateMeta(fileName, user.id, user.name, null);

            writer.write("{ \"filename\": \"" + fileName + "\", \"documentType\": \"" + documentType + "\" }");

        }
        catch (Exception e)
        {
            writer.write("{ \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // convert a file
    private static void Convert(HttpServletRequest request, HttpServletResponse response, PrintWriter writer) throws UnsupportedEncodingException {
        CookieManager cm = new CookieManager(request);
        response.setContentType("text/plain");

        try
        {
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            String bodyString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(bodyString);

            String fileName = FileUtility.GetFileName((String) body.get("filename"));
            String lang = cm.getCookie("ulang");
            String filePass = body.get("filePass") != null ? (String) body.get("filePass") : null;
            String fileUri = DocumentManager.GetDownloadUrl(fileName);
            String fileExt = FileUtility.GetFileExtension(fileName);
            FileType fileType = FileUtility.GetFileType(fileName);
            String internalFileExt = DocumentManager.GetInternalExtension(fileType);

            // check if the file with such an extension can be converted
            if (DocumentManager.GetConvertExts().contains(fileExt))
            {
                // generate document key
                String key = ServiceConverter.GenerateRevisionId(fileUri);

                // get the url to the converted file
                String newFileUri = ServiceConverter.GetConvertedUri(fileUri, fileExt, internalFileExt, key, filePass, true, lang);

                if (newFileUri.isEmpty())
                {
                    writer.write("{ \"step\" : \"0\", \"filename\" : \"" + fileName + "\"}");
                    return;
                }

                // get a file name of an internal file extension with an index if the file with such a name already exists
                String correctName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + internalFileExt, null);

                URL url = new URL(newFileUri);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();  // get input stream of the converted file

                if (stream == null)
                {
                    throw new Exception("Stream is null");
                }

                File convertedFile = new File(DocumentManager.StoragePath(correctName, null));
                try (FileOutputStream out = new FileOutputStream(convertedFile))
                {
                    int read;
                    final byte[] bytes = new byte[1024];
                    while ((read = stream.read(bytes)) != -1)
                    {
                        out.write(bytes, 0, read);  // write bytes to the output stream
                    }

                    // force write data to the output stream that can be cached in the current thread
                    out.flush();
                }

                connection.disconnect();

                // remove source file ?
                // File sourceFile = new File(DocumentManager.StoragePath(fileName, null));
                // sourceFile.delete();

                fileName = correctName;

                // create meta information about the converted file with the user id and name specified
                User user = Users.getUser(cm.getCookie("uid"));

                DocumentManager.CreateMeta(fileName, user.id, user.name, null);
            }

            writer.write("{ \"filename\" : \"" + fileName + "\"}");

        }
        catch (Exception ex)
        {
            writer.write("{ \"error\": \"" + ex.getMessage() + "\"}");
        }
    }

    // track file changes
    private static void Track(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        JSONObject body = null;

        // read request body
        try {
            body = TrackManager.readBody(request, writer);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // get status from the request body
        int status = Math.toIntExact((long) body.get("status"));
        int saved = 0;

        if (status == 1) { // editing
            JSONArray actions = (JSONArray) body.get("actions");
            JSONArray users = (JSONArray) body.get("users");
            JSONObject action = (JSONObject) actions.get(0);
            if (actions != null && action.get("type").toString().equals("0")) { // finished edit
                String user = (String) action.get("userid");  // the user who finished editing
                if (users.indexOf(user) == -1) {
                    String key = (String) body.get("key");
                    try {
                        TrackManager.commandRequest("forcesave", key);  // create a command request with the forcesave method
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        String userAddress = request.getParameter("userAddress");
        String fileName = FileUtility.GetFileName(request.getParameter("fileName"));

        if (status == 2 || status == 3) { // MustSave, Corrupted
            try {
                TrackManager.processSave(body, fileName, userAddress);
            } catch (Exception ex) {
                ex.printStackTrace();
                saved = 1;
            }

        }

        if (status == 6 || status == 7) { // MustForceSave, CorruptedForceSave
            try {
                TrackManager.processForceSave(body, fileName, userAddress);
            } catch (Exception ex) {
                ex.printStackTrace();
                saved = 1;
            }
        }

        writer.write("{\"error\":" + saved + "}");
    }

    // remove a file
    private static void Remove(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        try
        {
            String fileName = FileUtility.GetFileName(request.getParameter("filename"));
            String path = DocumentManager.StoragePath(fileName, null);

            // delete file
            File f = new File(path);
            delete(f);

            // delete file history
            File hist = new File(DocumentManager.HistoryDir(path));
            delete(hist);

            writer.write("{ \"success\": true }");
        }
        catch (Exception e)
        {
            writer.write("{ \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // get files information
    private static void Files(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        ArrayList<Map<String, Object>> files = null;

        try {
            Gson gson = new Gson();
            response.setContentType("application/json");

            if (request.getParameter("fileId") == null) {
                files = DocumentManager.GetFilesInfo();  // get the information about the files from the storage path
                writer.write(gson.toJson(files));
            }else {
                String fileId = request.getParameter("fileId");  // get file id from the request
                files = DocumentManager.GetFilesInfo(fileId);
                if(files.isEmpty()) {
                    writer.write("\"File not found\"");
                }else {
                    writer.write(gson.toJson(files));
                }
            }
        }
        catch (Exception e)
        {
            writer.write("{ \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // download a csv file
    private static void CSV(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        String fileName = "assets/sample/csv.csv";
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(fileName);
        Path filePath = null;
        try {
            filePath = Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        download(filePath.toString(), response, writer);
    }

    // get sample files from the assests
    private static void Assets(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        String fileName = "assets/sample/" + FileUtility.GetFileName(request.getParameter("name"));
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(fileName);
        Path filePath = null;
        try {
            filePath = Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        download(filePath.toString(), response, writer);
    }

    // download a file from history
    private static void DownloadHistory(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {   
        try {
            if (DocumentManager.TokenEnabled()) {

                String DocumentJwtHeader = ConfigManager.GetProperty("files.docservice.header");

                String header = (String) request.getHeader(DocumentJwtHeader == null || DocumentJwtHeader.isEmpty() ? "Authorization" : DocumentJwtHeader);
                if (header != null && !header.isEmpty()) {
                    String token = header.startsWith("Bearer ") ? header.substring(7) : header;
                    try {
                        Verifier verifier = HMACVerifier.newVerifier(DocumentManager.GetTokenSecret());
                        JWT jwt = JWT.getDecoder().decode(token, verifier);
                    } catch (Exception e) {
                        response.sendError(403, "JWT validation failed");
                        return;
                    }
                } else {
                    response.sendError(403, "JWT validation failed");
                    return;
                }
            }

            String fileName = FileUtility.GetFileName(request.getParameter("fileName"));
            String userAddress = request.getParameter("userAddress");

            String ver = request.getParameter("ver");  //  Document version
            String file = request.getParameter("file"); //   File. If not defined, then Prev.*
           
            String filePath = DocumentManager.HistoryPath(fileName, userAddress, ver, file);

            download(filePath, response, writer);
        } catch (Exception e) {
            writer.write("{ \"error\": \"File not found\"}");
        }
    }

    // download a file
    private static void Download(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        try {
            String fileName = FileUtility.GetFileName(request.getParameter("fileName"));
            String userAddress = request.getParameter("userAddress");
            String isEmbedded = request.getParameter("dmode");

            if (DocumentManager.TokenEnabled() && isEmbedded == null) {

                String DocumentJwtHeader = ConfigManager.GetProperty("files.docservice.header");

                String header = (String) request.getHeader(DocumentJwtHeader == null || DocumentJwtHeader.isEmpty() ? "Authorization" : DocumentJwtHeader);
                if (header != null && !header.isEmpty()) {
                    String token = header.startsWith("Bearer ") ? header.substring(7) : header;
                    try {
                        Verifier verifier = HMACVerifier.newVerifier(DocumentManager.GetTokenSecret());
                        JWT jwt = JWT.getDecoder().decode(token, verifier);
                    } catch (Exception e) {
                        response.sendError(403, "JWT validation failed");
                        return;
                    }
                }
            }

            String filePath = DocumentManager.ForcesavePath(fileName, userAddress, false);  // get the path to the force saved document version
            if (filePath.equals("")) {
                filePath = DocumentManager.StoragePath(fileName, userAddress);  // or to the original document
            }
            download(filePath, response, writer);
        } catch (Exception e) {
            writer.write("{ \"error\": \"File not found\"}");
        }
    }

    private static void delete(File f) throws Exception {
        // to delete a directory
        if (f.isDirectory()) {
            for (File c : f.listFiles())  // run through all the files in it
            delete(c);  // and delete them
        }
        if (!f.delete())
            throw new Exception("Failed to delete file: " + f);
    }

    // download data from the url to the file
    private static void download(String filePath, HttpServletResponse response, PrintWriter writer) {
        String fileType = null;
        try {
            fileType = Files.probeContentType(Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = new File(filePath);

        // set headers to the response
        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setHeader("Content-Type", fileType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8\'\'" + file.getName());

        BufferedInputStream inputStream = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            inputStream = new BufferedInputStream(fileInputStream);
            int readBytes = 0;
            while ((readBytes = inputStream.read()) != -1)  // write bytes to the output stream
                writer.write(readBytes);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // process get request
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    // process post request
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    // get servlet information
    @Override
    public String getServletInfo()
    {
        return "Handler";
    }
}
