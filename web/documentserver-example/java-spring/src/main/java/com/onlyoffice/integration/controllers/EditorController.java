package com.onlyoffice.integration.controllers;

import com.google.gson.Gson;
import com.onlyoffice.integration.Action;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.entities.enums.Language;
import com.onlyoffice.integration.entities.enums.Type;
import com.onlyoffice.integration.entities.filemodel.File;
import com.onlyoffice.integration.services.EditorServices;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
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
                        Model model){
        Action action = Action.edit;
        Type type = Type.desktop;
        Language language = Language.en;

        if(actionParam != null) action = Action.valueOf(actionParam);
        if(typeParam != null) type = Type.valueOf(typeParam);
        if(lang != null) language = Language.valueOf(lang);

        Optional<User> optionalUser = userService.findUserById(Integer.parseInt(uid));

        if(!optionalUser.isPresent()) return "index.html";

        User user = optionalUser.get();

        File file = editorService.createConfiguration(user, fileName, actionLink, action, language, type);

        Map<String, Object> dataInsertImage = new HashMap<>();
        dataInsertImage.put("fileType", "png");
        dataInsertImage.put("url", documentManager.getServerUrl(true) + "/css/img/logo.png");

        Map<String, Object> dataCompareFile = new HashMap<>();
        dataCompareFile.put("fileType", "docx");
        dataCompareFile.put("url", documentManager.getServerUrl(true) + "/assets?name=sample.docx");

        Map<String, Object> dataMailMergeRecipients = new HashMap<>();
        dataMailMergeRecipients.put("fileType", "csv");
        dataMailMergeRecipients.put("url", documentManager.getServerUrl(true) + "/csv");

        //TODO: Implementation
        List<Map<String, Object>> usersForMentions = new ArrayList<>();

        if(documentManager.tokenEnabled()){
            file.generateToken();
            dataInsertImage.put("token", documentManager.createToken(dataInsertImage));
            dataCompareFile.put("token", documentManager.createToken(dataInsertImage));
            dataMailMergeRecipients.put("token", documentManager.createToken(dataMailMergeRecipients));
        }

        //TODO: Get rid of GSON
        Gson gson = new Gson();

        model.addAttribute("model", file);
        model.addAttribute("docserviceApiUrl",docserviceSite + docserviceApiUrl);
        model.addAttribute("dataInsertImage",  gson.toJson(dataInsertImage).substring(1, gson.toJson(dataInsertImage).length()-1));
        model.addAttribute("dataCompareFile",  gson.toJson(dataCompareFile));
        model.addAttribute("dataMailMergeRecipients", gson.toJson(dataMailMergeRecipients));
        model.addAttribute("usersForMentions", usersForMentions);

        return "editor.html";
    }
}
