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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class TrackManager {
    private static final String DocumentJwtHeader = ConfigManager.GetProperty("files.docservice.header");

    // read request body
    public static JSONObject readBody(HttpServletRequest request, PrintWriter writer) throws Exception {
        String bodyString = "";

        try {
            // read request body by streams
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            bodyString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
        }
        catch (Exception ex) {
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
                            (LinkedHashMap<String, Object>)jwt.getObject("payload");

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
    public static void processSave(JSONObject body, String fileName, String userAddress) throws Exception {
        String downloadUri = (String) body.get("url");
        String changesUri = (String) body.get("changesurl");
        String key = (String) body.get("key");
        String newFileName = fileName;

        String curExt = FileUtility.GetFileExtension(fileName);  // get current file extension
        String downloadExt = FileUtility.GetFileExtension(downloadUri);  // get the extension of the downloaded file

        // convert downloaded file to the file with the current extension if these extensions aren't equal
        if (!curExt.equals(downloadExt)) {
            try {
                String newFileUri = ServiceConverter.GetConvertedUri(downloadUri, downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), null, false);  // convert file and get url to a new file
                if (newFileUri.isEmpty()) {
                    newFileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);  // get the correct file name if it already exists
                } else {
                    downloadUri = newFileUri;
                }
            } catch (Exception e){
                newFileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
            }
        }

        String storagePath = DocumentManager.StoragePath(newFileName, userAddress);  // get the file path
        File histDir = new File(DocumentManager.HistoryDir(storagePath));  // get the path to the history direction
        if (!histDir.exists()) histDir.mkdirs();  // if the path doesn't exist, create it

        String versionDir = DocumentManager.VersionDir(histDir.getAbsolutePath(), DocumentManager.GetFileVersion(histDir.getAbsolutePath()));  // get the path to the file version
        File ver = new File(versionDir);
        File lastVersion = new File(DocumentManager.StoragePath(fileName, userAddress));
        File toSave = new File(storagePath);

        if (!ver.exists()) ver.mkdirs();

        lastVersion.renameTo(new File(versionDir + File.separator + "prev" + curExt));  // get the path to the previous file version and rename the last file version with it

        downloadToFile(downloadUri, toSave);  // save file to the storage path 
        downloadToFile(changesUri, new File(versionDir + File.separator + "diff.zip"));  // save file changes to the diff.zip archive

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

    // file force saving process
    public static void processForceSave(JSONObject body, String fileName, String userAddress) throws Exception {

        String downloadUri = (String) body.get("url");

        String curExt = FileUtility.GetFileExtension(fileName);  // get current file extension
        String downloadExt = FileUtility.GetFileExtension(downloadUri);  // get the extension of the downloaded file
        Boolean newFileName = false;

        // convert downloaded file to the file with the current extension if these extensions aren't equal
        if (!curExt.equals(downloadExt)) {
            try {
                String newFileUri = ServiceConverter.GetConvertedUri(downloadUri, downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), null, false);  // convert file and get url to a new file
                if (newFileUri.isEmpty()) {
                    newFileName = true;
                } else {
                    downloadUri = newFileUri;
                }
            } catch (Exception e){
                newFileName = true;
            }
        }

        String forcesavePath = "";
        boolean isSubmitForm = body.get("forcesavetype").toString().equals("3");  // SubmitForm

        if (isSubmitForm) {  // if the form is submitted
            // new file
            if (newFileName){
                fileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + "-form" + downloadExt, userAddress);  // get the correct file name if it already exists
            } else {
                fileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + "-form" + curExt, userAddress);
            }
            forcesavePath = DocumentManager.StoragePath(fileName, userAddress);
        } else {
            if (newFileName){
                fileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
            }

            // create forcesave path if it doesn't exist
            forcesavePath = DocumentManager.ForcesavePath(newFileName, userAddress, false);
            if (forcesavePath == "") {
                forcesavePath = DocumentManager.ForcesavePath(fileName, userAddress, true);
            }
        }

        File toSave = new File(forcesavePath);
        downloadToFile(downloadUri, toSave);

        if (isSubmitForm) {
            JSONArray actions = (JSONArray) body.get("actions");
            JSONObject action = (JSONObject) actions.get(0);
            String user = (String) action.get("userid");  // get the user id
            DocumentManager.CreateMeta(fileName, user, "Filling Form", userAddress);  // create meta data for forcesaved file
        }
    }

    // save file information from the url to the file specified
    private static void downloadToFile(String url, File file) throws Exception {
        if (url == null || url.isEmpty()) throw new Exception("argument url");  // url isn't specified
        if (file == null) throw new Exception("argument path");  // file isn't specified

        URL uri = new URL(url);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) uri.openConnection();
        InputStream stream = connection.getInputStream();  // get input stream of the file information from the url

        if (stream == null)
        {
            throw new Exception("Stream is null");
        }

        try (FileOutputStream out = new FileOutputStream(file))
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
    }

    // create a command request
    public static void commandRequest(String method, String key) throws Exception {
        String DocumentCommandUrl = ConfigManager.GetProperty("files.docservice.url.site") + ConfigManager.GetProperty("files.docservice.url.command");

        URL url = new URL(DocumentCommandUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("c", method);
        params.put("key", key);

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
        InputStream stream = connection.getInputStream();;  // get input stream

        if (stream == null)
            throw new Exception("Could not get an answer");

        String jsonString = ServiceConverter.ConvertStreamToString(stream);  // convert stream to json string
        connection.disconnect();

        JSONObject response = ServiceConverter.ConvertStringToJSON(jsonString);  // convert json string to json object
        if (!response.get("error").toString().equals("0")){
            throw new Exception(response.toJSONString());
        }
    }
}
