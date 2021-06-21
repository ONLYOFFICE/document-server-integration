package com.onlyoffice.integration.entities.filemodel;

import com.onlyoffice.integration.entities.enums.DocumentType;
import com.onlyoffice.integration.entities.enums.Type;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@Scope("prototype")
public class File {
    private Document document;
    private DocumentType documentType;
    private EditorConfig editorConfig;
    private String token;
    private Type type;

    @Autowired
    private DocumentManager documentManager;

    public void configure(Document doc, DocumentType documentType, EditorConfig config, Type type){
        this.document = doc;
        this.documentType = documentType;
        this.editorConfig = config;
        this.type = type;
    }

    //TODO: Implement
    public String[] GetHistory(){
        System.out.println("================DON'T FORGET TO IMPLEMENT ME============= \n");
        return new String[] { "", "" };
    }

    public void generateToken(){
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("documentType", documentType);
        map.put("document", document);
        map.put("editorConfig", editorConfig);

        token = documentManager.createToken(map);
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public EditorConfig getEditorConfig() {
        return editorConfig;
    }

    public void setEditorConfig(EditorConfig editorConfig) {
        this.editorConfig = editorConfig;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
