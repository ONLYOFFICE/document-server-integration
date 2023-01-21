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

import com.onlyoffice.integration.documentserver.models.configurations.Customization;
import com.onlyoffice.integration.documentserver.models.configurations.Embedded;
import com.onlyoffice.integration.documentserver.models.enums.Mode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
@Scope("prototype")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditorConfig {  /* the parameters pertaining to the editor interface: opening mode (viewer or editor),
 interface language, additional buttons, etc. */
    private HashMap<String, Object> actionLink = null;  /* the data which contains the information about the
    action in the document that will be scrolled to */
    private String callbackUrl;  // the absolute URL to the document storage service
    private HashMap<String, Object> coEditing = null;
    private String createUrl;  // the absolute URL of the document where it will be created and available after creation
    @Autowired
    private Customization customization;  /* the parameters which allow to customize the editor interface
     so that it looked like your other products (if there are any) and change the presence or absence
      of the additional buttons, links, change logos and editor owner details */
    @Autowired
    private Embedded embedded;  /* the parameters which allow to change the settings which define
     the behavior of the buttons in the embedded mode */
    private String lang;  // the editor interface language
    private Mode mode;  // the editor opening mode
    @Autowired
    private User user;  // the user currently viewing or editing the document
    private List<Template> templates;  /* the presence or absence
     of the templates in the <b>Create New...</b> menu option */
}
