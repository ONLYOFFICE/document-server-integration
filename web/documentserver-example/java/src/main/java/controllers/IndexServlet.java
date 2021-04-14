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

@WebServlet(name = "IndexServlet", urlPatterns = {"/IndexServlet"})
@MultipartConfig
public class IndexServlet extends HttpServlet
{
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String action = request.getParameter("type");

        if (action == null)
        {
            request.getRequestDispatcher("index.jsp").forward(request, response);
            return;
        }

        DocumentManager.Init(request, response);
        PrintWriter writer = response.getWriter();

        switch (action.toLowerCase())
        {
            case "upload":
                Upload(request, response, writer);
                break;
            case "download":
                Download(request, response, writer);
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
        }
    }


    private static void Upload(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        response.setContentType("text/plain");

        try
        {
            Part httpPostedFile = request.getPart("file");

            String fileName = "";
            for (String content : httpPostedFile.getHeader("content-disposition").split(";"))
            {
                if (content.trim().startsWith("filename"))
                {
                    fileName = content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
                }
            }

            long curSize = httpPostedFile.getSize();
            if (DocumentManager.GetMaxFileSize() < curSize || curSize <= 0)
            {
                writer.write("{ \"error\": \"File size is incorrect\"}");
                return;
            }

            String curExt = FileUtility.GetFileExtension(fileName);
            if (!DocumentManager.GetFileExts().contains(curExt))
            {
                writer.write("{ \"error\": \"File type is not supported\"}");
                return;
            }

            InputStream fileStream = httpPostedFile.getInputStream();

            fileName = DocumentManager.GetCorrectName(fileName, null);
            String fileStoragePath = DocumentManager.StoragePath(fileName, null);
            String documentType = FileUtility.GetFileType(fileName).toString().toLowerCase();

            File file = new File(fileStoragePath);

            try (FileOutputStream out = new FileOutputStream(file))
            {
                int read;
                final byte[] bytes = new byte[1024];
                while ((read = fileStream.read(bytes)) != -1)
                {
                    out.write(bytes, 0, read);
                }

                out.flush();
            }

            CookieManager cm = new CookieManager(request);
            DocumentManager.CreateMeta(fileName, cm.getCookie("uid"), cm.getCookie("uname"), null);

            writer.write("{ \"filename\": \"" + fileName + "\", \"documentType\": \"" + documentType + "\" }");

        }
        catch (Exception e)
        {
            writer.write("{ \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private static void Convert(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
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
            String filePass = body.get("filePass") != null ? (String) body.get("filePass") : null;
            String fileUri = DocumentManager.GetFileUri(fileName, true);
            String fileExt = FileUtility.GetFileExtension(fileName);
            FileType fileType = FileUtility.GetFileType(fileName);
            String internalFileExt = DocumentManager.GetInternalExtension(fileType);

            if (DocumentManager.GetConvertExts().contains(fileExt))
            {
                String key = ServiceConverter.GenerateRevisionId(fileUri);

                String newFileUri = ServiceConverter.GetConvertedUri(fileUri, fileExt, internalFileExt, key, filePass, true);

                if (newFileUri.isEmpty())
                {
                    writer.write("{ \"step\" : \"0\", \"filename\" : \"" + fileName + "\"}");
                    return;
                }

                String correctName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + internalFileExt, null);

                URL url = new URL(newFileUri);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();

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
                        out.write(bytes, 0, read);
                    }

                    out.flush();
                }

                connection.disconnect();

                //remove source file ?
                //File sourceFile = new File(DocumentManager.StoragePath(fileName, null));
                //sourceFile.delete();

                fileName = correctName;

                CookieManager cm = new CookieManager(request);
                DocumentManager.CreateMeta(fileName, cm.getCookie("uid"), cm.getCookie("uname"), null);
            }

            writer.write("{ \"filename\" : \"" + fileName + "\"}");

        }
        catch (Exception ex)
        {
            writer.write("{ \"error\": \"" + ex.getMessage() + "\"}");
        }
    }

    private static void Track(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        JSONObject body = null;

        try {
            body = TrackManager.readBody(request, writer);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        int status = Math.toIntExact((long) body.get("status"));
        int saved = 0;

        if (status == 1) { //Editing
            JSONArray actions = (JSONArray) body.get("actions");
            JSONArray users = (JSONArray) body.get("users");
            JSONObject action = (JSONObject) actions.get(0);
            if (actions != null && action.get("type").toString().equals("0")) { //finished edit
                String user = (String) action.get("userid");
                if (users.indexOf(user) == -1) {
                    String key = (String) body.get("key");
                    try {
                        TrackManager.commandRequest("forcesave", key);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        String userAddress = request.getParameter("userAddress");
        String fileName = FileUtility.GetFileName(request.getParameter("fileName"));

        if (status == 2 || status == 3) { //MustSave, Corrupted
            try {
                TrackManager.processSave(body, fileName, userAddress);
            } catch (Exception ex) {
                ex.printStackTrace();
                saved = 1;
            }

        }

        if (status == 6 || status == 7) { //MustForceSave, CorruptedForceSave
            try {
                TrackManager.processForceSave(body, fileName, userAddress);
            } catch (Exception ex) {
                ex.printStackTrace();
                saved = 1;
            }
        }

        writer.write("{\"error\":" + saved + "}");
    }

    private static void Remove(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        try
        {
            String fileName = FileUtility.GetFileName(request.getParameter("filename"));
            String path = DocumentManager.StoragePath(fileName, null);

            File f = new File(path);
            delete(f);

            File hist = new File(DocumentManager.HistoryDir(path));
            delete(hist);

            writer.write("{ \"success\": true }");
        }
        catch (Exception e)
        {
            writer.write("{ \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private static void Files(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        ArrayList<Map<String, Object>> files = null;

        try {
            Gson gson = new Gson();
            response.setContentType("application/json");

            if (request.getParameter("fileId") == null) {
                files = DocumentManager.GetFilesInfo();
                writer.write(gson.toJson(files));
            }else {
                String fileId = request.getParameter("fileId");
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

    private static void Download(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        try {
            String fileName = FileUtility.GetFileName(request.getParameter("name"));
            String filePath = DocumentManager.ForcesavePath(fileName, null, false);
            if (filePath.equals("")) {
                filePath = DocumentManager.StoragePath(fileName, null);
            }
            download(filePath, response, writer);
        } catch (Exception e) {
            writer.write("{ \"error\": \"File not found\"}");
        }
    }

    private static void delete(File f) throws Exception {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
            delete(c);
        }
        if (!f.delete())
            throw new Exception("Failed to delete file: " + f);
    }

    private static void download(String filePath, HttpServletResponse response, PrintWriter writer) {
        String fileType = null;
        try {
            fileType = Files.probeContentType(Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = new File(filePath);

        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setHeader("Content-Type", fileType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8\'\'" + file.getName());

        BufferedInputStream inputStream = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            inputStream = new BufferedInputStream(fileInputStream);
            int readBytes = 0;
            while ((readBytes = inputStream.read()) != -1)
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo()
    {
        return "Handler";
    }
}
