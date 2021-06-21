package com.onlyoffice.integration.entities.configurations;

import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Scope("prototype")
public class Goback {

    @Autowired
    private DocumentManager documentManager;

    @Value("${url.index}")
    private String indexMapping;

    private String url;

    @PostConstruct
    private void init(){
        this.url = documentManager.getServerUrl(false)+indexMapping;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
