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

package com.onlyoffice.integration.entities.filemodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.onlyoffice.integration.Action;
import com.onlyoffice.integration.entities.configurations.Customization;
import com.onlyoffice.integration.entities.configurations.Embedded;
import com.onlyoffice.integration.entities.enums.Language;
import com.onlyoffice.integration.entities.enums.Mode;
import com.onlyoffice.integration.entities.enums.ToolbarDocked;
import com.onlyoffice.integration.entities.enums.Type;
import com.onlyoffice.integration.util.documentManagers.DocumentManagerExts;
import com.onlyoffice.integration.util.fileUtilities.FileUtility;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import com.onlyoffice.integration.util.objects.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@Scope("prototype")
public class EditorConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private DocumentManagerExts documentManagerExts;
    @Autowired
    private FileUtility fileUtility;

    private HashMap<String, Object> actionLink = null;
    private String callbackUrl;
    private String createUrl;
    private Customization customization;
    private Embedded embedded;
    private Language lang;
    private Mode mode;
    private User user;
    private List<Template> templates;

    public void configure(com.onlyoffice.integration.entities.User user,
                          String fileName,
                          String actionData,
                          Action action,
                          Language lang,
                          Type type){
        if (actionData != null) {
            ObjectMapper om=new ObjectMapper();
            try {
                this.actionLink = om.readValue(actionData, (JavaType) new TypeToken<HashMap<String, Object>>() { }.getType());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        this.setTemplatesData(fileName);
        this.callbackUrl = documentManager.getCallback(fileName);
        this.createUrl = documentManager.getCreateUrl(fileName, false);
        this.lang = lang;
        Boolean canEdit = documentManagerExts.getEditedExts().contains(fileUtility.getFileExtension(fileName));
        this.customization = applicationContext.getBean(Customization.class);
        this.customization.setSubmitForm(canEdit && (action.equals(Action.edit) || action.equals(Action.fillForms)));
        this.mode = canEdit && !action.equals(Action.view) ? Mode.edit : Mode.view;

        User userModel = applicationContext.getBean(User.class);
        userModel.configure(user.getId(), user.getName(), user.getGroup() != null ? user.getGroup().getName() : null);

        this.user = userModel;

        if(type.equals(Type.embedded)) this.initDesktop(documentManager.getFileUri(fileName, false));
    }

    private void initDesktop(String url){
        Embedded embedded = applicationContext.getBean(Embedded.class);
        embedded.configure(url,url,url, ToolbarDocked.top);
        this.embedded = embedded;
    }

    private void setTemplatesData(String fileName){
        this.templates = new ArrayList<>();
        templates.add(new Template("", "Blank", documentManager.getCreateUrl(fileName, false)));
        templates.add(new Template(documentManager.getTemplateImageUrl(fileName), "With sample content", documentManager.getCreateUrl(fileName, true)));
    }

    public HashMap<String, Object> getActionLink() {
        return actionLink;
    }

    public void setActionLink(HashMap<String, Object> actionLink) {
        this.actionLink = actionLink;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public Customization getCustomization() {
        return customization;
    }

    public void setCustomization(Customization customization) {
        this.customization = customization;
    }

    public Embedded getEmbedded() {
        return embedded;
    }

    public void setEmbedded(Embedded embedded) {
        this.embedded = embedded;
    }

    public Language getLang() {
        return lang;
    }

    public void setLang(Language lang) {
        this.lang = lang;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCreateUrl() {
        return createUrl;
    }

    public void setCreateUrl(String createUrl) {
        this.createUrl = createUrl;
    }

    public List<Template> getTemplates(){
        return templates;
    }

    public void setTemplates(List<Template> templates){
        this.templates = templates;
    }
}
