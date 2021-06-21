package com.onlyoffice.integration.entities.configurations;

import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Scope("prototype")
public class Customization {

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${url.index}")
    private String indexUrl;

    private Boolean autosave = true;
    private Boolean chat = true;
    private Boolean comments = true;
    private Boolean compactHeader = false;
    private Boolean compactToolbar = false;
    private Boolean compatibleFeatures = false;
    private Boolean forcesave = false;
    private Goback goback;
    private Boolean help = true;
    private Boolean hideRightMenu = false;
    private Boolean hideRulers = false;
    private Logo logo;
    private Boolean submitForm = false;

    @PostConstruct
    public void init(){
        this.goback = applicationContext.getBean(Goback.class);
        this.logo = applicationContext.getBean(Logo.class);
        this.goback.setUrl(documentManager.getServerUrl(false)+indexUrl);
    }

    public Boolean getAutosave() {
        return autosave;
    }

    public void setAutosave(Boolean autosave) {
        this.autosave = autosave;
    }

    public Boolean getChat() {
        return chat;
    }

    public void setChat(Boolean chat) {
        this.chat = chat;
    }

    public Boolean getComments() {
        return comments;
    }

    public void setComments(Boolean comments) {
        this.comments = comments;
    }

    public Boolean getCompactHeader() {
        return compactHeader;
    }

    public void setCompactHeader(Boolean compactHeader) {
        this.compactHeader = compactHeader;
    }

    public Boolean getCompactToolbar() {
        return compactToolbar;
    }

    public void setCompactToolbar(Boolean compactToolbar) {
        this.compactToolbar = compactToolbar;
    }

    public Boolean getCompatibleFeatures() {
        return compatibleFeatures;
    }

    public void setCompatibleFeatures(Boolean compatibleFeatures) {
        this.compatibleFeatures = compatibleFeatures;
    }

    public Boolean getForcesave() {
        return forcesave;
    }

    public void setForcesave(Boolean forcesave) {
        this.forcesave = forcesave;
    }

    public Goback getGoback() {
        return goback;
    }

    public void setGoback(Goback goback) {
        this.goback = goback;
    }

    public Boolean getHelp() {
        return help;
    }

    public void setHelp(Boolean help) {
        this.help = help;
    }

    public Boolean getHideRightMenu() {
        return hideRightMenu;
    }

    public void setHideRightMenu(Boolean hideRightMenu) {
        this.hideRightMenu = hideRightMenu;
    }

    public Boolean getHideRulers() {
        return hideRulers;
    }

    public void setHideRulers(Boolean hideRulers) {
        this.hideRulers = hideRulers;
    }

    public Boolean getSubmitForm() {
        return submitForm;
    }

    public void setSubmitForm(Boolean submitForm) {
        this.submitForm = submitForm;
    }

    public Logo getLogo() {
        return logo;
    }

    public void setLogo(Logo logo) {
        this.logo = logo;
    }
}
