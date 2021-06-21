package com.onlyoffice.integration.util;

import com.google.gson.Gson;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import com.onlyoffice.integration.util.fileUtilities.FileUtility;
import com.onlyoffice.integration.util.serviceConverter.ServiceConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.primeframework.jwt.domain.JWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

//TODO: Refactor everything
@Component
public class TrackManager {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private FileUtility fileUtility;

    @Autowired
    private ServiceConverter serviceConverter;

    @Value("${files.docservice.url.site}")
    private String docserviceUrlSite;

    @Value("${files.docservice.url.command}")
    private String docserviceUrlCommand;

    @Value("${files.docservice.header}")
    private String documentJwtHeader;

    //TODO: A separate @RequestBody class
    public JSONObject readBody() throws Exception {
        String bodyString = "";

        try {
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            bodyString = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        if (bodyString.isEmpty()) {
            throw new Exception("{\"error\":1,\"message\":\"Request payload is empty\"}");
        }

        JSONParser parser = new JSONParser();
        JSONObject body;

        try {
            Object obj = parser.parse(bodyString);
            body = (JSONObject) obj;
        } catch (Exception ex) {
            throw new Exception("{\"error\":1,\"message\":\"JSON Parsing error\"}");
        }

        if (documentManager.tokenEnabled()) {
            String token = (String) body.get("token");

            if (token == null) {
                String header = (String) request.getHeader(documentJwtHeader == null
                        || documentJwtHeader.isEmpty() ? "Authorization" : documentJwtHeader);
                if (header != null && !header.isEmpty()) {
                    token = header.startsWith("Bearer ") ? header.substring(7) : header;
                }
            }

            if (token == null || token.isEmpty()) {
                throw new Exception("{\"error\":1,\"message\":\"JWT expected\"}");
            }

            JWT jwt = documentManager.readToken(token);
            if (jwt == null) {
                throw new Exception("{\"error\":1,\"message\":\"JWT validation failed\"}");
            }

            if (jwt.getObject("payload") != null) {
                try {
                    @SuppressWarnings("unchecked") LinkedHashMap<String, Object> payload =
                            (LinkedHashMap<String, Object>)jwt.getObject("payload");

                    jwt.claims = payload;
                } catch (Exception ex) {
                    throw new Exception("{\"error\":1,\"message\":\"Wrong payload\"}");
                }
            }

            try {
                Gson gson = new Gson();
                Object obj = parser.parse(gson.toJson(jwt.claims));
                body = (JSONObject) obj;
            } catch (Exception ex) {
                throw new Exception("{\"error\":1,\"message\":\"Parsing error\"}");
            }
        }

        return body;
    }

    //TODO: A separate @RequestBody class
    public void processSave(JSONObject body, String fileName, String userAddress) throws Exception{
        String downloadUri = (String) body.get("url");
        String changesUri = (String) body.get("changesurl");
        String key = (String) body.get("key");
        String newFileName = fileName;

        String curExt = fileUtility.getFileExtension(fileName);  // get current file extension
        String downloadExt = fileUtility.getFileExtension(downloadUri);  // get the extension of the downloaded file

        // convert downloaded file to the file with the current extension if these extensions aren't equal
        if (!curExt.equals(downloadExt)) {
            try {
                String newFileUri = serviceConverter.getConvertedUri(downloadUri, downloadExt, curExt, serviceConverter.generateRevisionId(downloadUri), null, false);  // convert file and get url to a new file
                if (newFileUri.isEmpty()) {
                    newFileName = documentManager
                            .getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + downloadExt, userAddress);  // get the correct file name if it already exists
                } else {
                    downloadUri = newFileUri;
                }
            } catch (Exception e){
                newFileName = documentManager.getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + downloadExt, userAddress);
            }
        }

        String storagePath = documentManager.storagePath(newFileName, userAddress);  // get the file path
        Path histDir = Paths.get(documentManager.historyDir(storagePath));  // get the path to the history direction
        if (!Files.exists(histDir)) Files.createDirectories(histDir);  // if the path doesn't exist, create it

        String versionDir = documentManager.versionDir(histDir.toAbsolutePath().toString(),
                documentManager.getFileVersion(histDir.toAbsolutePath().toString()));  // get the path to the file version

        //TODO: Replace with NIO
        File ver = new File(versionDir);
        File lastVersion = new File(documentManager.storagePath(fileName, userAddress));
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

        String forcesavePath = documentManager.forcesavePath(newFileName, userAddress, false);  // get the path to the forcesaved file version
        if (!forcesavePath.equals("")) {  // if the forcesaved file version exists
            File forceSaveFile = new File(forcesavePath);
            forceSaveFile.delete();  // remove it
        }
    }
    private void downloadToFile(String url, File file) throws Exception {
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

    public void commandRequest(String method, String key) throws Exception {
        String DocumentCommandUrl = docserviceUrlSite + docserviceUrlCommand;

        URL url = new URL(DocumentCommandUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("c", method);
        params.put("key", key);

        String headerToken = "";
        if (documentManager.tokenEnabled())  // check if a secret key to generate token exists or not
        {
            Map<String, Object> payloadMap = new HashMap<String, Object>();
            payloadMap.put("payload", params);
            headerToken = documentManager.createToken(payloadMap);  // encode a payload object into a header token

            // add a header Authorization with a header token and Authorization prefix in it
            connection.setRequestProperty(documentJwtHeader.equals("") ? "Authorization" : documentJwtHeader, "Bearer " + headerToken);

            String token = documentManager.createToken(params);  // encode a payload object into a body token
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

        String jsonString = serviceConverter.convertStreamToString(stream);  // convert stream to json string
        connection.disconnect();

        JSONObject response = serviceConverter.convertStringToJSON(jsonString);  // convert json string to json object
        if (!response.get("error").toString().equals("0")){
            throw new Exception(response.toJSONString());
        }
    }

    public void processForceSave(JSONObject body, String fileName, String userAddress) throws Exception {

        String downloadUri = (String) body.get("url");

        String curExt = fileUtility.getFileExtension(fileName);  // get current file extension
        String downloadExt = fileUtility.getFileExtension(downloadUri);  // get the extension of the downloaded file
        Boolean newFileName = false;

        // convert downloaded file to the file with the current extension if these extensions aren't equal
        if (!curExt.equals(downloadExt)) {
            try {
                String newFileUri = serviceConverter.getConvertedUri(downloadUri, downloadExt,
                        curExt, serviceConverter.generateRevisionId(downloadUri), null, false);  // convert file and get url to a new file
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
                fileName = documentManager
                        .getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + "-form" + downloadExt, userAddress);  // get the correct file name if it already exists
            } else {
                fileName = documentManager.getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + "-form" + curExt, userAddress);
            }
            forcesavePath = documentManager.storagePath(fileName, userAddress);
        } else {
            if (newFileName){
                fileName = documentManager.getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + downloadExt, userAddress);
            }

            // create forcesave path if it doesn't exist
            forcesavePath = documentManager.forcesavePath(fileName, userAddress, false);
            if (forcesavePath == "") {
                forcesavePath = documentManager.forcesavePath(fileName, userAddress, true);
            }
        }

        File toSave = new File(forcesavePath);
        downloadToFile(downloadUri, toSave);

        if (isSubmitForm) {
            JSONArray actions = (JSONArray) body.get("actions");
            JSONObject action = (JSONObject) actions.get(0);
            String user = (String) action.get("userid");  // get the user id
            documentManager.createMeta(fileName, user, "Filling Form", userAddress);  // create meta data for forcesaved file
        }
    }
}
