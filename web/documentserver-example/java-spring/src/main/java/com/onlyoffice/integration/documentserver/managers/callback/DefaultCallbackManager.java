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

package com.onlyoffice.integration.documentserver.managers.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.documentserver.managers.document.DocumentManager;
import com.onlyoffice.integration.documentserver.managers.jwt.JwtManager;
import com.onlyoffice.integration.documentserver.storage.FileStorageMutator;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.dto.Action;
import com.onlyoffice.integration.documentserver.util.service.ServiceConverter;
import com.onlyoffice.integration.dto.Track;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

//TODO: Refactoring
@Component
@Primary
public class DefaultCallbackManager implements CallbackManager {

    @Value("${files.docservice.url.site}")
    private String docserviceUrlSite;
    @Value("${files.docservice.url.command}")
    private String docserviceUrlCommand;
    @Value("${files.docservice.header}")
    private String documentJwtHeader;

    @Autowired
    private DocumentManager documentManager;
    @Autowired
    private JwtManager jwtManager;
    @Autowired
    private FileUtility fileUtility;
    @Autowired
    private FileStorageMutator storageMutator;
    @Autowired
    private FileStoragePathBuilder storagePathBuilder;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ServiceConverter serviceConverter;

    private void downloadToFile(String url, Path path) throws Exception {
        if (url == null || url.isEmpty()) throw new RuntimeException("Url argument is not specified");
        if (path == null) throw new RuntimeException("Path argument is not specified");

        URL uri = new URL(url);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) uri.openConnection();
        InputStream stream = connection.getInputStream();

        if (stream == null) {
            connection.disconnect();
            throw new RuntimeException("Input stream is null");
        }

        storageMutator.createFile(path, stream);
    }

    @SneakyThrows
    public void processSave(Track body, String fileName) {
        String downloadUri = body.getUrl();
        String changesUri = body.getChangesurl();
        String key = body.getKey();
        String newFileName = fileName;

        String curExt = fileUtility.getFileExtension(fileName);
        String downloadExt = fileUtility.getFileExtension(downloadUri);

        //TODO: Refactoring
        if (!curExt.equals(downloadExt)) {
            try {
                String newFileUri = serviceConverter.getConvertedUri(downloadUri, downloadExt, curExt, serviceConverter.generateRevisionId(downloadUri), null, false);  // convert file and get url to a new file
                if (newFileUri.isEmpty()) {
                    newFileName = documentManager
                            .getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + downloadExt);  // get the correct file name if it already exists
                } else {
                    downloadUri = newFileUri;
                }
            } catch (Exception e){
                newFileName = documentManager.getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + downloadExt);
            }
        }

        String storagePath = storagePathBuilder.getFileLocation(newFileName);
        Path histDir = Paths.get(storagePathBuilder.getHistoryDir(storagePath));
        storageMutator.createDirectory(histDir);

        String versionDir = documentManager.versionDir(histDir.toAbsolutePath().toString(),
                storagePathBuilder.getFileVersion(histDir.toAbsolutePath().toString(), false), true);

        Path ver = Paths.get(versionDir);
        Path lastVersion = Paths.get(storagePathBuilder.getFileLocation(fileName));
        Path toSave = Paths.get(storagePath);

        storageMutator.createDirectory(ver);
        storageMutator.moveFile(lastVersion,  Paths.get(versionDir + File.separator + "prev" + curExt));

        downloadToFile(downloadUri, toSave);
        downloadToFile(changesUri, Path.of(versionDir + File.separator + "diff.zip"));

        JSONObject jsonChanges = new JSONObject();
        jsonChanges.put("changes", body.getHistory().getChanges());
        jsonChanges.put("serverVersion", body.getHistory().getServerVersion());
        String history = objectMapper.writeValueAsString(jsonChanges);

        if(history==null && body.getHistory()!=null){
            history = objectMapper.writeValueAsString(body.getHistory());
        }

        if (history != null && !history.isEmpty()) {
            storageMutator.writeToFile(versionDir + File.separator + "changes.json", history);
        }

        storageMutator.writeToFile(versionDir + File.separator + "key.txt", key);
        storageMutator.deleteFile(storagePathBuilder.getForcesavePath(newFileName, false));
    }

    //TODO: Replace (String method) with (Enum method)
    @SneakyThrows
    public void commandRequest(String method, String key) {
        String DocumentCommandUrl = docserviceUrlSite + docserviceUrlCommand;

        URL url = new URL(DocumentCommandUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

        HashMap<String, Object> params = new HashMap<>();
        params.put("c", method);
        params.put("key", key);

        String headerToken;
        if (jwtManager.tokenEnabled())
        {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("payload", params);
            headerToken = jwtManager.createToken(payloadMap);
            connection.setRequestProperty(documentJwtHeader.equals("") ? "Authorization" : documentJwtHeader, "Bearer " + headerToken);

            String token = jwtManager.createToken(params);
            params.put("token", token);
        }

        String bodyString = objectMapper.writeValueAsString(params);

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);
        connection.connect();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(bodyByte);
        }

        InputStream stream = connection.getInputStream();

        if (stream == null) throw new RuntimeException("Could not get an answer");

        String jsonString = serviceConverter.convertStreamToString(stream);
        connection.disconnect();

        JSONObject response = serviceConverter.convertStringToJSON(jsonString);
        //TODO: Add errors ENUM
        if (!response.get("error").toString().equals("0")){
            throw new RuntimeException(response.toJSONString());
        }
    }

    @SneakyThrows
    public void processForceSave(Track body, String fileName) {

        String downloadUri = body.getUrl();

        String curExt = fileUtility.getFileExtension(fileName);
        String downloadExt = fileUtility.getFileExtension(downloadUri);
        Boolean newFileName = false;

        // convert downloaded file to the file with the current extension if these extensions aren't equal
        //TODO: Extract function
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

        //TODO: Use ENUMS
        //TODO: Pointless toString conversion
        boolean isSubmitForm = body.getForcesavetype().toString().equals("3");

        //TODO: Extract function
        if (isSubmitForm) {
            if (newFileName){
                fileName = documentManager
                        .getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + "-form" + downloadExt);  // get the correct file name if it already exists
            } else {
                fileName = documentManager.getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + "-form" + curExt);
            }
            forcesavePath = storagePathBuilder.getFileLocation(fileName);
            List<Action> actions =  body.getActions();
            Action action = actions.get(0);
            String user = action.getUserid();
            storageMutator.createMeta(fileName, user, "Filling Form");
        } else {
            if (newFileName){
                fileName = documentManager.getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + downloadExt);
            }

            forcesavePath = storagePathBuilder.getForcesavePath(fileName, false);
            if (forcesavePath.isEmpty()) {
                forcesavePath = storagePathBuilder.getForcesavePath(fileName, true);
            }
        }

        downloadToFile(downloadUri, Path.of(forcesavePath));
    }
}
