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

import com.onlyoffice.integration.documentserver.models.configurations.Embedded;
import com.onlyoffice.integration.documentserver.models.enums.ToolbarDocked;
import com.onlyoffice.integration.documentserver.models.enums.Type;
import com.onlyoffice.integration.services.configurers.EmbeddedConfigurer;
import com.onlyoffice.integration.services.configurers.wrappers.DefaultEmbeddedWrapper;
import com.onlyoffice.integration.documentserver.managers.document.DocumentManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class DefaultEmbeddedConfigurer implements EmbeddedConfigurer<DefaultEmbeddedWrapper> {

    @Autowired
    private DocumentManager documentManager;

    public void configure(Embedded embedded, DefaultEmbeddedWrapper wrapper){
        if(wrapper.getType().equals(Type.embedded)) {
            String url = documentManager.getFileUri(wrapper.getFileName(), false);
            embedded.setEmbedUrl(url);
            embedded.setSaveUrl(url);
            embedded.setShareUrl(url);
            embedded.setToolbarDocked(ToolbarDocked.top);
        };
    }
}
