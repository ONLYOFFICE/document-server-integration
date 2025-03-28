/**
 *
 * (c) Copyright Ascensio System SIA 2025
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
import com.onlyoffice.integration.documentserver.managers.history.HistoryManager;
import com.onlyoffice.integration.documentserver.storage.FileStorageMutator;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.sdk.manager.DocumentManager;
import com.onlyoffice.integration.sdk.manager.UrlManager;
import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.model.commandservice.CommandRequest;
import com.onlyoffice.model.commandservice.commandrequest.Command;
import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Action;
import com.onlyoffice.model.documenteditor.callback.ForcesaveType;
import com.onlyoffice.model.documenteditor.callback.action.Type;
import com.onlyoffice.service.command.CommandService;
import com.onlyoffice.service.convert.ConvertService;
import lombok.SneakyThrows;
import org.apache.hc.core5.http.HttpEntity;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
// todo: Refactoring
@Component
@Primary
public class DefaultCallbackManager implements CallbackManager {

    @Autowired
    private DocumentManager documentManager;
    @Autowired
    private FileStorageMutator storageMutator;
    @Autowired
    private FileStoragePathBuilder storagePathBuilder;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ConvertService convertService;
    @Autowired
    private RequestManager requestManager;
    @Autowired
    private CommandService commandService;
    @Autowired
    private HistoryManager historyManager;

    @Autowired
    private UrlManager urlManager;

    // download file from url
    @SneakyThrows
    private byte[] getDownloadFile(final String url) {
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("Url argument is not specified");  // URL isn't specified
        }

        return requestManager.executeGetRequest(url, new RequestManager.Callback<byte[]>() {
            public byte[] doWork(final Object response) throws IOException {
                InputStream stream = ((HttpEntity) response).getContent(); // get input stream of the converted
                // file

                if (stream == null) {
                    throw new RuntimeException("Input stream is null");
                }

                return stream.readAllBytes();
            }
        });
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

    @Override
    public void processEditing(final Callback callback, final String fileName) {
        Action action = callback.getActions().get(0);  // get the user ID who is editing the document
        if (action.getType().equals(Type.CONNECTED)) {  // if this value is not equal to the user ID
            String user = action.getUserid();  // get user ID
            if (!callback.getUsers().contains(user)) {  // if this user is not specified in the body
                CommandRequest commandRequest = CommandRequest.builder()
                        .c(Command.FORCESAVE)
                        .key(callback.getKey())
                        .build();

                // create a command request to forcibly save the document being edited without closing it
                try {
                    commandService.processCommand(commandRequest, fileName);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @SneakyThrows
    public void processSave(final Callback callback, final String fileName) {  // file saving process
        String downloadUri = callback.getUrl();
        String changesUri = callback.getChangesurl();
        String key = callback.getKey();
        String newFileName = fileName;

        String curExt = documentManager.getExtension(fileName);  // get current file extension
        String downloadExt = callback.getFiletype(); // get an extension of the downloaded file
        String storagePath = storagePathBuilder.getFileLocation(newFileName);  // get the path to a new file

        if (!Paths.get(storagePath).toFile().exists()) {
            throw new RuntimeException("{\"error\":1, \"message\":\"file does not exist\"}");
        }

        downloadUri = urlManager.replaceToInnerDocumentServerUrl(downloadUri);
        changesUri = urlManager.replaceToInnerDocumentServerUrl(changesUri);

        // todo: Refactoring
        // convert downloaded file to the file with the current extension if these extensions aren't equal
        if (!curExt.equals(downloadExt)) {
            try {
                ConvertRequest convertRequest = ConvertRequest.builder()
                        .key(documentManager.generateRevisionId(downloadUri))
                        .url(downloadUri)
                        .outputtype(curExt)
                        .async(false)
                        .build();

                // convert a file and get URL to a new file
                ConvertResponse convertResponse = convertService.processConvert(convertRequest, fileName);

                String newFileUri = convertResponse.getFileUrl();

                if (newFileUri == null || newFileUri.isEmpty()) {
                    newFileName = documentManager
                            .getCorrectName(documentManager.getBaseName(fileName) + "."
                                    + downloadExt);  // get the correct file name if it already exists
                } else {
                    downloadUri = newFileUri;
                }
            } catch (Exception e) {
                newFileName = documentManager
                        .getCorrectName(documentManager.getBaseName(fileName) + "." + downloadExt);
            }
        }

        byte[] byteArrayFile = getDownloadFile(downloadUri);  // download document file

        Path lastVersion = Paths.get(storagePathBuilder
                .getFileLocation(fileName));  // get the path to the last file version

        if (lastVersion.toFile().exists()) {  // if the last file version exists
            Path histDir = Paths.get(storagePathBuilder.getHistoryDir(storagePath));  // get the history directory
            storageMutator.createDirectory(histDir);  // and create it

            String versionDir = historyManager
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
            jsonChanges.put("changes", callback.getHistory().getChanges());  // put the changes to the json object
            jsonChanges.put("serverVersion", callback.getHistory()
                    .getServerVersion());  // put the server version to the json object
            String history = objectMapper.writeValueAsString(jsonChanges);

            if (history == null && callback.getHistory() != null) {
                history = objectMapper.writeValueAsString(callback.getHistory());
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

    @SneakyThrows
    public void processForceSave(final Callback callback, final String fileNameParam) {  // file force saving process

        String downloadUri = callback.getUrl();
        String fileName = fileNameParam;

        String curExt = documentManager.getExtension(fileName);  // get current file extension
        String downloadExt = callback.getFiletype();  // get an extension of the downloaded file

        downloadUri = urlManager.replaceToInnerDocumentServerUrl(downloadUri);

        Boolean newFileName = false;

        // convert downloaded file to the file with the current extension if these extensions aren't equal
        // todo: Extract function
        if (!curExt.equals(downloadExt)) {
            try {
                ConvertRequest convertRequest = ConvertRequest.builder()
                        .key(documentManager.generateRevisionId(downloadUri))
                        .url(downloadUri)
                        .outputtype(curExt)
                        .async(false)
                        .build();

                // convert a file and get URL to a new file
                ConvertResponse convertResponse = convertService.processConvert(convertRequest, fileName);

                String newFileUri = convertResponse.getFileUrl();

                if (newFileUri == null || newFileUri.isEmpty()) {
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
        boolean isSubmitForm = callback.getForcesavetype().equals(ForcesaveType.SUBMIT_FORM);

        // todo: Extract function
        if (isSubmitForm) {  // if the form is submitted
            if (newFileName) {
                // get the correct file name if it already exists
                fileName = documentManager
                        .getCorrectName(documentManager
                                .getBaseName(fileName) + "-form." + downloadExt);
            } else {
                fileName = documentManager
                        .getCorrectName(documentManager.getBaseName(fileName) + "-form." + curExt);
            }
            forcesavePath = storagePathBuilder.getFileLocation(fileName);  // create forcesave path if it doesn't exist
            List<com.onlyoffice.model.documenteditor.callback.Action> actions = callback.getActions();
            com.onlyoffice.model.documenteditor.callback.Action action = actions.get(0);
            String user = action.getUserid();  // get the user ID
            // create meta data for the forcesaved file
            storageMutator.createMeta(fileName, user, "Filling Form");

            try {
                String formsDataUrl = callback.getFormsdataurl();

                formsDataUrl = urlManager.replaceToInnerDocumentServerUrl(formsDataUrl);

                if (formsDataUrl != null && !formsDataUrl.isEmpty()) {
                    String formsName = documentManager.getCorrectName(documentManager
                            .getBaseName(fileName) + ".txt");
                    String formsPath = storagePathBuilder.getFileLocation(formsName);

                    byte[] byteArrayFormsData = getDownloadFile(formsDataUrl);

                    saveFile(byteArrayFormsData, Paths.get(formsPath));
                } else {
                    throw new RuntimeException("Document editing service did not return formsDataUrl");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (newFileName) {
                fileName = documentManager
                        .getCorrectName(documentManager.getBaseName(fileName) + "." + downloadExt);
            }

            forcesavePath = storagePathBuilder.getForcesavePath(fileName, false);
            if (forcesavePath.isEmpty()) {
                forcesavePath = storagePathBuilder.getForcesavePath(fileName, true);
            }
        }

        saveFile(byteArrayFile, Path.of(forcesavePath));
    }
}
