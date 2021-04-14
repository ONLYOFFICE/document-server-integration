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

    public static JSONObject readBody(HttpServletRequest request, PrintWriter writer) throws Exception {
        String bodyString = "";

        try {
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            bodyString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
        }
        catch (Exception ex) {
            writer.write("get request.getInputStream error:" + ex.getMessage());
            throw ex;
        }

        if (bodyString.isEmpty()) {
            writer.write("empty request.getInputStream");
            throw new Exception("empty request.getInputStream");
        }

        JSONParser parser = new JSONParser();
        JSONObject body;

        try {
            Object obj = parser.parse(bodyString);
            body = (JSONObject) obj;
        } catch (Exception ex) {
            writer.write("JSONParser.parse error:" + ex.getMessage());
            throw ex;
        }

        if (DocumentManager.TokenEnabled()) {
            String token = (String) body.get("token");

            if (token == null) {
                String header = (String) request.getHeader(DocumentJwtHeader == null || DocumentJwtHeader.isEmpty() ? "Authorization" : DocumentJwtHeader);
                if (header != null && !header.isEmpty()) {
                    token = header.startsWith("Bearer ") ? header.substring(7) : header;
                }
            }

            if (token == null || token.isEmpty()) {
                writer.write("{\"error\":1,\"message\":\"JWT expected\"}");
                throw new Exception("{\"error\":1,\"message\":\"JWT expected\"}");
            }

            JWT jwt = DocumentManager.ReadToken(token);
            if (jwt == null) {
                writer.write("{\"error\":1,\"message\":\"JWT validation failed\"}");
                throw new Exception("{\"error\":1,\"message\":\"JWT validation failed\"}");
            }

            if (jwt.getObject("payload") != null) {
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

    public static void processSave(JSONObject body, String fileName, String userAddress) throws Exception {
        String downloadUri = (String) body.get("url");
        String changesUri = (String) body.get("changesurl");
        String key = (String) body.get("key");
        String newFileName = fileName;

        String curExt = FileUtility.GetFileExtension(fileName);
        String downloadExt = FileUtility.GetFileExtension(downloadUri);

        if (!curExt.equals(downloadExt)) {
            try {
                String newFileUri = ServiceConverter.GetConvertedUri(downloadUri, downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), null, false);
                if (newFileUri.isEmpty()) {
                    newFileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
                } else {
                    downloadUri = newFileUri;
                }
            } catch (Exception e){
                newFileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
            }
        }

        String storagePath = DocumentManager.StoragePath(newFileName, userAddress);
        File histDir = new File(DocumentManager.HistoryDir(storagePath));
        if (!histDir.exists()) histDir.mkdirs();

        String versionDir = DocumentManager.VersionDir(histDir.getAbsolutePath(), DocumentManager.GetFileVersion(histDir.getAbsolutePath()));
        File ver = new File(versionDir);
        File lastVersion = new File(DocumentManager.StoragePath(fileName, userAddress));
        File toSave = new File(storagePath);

        if (!ver.exists()) ver.mkdirs();

        lastVersion.renameTo(new File(versionDir + File.separator + "prev" + curExt));

        downloadToFile(downloadUri, toSave);
        downloadToFile(changesUri, new File(versionDir + File.separator + "diff.zip"));

        String history = (String) body.get("changeshistory");
        if (history == null && body.containsKey("history")) {
            history = ((JSONObject) body.get("history")).toJSONString();
        }
        if (history != null && !history.isEmpty()) {
            FileWriter fw = new FileWriter(new File(versionDir + File.separator + "changes.json"));
            fw.write(history);
            fw.close();
        }

        FileWriter fw = new FileWriter(new File(versionDir + File.separator + "key.txt"));
        fw.write(key);
        fw.close();

        String forcesavePath = DocumentManager.ForcesavePath(newFileName, userAddress, false);
        if (!forcesavePath.equals("")) {
            File forceSaveFile = new File(forcesavePath);
            forceSaveFile.delete();
        }
    }

    public static void processForceSave(JSONObject body, String fileName, String userAddress) throws Exception {

        String downloadUri = (String) body.get("url");

        String curExt = FileUtility.GetFileExtension(fileName);
        String downloadExt = FileUtility.GetFileExtension(downloadUri);
        Boolean newFileName = false;

        if (!curExt.equals(downloadExt)) {
            try {
                String newFileUri = ServiceConverter.GetConvertedUri(downloadUri, downloadExt, curExt, ServiceConverter.GenerateRevisionId(downloadUri), null, false);
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
        boolean isSubmitForm = body.get("forcesavetype").toString().equals("3");

        if (isSubmitForm) {
            //new file
            if (newFileName){
                fileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + "-form" + downloadExt, userAddress);
            } else {
                fileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + "-form" + curExt, userAddress);
            }
            forcesavePath = DocumentManager.StoragePath(fileName, userAddress);
        } else {
            if (newFileName){
                fileName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + downloadExt, userAddress);
            }

            forcesavePath = DocumentManager.ForcesavePath(fileName, userAddress, false);
            if (forcesavePath == "") {
                forcesavePath = DocumentManager.ForcesavePath(fileName, userAddress, true);
            }
        }

        File toSave = new File(forcesavePath);
        downloadToFile(downloadUri, toSave);

        if (isSubmitForm) {
            JSONArray actions = (JSONArray) body.get("actions");
            JSONObject action = (JSONObject) actions.get(0);
            String user = (String) action.get("userid");
            DocumentManager.CreateMeta(fileName, user, "Filling Form", userAddress);
        }
    }

    private static void downloadToFile(String url, File file) throws Exception {
        if (url == null || url.isEmpty()) throw new Exception("argument url");
        if (file == null) throw new Exception("argument path");

        URL uri = new URL(url);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) uri.openConnection();
        InputStream stream = connection.getInputStream();

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
                out.write(bytes, 0, read);
            }

            out.flush();
        }

        connection.disconnect();
    }

    public static void commandRequest(String method, String key) throws Exception {
        String DocumentCommandUrl = ConfigManager.GetProperty("files.docservice.url.site") + ConfigManager.GetProperty("files.docservice.url.command");

        URL url = new URL(DocumentCommandUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("c", method);
        params.put("key", key);

        String headerToken = "";
        if (DocumentManager.TokenEnabled())
        {
            Map<String, Object> payloadMap = new HashMap<String, Object>();
            payloadMap.put("payload", params);
            headerToken = DocumentManager.CreateToken(payloadMap);

            connection.setRequestProperty(DocumentJwtHeader.equals("") ? "Authorization" : DocumentJwtHeader, "Bearer " + headerToken);

            String token = DocumentManager.CreateToken(params);
            params.put("token", token);
        }

        Gson gson = new Gson();
        String bodyString = gson.toJson(params);

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);

        connection.connect();
        try (OutputStream os = connection.getOutputStream()) {
            os.write(bodyByte);
        }
        InputStream stream = connection.getInputStream();;

        if (stream == null)
            throw new Exception("Could not get an answer");

        String jsonString = ServiceConverter.ConvertStreamToString(stream);
        connection.disconnect();

        JSONObject response = ServiceConverter.ConvertStringToJSON(jsonString);
        if (!response.get("error").toString().equals("0")){
            throw new Exception(response.toJSONString());
        }
    }
}
