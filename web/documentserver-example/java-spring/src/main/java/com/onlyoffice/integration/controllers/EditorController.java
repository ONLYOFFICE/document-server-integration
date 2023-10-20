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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.documentserver.managers.jwt.JwtManager;
import com.onlyoffice.integration.documentserver.models.enums.Action;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.dto.Mentions;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.onlyoffice.integration.documentserver.util.Constants.ANONYMOUS_USER_ID;

@CrossOrigin("*")
@Controller
public class EditorController {

    @Value("${files.docservice.url.site}")
    private String docserviceSite;

    @Value("${files.docservice.url.api}")
    private String docserviceApiUrl;

    @Value("${files.docservice.languages}")
    private String langs;

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    @Autowired
    private JwtManager jwtManager;

    @Autowired
    private UserServices userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileConfigurer<DefaultFileWrapper> fileConfigurer;

    @GetMapping(path = "${url.editor}")
    // process request to open the editor page
    public String index(@RequestParam("fileName") final String fileName,
                        @RequestParam(value = "action", required = false) final String actionParam,
                        @RequestParam(value = "type", required = false) final String typeParam,
                        @RequestParam(value = "actionLink", required = false) final String actionLink,
                        @RequestParam(value = "directUrl", required = false,
                                defaultValue = "false") final Boolean directUrl,
                        @CookieValue(value = "uid") final String uid,
                        @CookieValue(value = "ulang") final String lang,
                        final Model model) throws JsonProcessingException {
        Action action = Action.edit;
        Type type = Type.desktop;
        Locale locale = new Locale("en");

        if (actionParam != null) {
            action = Action.valueOf(actionParam);
        }
        if (typeParam != null) {
            type = Type.valueOf(typeParam);
        }

        List<String> langsAndKeys = Arrays.asList(langs.split("\\|"));
        for (String langAndKey : langsAndKeys) {
            String[] couple = langAndKey.split(":");
            if (couple[0].equals(lang)) {
                String[] langAndCountry = couple[0].split("-");
                locale = new Locale(langAndCountry[0], langAndCountry.length > 1 ? langAndCountry[1] : "");
            }
        }

        Optional<User> optionalUser = userService.findUserById(Integer.parseInt(uid));

        // if the user is not present, return the ONLYOFFICE start page
        if (!optionalUser.isPresent()) {
            return "index.html";
        }

        User user = optionalUser.get();

        // get file model with the default file parameters
        FileModel fileModel = fileConfigurer.getFileModel(
                DefaultFileWrapper
                        .builder()
                        .fileName(fileName)
                        .type(type)
                        .lang(locale.toLanguageTag())
                        .action(action)
                        .user(user)
                        .actionData(actionLink)
                        .isEnableDirectUrl(directUrl)
                        .build()
        );

        // add attributes to the specified model
        // add file model with the default parameters to the original model
        model.addAttribute("model", fileModel);

        // create the document service api URL and add it to the model
        model.addAttribute("docserviceApiUrl", docserviceSite + docserviceApiUrl);

        // get an image and add it to the model
        model.addAttribute("dataInsertImage",  getInsertImage(directUrl));

        // get a document for comparison and add it to the model
        model.addAttribute("dataDocument",  getCompareFile(directUrl));

        // get recipients data for mail merging and add it to the model
        model.addAttribute("dataSpreadsheet", getSpreadsheet(directUrl));

        // get user data for mentions and add it to the model
        model.addAttribute("usersForMentions", getUserMentions(uid));
        return "editor.html";
    }

    private List<Mentions> getUserMentions(final String uid) {  // get user data for mentions
        List<Mentions> usersForMentions = new ArrayList<>();
        if (uid != null && !uid.equals("4")) {
            List<User> list = userService.findAll();
            for (User u : list) {
                if (u.getId() != Integer.parseInt(uid) && u.getId() != ANONYMOUS_USER_ID) {

                    // user data includes user names and emails
                    usersForMentions.add(new Mentions(u.getName(), u.getEmail()));
                }
            }
        }

        return usersForMentions;
    }

    @SneakyThrows
    private String getInsertImage(final Boolean directUrl) {  // get an image that will be inserted into the document
        Map<String, Object> dataInsertImage = new HashMap<>();
        dataInsertImage.put("fileType", "png");
        dataInsertImage.put("url", storagePathBuilder.getServerUrl(true) + "/css/img/logo.png");
        if (directUrl) {
            dataInsertImage.put("directUrl", storagePathBuilder
                    .getServerUrl(false) + "/css/img/logo.png");
        }

        // check if the document token is enabled
        if (jwtManager.tokenEnabled()) {

            // create token from the dataInsertImage object
            dataInsertImage.put("token", jwtManager.createToken(dataInsertImage));
        }

        return objectMapper.writeValueAsString(dataInsertImage)
                .substring(1, objectMapper.writeValueAsString(dataInsertImage).length() - 1);
    }

    // get a document that will be compared with the current document
    @SneakyThrows
    private String getCompareFile(final Boolean directUrl) {
        Map<String, Object> dataDocument = new HashMap<>();
        dataDocument.put("fileType", "docx");
        dataDocument.put("url", storagePathBuilder.getServerUrl(true) + "/assets?name=sample.docx");
        if (directUrl) {
            dataDocument.put("directUrl", storagePathBuilder
                    .getServerUrl(false) + "/assets?name=sample.docx");
        }

        // check if the document token is enabled
        if (jwtManager.tokenEnabled()) {

            // create token from the dataDocument object
            dataDocument.put("token", jwtManager.createToken(dataDocument));
        }

        return objectMapper.writeValueAsString(dataDocument);
    }

    @SneakyThrows
    private String getSpreadsheet(final Boolean directUrl) {
        Map<String, Object> dataSpreadsheet = new HashMap<>();  // get recipients data for mail merging
        dataSpreadsheet.put("fileType", "csv");
        dataSpreadsheet.put("url", storagePathBuilder.getServerUrl(true) + "/csv");
        if (directUrl) {
            dataSpreadsheet.put("directUrl", storagePathBuilder.getServerUrl(false) + "/csv");
        }

        // check if the document token is enabled
        if (jwtManager.tokenEnabled()) {

            // create token from the dataSpreadsheet object
            dataSpreadsheet.put("token", jwtManager.createToken(dataSpreadsheet));
        }

        return objectMapper.writeValueAsString(dataSpreadsheet);
    }
}
