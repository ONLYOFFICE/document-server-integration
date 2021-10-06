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

    //TODO: Refactoring
    @SneakyThrows
    public String[] getHistory(Document document) {
        String histDir = storagePathBuilder.getHistoryDir(storagePathBuilder.getFileLocation(document.getTitle()));
        Integer curVer = storagePathBuilder.getFileVersion(histDir, false);

        if (curVer > 0) {
            List<Object> hist = new ArrayList<>();
            Map<String, Object> histData = new HashMap<>();

            for (Integer i = 1; i <= curVer; i++) {
                Map<String, Object> obj = new HashMap<String, Object>();
                Map<String, Object> dataObj = new HashMap<String, Object>();
                String verDir = documentManager.versionDir(histDir, i, true);

                String key = i == curVer ? document.getKey() : readFileToEnd(new File(verDir + File.separator + "key.txt"));
                obj.put("key", key);
                obj.put("version", i);

                if (i == 1) {
                    String createdInfo = readFileToEnd(new File(histDir + File.separator + "createdInfo.json"));
                    JSONObject json = (JSONObject) parser.parse(createdInfo);

                    obj.put("created", json.get("created"));
                    Map<String, Object> user = new HashMap<String, Object>();
                    user.put("id", json.get("id"));
                    user.put("name", json.get("name"));
                    obj.put("user", user);
                }

                dataObj.put("key", key);
                dataObj.put("url", i == curVer ? document.getUrl() :
                        documentManager.getFileUri(documentManager.versionDir(histDir, i, true) + File.separator + "prev" + fileUtility.getFileExtension(document.getTitle()), true));
                dataObj.put("version", i);

                if (i > 1) {
                    JSONObject changes = (JSONObject) parser.parse(readFileToEnd(new File(documentManager.versionDir(histDir, i - 1, true) + File.separator + "changes.json")));
                    JSONObject change = (JSONObject) ((JSONArray) changes.get("changes")).get(0);

                    obj.put("changes", changes.get("changes"));
                    obj.put("serverVersion", changes.get("serverVersion"));
                    obj.put("created", change.get("created"));
                    obj.put("user", change.get("user"));

                    Map<String, Object> prev = (Map<String, Object>) histData.get(Integer.toString(i - 2));
                    Map<String, Object> prevInfo = new HashMap<String, Object>();
                    prevInfo.put("key", prev.get("key"));
                    prevInfo.put("url", prev.get("url"));
                    dataObj.put("previous", prevInfo);
                    dataObj.put("changesUrl", documentManager.getFileUri(documentManager.versionDir(histDir, i - 1, true) + File.separator + "diff.zip", true));
                }

                if (jwtManager.tokenEnabled()) dataObj.put("token", jwtManager.createToken(dataObj));

                hist.add(obj);
                histData.put(Integer.toString(i - 1), dataObj);
            }

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

    private String readFileToEnd(File file) {
        String output = "";
        try {
            try (FileInputStream is = new FileInputStream(file)) {
                Scanner scanner = new Scanner(is);
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
