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

import com.onlyoffice.integration.documentserver.storage.FileStorageMutator;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.documentserver.util.Misc;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.sdk.manager.DocumentManager;
import com.onlyoffice.integration.sdk.manager.UrlManager;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.integration.dto.FormatsList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin("*")
@Controller
public class IndexController {

    @Autowired
    private FileStorageMutator storageMutator;

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    @Autowired
    private Misc mistUtility;

    @Autowired
    private UserServices userService;

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private UrlManager urlManager;

    @Value("${url.converter}")
    private String urlConverter;

    @Value("${url.editor}")
    private String urlEditor;

    @Value("${files.docservice.languages}")
    private String langs;

    @Value("${server.version}")
    private String serverVersion;

    @Value("${enable-forgotten}")
    private String enableForgotten;

    @GetMapping("${url.index}")
    public String index(final Model model) {
        java.io.File[] files = storageMutator.getStoredFiles();  // get all the stored files from the storage
        List<String> docTypes = new ArrayList<>();
        List<Boolean> filesEditable = new ArrayList<>();
        List<String> versions = new ArrayList<>();
        List<Boolean> isFillFormDoc = new ArrayList<>();
        List<String> langsAndKeys = Arrays.asList(langs.split("\\|"));

        Map<String, String> languages = new LinkedHashMap<>();

        langsAndKeys.forEach((str) -> {
            String[] couple = str.split(":");
            languages.put(couple[0], couple[1]);
        });

        List<User> users = userService.findAll();  // get a list of all the users

        String tooltip = users.stream()  // get the tooltip with the user descriptions
                .map(user -> mistUtility.convertUserDescriptions(user.getName(),
                        user.getDescriptions()))  // convert user descriptions to the specified format
                .collect(Collectors.joining());

        for (java.io.File file:files) {  // run through all the files
            String fileName = file.getName();  // get file name
            docTypes.add(documentManager
                    .getDocumentType(fileName)
                    .toString()
                    .toLowerCase());  // add a document type of each file to the list
            filesEditable.add(documentManager.isEditable(fileName));  // specify if a file is editable or not
            versions.add(" [" + storagePathBuilder.
                    getFileVersion(fileName, true) + "]");  // add a file version to the list
            isFillFormDoc.add(documentManager.isFillable(fileName));
        }

        // add all the parameters to the model
        model.addAttribute("isFillFormDoc", isFillFormDoc);
        model.addAttribute("versions", versions);
        model.addAttribute("files", files);
        model.addAttribute("docTypes", docTypes);
        model.addAttribute("filesEditable", filesEditable);
        model.addAttribute("datadocs", urlManager.getDocumentServerPreloaderApiUrl());
        model.addAttribute("tooltip", tooltip);
        model.addAttribute("users", users);
        model.addAttribute("languages", languages);
        model.addAttribute("serverVersion", serverVersion);
        model.addAttribute("enableForgotten", Boolean.valueOf(enableForgotten));

        return "index.html";
    }

    @PostMapping("/config")
    @ResponseBody
    public HashMap<String, String> configParameters() {  // get configuration parameters
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("UrlConverter", urlConverter);
        configuration.put("UrlEditor", urlEditor);

        return configuration;
    }

    @GetMapping("/formats")
    @ResponseBody
    public ResponseEntity<FormatsList> formats() {  // return all the supported formats
        FormatsList list = new FormatsList(documentManager.getFormats());
        return ResponseEntity.ok(list);
    }
}
