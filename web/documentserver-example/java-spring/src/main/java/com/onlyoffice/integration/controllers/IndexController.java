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

import com.onlyoffice.integration.documentserver.storage.FileStorageMutator;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.documentserver.util.Misc;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    private FileUtility fileUtility;

    @Autowired
    private Misc mistUtility;

    @Autowired
    private UserServices userService;

    @Value("${files.docservice.url.site}")
    private String docserviceSite;

    @Value("${files.docservice.url.preloader}")
    private String docservicePreloader;

    @Value("${url.converter}")
    private String urlConverter;

    @Value("${url.editor}")
    private String urlEditor;

    @Value("${files.docservice.languages}")
    private String langs;

    @GetMapping("${url.index}")
    public String index(@RequestParam(value = "directUrl", required = false) final Boolean directUrl,
                        final Model model) {
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
            docTypes.add(fileUtility
                    .getDocumentType(fileName)
                    .toString()
                    .toLowerCase());  // add a document type of each file to the list
            filesEditable.add(fileUtility.getEditedExts()
                    .contains(fileUtility.getFileExtension(fileName)));  // specify if a file is editable or not
            versions.add(" [" + storagePathBuilder.
                    getFileVersion(fileName, true) + "]");  // add a file version to the list
            isFillFormDoc.add(fileUtility.getFillExts().contains(fileUtility.getFileExtension(fileName)));
        }

        // add all the parameters to the model
        model.addAttribute("isFillFormDoc", isFillFormDoc);
        model.addAttribute("versions", versions);
        model.addAttribute("files", files);
        model.addAttribute("docTypes", docTypes);
        model.addAttribute("filesEditable", filesEditable);
        model.addAttribute("datadocs", docserviceSite + docservicePreloader);
        model.addAttribute("tooltip", tooltip);
        model.addAttribute("users", users);
        model.addAttribute("languages", languages);
        model.addAttribute("directUrl", directUrl);

        return "index.html";
    }

    @PostMapping("/config")
    @ResponseBody
    public HashMap<String, String> configParameters() {  // get configuration parameters
        HashMap<String, String> configuration = new HashMap<>();

        configuration.put("FillExtList", String.join(",", fileUtility
                .getFillExts()));  // put a list of the extensions that can be filled to config
        configuration.put("ConverExtList", String.join(",", fileUtility
                .getConvertExts()));  // put a list of the extensions that can be converted to config
        configuration.put("EditedExtList", String.join(",", fileUtility
                .getEditedExts()));  // put a list of the extensions that can be edited to config
        configuration.put("UrlConverter", urlConverter);
        configuration.put("UrlEditor", urlEditor);

        return configuration;
    }
}
