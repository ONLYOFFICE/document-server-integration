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

package com.onlyoffice.integration.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.onlyoffice.integration.documentserver.callbacks.CallbackHandler;
import com.onlyoffice.integration.documentserver.managers.jwt.JwtManager;
import com.onlyoffice.integration.documentserver.storage.FileStorageMutator;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.dto.Converter;
import com.onlyoffice.integration.dto.ConvertedData;
import com.onlyoffice.integration.dto.Track;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.documentserver.models.enums.DocumentType;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.documentserver.util.service.ServiceConverter;
import com.onlyoffice.integration.documentserver.managers.document.DocumentManager;
import com.onlyoffice.integration.documentserver.managers.callback.CallbackManager;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin("*")
@Controller
public class FileController {

    @Value("${files.docservice.header}")
    private String documentJwtHeader;

    @Value("${filesize-max}")
    private String defaultFilesizeMax;

    @Value("${files.docservice.url.command}")
    private String docserviceUrlCommand;

    @Autowired
    private FileUtility fileUtility;
    @Autowired
    private DocumentManager documentManager;
    @Autowired
    private JwtManager jwtManager;
    @Autowired
    private FileStorageMutator storageMutator;
    @Autowired
    private FileStoragePathBuilder storagePathBuilder;
    @Autowired
    private UserServices userService;
    @Autowired
    private CallbackHandler callbackHandler;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ServiceConverter serviceConverter;
    @Autowired
    private CallbackManager callbackManager;

    private String getFileSizeMax() {
        var customFileSizeMax = System.getenv("FILESIZE_MAX");
        if (customFileSizeMax == null) {
            return defaultFilesizeMax;
        }
        return customFileSizeMax;
    }

    // create user metadata
    private String createUserMetadata(final String uid, final String fullFileName) {
        Optional<User> optionalUser = userService.findUserById(Integer.parseInt(uid));  // find a user by their ID
        String documentType = fileUtility.getDocumentType(fullFileName).toString().toLowerCase();  // get document type
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            storageMutator.createMeta(fullFileName,  // create meta information with the user ID and name specified
                    String.valueOf(user.getId()), user.getName());
        }
        return "{ \"filename\": \"" + fullFileName + "\", \"documentType\": \"" + documentType + "\" }";
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
            String fileExtension = fileUtility.getFileExtension(fullFileName);  // get file extension
            long fileSize = file.getSize();  // get file size
            byte[] bytes = file.getBytes();  // get file in bytes

            // check if the file size exceeds the maximum file size or is less than 0
            if (fileUtility.getMaxFileSize() < fileSize || fileSize <= 0) {
                return "{ \"error\": \"File size is incorrect\"}";  // if so, write an error message to the response
            }

            // check if file extension is supported by the editor
            if (!fileUtility.getFileExts().contains(fileExtension)) {

                // if not, write an error message to the response
                return "{ \"error\": \"File type is not supported\"}";
            }

            String fileNamePath = storageMutator.updateFile(fullFileName, bytes);  // update a file
            if (fileNamePath.isBlank()) {
                throw new IOException("Could not update a file");  // if the file cannot be updated, an error occurs
            }

            fullFileName = fileUtility.getFileNameWithoutExtension(fileNamePath) + fileExtension;  // get full file name

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

        // get URL for downloading a file with the specified name
        String fileUri = documentManager.getDownloadUrl(fileName, true);

        // get file password if it exists
        String filePass = body.getFilePass() != null ? body.getFilePass() : null;

        // get file extension
        String fileExt = fileUtility.getFileExtension(fileName);

        // get document type (word, cell or slide)
        DocumentType type = fileUtility.getDocumentType(fileName);

        // convert to .ooxml
        String internalFileExt = "ooxml";

        try {
            // check if the file with such an extension can be converted
            if (fileUtility.getConvertExts().contains(fileExt)) {
                String key = serviceConverter.generateRevisionId(fileUri);  // generate document key
                ConvertedData response = serviceConverter  // get the URL to the converted file
                        .getConvertedData(fileUri, fileExt, internalFileExt, key, filePass, true, lang);

                String newFileUri = response.getUri();
                String newFileType = "." + response.getFileType();

                if (newFileUri.isEmpty()) {
                    return "{ \"step\" : \"0\", \"filename\" : \"" + fileName + "\"}";
                }

                /* get a file name of an internal file extension with an index if the file
                 with such a name already exists */
                String nameWithInternalExt = fileUtility.getFileNameWithoutExtension(fileName) + newFileType;
                String correctedName = documentManager.getCorrectName(nameWithInternalExt);

                URL url = new URL(newFileUri);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();  // get input stream of the converted file

                if (stream == null) {
                    connection.disconnect();
                    throw new RuntimeException("Input stream is null");
                }

                // create the converted file with input stream
                storageMutator.createFile(Path.of(storagePathBuilder.getFileLocation(correctedName)), stream);
                fileName = correctedName;
            }

            // create meta information about the converted file with the user ID and name specified
            return createUserMetadata(uid, fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // if the operation of file converting is unsuccessful, an error occurs
        return "{ \"error\": \"" + "The file can't be converted.\"}";
    }

    @PostMapping("/delete")
    @ResponseBody
    public String delete(@RequestBody final Converter body) {  // delete a file
        try {
            String fullFileName = fileUtility.getFileName(body.getFileName());  // get full file name

            // delete a file from the storage and return the status of this operation (true or false)
            boolean fileSuccess = storageMutator.deleteFile(fullFileName);

            // delete file history and return the status of this operation (true or false)
            boolean historySuccess = storageMutator.deleteFileHistory(fullFileName);

            return "{ \"success\": \"" + (fileSuccess && historySuccess) + "\"}";
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
            if (jwtManager.tokenEnabled() && jwtManager.tokenUseForRequest()) {
                String header = request.getHeader(documentJwtHeader == null  // get the document JWT header
                        || documentJwtHeader.isEmpty() ? "Authorization" : documentJwtHeader);
                if (header != null && !header.isEmpty()) {
                    String token = header
                            .replace("Bearer ", "");  // token is the header without the Bearer prefix
                    jwtManager.readToken(token);  // read the token
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
            if (jwtManager.tokenEnabled() && userAddress != null && jwtManager.tokenUseForRequest()) {
                String header = request.getHeader(documentJwtHeader == null // get the document JWT header
                        || documentJwtHeader.isEmpty() ? "Authorization" : documentJwtHeader);
                if (header != null && !header.isEmpty()) {
                    String token = header
                            .replace("Bearer ", "");  // token is the header without the Bearer prefix
                    jwtManager.readToken(token);  // read the token
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
                if (fileName.isBlank() || fileName == null) {
                    throw new RuntimeException("You must have forgotten to add asset files");
                }
                return "redirect:editor?fileName=" + URLEncoder
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
        String fileName = Path.of("assets", "sample", fileUtility.getFileName(name)).toString();
        return downloadFile(fileName);
    }

    @GetMapping("/csv")
    public ResponseEntity<Resource> csv() {  // download a csv file
        String fileName = Path.of("assets", "sample", "csv.csv").toString();
        return downloadFile(fileName);
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
                        @RequestBody final Track body) {
        Track track;
        try {
            String bodyString = objectMapper
                    .writeValueAsString(body);  // write the request body to the object mapper as a string
            String header = request.getHeader(documentJwtHeader == null  // get the request header
                    || documentJwtHeader.isEmpty() ? "Authorization" : documentJwtHeader);

            if (bodyString.isEmpty()) {  // if the request body is empty, an error occurs
                throw new RuntimeException("{\"error\":1,\"message\":\"Request payload is empty\"}");
            }

            JSONObject bodyCheck = jwtManager.parseBody(bodyString, header);  // parse the request body
            track = objectMapper.readValue(bodyCheck.toJSONString(), Track.class);  // read the request body
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

        int error = callbackHandler.handle(track, fileName);

        return "{\"error\":" + error + "}";
    }

    @PostMapping("/saveas")
    @ResponseBody
    public String saveAs(@RequestBody final JSONObject body, @CookieValue("uid") final String uid) {
        String title = (String) body.get("title");
        String saveAsFileUrl = (String) body.get("url");

        try {
            String fileName = documentManager.getCorrectName(title);
            String curExt = fileUtility.getFileExtension(fileName);

            if (!fileUtility.getFileExts().contains(curExt)) {
                return "{\"error\":\"File type is not supported\"}";
            }

            URL url = new URL(saveAsFileUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            InputStream stream = connection.getInputStream();

            String filesizeMax = getFileSizeMax();

            if (Integer.parseInt(filesizeMax) < stream.available() || stream.available() <= 0) {
                return "{\"error\":\"File size is incorrect\"}";
            }
            storageMutator.createFile(Path.of(storagePathBuilder.getFileLocation(fileName)), stream);
            createUserMetadata(uid, fileName);

            return "{\"file\":  \"" + fileName + "\"}";
        } catch (IOException e) {
            e.printStackTrace();
            return "{ \"error\" : 1, \"message\" : \"" + e.getMessage() + "\"}";
        }
    }

    @PostMapping("/rename")
    @ResponseBody
    public String rename(@RequestBody final JSONObject body) {
        String newfilename = (String) body.get("newfilename");
        String dockey = (String) body.get("dockey");
        String origExt = "." + (String) body.get("ext");
        String curExt = newfilename;

        if (newfilename.indexOf(".") != -1) {
            curExt = (String) fileUtility.getFileExtension(newfilename);
        }

        if (origExt.compareTo(curExt) != 0) {
            newfilename += origExt;
        }

        HashMap<String, String> meta = new HashMap<>();
        meta.put("title", newfilename);

        try {
            callbackManager.commandRequest("meta", dockey, meta);
            return "result ok";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    @PostMapping("/reference")
    @ResponseBody
    public String reference(@RequestBody final JSONObject body) {
        try {

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();

            String userAddress = "";
            String fileName = "";

            if (body.containsKey("referenceData")) {
                JSONObject referenceDataObj = (JSONObject) body.get("referenceData");
                String instanceId = (String) referenceDataObj.get("instanceId");

                if (instanceId.equals(storagePathBuilder.getServerUrl(false))) {
                    JSONObject fileKey = (JSONObject) referenceDataObj.get("fileKey");
                    userAddress = (String) fileKey.get("userAddress");
                    if (userAddress.equals(InetAddress.getLocalHost().getHostAddress())) {
                        fileName = (String) fileKey.get("fileName");
                    }
                }
            }


            if (fileName.equals("")) {
                try {
                    String path = (String) body.get("path");
                    path = fileUtility.getFileName(path);
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
            referenceData.put("fileKey", fileKey);

            HashMap<String, Object> data = new HashMap<>();
            data.put("fileType", fileUtility.getFileExtension(fileName));
            data.put("url", documentManager.getDownloadUrl(fileName, true));
            data.put("directUrl", documentManager.getDownloadUrl(fileName, true));
            data.put("referenceData", referenceData);
            data.put("path", fileName);

            if (jwtManager.tokenEnabled()) {
                String token = jwtManager.createToken(data);
                data.put("token", token);
            }
            return gson.toJson(data);
        } catch (Exception e) {
            e.printStackTrace();
            return "{ \"error\" : 1, \"message\" : \"" + e.getMessage() + "\"}";
        }
    }
}
