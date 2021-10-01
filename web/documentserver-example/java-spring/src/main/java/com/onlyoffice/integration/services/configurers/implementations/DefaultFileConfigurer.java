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

package com.onlyoffice.integration.services.configurers.implementations;

import com.onlyoffice.integration.documentserver.managers.jwt.JwtManager;
import com.onlyoffice.integration.documentserver.models.enums.Action;
import com.onlyoffice.integration.mappers.Mapper;
import com.onlyoffice.integration.documentserver.models.enums.DocumentType;
import com.onlyoffice.integration.documentserver.models.filemodel.*;
import com.onlyoffice.integration.services.configurers.FileConfigurer;
import com.onlyoffice.integration.services.configurers.wrappers.DefaultDocumentWrapper;
import com.onlyoffice.integration.services.configurers.wrappers.DefaultFileWrapper;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Primary
public class DefaultFileConfigurer implements FileConfigurer<DefaultFileWrapper> {
    @Autowired
    private ObjectFactory<FileModel> fileModelObjectFactory;

    @Autowired
    private FileUtility fileUtility;

    @Autowired
    private JwtManager jwtManager;

    @Autowired
    private Mapper<com.onlyoffice.integration.entities.Permission, Permission> mapper;

    @Autowired
    private DefaultDocumentConfigurer defaultDocumentConfigurer;

    @Autowired
    private DefaultEditorConfigConfigurer defaultEditorConfigConfigurer;

    public void configure(FileModel fileModel, DefaultFileWrapper wrapper){
        if (fileModel != null){
            String fileName = wrapper.getFileName();
            Action action = wrapper.getAction();

            DocumentType documentType = fileUtility.getDocumentType(fileName);
            fileModel.setDocumentType(documentType);
            fileModel.setType(wrapper.getType());

            Permission userPermissions = mapper.toModel(wrapper.getUser().getPermissions());
            userPermissions.setComment(
                    !action.equals(Action.view)
                            && !action.equals(Action.fillForms)
                            && !action.equals(Action.embedded)
                            && !action.equals(Action.blockcontent)
            );

            DefaultDocumentWrapper documentWrapper = DefaultDocumentWrapper
                    .builder()
                    .fileName(fileName)
                    .permission(userPermissions)
                    .build();

            defaultDocumentConfigurer.configure(fileModel.getDocument(), documentWrapper);
            defaultEditorConfigConfigurer.configure(fileModel.getEditorConfig(), wrapper);

            Map<String, Object> map = new HashMap<>();
            map.put("type", fileModel.getType());
            map.put("documentType", documentType);
            map.put("document", fileModel.getDocument());
            map.put("editorConfig", fileModel.getEditorConfig());

            fileModel.setToken(jwtManager.createToken(map));
        }
    }

    @Override
    public FileModel getFileModel(DefaultFileWrapper wrapper) {
        FileModel fileModel = fileModelObjectFactory.getObject();
        configure(fileModel, wrapper);
        return fileModel;
    }
}
