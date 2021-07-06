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

package com.onlyoffice.integration.entities.filemodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.entities.enums.DocumentType;
import com.onlyoffice.integration.entities.enums.Type;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import com.onlyoffice.integration.util.documentManagers.DocumentTokenManager;
import com.onlyoffice.integration.util.fileUtilities.FileUtility;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Component
@Scope("prototype")
public class FileModel {
    private Document document;
    private DocumentType documentType;
    private EditorConfig editorConfig;
    private String token;
    private Type type;

    @Autowired
    private DocumentTokenManager documentTokenManager;
    @Autowired
    private DocumentManager documentManager;
    @Autowired
    private FileUtility fileUtility;

    public void configure(Document doc, DocumentType documentType, EditorConfig config, Type type){
        this.document = doc;
        this.documentType = documentType;
        this.editorConfig = config;
        this.type = type;
    }

    public void ChangeFavoriteFlag(){
        this.document.getInfo().setFavorite(!this.document.getInfo().getFavorite());
    }

    public String[] GetHistory(){
        JSONParser parser = new JSONParser();
        String histDir = documentManager.historyDir(documentManager.storagePath(document.getTitle(), null));
        if (documentManager.getFileVersion(histDir) > 0) {
            Integer curVer = documentManager.getFileVersion(histDir);

            List<Object> hist = new ArrayList<>();
            Map<String, Object> histData = new HashMap<>();

            for (Integer i = 1; i <= curVer; i++) {
                Map<String, Object> obj = new HashMap<String, Object>();
                Map<String, Object> dataObj = new HashMap<String, Object>();
                String verDir = documentManager.versionDir(histDir, i);

                try {
                    String key = null;

                    key = i == curVer ? document.getKey() : readFileToEnd(new File(verDir + File.separator + "key.txt"));

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
                            documentManager.getFileUri(verDir + File.separator + "prev" + fileUtility.getFileExtension(document.getTitle()),true));
                    dataObj.put("version", i);

                    if (i > 1) {
                        JSONObject changes = (JSONObject) parser.parse(readFileToEnd(new File(documentManager.versionDir(histDir, i - 1) + File.separator + "changes.json")));
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
                        dataObj.put("changesUrl", documentManager.getFileUri(documentManager.versionDir(histDir, i - 1) + File.separator + "diff.zip",true));
                    }

                    if (documentTokenManager.tokenEnabled())
                    {
                        dataObj.put("token", documentTokenManager.createToken(dataObj));
                    }

                    hist.add(obj);
                    histData.put(Integer.toString(i - 1), dataObj);

                } catch (Exception ex) { }
            }

            Map<String, Object> histObj = new HashMap<String, Object>();
            histObj.put("currentVersion", curVer);
            histObj.put("history", hist);

            ObjectMapper objectMapper=new ObjectMapper();
            try {
                return new String[] { objectMapper.writeValueAsString(histObj), objectMapper.writeValueAsString(histData) };
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return new String[] { "", "" };
    }

    private String readFileToEnd(File file) {
        String output = "";
        try {
            try(FileInputStream is = new FileInputStream(file))
            {
                Scanner scanner = new Scanner(is);
                scanner.useDelimiter("\\A");
                while (scanner.hasNext()) {
                    output += scanner.next();
                }
                scanner.close();
            }
        } catch (Exception e) { }
        return output;
    }

    public void generateToken(){
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("documentType", documentType);
        map.put("document", document);
        map.put("editorConfig", editorConfig);

        token = documentTokenManager.createToken(map);
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public EditorConfig getEditorConfig() {
        return editorConfig;
    }

    public void setEditorConfig(EditorConfig editorConfig) {
        this.editorConfig = editorConfig;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
