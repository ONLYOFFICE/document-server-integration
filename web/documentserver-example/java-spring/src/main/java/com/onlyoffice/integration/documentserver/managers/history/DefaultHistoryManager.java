/**
 * (c) Copyright Ascensio System SIA 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onlyoffice.integration.documentserver.managers.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.documentserver.managers.document.DocumentManager;
import com.onlyoffice.integration.documentserver.managers.jwt.JwtManager;
import com.onlyoffice.integration.documentserver.models.filemodel.Document;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import lombok.SneakyThrows;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

//TODO: Rebuild completely
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

    private JSONObject getFakeChanges() { // get fake json for changes
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("serverVersion", null);
        jsonObject.put("changes", null);
        return jsonObject;
    }

    //TODO: Refactoring
    @SneakyThrows
    public String[] getHistory(Document document) {  // get document history
        String histDir = storagePathBuilder.getHistoryDir(storagePathBuilder.getFileLocation(document.getTitle()));  // get history directory
        Integer curVer = storagePathBuilder.getFileVersion(histDir, false);  // get current file version

        if (curVer > 0) {  // check if the current file version is greater than 0
            List<Object> hist = new ArrayList<>();
            Map<String, Object> histData = new HashMap<>();

            for (Integer i = 1; i <= curVer; i++) {  // run through all the file versions
                Map<String, Object> obj = new HashMap<String, Object>();
                Map<String, Object> dataObj = new HashMap<String, Object>();
                String verDir = documentManager.versionDir(histDir, i, true);  // get the path to the given file version

                String key = i == curVer ? document.getKey() : readFileToEnd(new File(verDir + File.separator + "key.txt"));  // get document key
                obj.put("key", key);
                obj.put("version", i);

                if (i == 1) {  // check if the version number is equal to 1
                    String createdInfo = readFileToEnd(new File(histDir + File.separator + "createdInfo.json"));  // get file with meta data
                    JSONObject json = (JSONObject) parser.parse(createdInfo);  // and turn it into json object

                    // write meta information to the object (user information and creation date)
                    obj.put("created", json.get("created"));
                    Map<String, Object> user = new HashMap<String, Object>();
                    user.put("id", json.get("id"));
                    user.put("name", json.get("name"));
                    obj.put("user", user);
                }

                dataObj.put("fileType", fileUtility.getFileExtension(document.getTitle()).replace(".", ""));
                dataObj.put("key", key);
                dataObj.put("url", i == curVer ? document.getUrl() :
                        documentManager.getHistoryFileUrl(document.getTitle(), i, "prev" + fileUtility.getFileExtension(document.getTitle()), true));
                if (!document.getDirectUrl().equals("")) {
                    dataObj.put("directUrl", i == curVer ? document.getDirectUrl() :
                            documentManager.getHistoryFileUrl(document.getTitle(), i, "prev" + fileUtility.getFileExtension(document.getTitle()), false));
                }
                dataObj.put("version", i);

                if (i > 1) {  //check if the version number is greater than 1
                    // if so, get the path to the changes.json file
                    File changesFile = new File(documentManager.versionDir(histDir, i - 1, true) + File.separator + "changes.json");
                    JSONObject changes = changesFile.exists() ? (JSONObject) parser.parse(readFileToEnd(changesFile)) : this.getFakeChanges();
                    JSONObject change = changes.get("changes") != null ? (JSONObject) ((JSONArray) changes.get("changes")).get(0) : null;

                    // write information about changes to the object
                    obj.put("changes", changes.get("changes"));
                    obj.put("serverVersion", changes.get("serverVersion"));
                    obj.put("created", change != null ? change.get("created") : null);
                    obj.put("user", change != null ? change.get("user") : null);

                    Map<String, Object> prev = (Map<String, Object>) histData.get(Integer.toString(i - 2));  // get the history data from the previous file version
                    Map<String, Object> prevInfo = new HashMap<String, Object>();
                    prevInfo.put("fileType", prev.get("fileType"));
                    prevInfo.put("key", prev.get("key"));  // write key and URL information about previous file version
                    prevInfo.put("url", prev.get("url"));
                    if (!document.getDirectUrl().equals("")) {
                        prevInfo.put("directUrl", prev.get("directUrl"));
                    }
                    dataObj.put("previous", prevInfo);  // write information about previous file version to the data object
                    // write the path to the diff.zip archive with differences in this file version
                    Integer verdiff = i - 1;
                    dataObj.put("changesUrl", documentManager.getHistoryFileUrl(document.getTitle(), verdiff, "diff.zip", true));
                }

                if (jwtManager.tokenEnabled()) dataObj.put("token", jwtManager.createToken(dataObj));

                hist.add(obj);
                histData.put(Integer.toString(i - 1), dataObj);
            }

            // write history information about the current file version to the history object
            Map<String, Object> histObj = new HashMap<String, Object>();
            histObj.put("currentVersion", curVer);
            histObj.put("history", hist);

            try {
                return new String[]{objectMapper.writeValueAsString(histObj), objectMapper.writeValueAsString(histData)};
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return new String[]{"", ""};
    }

    // read a file
    private String readFileToEnd(File file) {
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
