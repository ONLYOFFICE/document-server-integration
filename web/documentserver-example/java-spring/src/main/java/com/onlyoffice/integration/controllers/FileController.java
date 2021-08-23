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
import com.onlyoffice.integration.documentserver.storage.IntegrationStorage;
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
import java.util.*;

@CrossOrigin("*")
@Controller
public class FileController {

    @Value("${files.docservice.header}")
    private String documentJwtHeader;

    @Autowired
    private FileUtility fileUtility;
    @Autowired
    private DocumentManager documentManager;
    @Autowired
    private JwtManager jwtManager;
    @Autowired
    private IntegrationStorage storage;
    @Autowired
    private UserServices userService;
    @Autowired
    private CallbackHandler callbackHandler;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ServiceConverter serviceConverter;

    private String createUserMetadata(String uid, String fullFileName) {
        Optional<User> optionalUser = userService.findUserById(Integer.parseInt(uid));
        String fileName = fileUtility.getFileNameWithoutExtension(fullFileName);
        String documentType = fileUtility.getDocumentType(fullFileName).toString().toLowerCase();
        if(optionalUser.isPresent()){
            User user = optionalUser.get();
            storage.createMeta(fileName,
                    String.valueOf(user.getId()), user.getName());
        }
        return "{ \"filename\": \"" + fullFileName + "\", \"documentType\": \"" + documentType + "\" }";
    }

    private ResponseEntity<Resource> downloadFile(String fileName){
        Resource resource = storage.loadFileAsResource(fileName);
        String contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/upload")
    @ResponseBody
    public String upload(@RequestParam("file") MultipartFile file,
                             @CookieValue("uid") String uid){
        try {
            String fullFileName = file.getOriginalFilename();
            String fileExtension = fileUtility.getFileExtension(fullFileName);
            long fileSize = file.getSize();
            byte[] bytes = file.getBytes();

            if(fileUtility.getMaxFileSize() < fileSize || fileSize <= 0){
                return "{ \"error\": \"File size is incorrect\"}";
            }

            if(!fileUtility.getFileExts().contains(fileExtension)){
                return "{ \"error\": \"File type is not supported\"}";
            }

            String fileNamePath = storage.updateFile(fullFileName, bytes);
            if (fileNamePath.isBlank()){
                throw new IOException("Could not update a file");
            }

            String fileName = fileUtility.getFileNameWithoutExtension(fileNamePath);

            return createUserMetadata(uid, fileName+fileExtension);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{ \"error\": \"Something went wrong when uploading the file.\"}";
    }

    @PostMapping(path = "${url.converter}")
    @ResponseBody
    public String convert(@RequestBody Converter body,
                          @CookieValue("uid") String uid){
        String fileName = body.getFileName();
        String fileUri = documentManager.getDownloadUrl(fileName);
        String filePass = body.getFilePass() != null ? body.getFilePass() : null;
        String fileExt = fileUtility.getFileExtension(fileName);
        DocumentType type = fileUtility.getDocumentType(fileName);
        String internalFileExt = fileUtility.getInternalExtension(type);

        try{
            if(fileUtility.getConvertExts().contains(fileExt)){
                String key = serviceConverter.generateRevisionId(fileUri);
                String newFileUri = serviceConverter
                        .getConvertedUri(fileUri, fileExt, internalFileExt, key, filePass, true);

                if(newFileUri.isEmpty()){
                    return "{ \"step\" : \"0\", \"filename\" : \"" + fileName + "\"}";
                }

                String nameWithInternalExt = fileUtility.getFileNameWithoutExtension(fileName) + internalFileExt;
                String correctedName = documentManager.getCorrectName(nameWithInternalExt);

                URL url = new URL(newFileUri);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();

                if (stream == null){
                    connection.disconnect();
                    throw new RuntimeException("Input stream is null");
                }

                storage.createFile(correctedName, stream);
                fileName = correctedName;
            }

            return createUserMetadata(uid, fileName+fileExt);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return "{ \"error\": \"" + "The file can't be converted.\"}";
    }

    @PostMapping("/delete")
    @ResponseBody
    public String delete(@RequestBody Converter body){
        try
        {
            String fullFileName = fileUtility.getFileName(body.getFileName());
            boolean success = storage.deleteFile(fullFileName);

            return "{ \"success\": \""+ success +"\"}";
        }
        catch (Exception e)
        {
            return "{ \"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping(path = "${url.download}")
    public ResponseEntity<Resource> download(@RequestParam("fileName") String fileName,
                                             HttpServletRequest request){
        try{
            if(jwtManager.tokenEnabled()){
                String header = request.getHeader(documentJwtHeader == null
                        || documentJwtHeader.isEmpty() ? "Authorization" : documentJwtHeader);
                if(header != null && !header.isEmpty()){
                    String token = header.startsWith("Bearer ") ? header.substring(7) : header;
                    jwtManager.readToken(token);
                }
            }
            return downloadFile(fileName);
        } catch(Exception e){
            return null;
        }
    }

    @GetMapping("/create")
    public String create(@RequestParam("fileExt") String fileExt,
                         @RequestParam(value = "sample", required = false) Optional<Boolean> isSample,
                         @CookieValue(value = "uid", required = false) String uid,
                         Model model){
        Boolean sampleData=(isSample.isPresent() && !isSample.isEmpty()) && isSample.get();
        if(fileExt!=null){
            try{
                Optional<User> user = userService.findUserById(Integer.parseInt(uid));
                String uname = user.get().getName();
                String fileName = documentManager.createDemo(fileExt, sampleData, uid, uname);
                return "redirect:editor?fileName=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            }catch (Exception ex){
                model.addAttribute("error", ex.getMessage());
                return "error.html";
            }
        }
        return "redirect:/";
    }

    @GetMapping("/assets")
    public ResponseEntity<Resource> assets(@RequestParam("name") String name)
    {
        String fileName = "assets/sample/" + fileUtility.getFileName(name);
        return downloadFile(fileName);
    }

    @GetMapping("/csv")
    public ResponseEntity<Resource> csv()
    {
        String fileName = "assets/sample/csv.csv";
        return downloadFile(fileName);
    }

    @GetMapping("/files")
    @ResponseBody
    public ArrayList<Map<String, Object>> files(@RequestParam(value = "fileId", required = false) String fileId){
        ArrayList<Map<String, Object>> files;

        if(fileId == null){
            files = documentManager.getFilesInfo();
        } else {
            files = documentManager.getFilesInfo(fileId);
        }

        return files;
    }

    @PostMapping(path = "${url.track}")
    @ResponseBody
    public String track(HttpServletRequest request,
                        @RequestParam("fileName") String fileName,
                        @RequestParam("userAddress") String userAddress,
                        @RequestBody Track body){
        try {
            String bodyString = objectMapper.writeValueAsString(body);
            String header = request.getHeader(documentJwtHeader == null
                    || documentJwtHeader.isEmpty() ? "Authorization" : documentJwtHeader);

            if (bodyString.isEmpty()) {
                throw new RuntimeException("{\"error\":1,\"message\":\"Request payload is empty\"}");
            }

            JSONObject bodyCheck = jwtManager.parseBody(bodyString, header);
            body = objectMapper.readValue(bodyCheck.toJSONString(), Track.class);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

        int error = callbackHandler.handle(body, fileName);

        return"{\"error\":" + error + "}";
    }
}
