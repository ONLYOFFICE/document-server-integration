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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.documentserver.managers.history.HistoryManager;
import com.onlyoffice.integration.documentserver.managers.jwt.JwtManager;
import com.onlyoffice.integration.documentserver.models.enums.Action;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.dto.Mentions;
import com.onlyoffice.integration.documentserver.models.enums.Language;
import com.onlyoffice.integration.documentserver.models.enums.Type;
import com.onlyoffice.integration.documentserver.models.filemodel.FileModel;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.integration.services.configurers.FileConfigurer;
import com.onlyoffice.integration.services.configurers.wrappers.DefaultFileWrapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.*;

@CrossOrigin("*")
@Controller
public class EditorController {

    @Value("${files.docservice.url.site}")
    private String docserviceSite;

    @Value("${files.docservice.url.api}")
    private String docserviceApiUrl;

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    @Autowired
    private JwtManager jwtManager;

    @Autowired
    private UserServices userService;

    @Autowired
    private HistoryManager historyManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileConfigurer<DefaultFileWrapper> fileConfigurer;

    @GetMapping(path = "${url.editor}")
    // process request to open the editor page
    public String index(@RequestParam("fileName") String fileName,
                        @RequestParam(value = "action", required = false) String actionParam,
                        @RequestParam(value = "type", required = false) String typeParam,
                        @RequestParam(value = "actionLink", required = false) String actionLink,
                        @CookieValue(value = "uid") String uid,
                        @CookieValue(value = "ulang") String lang,
                        Model model) throws JsonProcessingException {
        Action action = Action.edit;
        Type type = Type.desktop;
        Language language = Language.en;

        if(actionParam != null) action = Action.valueOf(actionParam);
        if(typeParam != null) type = Type.valueOf(typeParam);
        if(lang != null) language = Language.valueOf(lang);

        Optional<User> optionalUser = userService.findUserById(Integer.parseInt(uid));

        // if the user is not present, return the ONLYOFFICE start page
        if(!optionalUser.isPresent()) return "index.html";

        User user = optionalUser.get();

        // get file model with the default file parameters
        FileModel fileModel = fileConfigurer.getFileModel(
                DefaultFileWrapper
                        .builder()
                        .fileName(fileName)
                        .type(type)
                        .lang(language)
                        .action(action)
                        .user(user)
                        .actionData(actionLink)
                        .build()
        );

        // add attributes to the specified model
        model.addAttribute("model", fileModel);  // add file model with the default parameters to the original model
        model.addAttribute("fileHistory", historyManager.getHistory(fileModel.getDocument()));  // get file history and add it to the model
        model.addAttribute("docserviceApiUrl",docserviceSite + docserviceApiUrl);  // create the document service api URL and add it to the model
        model.addAttribute("dataInsertImage",  getInsertImage());  // get an image and add it to the model
        model.addAttribute("dataCompareFile",  getCompareFile());  // get a document for comparison and add it to the model
        model.addAttribute("dataMailMergeRecipients", getMailMerge());  // get recipients data for mail merging and add it to the model
        model.addAttribute("usersForMentions", getUserMentions(uid));  // get user data for mentions and add it to the model
        return "editor.html";
    }

    private List<Mentions> getUserMentions(String uid){  // get user data for mentions
        List<Mentions> usersForMentions=new ArrayList<>();
        if(uid!=null && !uid.equals("4")) {
            List<User> list = userService.findAll();
            for (User u : list) {
                if (u.getId()!=Integer.parseInt(uid) && u.getId()!=4) {
                    usersForMentions.add(new Mentions(u.getName(),u.getEmail()));  // user data includes user names and emails
                }
            }
        }

        return usersForMentions;
    }

    @SneakyThrows
    private String getInsertImage() {  // get an image that will be inserted into the document
        Map<String, Object> dataInsertImage = new HashMap<>();
        dataInsertImage.put("fileType", "png");
        dataInsertImage.put("url", storagePathBuilder.getServerUrl(true) + "/css/img/logo.png");

        // check if the document token is enabled
        if(jwtManager.tokenEnabled()){
            dataInsertImage.put("token", jwtManager.createToken(dataInsertImage));  // create token from the dataInsertImage object
        }

        return objectMapper.writeValueAsString(dataInsertImage).substring(1, objectMapper.writeValueAsString(dataInsertImage).length()-1);
    }

    @SneakyThrows
    private String getCompareFile(){  // get a document that will be compared with the current document
        Map<String, Object> dataCompareFile = new HashMap<>();
        dataCompareFile.put("fileType", "docx");
        dataCompareFile.put("url", storagePathBuilder.getServerUrl(true) + "/assets?name=sample.docx");

        // check if the document token is enabled
        if(jwtManager.tokenEnabled()){
            dataCompareFile.put("token", jwtManager.createToken(dataCompareFile));  // create token from the dataCompareFile object
        }

        return objectMapper.writeValueAsString(dataCompareFile);
    }

    @SneakyThrows
    private String getMailMerge(){
        Map<String, Object> dataMailMergeRecipients = new HashMap<>();  // get recipients data for mail merging
        dataMailMergeRecipients.put("fileType", "csv");
        dataMailMergeRecipients.put("url", storagePathBuilder.getServerUrl(true) + "/csv");

        // check if the document token is enabled
        if(jwtManager.tokenEnabled()){
            dataMailMergeRecipients.put("token", jwtManager.createToken(dataMailMergeRecipients));  // create token from the dataMailMergeRecipients object
        }

        return objectMapper.writeValueAsString(dataMailMergeRecipients);
    }
}
