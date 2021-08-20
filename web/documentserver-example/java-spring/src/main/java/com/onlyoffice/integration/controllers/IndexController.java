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

import com.onlyoffice.integration.documentserver.storage.IntegrationStorage;
import com.onlyoffice.integration.entities.*;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class IndexController {

    @Autowired
    private IntegrationStorage storage;

    @Autowired
    private FileUtility fileUtility;

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

    @GetMapping("${url.index}")
    public String index(Model model){
        java.io.File[] files = storage.getStoredFiles();
        List<String> docTypes = new ArrayList<>();
        List<Boolean> filesEditable = new ArrayList<>();
        List<String> versions = new ArrayList<>();

        List<User> users = userService.findAll();
        String tooltip = users.stream().map(user -> user.getDescriptions()).collect(Collectors.joining());

        for(java.io.File file:files){
            String fileName = file.getName();
            docTypes.add(fileUtility.getDocumentType(fileName).toString().toLowerCase());
            filesEditable.add(fileUtility.getEditedExts().contains(fileUtility.getFileExtension(fileName)));
            versions.add(" ["+storage.getFileVersion(fileName)+"]");
        }

        model.addAttribute("versions",versions);
        model.addAttribute("files", files);
        model.addAttribute("docTypes", docTypes);
        model.addAttribute("filesEditable", filesEditable);
        model.addAttribute("datadocs", docserviceSite+docservicePreloader);
        model.addAttribute("tooltip", tooltip);
        model.addAttribute("users", users);

        return "index.html";
    }

    @PostMapping("/config")
    @ResponseBody
    public HashMap<String, String> configParameters(){
        HashMap<String, String> configuration = new HashMap<>();

        configuration.put("ConverExtList", String.join(",",fileUtility.getConvertExts()));
        configuration.put("EditedExtList", String.join(",",fileUtility.getEditedExts()));
        configuration.put("UrlConverter", urlConverter);
        configuration.put("UrlEditor", urlEditor);

        return configuration;
    }
}
