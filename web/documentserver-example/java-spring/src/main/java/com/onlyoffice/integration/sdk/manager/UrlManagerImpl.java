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

package com.onlyoffice.integration.sdk.manager;

import com.onlyoffice.integration.documentserver.managers.document.DocumentManager;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.DefaultUrlManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlManagerImpl extends DefaultUrlManager {
    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    @Value("${url.index}")
    private String indexMapping;

    @Autowired
    private DocumentManager documentManager;

    public UrlManagerImpl(final SettingsManager settingsManager) {
        super(settingsManager);
    }

    @Override
    public String getFileUrl(final String fileId) {
        return documentManager.getDownloadUrl(fileId, true);
    }

    @Override
    public String getDirectFileUrl(final String fileId) {
        return documentManager.getDownloadUrl(fileId, false);
    }

    public String getCreateUrl(final String fileId) {
        return documentManager.getCreateUrl(fileId, false);
    }

    @Override
    public String getCallbackUrl(final String fileId) {
        return documentManager.getCallback(fileId);
    }

    @Override
    public String getGobackUrl(final String fileId) {
        return storagePathBuilder.getServerUrl(false) + indexMapping;
    }
}
