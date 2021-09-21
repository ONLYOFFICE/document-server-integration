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

package com.onlyoffice.integration.documentserver.managers.template;

import com.onlyoffice.integration.documentserver.models.enums.DocumentType;
import com.onlyoffice.integration.documentserver.models.filemodel.Template;
import com.onlyoffice.integration.documentserver.managers.document.DocumentManager;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Qualifier("sample")
public class SampleTemplateManager implements TemplateManager {
    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    @Autowired
    private FileUtility fileUtility;

    public List<Template> createTemplates(String fileName){
        List<Template> templates = List.of(
                new Template("", "Blank", documentManager.getCreateUrl(fileName, false)),
                new Template(getTemplateImageUrl(fileName), "With sample content", documentManager.getCreateUrl(fileName, true))
        );

        return templates;
    }

    public String getTemplateImageUrl(String fileName){
        DocumentType fileType = fileUtility.getDocumentType(fileName);
        String path = storagePathBuilder.getServerUrl(true);
        if(fileType.equals(DocumentType.word)){
            return path + "/css/img/file_docx.svg";
        } else if(fileType.equals(DocumentType.slide)){
            return path + "/css/img/file_pptx.svg";
        } else if(fileType.equals(DocumentType.cell)){
            return path + "/css/img/file_xlsx.svg";
        }
        return path + "/css/img/file_docx.svg";
    }
}
