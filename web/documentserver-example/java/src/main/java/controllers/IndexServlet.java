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

package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import entities.FileType;
import entities.User;
import helpers.ConfigManager;
import helpers.CookieManager;
import helpers.DocumentManager;
import helpers.FileUtility;
import helpers.ServiceConverter;
import helpers.TrackManager;
import helpers.Users;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.primeframework.jwt.Verifier;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACVerifier;
import utils.StatusType;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static utils.Constants.KILOBYTE_SIZE;

@WebServlet(name = "IndexServlet", urlPatterns = {"/IndexServlet"})
@MultipartConfig
public class IndexServlet extends HttpServlet {
    protected void processRequest(final HttpServletRequest request,
                                  final HttpServletResponse response) throws ServletException, IOException {
        // get the type parameter from the request
        String action = request.getParameter("type");

        if (action == null) {
            // forward the request and response objects to the index.jsp
            request.getRequestDispatcher("index.jsp").forward(request, response);
            return;
        }

        // charset for response headers if upload or convert
        if (action.matches("upload|convert")) {
           response.setCharacterEncoding("UTF-8");
        }

        DocumentManager.init(request, response);

        // create a variable to display information about the application and error messages
        PrintWriter writer = response.getWriter();

        // define functions for each type of operation
        switch (action.toLowerCase()) {
            case "upload":
                upload(request, response, writer);
                break;
            case "download":
                download(request, response, writer);
                break;
            case "downloadhistory":
                downloadHistory(request, response, writer);
                break;
            case "convert":
                convert(request, response, writer);
                break;
            case "track":
                track(request, response, writer);
                break;
            case "remove":
                remove(request, response, writer);
                break;
            case "assets":
                assets(request, response, writer);
                break;
            case "csv":
                csv(request, response, writer);
                break;
            case "files":
                files(request, response, writer);
                break;
            case "saveas":
                saveAs(request, response, writer);
                break;
            case "rename":
                rename(request, response, writer);
                break;
            case "reference":
                reference(request, response, writer);
                break;
            case "restore":
                restore(request, response, writer);
                break;
            default:
                break;
        }
    }

    private static void saveAs(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final PrintWriter writer) {
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
            int filesizeMax = Integer.parseInt(ConfigManager.getProperty("filesize-max"));

            URL url = new URL(saveAsFileUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            InputStream stream = connection.getInputStream();

            if (filesizeMax < stream.available() || stream.available() <= 0) {
                writer.write("{\"error\":\"File size is incorrect\"}");
            }

            String fileName = DocumentManager.getCorrectName(title, null);
            DocumentManager.createFile(Paths.get(DocumentManager.storagePath(fileName, null)), stream);

            DocumentManager.createMeta(fileName, user.getId(), user.getName(), null);

            writer.write("{\"file\":  \"" + fileName + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            writer.write("{ \"error\" : 1, \"message\" : \"" + e.getMessage() + "\"}");
        }
    }


    // upload a file
    private static void upload(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final PrintWriter writer) {
        response.setContentType("text/plain");

        try {
            Part httpPostedFile = request.getPart("file");

            // get file name from the content-disposition response header
            String fileName = "";
            for (String content : httpPostedFile.getHeader("content-disposition").split(";")) {
                if (content.trim().startsWith("filename")) {
                    fileName = content
                            .substring(content.indexOf('=') + 1)
                            .trim()
                            .replace("\"", "");
                }
            }

            long curSize = httpPostedFile.getSize();  // get file size

            // check if the file size exceeds the maximum file size or is less than 0
            if (DocumentManager.getMaxFileSize() < curSize || curSize <= 0) {

                // if so, write the error status and message to the response
                writer.write("{ \"error\": \"File size is incorrect\"}");
                return;
            }

            String curExt = FileUtility.getFileExtension(fileName);  // get current file extension

            // check if this extension is supported by the editor
            if (!DocumentManager.getFileExts().contains(curExt)) {

                // if not, write the error status and message to the response
                writer.write("{ \"error\": \"File type is not supported\"}");
                return;
            }

            // get input file stream
            InputStream fileStream = httpPostedFile.getInputStream();

            // get a file name with an index if the file with such a name already exists
            fileName = DocumentManager.getCorrectName(fileName, null);

            // get the storage path of the file
            String fileStoragePath = DocumentManager.storagePath(fileName, null);
            String documentType = FileUtility.getFileType(fileName).toString().toLowerCase();

            File file = new File(fileStoragePath);

            try (FileOutputStream out = new FileOutputStream(file)) {
                int read;
                final byte[] bytes = new byte[KILOBYTE_SIZE];
                while ((read = fileStream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);  // write bytes to the output stream
                }

                // force write data to the output stream that can be cached in the current thread
                out.flush();
            }

            // create meta information with the user id and name specified
            CookieManager cm = new CookieManager(request);
            User user = Users.getUser(cm.getCookie("uid"));

            DocumentManager.createMeta(fileName, user.getId(), user.getName(), null);

            writer.write("{ \"filename\": \"" + fileName + "\", \"documentType\": \"" + documentType + "\" }");

        } catch (Exception e) {
            writer.write("{ \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // convert a file
    private static void convert(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final PrintWriter writer) throws UnsupportedEncodingException {
        CookieManager cm = new CookieManager(request);
        response.setContentType("text/plain");

        try {
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            String bodyString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(bodyString);

            String fileName = FileUtility.getFileName((String) body.get("filename"));
            String lang = cm.getCookie("ulang");
            String filePass = body.get("filePass") != null ? (String) body.get("filePass") : null;
            String fileUri = DocumentManager.getDownloadUrl(fileName, true);
            String fileExt = FileUtility.getFileExtension(fileName);
            FileType fileType = FileUtility.getFileType(fileName);
            String internalFileExt = "ooxml";

            // check if the file with such an extension can be converted
            if (DocumentManager.getConvertExts().contains(fileExt)) {
                // generate document key
                String key = ServiceConverter.generateRevisionId(fileUri);

                // get the url and file type to the converted file
                Map<String, String> newFileData = ServiceConverter
                        .getConvertedData(fileUri, fileExt, internalFileExt, key, filePass, true, lang);
                String newFileUri = newFileData.get("fileUrl");
                String newFileType = "." + newFileData.get("fileType");

                if (newFileUri.isEmpty()) {
                    writer.write("{ \"step\" : \"0\", \"filename\" : \"" + fileName + "\"}");
                    return;
                }

                /* get a file name of an internal file extension with an index if the file
                 with such a name already exists */
                String correctName = DocumentManager.getCorrectName(FileUtility
                                .getFileNameWithoutExtension(fileName) + newFileType, null);

                URL url = new URL(newFileUri);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();  // get input stream of the converted file

                if (stream == null) {
                    throw new Exception("Stream is null");
                }

                File convertedFile = new File(DocumentManager.storagePath(correctName, null));
                try (FileOutputStream out = new FileOutputStream(convertedFile)) {
                    int read;
                    final byte[] bytes = new byte[KILOBYTE_SIZE];
                    while ((read = stream.read(bytes)) != -1) {
                        out.write(bytes, 0, read);  // write bytes to the output stream
                    }

                    // force write data to the output stream that can be cached in the current thread
                    out.flush();
                }

                connection.disconnect();

                // remove source file
                File sourceFile = new File(DocumentManager.storagePath(fileName, null));
                sourceFile.delete();

                fileName = correctName;

                // create meta information about the converted file with the user id and name specified
                User user = Users.getUser(cm.getCookie("uid"));

                DocumentManager.createMeta(fileName, user.getId(), user.getName(), null);
            }

            writer.write("{ \"filename\" : \"" + fileName + "\"}");

        } catch (Exception ex) {
            writer.write("{ \"error\": \"" + ex.getMessage() + "\"}");
        }
    }

    // track file changes
    private static void track(final HttpServletRequest request,
                              final HttpServletResponse response,
                              final PrintWriter writer) {
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

        if (status == StatusType.EDITING.getCode()) { // editing
            JSONArray actions = (JSONArray) body.get("actions");
            JSONArray users = (JSONArray) body.get("users");
            JSONObject action = (JSONObject) actions.get(0);
            if (actions != null && action.get("type").toString().equals("0")) { // finished edit
                String user = (String) action.get("userid");  // the user who finished editing
                if (users.indexOf(user) == -1) {
                    String key = (String) body.get("key");
                    try {

                        // create a command request with the forcesave method
                        TrackManager.commandRequest("forcesave", key, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        String userAddress = request.getParameter("userAddress");
        String fileName = FileUtility.getFileName(request.getParameter("fileName"));

        // MustSave, Corrupted
        if (status == StatusType.MUST_SAVE.getCode() || status == StatusType.CORRUPTED.getCode()) {
            try {
                TrackManager.processSave(body, fileName, userAddress);
            } catch (Exception ex) {
                ex.printStackTrace();
                saved = 1;
            }

        }

        // MustForceSave, CorruptedForceSave
        if (status == StatusType.MUST_FORCE_SAVE.getCode() || status == StatusType.CORRUPTED_FORCE_SAVE.getCode()) {
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
    private static void remove(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final PrintWriter writer) {
        try {
            String fileName = FileUtility.getFileName(request.getParameter("filename"));
            String path = DocumentManager.storagePath(fileName, null);

            // delete file
            File f = new File(path);
            delete(f);

            // delete file history
            File hist = new File(DocumentManager.historyDir(path));
            delete(hist);

            writer.write("{ \"success\": true }");
        } catch (Exception e) {
            writer.write("{ \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // get files information
    private static void files(final HttpServletRequest request,
                              final HttpServletResponse response,
                              final PrintWriter writer) {
        ArrayList<Map<String, Object>> files = null;

        try {
            Gson gson = new Gson();
            response.setContentType("application/json");

            if (request.getParameter("fileId") == null) {
                files = DocumentManager.getFilesInfo();  // get the information about the files from the storage path
                writer.write(gson.toJson(files));
            } else {
                String fileId = request.getParameter("fileId");  // get file id from the request
                files = DocumentManager.getFilesInfo(fileId);
                if (files.isEmpty()) {
                    writer.write("\"File not found\"");
                } else {
                    writer.write(gson.toJson(files));
                }
            }
        } catch (Exception e) {
            writer.write("{ \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // download a csv file
    private static void csv(final HttpServletRequest request,
                            final HttpServletResponse response,
                            final PrintWriter writer) {
        String fileName = "assets/document-templates/sample/csv.csv";
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
    private static void assets(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final PrintWriter writer) {
        String fileName = "assets/document-templates/sample/" + FileUtility.getFileName(request.getParameter("name"));
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
    private static void downloadHistory(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final PrintWriter writer) {
        try {
            if (DocumentManager.tokenEnabled() && DocumentManager.tokenUseForRequest()) {

                String documentJwtHeader = ConfigManager.getProperty("files.docservice.header");

                String header = (String) request.getHeader(documentJwtHeader == null || documentJwtHeader.isEmpty()
                        ? "Authorization" : documentJwtHeader);
                if (header != null && !header.isEmpty()) {
                    String bearerPrefix = "Bearer ";
                    String token = header.startsWith(bearerPrefix) ? header.substring(bearerPrefix.length()) : header;
                    try {
                        Verifier verifier = HMACVerifier.newVerifier(DocumentManager.getTokenSecret());
                        JWT jwt = JWT.getDecoder().decode(token, verifier);
                    } catch (Exception e) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "JWT validation failed");
                        return;
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "JWT validation failed");
                    return;
                }
            }

            String fileName = FileUtility.getFileName(request.getParameter("fileName"));
            String userAddress = request.getParameter("userAddress");

            String ver = request.getParameter("ver");  //  Document version
            String file = request.getParameter("file"); //   File. If not defined, then Prev.*

            String filePath = DocumentManager.historyPath(fileName, userAddress, ver, file);

            download(filePath, response, writer);
        } catch (Exception e) {
            writer.write("{ \"error\": \"File not found\"}");
        }
    }

    // download a file
    private static void download(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final PrintWriter writer) {
        try {
            String fileName = FileUtility.getFileName(request.getParameter("fileName"));
            String userAddress = request.getParameter("userAddress");
            String isEmbedded = request.getParameter("dmode");

            if (DocumentManager.tokenEnabled() && isEmbedded == null && userAddress != null
                    && DocumentManager.tokenUseForRequest()) {

                String documentJwtHeader = ConfigManager.getProperty("files.docservice.header");

                String header = (String) request.getHeader(documentJwtHeader == null || documentJwtHeader.isEmpty()
                        ? "Authorization" : documentJwtHeader);
                String token = "";
                if (header != null && !header.isEmpty()) {
                    String bearerPrefix = "Bearer ";
                    token = header.startsWith(bearerPrefix) ? header.substring(bearerPrefix.length()) : header;
                }
                try {
                    Verifier verifier = HMACVerifier.newVerifier(DocumentManager.getTokenSecret());
                    JWT jwt = JWT.getDecoder().decode(token, verifier);
                } catch (Exception e) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "JWT validation failed");
                    return;
                }
            }

            // get the path to the force saved document version
            String filePath = DocumentManager.forcesavePath(fileName, userAddress, false);
            if (filePath.equals("")) {
                filePath = DocumentManager.storagePath(fileName, userAddress);  // or to the original document
            }
            download(filePath, response, writer);
        } catch (Exception e) {
            writer.write("{ \"error\": \"File not found\"}");
        }
    }

    private static void delete(final File f) throws Exception {
        // to delete a directory
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {  // run through all the files in it
                delete(c);  // and delete them
            }
        }
        if (!f.delete()) {
            throw new Exception("Failed to delete file: " + f);
        }
    }

    // download data from the url to the file
    private static void download(final String filePath, final HttpServletResponse response, final PrintWriter writer) {
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
            while ((readBytes = inputStream.read()) != -1) {  // write bytes to the output stream
                writer.write(readBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // rename a file
    private static void rename(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final PrintWriter writer) {
        try {
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            String bodyString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(bodyString);

            String newfilename = (String) body.get("newfilename");
            String dockey = (String) body.get("dockey");

            String origExt = "." + (String) body.get("ext");
            String curExt = newfilename;

            if (newfilename.indexOf(".") != -1) {
                curExt = (String) FileUtility.getFileExtension(newfilename);
            }

            if (origExt.compareTo(curExt) != 0) {
                newfilename += origExt;
            }

            HashMap<String, String> meta = new HashMap<>();
            meta.put("title", newfilename);

            TrackManager.commandRequest("meta", dockey, meta);

        } catch (Exception e) {
            e.printStackTrace();
            writer.write("{ \"error\" : 1, \"message\" : \"" + e.getMessage() + "\"}");
        }
    }

    // reference data
    private static void reference(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final PrintWriter writer) {
        try {
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            String bodyString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            String fileKeyValue = "";
            String userAddress = "";
            String fileName = "";
            boolean incorrectFileKey = false;

            JSONParser parser = new JSONParser();
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            JSONObject body = (JSONObject) parser.parse(bodyString);

            if (body.containsKey("referenceData")) {
                JSONObject referenceDataObj = (JSONObject) body.get("referenceData");
                String instanceId = (String) referenceDataObj.get("instanceId");

                if (instanceId.equals(DocumentManager.getServerUrl(false))) {
                    try {
                        JSONObject fileKey = (JSONObject) parser.parse((String) referenceDataObj.get("fileKey"));
                        userAddress = (String) fileKey.get("userAddress");

                        if (userAddress.equals(DocumentManager.curUserHostAddress(null))) {
                            fileName = (String) fileKey.get("fileName");
                        }
                    } catch (Exception e) {
                        incorrectFileKey = true; //data from DocEditor can give incorrect fileKey param in java Example
                    }
                }
            }

            if (fileName.equals("")) {
                try {
                    String path = (String) body.get("path");
                    path = FileUtility.getFileName(path);
                    File f = new File(DocumentManager.storagePath(path, null));
                    if (f.exists()) {
                        fileName = path;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    writer.write("{ \"error\" : 1, \"message\" : \"" + e.getMessage() + "\"}");
                }
            }

            if (fileName.equals("")) {
                writer.write("{ \"error\": \"File not found\"}");
                return;
            }

            boolean directUrl = (boolean) body.get("directUrl");

            HashMap<String, Object> fileKey = new HashMap<>();
            fileKey.put("fileName", fileName);
            fileKey.put("userAddress", DocumentManager.curUserHostAddress(null));

            HashMap<String, Object> referenceData = new HashMap<>();
            referenceData.put("instanceId", DocumentManager.getServerUrl(false));
            referenceData.put("fileKey", gson.toJson(fileKey));

            HashMap<String, Object> data = new HashMap<>();
            data.put("fileType", FileUtility.getFileExtension(fileName).replace(".", ""));
            data.put("url", DocumentManager.getDownloadUrl(fileName, true));
            data.put("directUrl", directUrl ? DocumentManager.getDownloadUrl(fileName, false) : null);
            data.put("referenceData", referenceData);
            data.put("path", fileName);

            if (DocumentManager.tokenEnabled()) {
                String token = DocumentManager.createToken(data);
                data.put("token", token);
            }
            writer.write(gson.toJson(data));
        } catch (Exception e) {
            e.printStackTrace();
            writer.write("{ \"error\" : 1, \"message\" : \"" + e.getMessage() + "\"}");
        }
    }

    private static void restore(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final PrintWriter writer) {
        try {
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            String bodyString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(bodyString);

            String sourceBasename = (String) body.get("fileName");
            Integer version = ((Long) body.get("version")).intValue();
            String userID = (String) body.get("userId");

            String sourceStringFile = DocumentManager.storagePath(sourceBasename, null);
            File sourceFile = new File(sourceStringFile);
            Path sourcePathFile = sourceFile.toPath();
            String historyDirectory = DocumentManager.historyDir(sourceStringFile);

            Integer bumpedVersion = DocumentManager.getFileVersion(historyDirectory);
            String bumpedVersionStringDirectory = DocumentManager.versionDir(historyDirectory, bumpedVersion);
            File bumpedVersionDirectory = new File(bumpedVersionStringDirectory);
            if (!bumpedVersionDirectory.exists()) {
                bumpedVersionDirectory.mkdir();
            }

            Path bumpedKeyPathFile = Paths.get(bumpedVersionStringDirectory, "key.txt");
            String bumpedKeyStringFile = bumpedKeyPathFile.toString();
            File bumpedKeyFile = new File(bumpedKeyStringFile);
            String bumpedKey = ServiceConverter.generateRevisionId(
                DocumentManager.curUserHostAddress(null)
                + "/"
                + sourceBasename
                + "/"
                + Long.toString(sourceFile.lastModified())
            );
            FileWriter bumpedKeyFileWriter = new FileWriter(bumpedKeyFile);
            bumpedKeyFileWriter.write(bumpedKey);
            bumpedKeyFileWriter.close();

            User user = Users.getUser(userID);

            Path bumpedChangesPathFile = Paths.get(bumpedVersionStringDirectory, "changes.json");
            String bumpedChangesStringFile = bumpedChangesPathFile.toString();
            File bumpedChangesFile = new File(bumpedChangesStringFile);
            JSONObject bumpedChangesUser = new JSONObject();
            bumpedChangesUser.put("id", user.getId());
            bumpedChangesUser.put("name", user.getName());
            JSONObject bumpedChangesChangesItem = new JSONObject();
            bumpedChangesChangesItem.put("created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            bumpedChangesChangesItem.put("user", bumpedChangesUser);
            JSONArray bumpedChangesChanges = new JSONArray();
            bumpedChangesChanges.add(bumpedChangesChangesItem);
            JSONObject bumpedChanges = new JSONObject();
            bumpedChanges.put("serverVersion", null);
            bumpedChanges.put("changes", bumpedChangesChanges);
            String bumpedChangesContent = bumpedChanges.toJSONString();
            FileWriter bumpedChangesFileWriter = new FileWriter(bumpedChangesFile);
            bumpedChangesFileWriter.write(bumpedChangesContent);
            bumpedChangesFileWriter.close();

            String sourceExtension = FileUtility.getFileExtension(sourceBasename);
            String previousBasename = "prev" + sourceExtension;

            Path bumpedFile = Paths.get(bumpedVersionStringDirectory, previousBasename);
            Files.move(sourcePathFile, bumpedFile);

            String recoveryVersionStringDirectory = DocumentManager.versionDir(historyDirectory, version);
            Path recoveryPathFile = Paths.get(recoveryVersionStringDirectory, previousBasename);
            String recoveryStringFile = recoveryPathFile.toString();
            FileInputStream recoveryStream = new FileInputStream(recoveryStringFile);
            DocumentManager.createFile(sourcePathFile, recoveryStream);
            recoveryStream.close();

            JSONObject responseBody = new JSONObject();
            responseBody.put("error", null);
            responseBody.put("success", true);
            String responseContent = responseBody.toJSONString();
            writer.write(responseContent);
        } catch (Exception error) {
            error.printStackTrace();
            JSONObject responseBody = new JSONObject();
            responseBody.put("error", error.getMessage());
            responseBody.put("success", false);
            String responseContent = responseBody.toJSONString();
            writer.write(responseContent);
        }
    }

    // process get request
    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    // process post request
    @Override
    protected void doPost(final HttpServletRequest request,
                          final HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPut(final HttpServletRequest request,
                          final HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    // get servlet information
    @Override
    public String getServletInfo() {
        return "Handler";
    }
}
