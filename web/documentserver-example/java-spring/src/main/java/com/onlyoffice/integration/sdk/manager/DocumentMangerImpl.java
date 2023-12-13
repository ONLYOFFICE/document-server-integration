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

import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.manager.document.DefaultDocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.onlyoffice.integration.documentserver.util.Constants.MAX_KEY_LENGTH;

@Component
public class DocumentMangerImpl extends DefaultDocumentManager {

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    public DocumentMangerImpl(final SettingsManager settingsManager) {
        super(settingsManager);
    }

    @Override
    public String getDocumentKey(final String fileId, final boolean embedded) {
        String expectedKey = storagePathBuilder.getStorageLocation()
                + "/" + fileId + "/"
                + new File(storagePathBuilder.getFileLocation(fileId)).lastModified();

        String formatKey = expectedKey.length() > MAX_KEY_LENGTH
                ? Integer.toString(expectedKey.hashCode()) : expectedKey;
        String key = formatKey.replace("[^0-9-.a-zA-Z_=]", "_");
        key = key.substring(0, Math.min(key.length(), MAX_KEY_LENGTH));

        return embedded ? key + "_embedded" : key;
    }

    @Override
    public String getDocumentName(final String fileId) {
       File file = new File(storagePathBuilder.getFileLocation(fileId));

       if (file.exists()) {
           return file.getName();
       }

       return null;
    }
}
