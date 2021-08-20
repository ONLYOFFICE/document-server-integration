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

package com.onlyoffice.integration.documentserver.models.filemodel;

import com.onlyoffice.integration.documentserver.models.configurations.Customization;
import com.onlyoffice.integration.documentserver.models.configurations.Embedded;
import com.onlyoffice.integration.documentserver.models.enums.Language;
import com.onlyoffice.integration.documentserver.models.enums.Mode;
import lombok.*;
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
public class EditorConfig {
    private HashMap<String, Object> actionLink = null;
    private String callbackUrl;
    private String createUrl;
    @Autowired
    private Customization customization;
    @Autowired
    private Embedded embedded;
    private Language lang;
    private Mode mode;
    @Autowired
    private User user;
    private List<Template> templates;
}
