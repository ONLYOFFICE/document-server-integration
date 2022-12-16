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

package helpers;

import com.google.gson.Gson;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.primeframework.jwt.domain.JWT;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class TrackManager {
    private static final String DocumentJwtHeader = ConfigManager.GetProperty("files.docservice.header");

    // create a new file if it does not exist
    private static boolean createFile(byte[] byteArray, Path path) {
        if (Files.exists(path)) {
            return true;
        }
        try(ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray)) {
            File file = Files.createFile(path).toFile();  // create a new file in the specified path
            try (FileOutputStream out = new FileOutputStream(file)) {
                int read;
                final byte[] bytes = new byte[1024];
                while ((read = byteArrayInputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);  // write bytes to the output stream
                }
                out.flush();  // force write data to the output stream that can be cached in the current thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // get byte array from stream
    private static byte[] getAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    // save file
    private static boolean saveFile(byte[] byteArray, Path path) {
        if (path == null) throw new RuntimeException("Path argument is not specified");  // file isn't specified
        if (!Files.exists(path)) { // if the specified file does not exist
            return createFile(byteArray, path);  // create it in the specified directory
        } else {
            try {
                Files.write(path, byteArray);  // otherwise, write new information in the bytes format to the file
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // download file from url
    private static byte[] getDownloadFile(String url) throws Exception {
        if (url == null || url.isEmpty())
            throw new RuntimeException("Url argument is not specified");  // URL isn't specified

        URL uri = new URL(url);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) uri.openConnection();
        connection.setConnectTimeout(5000);
        InputStream stream = connection.getInputStream();  // get input stream of the file information from the URL

        int statusCode = connection.getResponseCode();

        if (statusCode != 200) {  // checking status code
            connection.disconnect();
            throw new RuntimeException("Document editing service returned status: " + statusCode);
        }

        if (stream == null) {
            connection.disconnect();
            throw new RuntimeException("Input stream is null");
        }

        return getAllBytes(stream);
    }

    // read request body
    public static JSONObject readBody(HttpServletRequest request, PrintWriter writer) throws Exception {
        String bodyString = "";

        try {
            // read request body by streams
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            bodyString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
        } catch (Exception ex) {
            writer.write("get request.getInputStream error:" + ex.getMessage());
            throw ex;
        }

        // error when the bodyString object is empty
        if (bodyString.isEmpty()) {
            writer.write("empty request.getInputStream");
            throw new Exception("empty request.getInputStream");
        }

        JSONParser parser = new JSONParser();
        JSONObject body;

        try {
            Object obj = parser.parse(bodyString);  // parse bodyString object
            body = (JSONObject) obj;
        } catch (Exception ex) {
            writer.write("JSONParser.parse error:" + ex.getMessage());
            throw ex;
        }

        // if the secret key to generate token exists
        if (DocumentManager.TokenEnabled()) {
            String token = (String) body.get("token");  // get the document token

            if (token == null) {  // if JSON web token is not received
                String header = (String) request.getHeader(DocumentJwtHeader == null || DocumentJwtHeader.isEmpty() ? "Authorization" : DocumentJwtHeader);  // get it from the Authorization header
                if (header != null && !header.isEmpty()) {
                    token = header.startsWith("Bearer ") ? header.substring(7) : header;  // and save it without Authorization prefix
                }
            }

            if (token == null || token.isEmpty()) {  // if the token is not received
                writer.write("{\"error\":1,\"message\":\"JWT expected\"}");  // an error occurs
                throw new Exception("{\"error\":1,\"message\":\"JWT expected\"}");
            }

            JWT jwt = DocumentManager.ReadToken(token);  // read token
            if (jwt == null) {
                writer.write("{\"error\":1,\"message\":\"JWT validation failed\"}");  // an error occurs
                throw new Exception("{\"error\":1,\"message\":\"JWT validation failed\"}");
            }

            if (jwt.getObject("payload") != null) {  // get the payload object from the request body
                try {
                    @SuppressWarnings("unchecked") LinkedHashMap<String, Object> payload =
                            (LinkedHashMap<String, Object>) jwt.getObject("payload");

                    jwt.claims = payload;
                } catch (Exception ex) {
                    writer.write("{\"error\":1,\"message\":\"Wrong payload\"}");
                    throw ex;
                }
            }

            try {
                Gson gson = new Gson();
                Object obj = parser.parse(gson.toJson(jwt.claims));
                body = (JSONObject) obj;
            } catch (Exception ex) {
                writer.write("JSONParser.parse error:" + ex.getMessage());
                throw ex;
            }
        }

        return body;
    }

    // file saving process
    public static int processSave(JSONObject body, String fileName, String userAddress) throws Exception {
        if (body.get("url") == null) {
            throw new Exception("DownloadUrl is null");
        }
        String downloadUri = (String) body.get("url");
        String changesUri = (String) body.get("changesurl");
        String key = (String) body.get("key");
        String newFileName = fileName;

        String curExt = FileUtility.GetFileExtension(fileName);  // get current file extension
        String downloadExt = "." + (String) body.get("filetype");  // get the extension of the downloaded file

        // Todo [Delete in version 7.0 or higher]
        if (downloadExt == "." + null)
            downloadExt = FileUtility.GetFileExtension(downloadUri); // Support for versions below 7.0

        // convert downloaded file to the file with the current extension if these extensions aren't equal
        if (!curExt.equals(downloadExt)) {
            try {
                String newFileUri = ServiceConverter.GetConvertedUri(downloadUri, downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), null, false, null);  // convert file and get url to a new file
                if (newFileUri.isEmpty()) {
                    newFileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);  // get the correct file name if it already exists
                } else {
                    downloadUri = newFileUri;
                }
            } catch (Exception e) {
                newFileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
            }
        }

        byte[] byteArrayFile = getDownloadFile(downloadUri);// download document file
        String storagePath = DocumentManager.StoragePath(newFileName, userAddress);  // get the file path
        Path toSave = Paths.get(storagePath);

        Path lastVersion = Paths.get(DocumentManager.StoragePath(fileName, userAddress));  // get the path to the last file version
        if (lastVersion.toFile().exists()) {  // if the last file version exists
            Path histDir = Paths.get(DocumentManager.HistoryDir(storagePath));  // get the path to the history direction
            if (!Files.exists(histDir)) Files.createDirectories(histDir);  // if the path doesn't exist, create it
            String versionDir = DocumentManager.VersionDir(histDir.toAbsolutePath().toString(),
                    DocumentManager.GetFileVersion(histDir.toAbsolutePath().toString()));  // get the path to the file version

            Path ver = Paths.get(versionDir);

            if (!Files.exists(ver)) Files.createDirectories(ver);
            Files.move(lastVersion, Paths.get(versionDir + File.separator + "prev" + curExt)); // move the latest file version to the previous file version

            saveFile(byteArrayFile, toSave); // save document file

            byte[] byteArrayChanges = getDownloadFile(changesUri);
            saveFile(byteArrayChanges, Paths.get(versionDir + File.separator + "diff.zip"));

            String history = (String) body.get("changeshistory");
            if (history == null && body.containsKey("history")) {
                history = ((JSONObject) body.get("history")).toJSONString();
            }
            if (history != null && !history.isEmpty()) {
                FileWriter fw = new FileWriter(new File(versionDir + File.separator + "changes.json"));  // write the history changes to the changes.json file
                fw.write(history);
                fw.close();
            }

            FileWriter fw = new FileWriter(new File(versionDir + File.separator + "key.txt"));  // write the key value to the key.txt file
            fw.write(key);
            fw.close();

            String forcesavePath = DocumentManager.ForcesavePath(newFileName, userAddress, false);  // get the path to the forcesaved file version
            if (!forcesavePath.equals("")) {  // if the forcesaved file version exists
                File forceSaveFile = new File(forcesavePath);
                forceSaveFile.delete();  // remove it
            }
        }

        return 0;
    }

    // file force saving process
    public static int processForceSave(JSONObject body, String fileName, String userAddress) throws Exception {
        if (body.get("url") == null) {
            throw new Exception("DownloadUrl is null");
        }
        String downloadUri = (String) body.get("url");
        String curExt = FileUtility.GetFileExtension(fileName);  // get current file extension
        String downloadExt = "." + (String) body.get("filetype");  // get the extension of the downloaded file

        // Todo [Delete in version 7.0 or higher]
        if (downloadExt == "." + null)
            downloadExt = FileUtility.GetFileExtension(downloadUri);    // Support for versions below 7.0

        Boolean newFileName = false;

        // convert downloaded file to the file with the current extension if these extensions aren't equal
        if (!curExt.equals(downloadExt)) {
            try {
                String newFileUri = ServiceConverter.GetConvertedUri(downloadUri, downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), null, false, null);  // convert file and get url to a new file
                if (newFileUri.isEmpty()) {
                    newFileName = true;
                } else {
                    downloadUri = newFileUri;
                }
            } catch (Exception e) {
                newFileName = true;
            }
        }

        byte[] byteArrayFile = getDownloadFile(downloadUri);// download document file
        String forcesavePath = "";
        boolean isSubmitForm = body.get("forcesavetype").toString().equals("3");  // SubmitForm

        if (isSubmitForm) {  // if the form is submitted
            // new file
            if (newFileName) {
                fileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + "-form" + downloadExt, userAddress);  // get the correct file name if it already exists
            } else {
                fileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + "-form" + curExt, userAddress);
            }
            forcesavePath = DocumentManager.StoragePath(fileName, userAddress);
        } else {
            if (newFileName) {
                fileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
            }

            // create forcesave path if it doesn't exist
            forcesavePath = DocumentManager.ForcesavePath(fileName, userAddress, false);
            if (forcesavePath == "") {
                forcesavePath = DocumentManager.ForcesavePath(fileName, userAddress, true);
            }
        }

        saveFile(byteArrayFile, Paths.get(forcesavePath));

        if (isSubmitForm) {
            JSONArray actions = (JSONArray) body.get("actions");
            JSONObject action = (JSONObject) actions.get(0);
            String user = (String) action.get("userid");  // get the user id
            DocumentManager.CreateMeta(fileName, user, "Filling Form", userAddress);  // create meta data for forcesaved file
        }

        return 0;
    }

    // create a command request
    public static void commandRequest(String method, String key, HashMap meta) throws Exception {
        String DocumentCommandUrl = ConfigManager.GetProperty("files.docservice.url.site") + ConfigManager.GetProperty("files.docservice.url.command");

        URL url = new URL(DocumentCommandUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("c", method);
        params.put("key", key);

        if (meta != null) {
            params.put("meta", meta);
        }

        String headerToken = "";
        if (DocumentManager.TokenEnabled())  // check if a secret key to generate token exists or not
        {
            Map<String, Object> payloadMap = new HashMap<String, Object>();
            payloadMap.put("payload", params);
            headerToken = DocumentManager.CreateToken(payloadMap);  // encode a payload object into a header token

            // add a header Authorization with a header token and Authorization prefix in it
            connection.setRequestProperty(DocumentJwtHeader.equals("") ? "Authorization" : DocumentJwtHeader, "Bearer " + headerToken);

            String token = DocumentManager.CreateToken(params);  // encode a payload object into a body token
            params.put("token", token);
        }

        Gson gson = new Gson();
        String bodyString = gson.toJson(params);

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);

        connection.setRequestMethod("POST");  // set the request method
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");  // set the Content-Type header
        connection.setDoOutput(true); // set the doOutput field to true

        connection.connect();
        try (OutputStream os = connection.getOutputStream()) {
            os.write(bodyByte);  // write bytes to the output stream
        }
        InputStream stream = connection.getInputStream();  // get input stream

        if (stream == null)
            throw new Exception("Could not get an answer");

        String jsonString = ServiceConverter.ConvertStreamToString(stream);  // convert stream to json string
        connection.disconnect();

        JSONObject response = ServiceConverter.ConvertStringToJSON(jsonString);  // convert json string to json object
        if (!response.get("error").toString().equals("0")) {
            throw new Exception(response.toJSONString());
        }
    }
}
