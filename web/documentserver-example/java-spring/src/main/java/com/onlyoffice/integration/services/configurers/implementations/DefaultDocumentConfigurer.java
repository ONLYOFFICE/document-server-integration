/**
 *
 * (c) Copyright Ascensio System SIA 2023
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

import com.google.gson.Gson;
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
import java.net.InetAddress;
import java.util.HashMap;

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

    public void configure(final Document document,
                          final DefaultDocumentWrapper wrapper) {  // define the document configurer
        String fileName = wrapper.getFileName();  // get the fileName parameter from the document wrapper
        Permission permission = wrapper.getPermission();  // get the permission parameter from the document wrapper

        document.setTitle(fileName);  // set the title to the document config

        // set the URL to download a file to the document config
        document.setUrl(documentManager.getDownloadUrl(fileName, true));
        document.setUrlUser(documentManager
                .getFileUri(fileName, false));  // set the file URL to the document config
        document.setDirectUrl(wrapper.getIsEnableDirectUrl() ? documentManager
                .getDownloadUrl(fileName, false) : "");
        document.setFileType(fileUtility.getFileExtension(fileName)
                .replace(".", ""));  // set the file type to the document config
        document.getInfo().setFavorite(wrapper.getFavorite());  // set the favorite parameter to the document config

        // get the document key
        String key = serviceConverter
                .generateRevisionId(storagePathBuilder.getStorageLocation()
                        + "/" + fileName + "/"
                        + new File(storagePathBuilder.getFileLocation(fileName)).lastModified());

        Gson gson = new Gson();
        HashMap<String, String> fileKey = new HashMap<>();
        fileKey.put("fileName", fileName);
        try {
            fileKey.put("userAddress", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        HashMap<String, Object> referenceData = new HashMap<>();
        referenceData.put("instanceId", storagePathBuilder.getServerUrl(true));
        referenceData.put("fileKey", gson.toJson(fileKey));

        document.setKey(key);  // set the key to the document config
        document.setPermissions(permission);  // set the permission parameters to the document config
        document.setReferenceData(referenceData);
    }
}
