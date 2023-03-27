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

package com.onlyoffice.integration.documentserver.models.filemodel;

import com.onlyoffice.integration.documentserver.models.configurations.Info;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@Scope("prototype")
@Getter
@Setter
public class Document {  // the parameters pertaining to the document (title, url, file type, etc.)
    @Autowired
    private Info info;  /* additional parameters for the document (document owner, folder where the document is stored,
     uploading date, sharing settings) */
    @Autowired
    private Permission permissions;  // the permission for the document to be edited and downloaded or not
    private String fileType;  //  the file type for the source viewed or edited document
    private String key;  // the unique document identifier used by the service to recognize the document
    private String urlUser;  /* the absolute URL that will allow the document to be saved
    onto the user personal computer */
    private String title;  /* the desired file name for the viewed or edited document which will also be used
    as file name when the document is downloaded */
    private String url;  // the absolute URL where the source viewed or edited document is stored
    private String directUrl;
    private HashMap<String, Object> referenceData;
}
