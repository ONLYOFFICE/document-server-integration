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

package com.onlyoffice.integration.documentserver.managers.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.documentserver.managers.document.DocumentManager;
import com.onlyoffice.integration.documentserver.managers.jwt.JwtManager;
import com.onlyoffice.integration.documentserver.models.filemodel.Document;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.documentserver.util.service.ServiceConverter;
import lombok.SneakyThrows;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

// todo: Rebuild completely
@Component
public class DefaultHistoryManager implements HistoryManager {

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private JwtManager jwtManager;

    @Autowired
    private FileUtility fileUtility;

    @Autowired
    private JSONParser parser;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServiceConverter serviceConverter;

    // todo: Refactoring
    @SneakyThrows
    public String[] getHistory(final Document document) {  // get document history

        // get history directory
        String histDir = storagePathBuilder.getHistoryDir(storagePathBuilder.getFileLocation(document.getTitle()));
        Integer curVer = storagePathBuilder.getFileVersion(histDir, false);  // get current file version

        if (curVer > 0) {  // check if the current file version is greater than 0
            List<Object> hist = new ArrayList<>();
            Map<String, Object> histData = new HashMap<>();

            for (Integer i = 1; i <= curVer; i++) {  // run through all the file versions
                Map<String, Object> obj = new HashMap<String, Object>();
                Map<String, Object> dataObj = new HashMap<String, Object>();
                String verDir = documentManager
                        .versionDir(histDir, i, true);  // get the path to the given file version

                String key = i == curVer ? document.getKey() : readFileToEnd(new File(verDir
                        + File.separator + "key.txt"));  // get document key
                obj.put("key", key);
                obj.put("version", i);

                if (i == 1) {  // check if the version number is equal to 1
                    String createdInfo = readFileToEnd(new File(histDir
                            + File.separator + "createdInfo.json"));  // get file with meta data
                    JSONObject json = (JSONObject) parser.parse(createdInfo);  // and turn it into json object

                    // write meta information to the object (user information and creation date)
                    obj.put("created", json.get("created"));
                    Map<String, Object> user = new HashMap<String, Object>();
                    user.put("id", json.get("id"));
                    user.put("name", json.get("name"));
                    obj.put("user", user);
                }

                dataObj.put("fileType", fileUtility
                        .getFileExtension(document.getTitle()));
                dataObj.put("key", key);
                dataObj.put("url", i == curVer ? document.getUrl()
                        : documentManager.getHistoryFileUrl(document.getTitle(), i, "prev." + fileUtility
                        .getFileExtension(document.getTitle()), true));
                if (!document.getDirectUrl().equals("")) {
                    dataObj.put("directUrl", i == curVer ? document.getDirectUrl()
                            : documentManager.getHistoryFileUrl(document.getTitle(), i, "prev." + fileUtility
                            .getFileExtension(document.getTitle()), false));
                }
                dataObj.put("version", i);

                if (i > 1) {  //check if the version number is greater than 1
                    // if so, get the path to the changes.json file
                    JSONObject changes = (JSONObject) parser.parse(readFileToEnd(new File(documentManager
                            .versionDir(histDir, i - 1, true) + File.separator + "changes.json")));
                    JSONObject change = (JSONObject) ((JSONArray) changes.get("changes")).get(0);

                    // write information about changes to the object
                    obj.put("changes", changes.get("changes"));
                    obj.put("serverVersion", changes.get("serverVersion"));
                    obj.put("created", change.get("created"));
                    obj.put("user", change.get("user"));

                    // get the history data from the previous file version
                    Map<String, Object> prev = (Map<String, Object>) histData.get(Integer.toString(i - 2));
                    Map<String, Object> prevInfo = new HashMap<String, Object>();
                    prevInfo.put("fileType", prev.get("fileType"));
                    prevInfo.put("key", prev.get("key"));  // write key and URL information about previous file version
                    prevInfo.put("url", prev.get("url"));
                    if (!document.getDirectUrl().equals("")) {
                        prevInfo.put("directUrl", prev.get("directUrl"));
                    }

                    // write information about previous file version to the data object
                    dataObj.put("previous", prevInfo);
                    // write the path to the diff.zip archive with differences in this file version
                    Integer verdiff = i - 1;
                    dataObj.put("changesUrl", documentManager
                            .getHistoryFileUrl(document.getTitle(), verdiff, "diff.zip", true));
                }

                if (jwtManager.tokenEnabled()) {
                    dataObj.put("token", jwtManager.createToken(dataObj));
                }

                hist.add(obj);
                histData.put(Integer.toString(i - 1), dataObj);
            }

            // write history information about the current file version to the history object
            Map<String, Object> histObj = new HashMap<String, Object>();
            histObj.put("currentVersion", curVer);
            histObj.put("history", hist);

            try {
                return new String[]{objectMapper.writeValueAsString(histObj),
                        objectMapper.writeValueAsString(histData)};
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return new String[]{"", ""};
    }

    // todo: Refactoring
    @SneakyThrows
    public String getHistory(final String fileName) {  // get document history

        // get history directory
        String histDir = storagePathBuilder.getHistoryDir(storagePathBuilder.getFileLocation(fileName));
        Integer curVer = storagePathBuilder.getFileVersion(histDir, false);  // get current file version

        if (curVer > 0) {  // check if the current file version is greater than 0
            List<Object> hist = new ArrayList<>();

            for (Integer i = 1; i <= curVer; i++) {  // run through all the file versions
                Map<String, Object> obj = new HashMap<String, Object>();
                String verDir = documentManager
                        .versionDir(histDir, i, true);  // get the path to the given file version

                String key;
                if (i == curVer) {
                    key = serviceConverter
                            .generateRevisionId(storagePathBuilder.getStorageLocation()
                                    + "/" + fileName + "/"
                                    + new File(storagePathBuilder.getFileLocation(fileName)).lastModified());
                } else {
                    key = readFileToEnd(new File(verDir + File.separator + "key.txt"));
                }

                obj.put("key", key);
                obj.put("version", i);

                if (i == 1) {  // check if the version number is equal to 1
                    String createdInfo = readFileToEnd(new File(histDir
                            + File.separator + "createdInfo.json"));  // get file with meta data
                    JSONObject json = (JSONObject) parser.parse(createdInfo);  // and turn it into json object

                    // write meta information to the object (user information and creation date)
                    obj.put("created", json.get("created"));
                    Map<String, Object> user = new HashMap<String, Object>();
                    user.put("id", json.get("id"));
                    user.put("name", json.get("name"));
                    obj.put("user", user);
                }

                if (i > 1) {  //check if the version number is greater than 1
                    // if so, get the path to the changes.json file
                    JSONObject changes = (JSONObject) parser.parse(readFileToEnd(new File(documentManager
                            .versionDir(histDir, i - 1, true) + File.separator + "changes.json")));
                    JSONObject change = (JSONObject) ((JSONArray) changes.get("changes")).get(0);

                    // write information about changes to the object
                    obj.put("changes", changes.get("changes"));
                    obj.put("serverVersion", changes.get("serverVersion"));
                    obj.put("created", change.get("created"));
                    obj.put("user", change.get("user"));
                }

                hist.add(obj);
            }

            // write history information about the current file version to the history object
            Map<String, Object> histObj = new HashMap<String, Object>();
            histObj.put("currentVersion", curVer);
            histObj.put("history", hist);

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
    public String getHistoryData(final String fileName, final String version, final Boolean directUrl) {
        // get history directory
        String histDir = storagePathBuilder.getHistoryDir(storagePathBuilder.getFileLocation(fileName));
        Integer curVer = storagePathBuilder.getFileVersion(histDir, false);  // get current file version

        if (curVer > 0) {  // check if the current file version is greater than 0
            Map<String, Object> histData = new HashMap<>();

            for (Integer i = 1; i <= curVer; i++) {  // run through all the file versions
                Map<String, Object> dataObj = new HashMap<String, Object>();
                String verDir = documentManager
                        .versionDir(histDir, i, true);  // get the path to the given file version

                String key;
                if (i == curVer) {
                    key = serviceConverter
                            .generateRevisionId(storagePathBuilder.getStorageLocation()
                                    + "/" + fileName + "/"
                                    + new File(storagePathBuilder.getFileLocation(fileName)).lastModified());
                } else {
                    key = readFileToEnd(new File(verDir + File.separator + "key.txt"));
                }

                dataObj.put("fileType", fileUtility
                        .getFileExtension(fileName).replace(".", ""));
                dataObj.put("key", key);
                dataObj.put("url", i == curVer ? documentManager.getDownloadUrl(fileName, true)
                        : documentManager.getHistoryFileUrl(fileName, i, "prev" + fileUtility
                        .getFileExtension(fileName), true));
                if (directUrl) {
                    dataObj.put("directUrl", i == curVer
                            ? documentManager.getDownloadUrl(fileName, false)
                            : documentManager.getHistoryFileUrl(fileName, i, "prev" + fileUtility
                                    .getFileExtension(fileName), false));
                }
                dataObj.put("version", i);

                if (i > 1) {  //check if the version number is greater than 1
                    // get the history data from the previous file version
                    Map<String, Object> prev = (Map<String, Object>) histData.get(Integer.toString(i - 1));
                    Map<String, Object> prevInfo = new HashMap<String, Object>();
                    prevInfo.put("fileType", prev.get("fileType"));
                    prevInfo.put("key", prev.get("key"));  // write key and URL information about previous file version
                    prevInfo.put("url", prev.get("url"));
                    if (directUrl) {
                        prevInfo.put("directUrl", prev.get("directUrl"));
                    }

                    // write information about previous file version to the data object
                    dataObj.put("previous", prevInfo);
                    // write the path to the diff.zip archive with differences in this file version
                    Integer verdiff = i - 1;
                    dataObj.put("changesUrl", documentManager
                            .getHistoryFileUrl(fileName, verdiff, "diff.zip", true));
                }

                if (jwtManager.tokenEnabled()) {
                    dataObj.put("token", jwtManager.createToken(dataObj));
                }

                histData.put(Integer.toString(i), dataObj);
            }

            try {
                return objectMapper.writeValueAsString(histData.get(version));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return "";
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
}
