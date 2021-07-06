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

package com.onlyoffice.integration.services;

import com.onlyoffice.integration.Action;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.entities.enums.DocumentType;
import com.onlyoffice.integration.entities.enums.Language;
import com.onlyoffice.integration.entities.enums.Type;
import com.onlyoffice.integration.entities.filemodel.Document;
import com.onlyoffice.integration.entities.filemodel.EditorConfig;
import com.onlyoffice.integration.entities.filemodel.FileModel;
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

    public FileModel createConfiguration(User user, String fileName, String actionData,
                                         Action action, Language lang, Type type){
        DocumentType documentType = fileUtility.getDocumentType(fileName);

        Permission permissions = createPermissions(user);

        Document doc = context.getBean(Document.class);
        doc.configure(fileName, permissions,"uid-"+user.getId());

        EditorConfig config = this.createEditorConfig(user, fileName, actionData, action, lang, type);

        FileModel fileModel = context.getBean(FileModel.class);
        fileModel.configure(doc, documentType, config, type);
        return fileModel;
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
