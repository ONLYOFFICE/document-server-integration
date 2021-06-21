package com.onlyoffice.integration.entities.filemodel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.onlyoffice.integration.Action;
import com.onlyoffice.integration.entities.configurations.Customization;
import com.onlyoffice.integration.entities.configurations.Embedded;
import com.onlyoffice.integration.entities.enums.Language;
import com.onlyoffice.integration.entities.enums.Mode;
import com.onlyoffice.integration.entities.enums.ToolbarDocked;
import com.onlyoffice.integration.entities.enums.Type;
import com.onlyoffice.integration.util.fileUtilities.FileUtility;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@Scope("prototype")
public class EditorConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private FileUtility fileUtility;

    private HashMap<String, Object> actionLink = null;
    private String callbackUrl;
    private Customization customization;
    private Embedded embedded;
    private Language lang;
    private Mode mode;
    private User user;

    //TODO: Refactor
    public void configure(com.onlyoffice.integration.entities.User user,
                          String fileName,
                          String actionData,
                          Action action,
                          Language lang,
                          Type type){
        if (actionData != null) {
            Gson gson = new Gson();
            this.actionLink = gson.fromJson(actionData, new TypeToken<HashMap<String, Object>>() { }.getType());
        }
        this.callbackUrl = documentManager.getCallback(fileName);
        this.lang = lang;
        Boolean canEdit = documentManager.getEditedExts().contains(fileUtility.getFileExtension(fileName));
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
}
