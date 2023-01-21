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

import com.onlyoffice.integration.documentserver.models.enums.DocumentType;
import com.onlyoffice.integration.documentserver.models.enums.Type;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Getter
@Setter
/* the file base parameters which include the platform type used,
 document display size (width and height) and type of the document opened */
public class FileModel {
    @Autowired
    private Document document;  // the parameters pertaining to the document (title, url, file type, etc.)
    private DocumentType documentType;  // the document type to be opened
    @Autowired
    private EditorConfig editorConfig;  /*  the parameters pertaining to the
     editor interface: opening mode (viewer or editor), interface language, additional buttons, etc. */
    private String token;  // the encrypted signature added to the Document Server config
    private Type type;  // the platform type used to access the document
}
