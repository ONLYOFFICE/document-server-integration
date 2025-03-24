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

package com.onlyoffice.integration.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.integration.documentserver.managers.history.HistoryManager;
import com.onlyoffice.integration.documentserver.storage.FileStorageMutator;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.dto.Converter;
import com.onlyoffice.integration.dto.FormatsList;
import com.onlyoffice.integration.dto.Reference;
import com.onlyoffice.integration.dto.RefreshConfig;
import com.onlyoffice.integration.dto.Rename;
import com.onlyoffice.integration.dto.Restore;
import com.onlyoffice.integration.dto.SaveAs;
import com.onlyoffice.integration.dto.RefreshConfig.Document;
import com.onlyoffice.integration.dto.RefreshConfig.EditorConfig;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.sdk.manager.DocumentManager;
import com.onlyoffice.integration.sdk.service.ConfigService;
import com.onlyoffice.integration.services.UserServices;

import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.commandservice.CommandRequest;
import com.onlyoffice.model.commandservice.CommandResponse;
import com.onlyoffice.model.commandservice.commandrequest.Command;
import com.onlyoffice.model.commandservice.commandrequest.Meta;
import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.config.document.ReferenceData;
import com.onlyoffice.service.convert.ConvertService;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin("*")
@Controller
public class FileController {

    @Autowired
    private JwtManager jwtManager;
    @Autowired
    private FileStorageMutator storageMutator;
    @Autowired
    private FileStoragePathBuilder storagePathBuilder;
    @Autowired
    private UserServices userService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private HistoryManager historyManager;
    @Autowired
    private DocumentManager documentManager;
    @Autowired
    private ConvertService convertService;
    @Autowired
    private DocumentServerClient documentServerClient;
    @Autowired
    private SettingsManager settingsManager;
    @Autowired
    private CallbackService callbackService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private UrlManager urlManager;

    // create user metadata
    private String createUserMetadata(final String uid, final String fullFileName) {
        Optional<User> optionalUser = userService.findUserById(Integer.parseInt(uid));  // find a user by their ID
        // get document type
        String documentType = documentManager.getDocumentType(fullFileName).toString().toLowerCase();
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            storageMutator.createMeta(fullFileName,  // create meta information with the user ID and name specified
                    String.valueOf(user.getId()), user.getName());
        }
        return "{ \"filename\": \"" + fullFileName + "\", \"documentType\": \"" + documentType + "\",\"percent\":100}";
    }

    // download data from the specified file
    private ResponseEntity<Resource> downloadFile(final String fileName) {
        Resource resource = storageMutator.loadFileAsResource(fileName);  // load the specified file as a resource
        String contentType = "application/octet-stream";

        // create a response with the content type, header and body with the file data
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    private ResponseEntity<Resource> downloadSample(final String fileName) {
        String serverPath = System.getProperty("user.dir");
        String contentType = "application/octet-stream";
        String[] fileLocation = new String[] {serverPath, "src", "main", "resources", "assets", "document-templates",
                                              "sample", fileName};
        Path filePath = Paths.get(String.join(File.separator, fileLocation));
        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
        }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // download data from the specified history file
    private ResponseEntity<Resource> downloadFileHistory(final String fileName,
                                                         final String version,
                                                         final String file) {

        // load the specified file as a resource
        Resource resource = storageMutator.loadFileAsResourceHistory(fileName, version, file);
        String contentType = "application/octet-stream";

        // create a response with the content type, header and body with the file data
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/upload")
    @ResponseBody
    public String upload(@RequestParam("file") final MultipartFile file,  // upload a file
                             @CookieValue("uid") final String uid) {
        try {
            String fullFileName = file.getOriginalFilename();  // get file name
            String fileExtension = documentManager.getExtension(fullFileName);  // get file extension
            long fileSize = file.getSize();  // get file size
            byte[] bytes = file.getBytes();  // get file in bytes

            // check if the file size exceeds the maximum file size or is less than 0
            if (documentManager.getMaxFileSize() < fileSize || fileSize <= 0) {
                return "{ \"error\": \"File size is incorrect\"}";  // if so, write an error message to the response
            }

            // check if file extension is supported by the editor
            if (documentManager.getDocumentType(fullFileName) == null) {

                // if not, write an error message to the response
                return "{ \"error\": \"File type is not supported\"}";
            }

            String fileNamePath = storageMutator.updateFile(fullFileName, bytes);  // update a file
            if (fileNamePath.isBlank()) {
                throw new IOException("Could not update a file");  // if the file cannot be updated, an error occurs
            }

            fullFileName = documentManager.getBaseName(fileNamePath)
                    + "." + fileExtension;  // get full file name

            return createUserMetadata(uid, fullFileName);  // create user metadata and return it
        } catch (Exception e) {
            e.printStackTrace();
        }

        // if the operation of file uploading is unsuccessful, an error occurs
        return "{ \"error\": \"Something went wrong when uploading the file.\"}";
    }

    @PostMapping(path = "${url.converter}")
    @ResponseBody
    public String convert(@RequestBody final Converter body,  // convert a file
                          @CookieValue("uid") final String uid, @CookieValue("ulang") final String lang) {
        // get file name
        String fileName = body.getFileName();
        // get file password if it exists
        String filePass = body.getFilePass() != null ? body.getFilePass() : null;
        // get an auto-conversion extension from the request body or set it to the ooxml extension
        String conversionExtension = body.getFileExt() != null ? body.getFileExt() : "ooxml";
        Boolean keepOriginal = body.getKeepOriginal() != null ? body.getKeepOriginal() : false;

        try {
            // check if the file with such an extension can be converted
            if (documentManager.getDefaultConvertExtension(fileName) != null || body.getFileExt() != null) {
                ConvertRequest convertRequest = ConvertRequest.builder()
                        .password(filePass)
                        .outputtype(conversionExtension)
                        .region(lang)
                        .async(true)
                        .title(fileName)
                        .build();

                ConvertResponse convertResponse = convertService.processConvert(convertRequest, fileName);

                if (convertResponse.getError() != null || convertResponse.getFileUrl()  == null) {
                    return objectMapper.writeValueAsString(convertResponse);
                }

                String newFileUri = convertResponse.getFileUrl();
                String newFileType = convertResponse.getFileType();

                if (!new FormatsList(documentManager.getFormats()).getFormats().stream().anyMatch(
                        f -> newFileType.equals(f.getName()) && f.getType() != null)
                    ) {
                        return "{ \"percent\" : \"100\", \"filename\" : \"" + newFileUri
                                     + "\", \"error\":\"FileTypeIsNotSupported\"}";
                }

                /* get a file name of an internal file extension with an index if the file
                 with such a name already exists */
                final String oldFileName = fileName;
                String nameWithInternalExt = documentManager.getBaseName(fileName) + "." + newFileType;
                String correctedName = documentManager.getCorrectName(nameWithInternalExt);

                File file = storageMutator.createFile(Path.of(storagePathBuilder.getFileLocation(correctedName)));
                try {
                    documentServerClient.getFile(newFileUri, Files.newOutputStream(file.toPath()));
                } catch (Exception e) {
                    file.delete();

                    throw e;
                }

                if (!keepOriginal) {
                    storageMutator.deleteFile(oldFileName);
                }

                fileName = correctedName;
            }

            // create meta information about the converted file with the user ID and name specified
            return createUserMetadata(uid, fileName);
        } catch (Exception e) {
            e.printStackTrace();

            // if the operation of file converting is unsuccessful, an error occurs
            return "{ \"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @PostMapping("/delete")
    @ResponseBody
    public String delete(@RequestBody final Converter body) {  // delete a file
        try {
            String filename = body.getFileName();
            boolean success = false;

            if (filename != null) {
                String fullFileName = documentManager.getDocumentName(filename);  // get full file name

                // delete a file from the storage and return the status of this operation (true or false)
                boolean fileSuccess = storageMutator.deleteFile(fullFileName);

                // delete file history and return the status of this operation (true or false)
                boolean historySuccess = storageMutator.deleteFileHistory(fullFileName);
                success = fileSuccess && historySuccess;
            } else {
                // delete the user's folder and return the boolean status
                success = storageMutator.deleteUserFolder();
            }
            return "{ \"success\": \"" + (success) + "\"}";
        } catch (Exception e) {
            // if the operation of file deleting is unsuccessful, an error occurs
            return "{ \"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping("/downloadhistory")
    public ResponseEntity<Resource> downloadHistory(final HttpServletRequest request, // download a file
                                             @RequestParam("fileName") final String fileName,
                                             @RequestParam("ver") final String version,
                                             @RequestParam("file") final String file) { // history file
        try {
            // check if a token is enabled or not
            if (settingsManager.isSecurityEnabled()) {
                String header = request.getHeader(settingsManager.getSecurityHeader());
                if (header != null && !header.isEmpty()) {
                    String token = header
                            .replace("Bearer ", "");  // token is the header without the Bearer prefix
                    jwtManager.verify(token);  // read the token
                } else {
                    return null;
                }
            }
            return downloadFileHistory(fileName, version, file);  // download data from the specified file
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping(path = "${url.download}")
    public ResponseEntity<Resource> download(final HttpServletRequest request,  // download a file
                                             @RequestParam("fileName") final String fileName,
                                             @RequestParam(value = "userAddress", required = false)
                                                 final String userAddress) {
        try {
            // check if a token is enabled or not
            if (settingsManager.isSecurityEnabled() && userAddress != null) {
                String header = request.getHeader(settingsManager.getSecurityHeader());
                if (header != null && !header.isEmpty()) {
                    String token = header
                            .replace("Bearer ", "");  // token is the header without the Bearer prefix
                    jwtManager.verify(token);  // read the token
                } else {
                    return null;
                }
            }
            return downloadFile(fileName);  // download data from the specified file
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/create")
    public String create(@RequestParam("fileExt")
                             final String fileExt, // create a sample file of the specified extension
                         @RequestParam(value = "sample", required = false) final Optional<Boolean> isSample,
                         @CookieValue(value = "uid", required = false) final String uid,
                         final Model model) {
        // specify if the sample data exists or not
        Boolean sampleData = (isSample.isPresent() && !isSample.isEmpty()) && isSample.get();
        if (fileExt != null) {
            try {
                Optional<User> user = userService.findUserById(Integer.parseInt(uid));  // find a user by their ID
                if (!user.isPresent()) {
                    // if the user with the specified ID doesn't exist, an error occurs
                    throw new RuntimeException("Could not fine any user with id = " + uid);
                }
                String fileName = documentManager.createDemo(fileExt,
                        sampleData,
                        uid,
                        user.get().getName());  // create a demo document with the sample data
                if (fileName == null || fileName.isBlank()) {
                    throw new RuntimeException("You must have forgotten to add asset files");
                }
                return "redirect:editor?action=edit&fileName=" + URLEncoder
                        .encode(fileName, StandardCharsets.UTF_8);  // redirect the request
            } catch (Exception ex) {
                model.addAttribute("error", ex.getMessage());
                return "error.html";
            }
        }
        return "redirect:/";
    }

    @GetMapping("/assets")
    public ResponseEntity<Resource> assets(@RequestParam("name")
                                               final String name) {  // get sample files from the assests
        return downloadSample(name);
    }

    @GetMapping("/csv")
    public ResponseEntity<Resource> csv() {  // download a csv file
        return downloadSample("csv.csv");
    }

    @GetMapping("/files")
    @ResponseBody
    public ArrayList<Map<String, Object>> files(@RequestParam(value = "fileId", required = false)
                                                    final String fileId) {  // get files information
        return fileId == null ? documentManager.getFilesInfo() : documentManager.getFilesInfo(fileId);
    }

    @PostMapping(path = "${url.track}")
    @ResponseBody
    public String track(final HttpServletRequest request,  // track file changes
                        @RequestParam("fileName") final String fileName,
                        @RequestParam("userAddress") final String userAddress,
                        @RequestBody final Callback body) {
        Callback callback;
        try {
            String bodyString = objectMapper
                    .writeValueAsString(body);  // write the request body to the object mapper as a string

            if (bodyString.isEmpty()) {  // if the request body is empty, an error occurs
                throw new RuntimeException("{\"error\":1,\"message\":\"Request payload is empty\"}");
            }

            String authorizationHeader = request.getHeader(settingsManager.getSecurityHeader());
            callback = callbackService.verifyCallback(body, authorizationHeader);

            callbackService.processCallback(callback, fileName);
        } catch (Exception e) {
            String message = e.getMessage();
            if (!message.contains("\"error\":1")) {
                e.printStackTrace();
            }
            return message;
        }

        return "{\"error\":\"0\"}";
    }

    @PostMapping("/saveas")
    @ResponseBody
    public String saveAs(@RequestBody final SaveAs body, @CookieValue("uid") final String uid) {
        try {
            String fileName = documentManager.getCorrectName(body.getTitle());

            if (documentManager.getDocumentType(fileName) == null) {
                return "{\"error\":\"File type is not supported\"}";
            }

            String url = body.getUrl();

            url = urlManager.replaceToInnerDocumentServerUrl(url);

            File file = storageMutator.createFile(Path.of(storagePathBuilder.getFileLocation(fileName)));
            try {
                documentServerClient.getFile(url, Files.newOutputStream(file.toPath()));
            } catch (Exception e) {
                file.delete();

                throw e;
            }

            createUserMetadata(uid, fileName);

            return "{\"file\":  \"" + fileName + "\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{ \"error\" : 1, \"message\" : \"" + e.getMessage() + "\"}";
        }
    }

    @PostMapping("/rename")
    @ResponseBody
    public String rename(@RequestBody final Rename body) {
        CommandRequest commandRequest = CommandRequest.builder()
                .key(body.getFileKey())
                .c(Command.META)
                .meta(Meta.builder()
                        .title(body.getFileName() + "." + body.getFileType())
                        .build())
                .build();

        try {

            CommandResponse commandResponse = documentServerClient.command(commandRequest);
            return commandResponse.getError().getDescription();
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    @PostMapping("/reference")
    @ResponseBody
    public String reference(@RequestBody final Reference body) {
        try {
            JSONParser parser = new JSONParser();
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();

            String userAddress = "";
            String fileName = "";

            if (body.getReferenceData() != null) {
                ReferenceData referenceData = body.getReferenceData();

                if (referenceData.getInstanceId().equals(storagePathBuilder.getServerUrl(false))) {
                    JSONObject fileKey = (JSONObject) parser.parse(referenceData.getFileKey());
                    userAddress = (String) fileKey.get("userAddress");
                    if (userAddress.equals(InetAddress.getLocalHost().getHostAddress())) {
                        fileName = (String) fileKey.get("fileName");
                    }
                }
            }

            String link = body.getLink();
            if (fileName.equals("") && link != null) {
                if (!link.contains(storagePathBuilder.getServerUrl(true))) {
                    HashMap<String, String> data = new HashMap<>();
                    data.put("url", link);
                    return gson.toJson(data);
                }

                UriComponents uriComponents = UriComponentsBuilder.fromUriString(body.getLink()).build();
                fileName = uriComponents.getQueryParams().getFirst("fileName");
                boolean fileExists = new File(storagePathBuilder.getFileLocation(fileName)).exists();
                if (!fileExists) {
                    return "{ \"error\": \"File is not exist\"}";
                }
            }

            if (fileName.equals("")) {
                try {
                    String path = (String) body.getPath();
                    path = path.substring(path.lastIndexOf('/') + 1);
                    path = path.split("\\?")[0];
                    File f = new File(storagePathBuilder.getFileLocation(path));
                    if (f.exists()) {
                        fileName = path;
                    }
                } catch (Exception e) {
                    return "{ \"error\" : 1, \"message\" : \"" + e.getMessage() + "\"}";
                }
            }

            if (fileName.equals("")) {
                return "{ \"error\": \"File not found\"}";
            }

            HashMap<String, Object> fileKey = new HashMap<>();
            fileKey.put("fileName", fileName);
            fileKey.put("userAddress", InetAddress.getLocalHost().getHostAddress());

            HashMap<String, Object> referenceData = new HashMap<>();
            referenceData.put("instanceId", storagePathBuilder.getServerUrl(true));
            referenceData.put("fileKey", gson.toJson(fileKey));

            HashMap<String, Object> data = new HashMap<>();
            data.put("fileType", documentManager.getDocumentName(fileName));
            data.put("key", documentManager.generateRevisionId(
                storagePathBuilder.getStorageLocation()
                + "/" + fileName + "/"
                + new File(storagePathBuilder.getFileLocation(fileName)).lastModified()
                ));
            data.put("url", urlManager.getFileUrl(fileName));
            data.put("referenceData", referenceData);
            data.put("path", fileName);
            data.put("link", storagePathBuilder.getServerUrl(true) + "/editor?fileName=" + fileName);

            if (settingsManager.isSecurityEnabled()) {
                String token = jwtManager.createToken(data);
                data.put("token", token);
            }
            return gson.toJson(data);
        } catch (Exception e) {
            e.printStackTrace();
            return "{ \"error\" : 1, \"message\" : \"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping("/history")
    @ResponseBody
    public String history(@RequestParam("fileName") final String fileName) {
        return historyManager.getHistory(fileName);
    }

    @GetMapping("/historydata")
    @ResponseBody
    public String history(@RequestParam("fileName") final String fileName,
                          @RequestParam("version") final String version) {
        return historyManager.getHistoryData(fileName, version);
    }

    @PutMapping("/restore")
    @ResponseBody
    public String restore(@RequestBody final Restore body, @CookieValue("uid") final Integer uid) {
        try {
            String sourceStringFile = storagePathBuilder.getFileLocation(body.getFileName());
            File sourceFile = new File(sourceStringFile);
            Path sourcePathFile = sourceFile.toPath();
            String historyDirectory = storagePathBuilder.getHistoryDir(sourcePathFile.toString());

            Integer bumpedVersion = storagePathBuilder.getFileVersion(historyDirectory, false);
            String bumpedVersionStringDirectory = historyManager.versionDir(historyDirectory, bumpedVersion, true);
            File bumpedVersionDirectory = new File(bumpedVersionStringDirectory);
            if (!bumpedVersionDirectory.exists()) {
                bumpedVersionDirectory.mkdir();
            }

            Path bumpedKeyPathFile = Paths.get(bumpedVersionStringDirectory, "key.txt");
            String bumpedKeyStringFile = bumpedKeyPathFile.toString();
            File bumpedKeyFile = new File(bumpedKeyStringFile);
            String bumpedKey = documentManager.generateRevisionId(
                storagePathBuilder.getStorageLocation()
                + "/"
                + body.getFileName()
                + "/"
                + Long.toString(sourceFile.lastModified())
            );
            FileWriter bumpedKeyFileWriter = new FileWriter(bumpedKeyFile);
            bumpedKeyFileWriter.write(bumpedKey);
            bumpedKeyFileWriter.close();

            User user = userService.findUserById(uid).get();

            Path bumpedChangesPathFile = Paths.get(bumpedVersionStringDirectory, "changes.json");
            String bumpedChangesStringFile = bumpedChangesPathFile.toString();
            File bumpedChangesFile = new File(bumpedChangesStringFile);
            JSONObject bumpedChangesUser = new JSONObject();
            // Don't add the `uid-` prefix.
            // https://github.com/ONLYOFFICE/document-server-integration/issues/437#issuecomment-1663526562
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

            String sourceExtension = documentManager.getExtension(body.getFileName());
            String previousBasename = "prev." + sourceExtension;

            Path bumpedFile = Paths.get(bumpedVersionStringDirectory, previousBasename);
            Files.move(sourcePathFile, bumpedFile);

            if (body.getUrl() != null) {
                File file = storageMutator.createFile(sourcePathFile);
                try {
                    documentServerClient.getFile(body.getUrl(), Files.newOutputStream(file.toPath()));
                } catch (Exception e) {
                    file.delete();

                    throw e;
                }
            } else {
                String recoveryVersionStringDirectory = historyManager.versionDir(
                        historyDirectory,
                        body.getVersion(),
                        true
                );
                Path recoveryPathFile = Paths.get(recoveryVersionStringDirectory, previousBasename);
                String recoveryStringFile = recoveryPathFile.toString();
                FileInputStream recoveryStream = new FileInputStream(recoveryStringFile);
                storageMutator.createFile(sourcePathFile, recoveryStream);
                recoveryStream.close();
            }

            JSONObject responseBody = new JSONObject();
            responseBody.put("error", null);
            responseBody.put("success", true);
            return responseBody.toJSONString();
        } catch (Exception error) {
            error.printStackTrace();
            JSONObject responseBody = new JSONObject();
            responseBody.put("error", error.getMessage());
            responseBody.put("success", false);
            return responseBody.toJSONString();
        }
    }

    @GetMapping("/config")
    public ResponseEntity<RefreshConfig> config(@RequestParam("fileName") final String fileName,
                            @RequestParam("permissions") final String permissions,
                            @CookieValue(value = "uid", required = false) final String uid) {
        try {
            if (!new File(storagePathBuilder.getFileLocation(fileName)).exists()) {
                throw(new Exception("File not found"));
            }

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();

            Document document = new Document();
            document.setTitle(fileName);
            document.setKey(documentManager.getDocumentKey(fileName, false));
            document.setUrl(urlManager.getFileUrl(fileName));
            document.setReferenceData(configService.getReferenceData(fileName));
            document.setPermissions(gson.fromJson(permissions, new TypeToken<Map<String, Object>>() { }.getType()));

            EditorConfig editorConfig = new EditorConfig();
            editorConfig.setCallbackUrl(urlManager.getCallbackUrl(fileName));

            RefreshConfig config = new RefreshConfig();
            config.setDocument(document);
            config.setEditorConfig(editorConfig);
            if (settingsManager.isSecurityEnabled()) {
                config.setToken(jwtManager.createToken(config));
            }

            return ResponseEntity.ok(config);
        } catch (Exception e) {
            RefreshConfig error = new RefreshConfig();
            error.setError(e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
