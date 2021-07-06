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
import com.onlyoffice.integration.Action;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.controllers.objects.UserForMention;
import com.onlyoffice.integration.entities.enums.Language;
import com.onlyoffice.integration.entities.enums.Type;
import com.onlyoffice.integration.entities.filemodel.FileModel;
import com.onlyoffice.integration.services.EditorServices;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import com.onlyoffice.integration.util.documentManagers.DocumentTokenManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.*;

@Controller
public class EditorController {

    @Value("${files.docservice.url.site}")
    private String docserviceSite;

    @Value("${files.docservice.url.api}")
    private String docserviceApiUrl;

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private DocumentTokenManager documentTokenManager;

    @Autowired
    private UserServices userService;

    @Autowired
    private EditorServices editorService;

    @GetMapping("/editor")
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

        if(!optionalUser.isPresent()) return "index.html";

        User user = optionalUser.get();

        FileModel fileModel = editorService.createConfiguration(user, fileName, actionLink, action, language, type);

        Map<String, Object> dataInsertImage = new HashMap<>();
        dataInsertImage.put("fileType", "png");
        dataInsertImage.put("url", documentManager.getServerUrl(true) + "/css/img/logo.png");

        Map<String, Object> dataCompareFile = new HashMap<>();
        dataCompareFile.put("fileType", "docx");
        dataCompareFile.put("url", documentManager.getServerUrl(true) + "/assets?name=sample.docx");

        Map<String, Object> dataMailMergeRecipients = new HashMap<>();
        dataMailMergeRecipients.put("fileType", "csv");
        dataMailMergeRecipients.put("url", documentManager.getServerUrl(true) + "/csv");

        List<UserForMention> usersForMentions=new ArrayList<>();
        if(uid!=null && !uid.equals("uid-0")) {
            List<User> list = userService.findAll();
            for (User u : list) {
                if (u.getName()!=null &&u.getEmail()!=null && "uid-"+u.getId()!=uid) {
                    usersForMentions.add(new UserForMention(u.getName(), u.getEmail()));
                }
            }
        }

        if(documentTokenManager.tokenEnabled()){
            fileModel.generateToken();
            dataInsertImage.put("token", documentTokenManager.createToken(dataInsertImage));
            dataCompareFile.put("token", documentTokenManager.createToken(dataInsertImage));
            dataMailMergeRecipients.put("token", documentTokenManager.createToken(dataMailMergeRecipients));
        }

        ObjectMapper objectMapper=new ObjectMapper();

        model.addAttribute("model", fileModel);
        model.addAttribute("docserviceApiUrl",docserviceSite + docserviceApiUrl);
        model.addAttribute("dataInsertImage",  objectMapper.writeValueAsString(dataInsertImage).substring(1, objectMapper.writeValueAsString(dataInsertImage).length()-1));
        model.addAttribute("dataCompareFile",  objectMapper.writeValueAsString(dataCompareFile));
        model.addAttribute("dataMailMergeRecipients", objectMapper.writeValueAsString(dataMailMergeRecipients));
        model.addAttribute("usersForMentions", usersForMentions);
        return "editor.html";
    }
}
