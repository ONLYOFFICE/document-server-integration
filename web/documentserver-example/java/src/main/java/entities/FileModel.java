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

package entities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import helpers.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileModel {
    public String type = "desktop";
    public String mode = "edit";
    public String documentType;
    public Document document;
    public EditorConfig editorConfig;
    public String token;

    // create file model
    public FileModel(String fileName, String lang, String actionData, User user, Boolean isEnableDirectUrl) {
        if (fileName == null) {
            fileName = "";
        }
        fileName = fileName.trim();  // remove extra spaces in the file name

        // get file type from the file name (word, cell or slide)
        documentType = FileUtility.GetFileType(fileName).toString().toLowerCase();

        // set the document parameters
        document = new Document();
        document.title = fileName;
        document.url = DocumentManager.GetDownloadUrl(fileName, true);  // get file url
        document.directUrl = isEnableDirectUrl ? DocumentManager.GetDownloadUrl(fileName, false) : "";  // get direct url
        document.fileType = FileUtility.GetFileExtension(fileName).replace(".", "");  // get file extension from the file name
        // generate document key
        document.key = ServiceConverter.GenerateRevisionId(DocumentManager.CurUserHostAddress(null) + "/" + fileName + "/" + Long.toString(new File(DocumentManager.StoragePath(fileName, null)).lastModified()));
        document.info = new Info();
        document.info.favorite = user.favorite;

        String templatesImageUrl = DocumentManager.GetTemplateImageUrl(FileUtility.GetFileType(fileName));
        List<Map<String, String>> templates = new ArrayList<>();
        String createUrl = DocumentManager.GetCreateUrl(FileUtility.GetFileType(fileName));

        // add templates for the "Create New" from menu option
        Map<String, String> templateForBlankDocument = new HashMap<>();
        templateForBlankDocument.put("image", "");
        templateForBlankDocument.put("title", "Blank");
        templateForBlankDocument.put("url", createUrl);
        templates.add(templateForBlankDocument);
        Map<String, String> templateForDocumentWithSampleContent = new HashMap<>();
        templateForDocumentWithSampleContent.put("image", templatesImageUrl);
        templateForDocumentWithSampleContent.put("title", "With sample content");
        templateForDocumentWithSampleContent.put("url", createUrl + "&sample=true");
        templates.add(templateForDocumentWithSampleContent);

        // set the editor config parameters
        editorConfig = new EditorConfig(actionData);
        editorConfig.callbackUrl = DocumentManager.GetCallback(fileName);  // get callback url

        editorConfig.coEditing = mode.equals("view") && user.id.equals("uid-0")
                ? new HashMap<String, Object>()  {{
            put("mode", "strict");
            put("change", false);
        }} : null;
        if (lang != null) {
            editorConfig.lang = lang;  // write language parameter to the config
        }

        editorConfig.createUrl = !user.id.equals("uid-0") ? createUrl : null;
        editorConfig.templates = user.templates ? templates : null;

        // write user information to the config (id, name and group)
        editorConfig.user.id = !user.id.equals("uid-0") ? user.id : null;
        editorConfig.user.name = user.name;
        editorConfig.user.group = user.group;

        // write the absolute URL to the file location
        editorConfig.customization.goback.url = DocumentManager.GetServerUrl(false) + "/IndexServlet";

        changeType(mode, type, user, fileName);
    }

    // change the document type
    public void changeType(String modeParam, String typeParam, User user, String fileName) {
        if (modeParam != null) {
            mode = modeParam;
        }
        if (typeParam != null) {
            type = typeParam;
        }

        // check if the file with such an extension can be edited
        String fileExt = FileUtility.GetFileExtension(document.title);
        Boolean canEdit = DocumentManager.GetEditedExts().contains(fileExt);
        // check if the Submit form button is displayed or not
        editorConfig.customization.submitForm = false;

        if ((!canEdit && mode.equals("edit") || mode.equals("fillForms")) && DocumentManager.GetFillExts().contains(fileExt)) {
            canEdit = true;
            mode = "fillForms";
        }
        // set the mode parameter: change it to view if the document can't be edited
        editorConfig.mode = canEdit && !mode.equals("view") ? "edit" : "view";

        // set document permissions
        document.permissions = new Permissions(mode, type, canEdit, user);

        if (type.equals("embedded")) {
            InitDesktop(fileName);  // set parameters for the embedded document
        }
    }

    public void InitDesktop(String fileName) {
        editorConfig.InitDesktop(DocumentManager.GetDownloadUrl(fileName, false) + "&dmode=emb");
    }

    // generate document token
    public void BuildToken() {
        // write all the necessary document parameters to the map
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("documentType", documentType);
        map.put("document", document);
        map.put("editorConfig", editorConfig);

        // and create token from them
        token = DocumentManager.CreateToken(map);
    }

    // get document history
    public String[] GetHistory() {
        JSONParser parser = new JSONParser();
        String histDir = DocumentManager.HistoryDir(DocumentManager.StoragePath(document.title, null));  // get history directory
        if (DocumentManager.GetFileVersion(histDir) > 0) {
            Integer curVer = DocumentManager.GetFileVersion(histDir);  // get current file version if it is greater than 0

            List<Object> hist = new ArrayList<>();
            Map<String, Object> histData = new HashMap<String, Object>();

            for (Integer i = 1; i <= curVer; i++) {  // run through all the file versions
                Map<String, Object> obj = new HashMap<String, Object>();
                Map<String, Object> dataObj = new HashMap<String, Object>();
                String verDir = DocumentManager.VersionDir(histDir, i);  // get the path to the given file version

                try {
                    String key = null;

                    // get document key
                    key = i == curVer ? document.key : readFileToEnd(new File(verDir + File.separator + "key.txt"));

                    obj.put("key", key);
                    obj.put("version", i);

                    if (i == 1) {  // check if the version number is equal to 1
                        String createdInfo = readFileToEnd(new File(histDir + File.separator + "createdInfo.json")); // get file with meta data
                        JSONObject json = (JSONObject) parser.parse(createdInfo);  // and turn it into json object

                        // write meta information to the object (user information and creation date)
                        obj.put("created", json.get("created"));
                        Map<String, Object> user = new HashMap<String, Object>();
                        user.put("id", json.get("id"));
                        user.put("name", json.get("name"));
                        obj.put("user", user);
                    }

                    dataObj.put("fileType", FileUtility.GetFileExtension(document.title).substring(1));
                    dataObj.put("key", key);
                    dataObj.put("url", i == curVer ? document.url : DocumentManager.GetDownloadHistoryUrl(document.title, i, "prev" + FileUtility.GetFileExtension(document.title), true));
                    if (!document.directUrl.equals("")) {
                        dataObj.put("directUrl", i == curVer ? document.url : DocumentManager.GetDownloadHistoryUrl(document.title, i, "prev" + FileUtility.GetFileExtension(document.title), false));
                    }
                    dataObj.put("version", i);

                    if (i > 1) {  //check if the version number is greater than 1
                        // if so, get the path to the changes.json file
                        JSONObject changes = (JSONObject) parser.parse(readFileToEnd(new File(DocumentManager.VersionDir(histDir, i - 1) + File.separator + "changes.json")));
                        JSONObject change = (JSONObject) ((JSONArray) changes.get("changes")).get(0);

                        // write information about changes to the object
                        obj.put("changes", !change.isEmpty() ? changes.get("changes") : null);
                        obj.put("serverVersion", changes.get("serverVersion"));
                        obj.put("created", !change.isEmpty() ? change.get("created") : null);
                        obj.put("user", !change.isEmpty() ? change.get("user") : null);

                        Map<String, Object> prev = (Map<String, Object>) histData.get(Integer.toString(i - 2));  // get the history data from the previous file version
                        Map<String, Object> prevInfo = new HashMap<String, Object>();
                        prevInfo.put("fileType", prev.get("fileType"));
                        prevInfo.put("key", prev.get("key"));  // write key and url information about previous file version
                        prevInfo.put("url", prev.get("url"));
                        dataObj.put("previous", prevInfo);  // write information about previous file version to the data object
                        // write the path to the diff.zip archive with differences in this file version
                        Integer verdiff = i - 1;
                        String changesUrl = DocumentManager.GetDownloadHistoryUrl(document.title, verdiff, "diff.zip", true);
                        dataObj.put("changesUrl", changesUrl);
                    }

                    if (DocumentManager.TokenEnabled()) {
                        dataObj.put("token", DocumentManager.CreateToken(dataObj));
                    }

                    hist.add(obj);
                    histData.put(Integer.toString(i - 1), dataObj);

                } catch (Exception ex) { }
            }

            // write history information about the current file version to the history object
            Map<String, Object> histObj = new HashMap<String, Object>();
            histObj.put("currentVersion", curVer);
            histObj.put("history", hist);

            Gson gson = new Gson();
            return new String[] {gson.toJson(histObj), gson.toJson(histData) };
        }
        return new String[] {"", "" };
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
        } catch (Exception e) { }
        return output;
    }

    // the document parameters
    public class Document {
        public String title;
        public String url;
        public String directUrl;
        public String fileType;
        public String key;
        public Info info;
        public Permissions permissions;
    }

    // the permissions parameters
    public class Permissions {
        public Boolean comment;
        public Boolean copy;
        public Boolean download;
        public Boolean edit;
        public Boolean print;
        public Boolean fillForms;
        public Boolean modifyFilter;
        public Boolean modifyContentControl;
        public Boolean review;
        public Boolean chat;
        public List<String> reviewGroups;
        public CommentGroups commentGroups;
        public List<String> userInfoGroups;
        //public Gson gson = new Gson();

        // defines what can be done with a document
        public Permissions(String mode, String type, Boolean canEdit, User user) {
            comment = !mode.equals("view") && !mode.equals("fillForms") && !mode.equals("embedded") && !mode.equals("blockcontent");
            copy = !user.deniedPermissions.contains("сopy");
            download = !user.deniedPermissions.contains("download");
            edit = canEdit && (mode.equals("edit") || mode.equals("view") || mode.equals("filter") || mode.equals("blockcontent"));
            print = !user.deniedPermissions.contains("print");
            fillForms = !mode.equals("view") && !mode.equals("comment") && !mode.equals("embedded") && !mode.equals("blockcontent");
            modifyFilter = !mode.equals("filter");
            modifyContentControl = !mode.equals("blockcontent");
            review = canEdit && (mode.equals("edit") || mode.equals("review"));
            chat = !user.id.equals("uid-0");
            reviewGroups = user.reviewGroups;
            commentGroups = user.commentGroups;
            userInfoGroups = user.userInfoGroups;
        }
    }

    // the Favorite icon state
    public class Info {
        public String owner = "Me";
        public Boolean favorite;
        public String uploaded = getDate();

        private String getDate() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd yyyy", Locale.US);
            return simpleDateFormat.format(new Date());
        }
    }
    // the editor config parameters
    public class EditorConfig {
        public HashMap<String, Object> actionLink = null;
        public String mode = "edit";
        public String callbackUrl;
        public HashMap<String, Object> coEditing = null;
        public String lang = "en";
        public String createUrl;
        public List<Map<String, String>> templates;
        public User user;
        public Customization customization;
        public Embedded embedded;

        public EditorConfig(String actionData) {
            // get the action in the document that will be scrolled to (bookmark or comment)
            if (actionData != null) {
                Gson gson = new Gson();
                actionLink = gson.fromJson(actionData, new TypeToken<HashMap<String, Object>>() { }.getType());
            }
            user = new User();
            customization = new Customization();
        }

        // set parameters for the embedded document
        public void InitDesktop(String url) {
            embedded = new Embedded();
            embedded.saveUrl = url;  // the absolute URL that will allow the document to be saved onto the user personal computer
            embedded.embedUrl = url;  // the absolute URL to the document serving as a source file for the document embedded into the web page
            embedded.shareUrl = url;  // the absolute URL that will allow other users to share this document
            embedded.toolbarDocked = "top";  // the place for the embedded viewer toolbar, can be either top or bottom
        }

        // default user parameters (id, name and group)
        public class User {
            public String id;
            public String name;
            public String group;
        }

        // customization parameters
        public class Customization {
            public Goback goback;
            public Boolean forcesave;
            public Boolean submitForm;
            public Boolean about;
            public Boolean comments;
            public Boolean feedback;

            public Customization() {
                about = true;
                comments = true;
                feedback = true;
                forcesave = false;
                goback = new Goback();
            }

            public class Goback {
                public String url;
            }
        }

        // parameters for embedded document
        public class Embedded {
            public String saveUrl;
            public String embedUrl;
            public String shareUrl;
            public String toolbarDocked;
        }
    }


    // turn java objects into json strings
    public static String Serialize(FileModel model) {
        Gson gson = new Gson();
        return gson.toJson(model);
    }
}
