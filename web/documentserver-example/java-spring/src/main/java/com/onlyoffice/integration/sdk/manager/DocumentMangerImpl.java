/**
 *
 * (c) Copyright Ascensio System SIA 2025
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

import com.onlyoffice.integration.documentserver.storage.FileStorageMutator;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.manager.document.DefaultDocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.onlyoffice.integration.documentserver.util.Constants.KILOBYTE_SIZE;
import static com.onlyoffice.integration.documentserver.util.Constants.MAX_KEY_LENGTH;

@Component
public class DocumentMangerImpl extends DefaultDocumentManager implements DocumentManager {

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;
    @Autowired
    private FileStorageMutator storageMutator;

    public DocumentMangerImpl(final SettingsManager settingsManager) {
        super(settingsManager);
    }

    @Override
    public String getDocumentKey(final String fileId, final boolean embedded) {
        String expectedKey = storagePathBuilder.getStorageLocation()
                + "/" + fileId + "/"
                + new File(storagePathBuilder.getFileLocation(fileId)).lastModified();

        String key = generateRevisionId(expectedKey);

        return embedded ? key + "_embedded" : key;
    }

    @Override
    public String getDocumentName(final String fileId) {
        return storagePathBuilder.getFileName(fileId);
    }

    @Override
    public String getCorrectName(final String fileName) {
        String baseName = getBaseName(fileName);  // get file name without extension
        String ext = getExtension(fileName);  // get file extension
        String name = baseName + "." + ext;  // create a full file name

        Path path = Paths.get(storagePathBuilder.getFileLocation(name));

        // run through all the files with such a name in the storage directory
        for (int i = 1; Files.exists(path); i++) {
            name = baseName + " (" + i + ")." + ext;  // and add an index to the base name
            path = Paths.get(storagePathBuilder.getFileLocation(name));
        }

        return name;
    }

    // get file information
    @Override
    public ArrayList<Map<String, Object>> getFilesInfo() {
        ArrayList<Map<String, Object>> files = new ArrayList<>();

        // run through all the stored files
        for (File file : storageMutator.getStoredFiles()) {
            Map<String, Object> map = new LinkedHashMap<>();  // write all the parameters to the map
            map.put("version", storagePathBuilder.getFileVersion(file.getName(), false));
            map.put("id", generateRevisionId(storagePathBuilder.getStorageLocation()
                            + "/" + file.getName() + "/"
                            + Paths.get(storagePathBuilder.getFileLocation(file.getName()))
                            .toFile()
                            .lastModified()));
            map.put("contentLength", new BigDecimal(String.valueOf((file.length() / Double.valueOf(KILOBYTE_SIZE))))
                    .setScale(2, RoundingMode.HALF_UP) + " KB");
            map.put("pureContentLength", file.length());
            map.put("title", file.getName());
            map.put("updated", String.valueOf(new Date(file.lastModified())));
            files.add(map);
        }

        return files;
    }

    // get file information by its ID
    @Override
    public ArrayList<Map<String, Object>> getFilesInfo(final String fileId) {
        ArrayList<Map<String, Object>> file = new ArrayList<>();

        for (Map<String, Object> map : getFilesInfo()) {
            if (map.get("id").equals(fileId)) {
                file.add(map);
                break;
            }
        }

        return file;
    }

    // create demo document
    @Override
    public String createDemo(
        final String fileExt,
        final Boolean sample,
        final String lang,
        final String uid,
        final String uname
    ) {
        String demoName = (sample ? "sample." : "new.")
                + fileExt;  // create sample or new template file with the necessary extension
        String langPath = lang.contains("-") ? lang : "default";
        String demoPath =
                "assets"
                        + File.separator
                        + "document-templates"
                        + File.separator
                        + (sample ? "sample" : ("new" + File.separator + langPath))
                        + File.separator
                        + demoName;

        // get a file name with an index if the file with such a name already exists
        String fileName = getCorrectName(demoName);

        InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(demoPath);  // get the input file stream

        if (stream == null) {
            return null;
        }

        storageMutator.createFile(Path.of(storagePathBuilder
                .getFileLocation(fileName)), stream);  // create a file in the specified directory
        storageMutator.createMeta(fileName, uid, uname);  // create meta information of the demo file

        return fileName;
    }

    // generate document key
    @Override
    public String generateRevisionId(final String expectedKey) {
        /* if the expected key length is greater than 20
        then he expected key is hashed and a fixed length value is stored in the string format */
        String formatKey = expectedKey.length() > MAX_KEY_LENGTH
                ? Integer.toString(expectedKey.hashCode()) : expectedKey;
        String key = formatKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), MAX_KEY_LENGTH));  // the resulting key length is 20 or less
    }
}
