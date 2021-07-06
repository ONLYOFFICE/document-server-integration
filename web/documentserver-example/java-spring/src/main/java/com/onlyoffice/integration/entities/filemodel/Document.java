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

import com.onlyoffice.integration.entities.configurations.Info;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import com.onlyoffice.integration.util.fileUtilities.FileUtility;
import com.onlyoffice.integration.util.serviceConverter.ServiceConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.io.File;

@Component
@Scope("prototype")
public class Document {

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private FileUtility fileUtility;

    @Autowired
    private ServiceConverter serviceConverter;

    private String fileType;
    private Info info;
    private String key;
    private String urlUser;
    private String title;
    private String url;
    private Permission permissions;

    public void configure(String fileName, Permission permissions,String uid){
        this.title = fileName;
        this.url = documentManager.getDownloadUrl(fileName);
        this.urlUser = documentManager.getFileUri(fileName, false);
        this.fileType = fileUtility.getInternalExtension(fileUtility.getDocumentType(fileName)).replace(".","");
        this.key = serviceConverter.
                generateRevisionId(documentManager.curUserHostAddress(null)
                        + "/" + fileName + "/"
                        + Long.toString(new File(documentManager.storagePath(fileName, null)).lastModified()));
        this.info = applicationContext.getBean(Info.class);
        this.permissions = permissions;
        switch (uid){
            case "uid-0":{
                this.info.setFavorite(null);
            }
            case "uid-1":{
                this.info.setFavorite(null);
            }
            case "uid-2":{
                this.info.setFavorite(true);
            }
            case "uid-3":{
                this.info.setFavorite(false);
            }
        }
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrlUser() {
        return urlUser;
    }

    public void setUrlUser(String urlUser) {
        this.urlUser = urlUser;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Permission getPermissions() {
        return permissions;
    }

    public void setPermissions(Permission permissions) {
        this.permissions = permissions;
    }
}
