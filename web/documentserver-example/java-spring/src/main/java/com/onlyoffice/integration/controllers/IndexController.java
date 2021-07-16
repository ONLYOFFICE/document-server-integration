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

import com.onlyoffice.integration.serializer.FilterState;
import com.onlyoffice.integration.entities.*;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.integration.util.documentManagers.DocumentManagerExts;
import com.onlyoffice.integration.util.fileUtilities.FileUtility;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class IndexController {

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private DocumentManagerExts documentManagerExts;
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

    @PostConstruct
    private void init(){
        List<String> description_user_0=List.of(
                "The name is requested when the editor is opened",
                "Doesn’t belong to any group",
                "Can review all the changes",
                "Can perform all actions with comments",
                "The file favorite state is undefined",
                "Can't mention others in comments",
                "Can't create new files from the editor"
        );
        List<String> description_user_1 = List.of(
                "File author by default",
                "He doesn’t belong to any of the groups",
                "He can review all the changes",
                "He can do everything with the comments",
                "The file favorite state is undefined",
                "Can create a file from a template with data from the editor"
        );
        List<String> description_user_2 = List.of(
                "He belongs to Group2",
                "He can review only his own changes or the changes made by the users who don’t belong to any of the groups",
                "He can view every comment, edit his comments and the comments left by the users who don't belong to any of the groups and remove only his comments",
                "This file is favorite",
                "Can create a file from an editor"
        );
        List<String> description_user_3 = List.of(
                "He belongs to Group3",
                "He can review only the changes made by the users from Group2",
                "He can view the comments left by the users from Group2 and Group3 and edit the comments left by the users from Group2",
                "This file isn’t favorite",
                "He can’t copy data from the file into the clipboard",
                "He can’t download the file",
                "He can’t print the file",
                "Can create a file from an editor"
        );
        userService.createUser("John Smith", "smith@mail.ru",
                description_user_1, null, List.of(FilterState.NULL.toString()),
                List.of(FilterState.NULL.toString()),
                List.of(FilterState.NULL.toString()),
                List.of(FilterState.NULL.toString()),true);
        userService.createUser("Mark Pottato", "pottato@mail.ru",
                description_user_2, "group-2", List.of("","group-2"), List.of(FilterState.NULL.toString()),
                List.of("group-2", ""), List.of("group-2"),false);
        userService.createUser("Hamish Mitchell", "mitchell@mail.ru",
                description_user_3, "group-3", List.of("group-2"), List.of("group-2", "group-3"),
                List.of("group-2"), new ArrayList<>(),false);
        userService.createUser("Anonymous",null,description_user_0,null,
                List.of(FilterState.NULL.toString()), List.of(FilterState.NULL.toString()), List.of(FilterState.NULL.toString()), List.of(FilterState.NULL.toString()),null);

    }

    @GetMapping("${url.index}")
    public String index(Model model){
        java.io.File[] files = documentManager.getStoredFiles(null);

        List<String> docTypes = Arrays.stream(files).map(
                file -> fileUtility.getDocumentType(file.getName()).toString().toLowerCase()
                ).collect(Collectors.toList());

        List<Boolean> filesEditable = Arrays.stream(files).map(
                file -> documentManagerExts.getEditedExts().contains(fileUtility.getFileExtension(file.getName()))
        ).collect(Collectors.toList());

        List<User> users = userService.findAll();
        String tooltip = users.stream().map(user -> user.getDescriptions()).collect(Collectors.joining());

        List<String> versions=new ArrayList<>();
        for(java.io.File file:files){
            versions.add(" ["+documentManager.getFileVersion(file.getName(),null)+"]");
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

        configuration.put("ConverExtList", String.join(",",documentManagerExts.getConvertExts()));
        configuration.put("EditedExtList", String.join(",",documentManagerExts.getEditedExts()));
        configuration.put("UrlConverter", urlConverter);
        configuration.put("UrlEditor", urlEditor);

        return configuration;
    }
}
