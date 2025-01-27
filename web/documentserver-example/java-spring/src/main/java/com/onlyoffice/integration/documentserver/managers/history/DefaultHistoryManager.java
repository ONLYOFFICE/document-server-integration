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

package com.onlyoffice.integration.documentserver.managers.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.sdk.manager.DocumentManager;
import com.onlyoffice.integration.sdk.manager.UrlManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.HistoryData;
import com.onlyoffice.model.documenteditor.callback.History;
import com.onlyoffice.model.documenteditor.history.Version;
import com.onlyoffice.model.documenteditor.historydata.Previous;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.nio.file.Paths;

// todo: Rebuild completely
@Component
public class DefaultHistoryManager implements HistoryManager {

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    @Autowired
    private JwtManager jwtManager;

    @Autowired
    private JSONParser parser;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SettingsManager settingsManager;

    @Autowired
    private UrlManager urlManager;

    @Autowired
    private DocumentManager documentManager;

    // todo: Refactoring
    @SneakyThrows
    public String getHistory(final String fileName) {  // get document history

        // get history directory
        String histDir = storagePathBuilder.getHistoryDir(storagePathBuilder.getFileLocation(fileName));
        Integer curVer = storagePathBuilder.getFileVersion(histDir, false);  // get current file version

        if (curVer > 0) {  // check if the current file version is greater than 0
            List<Version> history = new ArrayList<>();

            for (Integer i = 1; i <= curVer; i++) {  // run through all the file versions
                String verDir = versionDir(histDir, i, true);  // get the path to the given file version

                String key;
                if (i == curVer) {
                    key = documentManager
                            .generateRevisionId(storagePathBuilder.getStorageLocation()
                                    + "/" + fileName + "/"
                                    + new File(storagePathBuilder.getFileLocation(fileName)).lastModified());
                } else {
                    key = readFileToEnd(new File(verDir + File.separator + "key.txt"));
                }

                Version version = Version.builder()
                        .key(key)
                        .version(String.valueOf(i))
                        .build();

                if (i == 1) {  // check if the version number is equal to 1
                    String createdInfo = readFileToEnd(new File(histDir
                            + File.separator + "createdInfo.json"));  // get file with meta data
                    JSONObject json = (JSONObject) parser.parse(createdInfo);  // and turn it into json object

                    // write meta information to the object (user information and creation date)
                    version.setCreated(String.valueOf(json.get("created")));
                    version.setUser(User.builder()
                            .id(String.valueOf(json.get("id")))
                            .name(String.valueOf(json.get("name")))
                            .build()
                    );
                }

                if (i > 1) {  //check if the version number is greater than 1
                    // if so, get the path to the changes.json file
                    InputStream changesSteam = new FileInputStream(
                            versionDir(histDir, i - 1, true) + File.separator + "changes.json");

                    History changes = objectMapper.readValue(changesSteam, History.class);

                    List<Object> historyChanges = changes.getChanges();
                    Map<String, Object> historyChange = objectMapper.convertValue(historyChanges.get(0), Map.class);

                    // write information about changes to the object
                    version.setChanges(changes.getChanges());
                    version.setServerVersion(changes.getServerVersion());
                    version.setCreated((String) historyChange.get("created"));
                    version.setUser(objectMapper.convertValue(historyChange.get("user"), User.class));
                }

                history.add(version);
            }

            // write history information about the current file version to the history object
            Map<String, Object> histObj = new HashMap<String, Object>();
            histObj.put("currentVersion", curVer);
            histObj.put("history", history);

            try {
                return objectMapper.writeValueAsString(histObj);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    // todo: Refactoring
    @SneakyThrows
    public String getHistoryData(final String fileName, final String version) {
        // get history directory
        String histDir = storagePathBuilder.getHistoryDir(storagePathBuilder.getFileLocation(fileName));
        Integer curVer = storagePathBuilder.getFileVersion(histDir, false);  // get current file version

        if (curVer > 0) {  // check if the current file version is greater than 0
            Map<String, HistoryData> historyDataMap = new HashMap<>();

            for (Integer i = 1; i <= curVer; i++) {  // run through all the file versions
                String verDir = versionDir(histDir, i, true);  // get the path to the given file version

                String key;
                if (i == curVer) {
                    key = documentManager
                            .generateRevisionId(storagePathBuilder.getStorageLocation()
                                    + "/" + fileName + "/"
                                    + new File(storagePathBuilder.getFileLocation(fileName)).lastModified());
                } else {
                    key = readFileToEnd(new File(verDir + File.separator + "key.txt"));
                }
                HistoryData historyData = HistoryData.builder()
                        .fileType(documentManager.getExtension(fileName))
                        .key(key)
                        .url(i == curVer ? urlManager.getFileUrl(fileName)
                                : urlManager.getHistoryFileUrl(fileName, i, "prev" + documentManager
                                .getExtension(fileName), true))
                        .build();

                historyData.setVersion(String.valueOf(i));

                if (i > 1) {  //check if the version number is greater than 1
                    Integer verdiff = i - 1;
                    // get the history data from the previous file version
                    HistoryData historyDataPrev = historyDataMap.get(Integer.toString(verdiff));
                    Previous previous = Previous.builder()
                            .fileType(historyDataPrev.getFileType())
                            .key(historyDataPrev.getKey())
                            .url(historyDataPrev.getUrl())
                            .build();

                    // write information about previous file version to the data object
                    historyData.setPrevious(previous);

                    if (diffExists(histDir, verdiff)) {
                        // write the path to the diff.zip archive with differences in this file version
                        historyData.setChangesUrl(urlManager
                                .getHistoryFileUrl(fileName, verdiff, "diff.zip", true));
                    }
                }

                if (settingsManager.isSecurityEnabled()) {
                    historyData.setToken(jwtManager.createToken(historyData));
                }

                historyDataMap.put(Integer.toString(i), historyData);
            }

            try {
                return objectMapper.writeValueAsString(historyDataMap.get(version));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    @Override
    public String versionDir(final String path, final Integer version, final boolean historyPath) {
        if (!historyPath) {
            return storagePathBuilder.getHistoryDir(storagePathBuilder.getFileLocation(path)) + version;
        }
        return path + File.separator + version;
    }


    // read a file
    private String readFileToEnd(final File file) {
        String output = "";
        try {
            try (FileInputStream is = new FileInputStream(file)) {
                Scanner scanner = new Scanner(is);  // read data from the source
                scanner.useDelimiter("\\A");
                while (scanner.hasNext()) {
                    output += scanner.next();
                }
                scanner.close();
            }
        } catch (Exception e) {
        }
        return output;
    }

    // diff.zip existence check
    private Boolean diffExists(final String histDir, final Integer verdiff) {
        String filePath = Paths.get(histDir, String.valueOf(verdiff), "diff.zip").toString();
        File file = new File(filePath);
        return file.exists();
    }
}
