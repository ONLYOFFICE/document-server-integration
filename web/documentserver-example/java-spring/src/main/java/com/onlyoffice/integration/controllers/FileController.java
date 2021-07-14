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

import com.onlyoffice.integration.controllers.objects.ConverterBody;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.entities.enums.DocumentType;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.integration.util.documentManagers.DocumentManagerExts;
import com.onlyoffice.integration.util.documentManagers.DocumentTokenManager;
import com.onlyoffice.integration.util.fileUtilities.FileUtility;
import com.onlyoffice.integration.util.objects.ActionObject;
import com.onlyoffice.integration.util.objects.TrackManagerRequestBody;
import com.onlyoffice.integration.util.serviceConverter.ServiceConverter;
import com.onlyoffice.integration.util.TrackManager;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import org.json.simple.JSONObject;
import org.primeframework.jwt.Verifier;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACVerifier;
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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class FileController {

    @Autowired
    private FileUtility fileUtility;

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private DocumentTokenManager documentTokenManager;

    @Autowired
    private DocumentManagerExts documentManagerExts;
    @Autowired
    private TrackManager trackManager;

    @Autowired
    private UserServices userService;

    @Autowired
    private ServiceConverter serviceConverter;

    @Value("${files.docservice.header}")
    private String documentJwtHeader;

    @Value("${files.docservice.secret}")
    private String tokenSecret;

    private String createUserMetadata(String uid, String fullFileName) throws IOException {
        Optional<User> optionalUser = userService.findUserById(Integer.parseInt(uid));
        String fileName = fileUtility.getFileNameWithoutExtension(fullFileName);
        String documentType = fileUtility.getDocumentType(fullFileName).toString().toLowerCase();
        if(optionalUser.isPresent()){
            User user = optionalUser.get();
            documentManager.createMeta(fileName,
                    String.valueOf(user.getId()), user.getName(), null);
        }
        return "{ \"filename\": \"" + fullFileName + "\", \"documentType\": \"" + documentType + "\" }";
    }

    private ResponseEntity<Resource> downloadFile(String fileName){
        String fileLocation = documentManager.forceSavePath(fileName, null, false);
        if (fileLocation.equals("")){
            fileLocation = documentManager.storagePath(fileName, null);
        }

        Resource resource = documentManager.loadFileAsResource(fileLocation);

        String contentType = null;
        if(contentType == null) {
        contentType = "application/octet-stream";
        }

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
            String directory = documentManager.filesRootPath(null);

            if(documentManager.getMaxFileSize() < fileSize || fileSize <= 0){
                return "{ \"error\": \"File size is incorrect\"}";
            }

            if(!documentManagerExts.getFileExts().contains(fileExtension)){
                return "{ \"error\": \"File type is not supported\"}";
            }

            Path path = fileUtility.generateFilepath(directory, fullFileName);
            String fileName = fileUtility.getFileNameWithoutExtension(path.getFileName().toString());

            Files.write(path, bytes);

            return createUserMetadata(uid, fileName+fileExtension);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{ \"error\": \"Something went wrong when uploading the file.\"}";
    }

    @PostMapping(path = "${url.converter}")
    @ResponseBody
    public String convert(@RequestBody ConverterBody body,
                          @CookieValue("uid") String uid){
        String fileName = body.getFileName();
        String fileUri = documentManager.getDownloadUrl(fileName);
        String filePass = body.getFilePass() != null ? body.getFilePass() : null;
        String fileExt = fileUtility.getFileExtension(fileName);
        DocumentType type = fileUtility.getDocumentType(fileName);
        String internalFileExt = fileUtility.getInternalExtension(type);

        try{
            if(documentManagerExts.getConvertExts().contains(fileExt)){
                String key = serviceConverter.generateRevisionId(fileUri);
                String newFileUri = serviceConverter
                        .getConvertedUri(fileUri, fileExt, internalFileExt, key, filePass, true);

                if(newFileUri.isEmpty()){
                    return "{ \"step\" : \"0\", \"filename\" : \"" + fileName + "\"}";
                }

                String correctedName = documentManager
                        .getCorrectName(fileUtility.getFileNameWithoutExtension(fileName)+internalFileExt, null);

                URL url = new URL(newFileUri);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();

                if (stream == null)
                {
                    throw new Exception("Stream is null");
                }

                File convertedFile = new File(documentManager.storagePath(correctedName, null));
                try (FileOutputStream out = new FileOutputStream(convertedFile))
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
    public String delete(@RequestBody ConverterBody body){
        try
        {
            String fullFileName = fileUtility.getFileName(body.getFileName());
            String fileName = fileUtility.getFileNameWithoutExtension(body.getFileName());
            String fileLocation = URLDecoder.decode(documentManager.storagePath(fullFileName, null));
            String historyLocation =  URLDecoder.decode(documentManager.historyDir(documentManager
                    .storagePath(fileName, null)));
            String convertedHistoryLocation =  URLDecoder.decode(documentManager.historyDir(documentManager
                    .storagePath(fullFileName, null)));

            documentManager.deleteFilesRecursively(Paths.get(fileLocation));
            documentManager.deleteFilesRecursively(Paths.get(historyLocation));
            documentManager.deleteFilesRecursively(Paths.get(convertedHistoryLocation));

            return "{ \"success\": true }";
        }
        catch (Exception e)
        {
            return "{ \"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("fileName") String fileName,
                                             HttpServletRequest request){
        try{
            if(documentTokenManager.tokenEnabled()){
                String header = request.getHeader(documentJwtHeader == null
                        || documentJwtHeader.isEmpty() ? "Authorization" : documentJwtHeader);

                if(header != null && !header.isEmpty()){
                    String token = header.startsWith("Bearer ") ? header.substring(7) : header;
                    try {
                        Verifier verifier = HMACVerifier.newVerifier(tokenSecret);
                        JWT.getDecoder().decode(token, verifier);
                    } catch (Exception e) {
                        return null;
                    }
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
                         @CookieValue(value = "uid", required = false) String uid, Model model){
        Boolean sampleData=(isSample.isPresent() && !isSample.isEmpty()) && isSample.get();
        if(fileExt!=null){
            try{
                Optional<User> user = userService.findUserById(Integer.parseInt(uid));
                String uname = user.get().getName();
                String fileName = documentManager.createDemo(fileExt, sampleData, uid, uname);
                return "redirect:editor?fileName=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            }catch (Exception ex){
                model.addAttribute("error",ex.getMessage());
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
        ArrayList<Map<String, Object>> files = null;

        if(fileId == null){
            files = documentManager.getFilesInfo();
        } else {
            files = documentManager.getFilesInfo(fileId);
        }

        return files;
    }

    @PostMapping("/track")
    @ResponseBody
    public String track(@RequestParam("fileName") String fileName,
                        @RequestParam("userAddress") String userAddress,
                        @RequestBody TrackManagerRequestBody body){

        try {
            JSONObject bodyCheck = trackManager.readBody(body);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

        int status = body.getStatus();
        int saved = 0;

        if (status == 1) { //Editing
            List<ActionObject> actions =  body.getActions();
            List<String> users =  body.getUsers();
            ActionObject action =  actions.get(0);
            if (actions != null && action.getType().equals("0")) { //finished edit
                String user =  action.getUserid();
                if (users.indexOf(user) == -1) {
                    String key = body.getKey();
                    try {
                        trackManager.commandRequest("forcesave", key);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        fileName = fileUtility.getFileName(fileName);

        if (status == 2 || status == 3) { //MustSave, Corrupted
            try {
                trackManager.processSave(body, fileName, userAddress);
            } catch (Exception ex) {
                ex.printStackTrace();
                saved = 1;
            }

        }

        if (status == 6 || status == 7) { //MustForceSave, CorruptedForceSave
            try {
                trackManager.processForceSave(body, fileName, userAddress);
            } catch (Exception ex) {
                ex.printStackTrace();
                saved = 1;
            }
        }

        return"{\"error\":" + saved + "}";
    }
}
