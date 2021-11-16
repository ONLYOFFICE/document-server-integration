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

package com.onlyoffice.integration.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.documentserver.callbacks.CallbackHandler;
import com.onlyoffice.integration.documentserver.managers.jwt.JwtManager;
import com.onlyoffice.integration.documentserver.storage.FileStorageMutator;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.dto.Converter;
import com.onlyoffice.integration.dto.Track;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.documentserver.models.enums.DocumentType;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.documentserver.util.service.ServiceConverter;
import com.onlyoffice.integration.documentserver.managers.document.DocumentManager;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

@CrossOrigin("*")
@Controller
public class FileController {

    @Value("${files.docservice.header}")
    private String documentJwtHeader;

    @Value("${filesize-max}")
    private String filesizeMax;

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

    // create user metadata
    private String createUserMetadata(String uid, String fullFileName) {
        Optional<User> optionalUser = userService.findUserById(Integer.parseInt(uid));  // find a user by their ID
        String documentType = fileUtility.getDocumentType(fullFileName).toString().toLowerCase();  // get document type
        if(optionalUser.isPresent()){
            User user = optionalUser.get();
            storageMutator.createMeta(fullFileName,  // create meta information with the user ID and name specified
                    String.valueOf(user.getId()), user.getName());
        }
        return "{ \"filename\": \"" + fullFileName + "\", \"documentType\": \"" + documentType + "\" }";
    }

    // download data from the specified file
    private ResponseEntity<Resource> downloadFile(String fileName){
        Resource resource = storageMutator.loadFileAsResource(fileName);  // load the specified file as a resource
        String contentType = "application/octet-stream";

        // create a response with the content type, header and body with the file data
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/upload")
    @ResponseBody
    public String upload(@RequestParam("file") MultipartFile file,  // upload a file
                             @CookieValue("uid") String uid){
        try {
            String fullFileName = file.getOriginalFilename();  // get file name
            String fileExtension = fileUtility.getFileExtension(fullFileName);  // get file extension
            long fileSize = file.getSize();  // get file size
            byte[] bytes = file.getBytes();  // get file in bytes

            // check if the file size exceeds the maximum file size or is less than 0
            if(fileUtility.getMaxFileSize() < fileSize || fileSize <= 0){
                return "{ \"error\": \"File size is incorrect\"}";  // if so, write an error message to the response
            }

            // check if file extension is supported by the editor
            if(!fileUtility.getFileExts().contains(fileExtension)){
                return "{ \"error\": \"File type is not supported\"}";  // if not, write an error message to the response
            }

            String fileNamePath = storageMutator.updateFile(fullFileName, bytes);  // update a file
            if (fileNamePath.isBlank()){
                throw new IOException("Could not update a file");  // if the file cannot be updated, an error occurs
            }

            fullFileName = fileUtility.getFileNameWithoutExtension(fileNamePath) + fileExtension;  // get full file name

            return createUserMetadata(uid, fullFileName);  // create user metadata and return it
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{ \"error\": \"Something went wrong when uploading the file.\"}";  // if the operation of file uploading is unsuccessful, an error occurs
    }

    @PostMapping(path = "${url.converter}")
    @ResponseBody
    public String convert(@RequestBody Converter body,  // convert a file
                          @CookieValue("uid") String uid, @CookieValue("ulang") String lang){
        String fileName = body.getFileName();  // get file name
        String fileUri = documentManager.getDownloadUrl(fileName);  // get URL for downloading a file with the specified name
        String filePass = body.getFilePass() != null ? body.getFilePass() : null;  // get file password if it exists
        String fileExt = fileUtility.getFileExtension(fileName);  // get file extension
        DocumentType type = fileUtility.getDocumentType(fileName);  // get document type (word, cell or slide)
        String internalFileExt = fileUtility.getInternalExtension(type);  // get an editor internal extension (".docx", ".xlsx" or ".pptx")

        try{
            if(fileUtility.getConvertExts().contains(fileExt)){  // check if the file with such an extension can be converted
                String key = serviceConverter.generateRevisionId(fileUri);  // generate document key
                String newFileUri = serviceConverter  // get the URL to the converted file
                        .getConvertedUri(fileUri, fileExt, internalFileExt, key, filePass, true, lang);

                if(newFileUri.isEmpty()){
                    return "{ \"step\" : \"0\", \"filename\" : \"" + fileName + "\"}";
                }

                // get a file name of an internal file extension with an index if the file with such a name already exists
                String nameWithInternalExt = fileUtility.getFileNameWithoutExtension(fileName) + internalFileExt;
                String correctedName = documentManager.getCorrectName(nameWithInternalExt);

                URL url = new URL(newFileUri);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();  // get input stream of the converted file

                if (stream == null){
                    connection.disconnect();
                    throw new RuntimeException("Input stream is null");
                }

                // create the converted file with input stream
                storageMutator.createFile(Path.of(storagePathBuilder.getFileLocation(correctedName)), stream);
                fileName = correctedName;
            }

            // create meta information about the converted file with the user ID and name specified
            return createUserMetadata(uid, fileName);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return "{ \"error\": \"" + "The file can't be converted.\"}";  // if the operation of file converting is unsuccessful, an error occurs
    }

    @PostMapping("/delete")
    @ResponseBody
    public String delete(@RequestBody Converter body){  // delete a file
        try
        {
            String fullFileName = fileUtility.getFileName(body.getFileName());  // get full file name
            boolean fileSuccess = storageMutator.deleteFile(fullFileName);  // delete a file from the storage and return the status of this operation (true or false)
            boolean historySuccess = storageMutator.deleteFileHistory(fullFileName);  // delete file history and return the status of this operation (true or false)

            return "{ \"success\": \""+ (fileSuccess && historySuccess) +"\"}";
        }
        catch (Exception e)
        {
            return "{ \"error\": \"" + e.getMessage() + "\"}";  // if the operation of file deleting is unsuccessful, an error occurs
        }
    }

    @GetMapping(path = "${url.download}")
    public ResponseEntity<Resource> download(HttpServletRequest request,  // download a file
                                             @RequestParam("fileName") String fileName){
        try{
            // check if a token is enabled or not
            if(jwtManager.tokenEnabled()){
                String header = request.getHeader(documentJwtHeader == null  // get the document JWT header
                        || documentJwtHeader.isEmpty() ? "Authorization" : documentJwtHeader);
                if(header != null && !header.isEmpty()){
                    String token = header.replace("Bearer ", "");  // token is the header without the Bearer prefix
                    jwtManager.readToken(token);  // read the token
                }
            }
            return downloadFile(fileName);  // download data from the specified file
        } catch(Exception e){
            return null;
        }
    }

    @GetMapping("/create")
    public String create(@RequestParam("fileExt") String fileExt,  // create a sample file of the specified extension
                         @RequestParam(value = "sample", required = false) Optional<Boolean> isSample,
                         @CookieValue(value = "uid", required = false) String uid,
                         Model model){
        Boolean sampleData = (isSample.isPresent() && !isSample.isEmpty()) && isSample.get();  // specify if the sample data exists or not
        if(fileExt != null){
            try{
                Optional<User> user = userService.findUserById(Integer.parseInt(uid));  // find a user by their ID
                if (!user.isPresent()) throw new RuntimeException("Could not fine any user with id = "+uid);  // if the user with the specified ID doesn't exist, an error occurs
                String fileName = documentManager.createDemo(fileExt, sampleData, uid, user.get().getName());  // create a demo document with the sample data
                if (fileName.isBlank() || fileName == null) {
                    throw new RuntimeException("You must have forgotten to add asset files");
                }
                return "redirect:editor?fileName=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);  // redirect the request
            }catch (Exception ex){
                model.addAttribute("error", ex.getMessage());
                return "error.html";
            }
        }
        return "redirect:/";
    }

    @GetMapping("/assets")
    public ResponseEntity<Resource> assets(@RequestParam("name") String name)  // get sample files from the assests
    {
        String fileName = Path.of("assets", "sample", fileUtility.getFileName(name)).toString();
        return downloadFile(fileName);
    }

    @GetMapping("/csv")
    public ResponseEntity<Resource> csv()  // download a csv file
    {
        String fileName = Path.of("assets", "sample", "csv.csv").toString();
        return downloadFile(fileName);
    }

    @GetMapping("/files")
    @ResponseBody
    public ArrayList<Map<String, Object>> files(@RequestParam(value = "fileId", required = false) String fileId){  // get files information
        return fileId == null ? documentManager.getFilesInfo() : documentManager.getFilesInfo(fileId);
    }

    @PostMapping(path = "${url.track}")
    @ResponseBody
    public String track(HttpServletRequest request,  // track file changes
                        @RequestParam("fileName") String fileName,
                        @RequestParam("userAddress") String userAddress,
                        @RequestBody Track body){
        try {
            String bodyString = objectMapper.writeValueAsString(body);  // write the request body to the object mapper as a string
            String header = request.getHeader(documentJwtHeader == null  // get the request header
                    || documentJwtHeader.isEmpty() ? "Authorization" : documentJwtHeader);

            if (bodyString.isEmpty()) {  // if the request body is empty, an error occurs
                throw new RuntimeException("{\"error\":1,\"message\":\"Request payload is empty\"}");
            }

            JSONObject bodyCheck = jwtManager.parseBody(bodyString, header);  // parse the request body
            body = objectMapper.readValue(bodyCheck.toJSONString(), Track.class);  // read the request body
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

        int error = callbackHandler.handle(body, fileName);

        return"{\"error\":" + error + "}";
    }

    @PostMapping("/saveas")
    @ResponseBody
    public String saveAs(@RequestBody JSONObject body, @CookieValue("uid") String uid) {
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
}
