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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.documentserver.models.enums.Action;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.dto.Mentions;
import com.onlyoffice.integration.dto.Protect;
import com.onlyoffice.integration.dto.UserInfo;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.sdk.manager.UrlManager;
import com.onlyoffice.integration.sdk.service.ConfigService;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.Type;
import lombok.SneakyThrows;
import org.json.JSONObject;
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
    private SettingsManager settingsManager;

    @Autowired
    private ConfigService configService;

    @Autowired
    private UrlManager urlManager;

    @GetMapping(path = "${url.editor}")
    // process request to open the editor page
    public String index(@RequestParam("fileName") final String fileName,
                        @RequestParam(value = "action", required = false) final String actionParam,
                        @RequestParam(value = "type", required = false) final String typeParam,
                        @RequestParam(value = "actionLink", required = false) final String actionLink,
                        @CookieValue(value = "uid") final String uid,
                        @CookieValue(value = "ulang") final String lang,
                        final Model model) throws JsonProcessingException {
        Action action = null;
        Type type = Type.DESKTOP;
        Locale locale = new Locale("en");

        if (actionParam != null) {
            action = Action.valueOf(actionParam);
        }
        if (typeParam != null) {
            type = Type.valueOf(typeParam.toUpperCase());
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

        Config config = configService.createConfig(
                fileName,
                action,
                type
        );

        JSONObject actionData = null;

        if (actionLink != null && !actionLink.isEmpty()) {
            actionData = new JSONObject(actionLink);
        }

        config.getEditorConfig().setActionLink(actionData);
        config.getEditorConfig().setLang(locale.toLanguageTag());

        model.addAttribute("model", config);

        // create the document service api URL and add it to the model
        model.addAttribute("docserviceApiUrl", urlManager.getDocumentServerApiUrl());

        // get an image and add it to the model
        model.addAttribute("dataInsertImage",  getInsertImage());

        // get a document for comparison and add it to the model
        model.addAttribute("dataDocument",  getCompareFile());

        // get recipients data for mail merging and add it to the model
        model.addAttribute("dataSpreadsheet", getSpreadsheet());

        // get user data for mentions and add it to the model
        model.addAttribute("usersForMentions", getUserMentions(uid));

        model.addAttribute("usersInfo", getUsersInfo(uid));

        // get user data for protect and add it to the model
        model.addAttribute("usersForProtect", getUserProtect(uid));

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

    private List<UserInfo> getUsersInfo(final String uid) {  // get user data for mentions
        List<UserInfo> usersInfo = new ArrayList<>();
        if (uid != null && !uid.equals("4")) {
            List<User> list = userService.findAll();
            for (User u : list) {
                String image = u.getAvatar() ? storagePathBuilder.getServerUrl(true) + "/css/img/uid-"
                + u.getId() + ".png" : null;
                usersInfo.add(new UserInfo(u.getId(), u.getName(), u.getEmail(), image));
            }
        }
        return usersInfo;
    }

    private List<Protect> getUserProtect(final String uid) {  // get user data for protect
        List<Protect> usersForProtect = new ArrayList<>();
        if (uid != null && !uid.equals("4")) {
            List<User> list = userService.findAll();
            for (User u : list) {
                if (u.getId() != Integer.parseInt(uid) && u.getId() != ANONYMOUS_USER_ID) {

                    // user data includes user names, IDs and emails
                    usersForProtect.add(new Protect(u.getId(), u.getName(), u.getEmail()));
                }
            }
        }

        return usersForProtect;
    }


    @SneakyThrows
    private String getInsertImage() {  // get an image that will be inserted into the document
        Map<String, Object> dataInsertImage = new HashMap<>();
        Map<String, Object>[] images = new HashMap[1];
        images[0].put("fileType", "svg");
        images[0].put("url", storagePathBuilder.getServerUrl(true) + "/css/img/logo.svg");
        dataInsertImage.put("images", images);

        // check if the document token is enabled
        if (settingsManager.isSecurityEnabled()) {

            // create token from the dataInsertImage object
            dataInsertImage.put("token", jwtManager.createToken(dataInsertImage));
        }

        return objectMapper.writeValueAsString(dataInsertImage)
                .substring(1, objectMapper.writeValueAsString(dataInsertImage).length() - 1);
    }

    // get a document that will be compared with the current document
    @SneakyThrows
    private String getCompareFile() {
        Map<String, Object> dataDocument = new HashMap<>();
        dataDocument.put("fileType", "docx");
        dataDocument.put("url", storagePathBuilder.getServerUrl(true) + "/assets?name=sample.docx");

        // check if the document token is enabled
        if (settingsManager.isSecurityEnabled()) {

            // create token from the dataDocument object
            dataDocument.put("token", jwtManager.createToken(dataDocument));
        }

        return objectMapper.writeValueAsString(dataDocument);
    }

    @SneakyThrows
    private String getSpreadsheet() {
        Map<String, Object> dataSpreadsheet = new HashMap<>();  // get recipients data for mail merging
        dataSpreadsheet.put("fileType", "csv");
        dataSpreadsheet.put("url", storagePathBuilder.getServerUrl(true) + "/csv");

        // check if the document token is enabled
        if (settingsManager.isSecurityEnabled()) {

            // create token from the dataSpreadsheet object
            dataSpreadsheet.put("token", jwtManager.createToken(dataSpreadsheet));
        }

        return objectMapper.writeValueAsString(dataSpreadsheet);
    }
}
