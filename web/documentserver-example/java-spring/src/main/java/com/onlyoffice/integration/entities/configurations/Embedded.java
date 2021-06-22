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

package com.onlyoffice.integration.entities.configurations;

import com.onlyoffice.integration.entities.enums.ToolbarDocked;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class Embedded {
    private String embedUrl;
    private String saveUrl;
    private String shareUrl;
    private ToolbarDocked toolbarDocked;

    public void configure(String embedUrl, String saveUrl, String shareUrl, ToolbarDocked toolbarDocked){
        this.embedUrl = embedUrl;
        this.saveUrl = saveUrl;
        this.shareUrl = shareUrl;
        this.toolbarDocked = toolbarDocked;
    }

    public String getEmbedUrl() {
        return embedUrl.toString();
    }

    public void setEmbedUrl(String embedUrl) {
        this.embedUrl = embedUrl;
    }

    public String getSaveUrl() {
        return saveUrl;
    }

    public void setSaveUrl(String saveUrl) {
        this.saveUrl = saveUrl;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public ToolbarDocked getToolbarDocked() {
        return toolbarDocked;
    }

    public void setToolbarDocked(ToolbarDocked toolbarDocked) {
        this.toolbarDocked = toolbarDocked;
    }
}
