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

import com.onlyoffice.integration.documentserver.models.filemodel.Document;
import com.onlyoffice.integration.documentserver.models.filemodel.Permission;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.services.configurers.DocumentConfigurer;
import com.onlyoffice.integration.services.configurers.wrappers.DefaultDocumentWrapper;
import com.onlyoffice.integration.documentserver.managers.document.DocumentManager;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.documentserver.util.service.ServiceConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Random;

@Service
@Primary
public class DefaultDocumentConfigurer implements DocumentConfigurer<DefaultDocumentWrapper> {

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    @Autowired
    private FileUtility fileUtility;

    @Autowired
    private ServiceConverter serviceConverter;

    private Random rnd = new Random();

    public void configure(Document document, DefaultDocumentWrapper wrapper){
        String fileName = wrapper.getFileName();
        Permission permission = wrapper.getPermission();

        document.setTitle(fileName);
        document.setUrl(documentManager.getDownloadUrl(fileName));
        document.setUrlUser(documentManager.getFileUri(fileName, false));
        document.setFileType(fileUtility.getInternalExtension(fileUtility.getDocumentType(fileName)).replace(".",""));
        document.getInfo().setFavorite(rnd.nextBoolean());

        String key =  serviceConverter.
                        generateRevisionId(storagePathBuilder.getStorageLocation()
                        + "/" + fileName + "/"
                        + new File(storagePathBuilder.getFileLocation(fileName)).lastModified());

        document.setKey(key);
        document.setPermissions(permission);
    }
}
