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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.onlyoffice.integration.documentserver.util.Constants.FILE_SAVE_TIMEOUT;

// todo: Refactoring
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

    // download file from url
    @SneakyThrows
    private byte[] getDownloadFile(final String url) {
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("Url argument is not specified");  // URL isn't specified
        }

        URL uri = new URL(url);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) uri.openConnection();
        connection.setConnectTimeout(FILE_SAVE_TIMEOUT);
        InputStream stream = connection.getInputStream();  // get input stream of the file information from the URL

        int statusCode = connection.getResponseCode();
        if (statusCode != HttpStatus.OK.value()) {  // checking status code
            connection.disconnect();
            throw new RuntimeException("Document editing service returned status: " + statusCode);
        }

        if (stream == null) {
            connection.disconnect();
            throw new RuntimeException("Input stream is null");
        }

        return stream.readAllBytes();
    }

    // file saving
    @SneakyThrows
    private void saveFile(final byte[] byteArray, final Path path) {
        if (path == null) {
            throw new RuntimeException("Path argument is not specified");  // file isn't specified
        }
        // update a file or create a new one
        storageMutator.createOrUpdateFile(path, new ByteArrayInputStream(byteArray));
    }

    @SneakyThrows
    public void processSave(final Track body, final String fileName) {  // file saving process
        String downloadUri = body.getUrl();
        String changesUri = body.getChangesurl();
        String key = body.getKey();
        String newFileName = fileName;

        String curExt = fileUtility.getFileExtension(fileName);  // get current file extension
        String downloadExt = body.getFiletype(); // get an extension of the downloaded file

        // todo: Refactoring
        // convert downloaded file to the file with the current extension if these extensions aren't equal
        if (!curExt.equals(downloadExt)) {
            try {
                String newFileUri = serviceConverter
                        .getConvertedData(downloadUri, downloadExt, curExt,
                                serviceConverter.generateRevisionId(downloadUri), null, false,
                                null).getUri();  // convert a file and get URL to a new file
                if (newFileUri.isEmpty()) {
                    newFileName = documentManager
                            .getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + "."
                                    + downloadExt);  // get the correct file name if it already exists
                } else {
                    downloadUri = newFileUri;
                }
            } catch (Exception e) {
                newFileName = documentManager
                        .getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + "." + downloadExt);
            }
        }

        byte[] byteArrayFile = getDownloadFile(downloadUri);  // download document file

        String storagePath = storagePathBuilder.getFileLocation(newFileName);  // get the path to a new file
        Path lastVersion = Paths.get(storagePathBuilder
                .getFileLocation(fileName));  // get the path to the last file version

        if (lastVersion.toFile().exists()) {  // if the last file version exists
            Path histDir = Paths.get(storagePathBuilder.getHistoryDir(storagePath));  // get the history directory
            storageMutator.createDirectory(histDir);  // and create it

            String versionDir = documentManager
                    .versionDir(histDir.toAbsolutePath().toString(),  // get the file version directory
                    storagePathBuilder
                            .getFileVersion(histDir.toAbsolutePath().toString(), false), true);

            Path ver = Paths.get(versionDir);
            Path toSave = Paths.get(storagePath);

            storageMutator.createDirectory(ver);  // create the file version directory

            lastVersion.toFile().renameTo(new File(versionDir + File.separator + "prev." + curExt));

            saveFile(byteArrayFile, toSave); // save document file

            byte[] byteArrayChanges = getDownloadFile(changesUri); // download file changes
            saveFile(byteArrayChanges, Path
                    .of(versionDir + File.separator + "diff.zip")); // save file changes to the diff.zip archive

            JSONObject jsonChanges = new JSONObject();  // create a json object for document changes
            jsonChanges.put("changes", body.getHistory().getChanges());  // put the changes to the json object
            jsonChanges.put("serverVersion", body.getHistory()
                    .getServerVersion());  // put the server version to the json object
            String history = objectMapper.writeValueAsString(jsonChanges);

            if (history == null && body.getHistory() != null) {
                history = objectMapper.writeValueAsString(body.getHistory());
            }

            if (history != null && !history.isEmpty()) {
                // write the history changes to the changes.json file
                storageMutator.writeToFile(versionDir + File.separator + "changes.json", history);
            }

            // write the key value to the key.txt file
            storageMutator.writeToFile(versionDir + File.separator + "key.txt", key);

            // get the path to the forcesaved file version and remove it
            storageMutator.deleteFile(storagePathBuilder.getForcesavePath(newFileName, false));
        }
    }

    // todo: Replace (String method) with (Enum method)
    @SneakyThrows
    public void commandRequest(final String method,
                               final String key,
                               final HashMap meta) {  // create a command request
        String documentCommandUrl = docserviceUrlSite + docserviceUrlCommand;

        URL url = new URL(documentCommandUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("c", method);
        params.put("key", key);

        if (meta != null) {
            params.put("meta", meta);
        }

        String headerToken;
        // check if a secret key to generate token exists or not
        if (jwtManager.tokenEnabled() && jwtManager.tokenUseForRequest()) {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("payload", params);
            headerToken = jwtManager.createToken(payloadMap);  // encode a payload object into a header token

            // add a header Authorization with a header token and Authorization prefix in it
            connection.setRequestProperty(documentJwtHeader.equals("")
                    ? "Authorization" : documentJwtHeader, "Bearer " + headerToken);

            String token = jwtManager.createToken(params);  // encode a payload object into a body token
            params.put("token", token);
        }

        String bodyString = objectMapper.writeValueAsString(params);

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);

        connection.setRequestMethod("POST");  // set the request method
        connection
                .setRequestProperty("Content-Type", "application/json; charset=UTF-8");  // set the Content-Type header
        connection.setDoOutput(true);  // set the doOutput field to true
        connection.connect();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(bodyByte);  // write bytes to the output stream
        }

        InputStream stream = connection.getInputStream();  // get input stream

        if (stream == null) {
            throw new RuntimeException("Could not get an answer");
        }

        String jsonString = serviceConverter.convertStreamToString(stream);  // convert stream to json string
        connection.disconnect();

        JSONObject response = serviceConverter.convertStringToJSON(jsonString);  // convert json string to json object
        // todo: Add errors ENUM
        String responseCode = response.get("error").toString();
        switch (responseCode) {
            case "0":
            case "4":
                break;
            default:
                throw new RuntimeException(response.toJSONString());
            }
        }

    @SneakyThrows
    public void processForceSave(final Track body, final String fileNameParam) {  // file force saving process

        String downloadUri = body.getUrl();
        String fileName = fileNameParam;

        String curExt = fileUtility.getFileExtension(fileName);  // get current file extension
        String downloadExt = body.getFiletype();  // get an extension of the downloaded file

        Boolean newFileName = false;

        // convert downloaded file to the file with the current extension if these extensions aren't equal
        // todo: Extract function
        if (!curExt.equals(downloadExt)) {
            try {
                // convert file and get URL to a new file
                String newFileUri = serviceConverter
                        .getConvertedData(downloadUri, downloadExt, curExt, serviceConverter
                                .generateRevisionId(downloadUri), null, false, null).getUri();
                if (newFileUri.isEmpty()) {
                    newFileName = true;
                } else {
                    downloadUri = newFileUri;
                }
            } catch (Exception e) {
                newFileName = true;
            }
        }

        byte[] byteArrayFile = getDownloadFile(downloadUri);  // download document file
        String forcesavePath = "";

        // todo: Use ENUMS
        // todo: Pointless toString conversion
        boolean isSubmitForm = body.getForcesavetype().toString().equals("3");

        // todo: Extract function
        if (isSubmitForm) {  // if the form is submitted
            if (newFileName) {
                // get the correct file name if it already exists
                fileName = documentManager
                        .getCorrectName(fileUtility
                                .getFileNameWithoutExtension(fileName) + "-form." + downloadExt);
            } else {
                fileName = documentManager
                        .getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + "-form." + curExt);
            }
            forcesavePath = storagePathBuilder.getFileLocation(fileName);  // create forcesave path if it doesn't exist
            List<Action> actions =  body.getActions();
            Action action = actions.get(0);
            String user = action.getUserid();  // get the user ID
            // create meta data for the forcesaved file
            storageMutator.createMeta(fileName, user, "Filling Form");
        } else {
            if (newFileName) {
                fileName = documentManager
                        .getCorrectName(fileUtility.getFileNameWithoutExtension(fileName) + downloadExt);
            }

            forcesavePath = storagePathBuilder.getForcesavePath(fileName, false);
            if (forcesavePath.isEmpty()) {
                forcesavePath = storagePathBuilder.getForcesavePath(fileName, true);
            }
        }

        saveFile(byteArrayFile, Path.of(forcesavePath));
    }
}
