package com.onlyoffice.integration.services;

import com.onlyoffice.integration.Action;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.entities.enums.DocumentType;
import com.onlyoffice.integration.entities.enums.Language;
import com.onlyoffice.integration.entities.enums.Type;
import com.onlyoffice.integration.entities.filemodel.Document;
import com.onlyoffice.integration.entities.filemodel.EditorConfig;
import com.onlyoffice.integration.entities.filemodel.File;
import com.onlyoffice.integration.entities.filemodel.Permission;
import com.onlyoffice.integration.util.fileUtilities.FileUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class EditorServices {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private FileUtility fileUtility;

    public File createConfiguration(User user, String fileName, String actionData,
                                    Action action, Language lang, Type type){
        DocumentType documentType = fileUtility.getDocumentType(fileName);

        Permission permissions = createPermissions(user);

        Document doc = context.getBean(Document.class);
        doc.configure(fileName, permissions);

        EditorConfig config = this.createEditorConfig(user, fileName, actionData, action, lang, type);

        File file = context.getBean(File.class);
        file.configure(doc, documentType, config, type);
        return file;
    }

    private Permission createPermissions(User user){
        Permission permissions = context.getBean(Permission.class);
        permissions.configure(user);
        return permissions;
    }

    private EditorConfig createEditorConfig(User user, String fileName, String actionData, Action action, Language lang, Type type){
        EditorConfig config = context.getBean(EditorConfig.class);
        config.configure(user, fileName, actionData, action, lang, type);

        return config;
    }
}
