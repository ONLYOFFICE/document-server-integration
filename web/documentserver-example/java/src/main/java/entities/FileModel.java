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

package entities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import helpers.DocumentManager;
import helpers.FileUtility;
import helpers.ServiceConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class FileModel {
    private String type = "desktop";
    private String mode = "edit";
    private String documentType;
    private Document document;
    private EditorConfig editorConfig;
    private String token;

    // create file model
    public FileModel(final String fileNameParam,
                     final String lang,
                     final String actionData,
                     final User user,
                     final Boolean isEnableDirectUrl) {

        // remove extra spaces in the file name if not null
        String fileName = fileNameParam == null ? "" : fileNameParam.trim();

        // get file type from the file name (word, cell or slide)
        documentType = FileUtility.getFileType(fileName).toString().toLowerCase();

        // set the document parameters
        document = new Document();
        document.setTitle(fileName);
        document.setUrl(DocumentManager.getDownloadUrl(fileName, true)); // get file url

        // get direct url
        document.setDirectUrl(isEnableDirectUrl ? DocumentManager.getDownloadUrl(fileName, false) : "");

        // get file extension from the file name
        document.setFileType(FileUtility.getFileExtension(fileName).replace(".", ""));
        // generate document key
        document.setKey(ServiceConverter
                .generateRevisionId(DocumentManager
                        .curUserHostAddress(null) + "/" + fileName + "/"
                        + Long.toString(new File(DocumentManager.storagePath(fileName, null))
                        .lastModified())));
        document.setInfo(new Info());
        document.getInfo().setFavorite(user.getFavorite());
        document.setReferenceData(new ReferenceData(fileName, DocumentManager.curUserHostAddress(null), user));

        String templatesImageUrl = DocumentManager.getTemplateImageUrl(FileUtility.getFileType(fileName));
        List<Map<String, String>> templates = new ArrayList<>();
        String createUrl = DocumentManager.getCreateUrl(FileUtility.getFileType(fileName));

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
        editorConfig.setCallbackUrl(DocumentManager.getCallback(fileName));  // get callback url

        HashMap<String, Object> coEditing = mode.equals("view") && user.getId().equals("uid-0")
                ? new HashMap<String, Object>()  {{
            put("mode", "strict");
            put("change", false);
        }} : null;
        editorConfig.setCoEditing(coEditing);
        if (lang != null) {
            editorConfig.setLang(lang);  // write language parameter to the config
        }

        editorConfig.setCreateUrl(!user.getId().equals("uid-0") ? createUrl : null);
        editorConfig.setTemplates(user.getTemplates() ? templates : null);

        // write user information to the config (id, name and group)
        editorConfig.getUser().setId(!user.getId().equals("uid-0") ? user.getId() : null);
        editorConfig.getUser().setName(user.getName());
        editorConfig.getUser().setGroup(user.getGroup());

        // write the absolute URL to the file location
        editorConfig.getCustomization().getGoback()
                .setUrl(DocumentManager.getServerUrl(false) + "/IndexServlet");

        changeType(mode, type, user, fileName);
    }

    // change the document type
    public void changeType(final String modeParam, final String typeParam, final User user, final String fileName) {
        if (modeParam != null) {
            mode = modeParam;
        }
        if (typeParam != null) {
            type = typeParam;
        }

        // check if the file with such an extension can be edited
        String fileExt = FileUtility.getFileExtension(document.getTitle());
        Boolean canEdit = DocumentManager.getEditedExts().contains(fileExt);
        // check if the Submit form button is displayed or not
        editorConfig.getCustomization().setSubmitForm(false);

        if ((!canEdit && mode.equals("edit") || mode.equals("fillForms"))
                && DocumentManager.getFillExts().contains(fileExt)) {
            canEdit = true;
            mode = "fillForms";
        }
        // set the mode parameter: change it to view if the document can't be edited
        editorConfig.setMode(canEdit && !mode.equals("view") ? "edit" : "view");

        if (user.getId().equals("uid-0") && mode.equals("view")) {
            canEdit = false;
        }

        // set document permissions
        document.setPermissions(new Permissions(mode, type, canEdit, user));

        if (type.equals("embedded")) {
            initDesktop(fileName);  // set parameters for the embedded document
        }
    }

    public void initDesktop(final String fileName) {
        editorConfig.initDesktop(DocumentManager.getDownloadUrl(fileName, false) + "&dmode=emb");
    }

    // generate document token
    public void buildToken() {
        // write all the necessary document parameters to the map
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("documentType", documentType);
        map.put("document", document);
        map.put("editorConfig", editorConfig);

        // and create token from them
        token = DocumentManager.createToken(map);
    }

    // get document history
    public String[] getHistory() {
        JSONParser parser = new JSONParser();

        // get history directory
        String histDir = DocumentManager.historyDir(DocumentManager.storagePath(document.getTitle(), null));
        if (DocumentManager.getFileVersion(histDir) > 0) {

            // get current file version if it is greater than 0
            Integer curVer = DocumentManager.getFileVersion(histDir);

            List<Object> hist = new ArrayList<>();
            Map<String, Object> histData = new HashMap<String, Object>();

            for (Integer i = 1; i <= curVer; i++) {  // run through all the file versions
                Map<String, Object> obj = new HashMap<String, Object>();
                Map<String, Object> dataObj = new HashMap<String, Object>();
                String verDir = DocumentManager.versionDir(histDir, i);  // get the path to the given file version

                try {
                    String key = null;

                    // get document key
                    key = i == curVer
                            ? document.getKey() : readFileToEnd(new File(verDir + File.separator + "key.txt"));

                    obj.put("key", key);
                    obj.put("version", i);

                    if (i == 1) {  // check if the version number is equal to 1
                        String createdInfo = readFileToEnd(new File(histDir + File.separator
                                + "createdInfo.json")); // get file with meta data
                        JSONObject json = (JSONObject) parser.parse(createdInfo);  // and turn it into json object

                        // write meta information to the object (user information and creation date)
                        obj.put("created", json.get("created"));
                        Map<String, Object> user = new HashMap<String, Object>();
                        user.put("id", json.get("id"));
                        user.put("name", json.get("name"));
                        obj.put("user", user);
                    }

                    dataObj.put("fileType", FileUtility.getFileExtension(document.getTitle()).substring(1));
                    dataObj.put("key", key);
                    dataObj.put("url", i == curVer ? document.getUrl() : DocumentManager
                            .getDownloadHistoryUrl(document.getTitle(), i, "prev" + FileUtility
                                    .getFileExtension(document.getTitle()), true));
                    if (!document.getDirectUrl().equals("")) {
                        dataObj.put("directUrl", i == curVer ? document.getUrl() : DocumentManager
                                .getDownloadHistoryUrl(document.getTitle(), i, "prev" + FileUtility
                                        .getFileExtension(document.getTitle()), false));
                    }
                    dataObj.put("version", i);

                    if (i > 1) {  //check if the version number is greater than 1
                        // if so, get the path to the changes.json file
                        JSONObject changes = (JSONObject) parser.parse(readFileToEnd(new File(DocumentManager
                                .versionDir(histDir, i - 1) + File.separator + "changes.json")));
                        JSONObject change = (JSONObject) ((JSONArray) changes.get("changes")).get(0);

                        // write information about changes to the object
                        obj.put("changes", !change.isEmpty() ? changes.get("changes") : null);
                        obj.put("serverVersion", changes.get("serverVersion"));
                        obj.put("created", !change.isEmpty() ? change.get("created") : null);
                        obj.put("user", !change.isEmpty() ? change.get("user") : null);

                        // get the history data from the previous file version
                        Map<String, Object> prev = (Map<String, Object>) histData.get(Integer.toString(i - 2));
                        Map<String, Object> prevInfo = new HashMap<String, Object>();
                        prevInfo.put("fileType", prev.get("fileType"));

                        // write key and url information about previous file version
                        prevInfo.put("key", prev.get("key"));
                        prevInfo.put("url", prev.get("url"));

                        // write information about previous file version to the data object
                        dataObj.put("previous", prevInfo);
                        // write the path to the diff.zip archive with differences in this file version
                        Integer verdiff = i - 1;
                        String changesUrl = DocumentManager
                                .getDownloadHistoryUrl(document.getTitle(), verdiff,
                                        "diff.zip", true);
                        dataObj.put("changesUrl", changesUrl);
                    }

                    if (DocumentManager.tokenEnabled()) {
                        dataObj.put("token", DocumentManager.createToken(dataObj));
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
        } catch (Exception e) { }
        return output;
    }

    // the document parameters
    public class Document {
        private String title;
        private String url;
        private String directUrl;
        private String fileType;
        private String key;
        private Info info;
        private Permissions permissions;
        private ReferenceData referenceData;

        public String getTitle() {
            return title;
        }

        public void setTitle(final String titleParam) {
            this.title = titleParam;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(final String urlParam) {
            this.url = urlParam;
        }

        public String getDirectUrl() {
            return directUrl;
        }

        public void setDirectUrl(final String directUrlParam) {
            this.directUrl = directUrlParam;
        }

        public void setFileType(final String fileTypeParam) {
            this.fileType = fileTypeParam;
        }

        public String getKey() {
            return key;
        }

        public void setKey(final String keyParam) {
            this.key = keyParam;
        }

        public Info getInfo() {
            return info;
        }

        public void setInfo(final Info infoParam) {
            this.info = infoParam;
        }

        public Permissions getPermissions() {
            return permissions;
        }

        public void setPermissions(final Permissions permissionsParam) {
            this.permissions = permissionsParam;
        }

        public ReferenceData getReferenceData() {
            return referenceData;
        }
        public void setReferenceData(final ReferenceData referenceDataParam) {
            this.referenceData = referenceDataParam;
        }
    }

    // the permissions parameters
    public class Permissions {
        private final Boolean comment;
        private final Boolean copy;
        private final Boolean download;
        private final Boolean edit;
        private final Boolean print;
        private final Boolean fillForms;
        private final Boolean modifyFilter;
        private final Boolean modifyContentControl;
        private final Boolean review;
        private final Boolean chat;
        private final List<String> reviewGroups;
        private final CommentGroups commentGroups;
        private final List<String> userInfoGroups;
        private final Boolean protect;
        //public Gson gson = new Gson();

        // defines what can be done with a document
        public Permissions(final String modeParam, final String typeParam, final Boolean canEdit, final User user) {
            comment = !modeParam.equals("view") && !modeParam.equals("fillForms") && !modeParam.equals("embedded")
                    && !modeParam.equals("blockcontent");
            copy = !user.getDeniedPermissions().contains("сopy");
            download = !user.getDeniedPermissions().contains("download");
            edit = canEdit && (modeParam.equals("edit") || modeParam.equals("view") || modeParam.equals("filter")
                    || modeParam.equals("blockcontent"));
            print = !user.getDeniedPermissions().contains("print");
            fillForms = !modeParam.equals("view") && !modeParam.equals("comment") && !modeParam.equals("embedded")
                    && !modeParam.equals("blockcontent");
            modifyFilter = !modeParam.equals("filter");
            modifyContentControl = !modeParam.equals("blockcontent");
            review = canEdit && (modeParam.equals("edit") || modeParam.equals("review"));
            chat = !user.getId().equals("uid-0");
            reviewGroups = user.getReviewGroups();
            commentGroups = user.getCommentGroups();
            userInfoGroups = user.getUserInfoGroups();
            protect = !user.getDeniedPermissions().contains("protect");
        }
    }

    public class ReferenceData {
        private final String instanceId;
        private final Map<String, String> fileKey;
        public ReferenceData(final String fileName, final String curUserHostAddress, final User user) {
            instanceId = DocumentManager.getServerUrl(true);
            Map<String, String> fileKeyList = new HashMap<>();
            if (!user.getId().equals("uid-0")) {
                fileKeyList.put("fileName", fileName);
                fileKeyList.put("userAddress", curUserHostAddress);
            } else {
                fileKeyList = null;
            }
            fileKey = fileKeyList;
        }
    }
    // the Favorite icon state
    public class Info {
        private String owner = "Me";
        private Boolean favorite;
        private String uploaded = getDate();

        private String getDate() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd yyyy", Locale.US);
            return simpleDateFormat.format(new Date());
        }

        public void setFavorite(final Boolean favoriteParam) {
            this.favorite = favoriteParam;
        }
    }
    // the editor config parameters
    public class EditorConfig {
        private HashMap<String, Object> actionLink = null;
        private String mode = "edit";
        private String callbackUrl;
        private HashMap<String, Object> coEditing = null;
        private String lang = "en";
        private String createUrl;
        private List<Map<String, String>> templates;
        private User user;
        private Customization customization;
        private Embedded embedded;

        public EditorConfig(final String actionData) {
            // get the action in the document that will be scrolled to (bookmark or comment)
            if (actionData != null) {
                Gson gson = new Gson();
                actionLink = gson.fromJson(actionData, new TypeToken<HashMap<String, Object>>() { }.getType());
            }
            user = new User();
            customization = new Customization();
        }

        // set parameters for the embedded document
        public void initDesktop(final String url) {
            embedded = new Embedded();

            // the absolute URL that will allow the document to be saved onto the user personal computer
            embedded.setSaveUrl(url);

            // the absolute URL to the document serving as a source file for the document embedded into the web page
            embedded.setEmbedUrl(url);

            // the absolute URL that will allow other users to share this document
            embedded.setShareUrl(url);

            // the place for the embedded viewer toolbar, can be either top or bottom
            embedded.setToolbarDocked("top");
        }

        public String getMode() {
            return mode;
        }

        public void setMode(final String modeParam) {
            this.mode = modeParam;
        }

        public void setCallbackUrl(final String callbackUrlParam) {
            this.callbackUrl = callbackUrlParam;
        }

        public void setCoEditing(final HashMap<String, Object> coEditingParam) {
            this.coEditing = coEditingParam;
        }

        public void setLang(final String langParam) {
            this.lang = langParam;
        }

        public void setCreateUrl(final String createUrlParam) {
            this.createUrl = createUrlParam;
        }

        public void setTemplates(final List<Map<String, String>> templatesParam) {
            this.templates = templatesParam;
        }

        public User getUser() {
            return user;
        }

        public void setUser(final User userParam) {
            this.user = userParam;
        }

        public Customization getCustomization() {
            return customization;
        }

        // default user parameters (id, name and group)
        public class User {
            private String id;
            private String name;
            private String group;

            public String getId() {
                return id;
            }

            public void setId(final String idParam) {
                this.id = idParam;
            }

            public String getName() {
                return name;
            }

            public void setName(final String nameParam) {
                this.name = nameParam;
            }

            public String getGroup() {
                return group;
            }

            public void setGroup(final String groupParam) {
                this.group = groupParam;
            }
        }

        // customization parameters
        public class Customization {
            private Goback goback;
            private Boolean forcesave;
            private Boolean submitForm;
            private Boolean about;
            private Boolean comments;
            private Boolean feedback;

            public Goback getGoback() {
                return goback;
            }

            public void setSubmitForm(final Boolean submitFormParam) {
                this.submitForm = submitFormParam;
            }

            public Customization() {
                about = true;
                comments = true;
                feedback = true;
                forcesave = false;
                goback = new Goback();
            }

            public class Goback {
                private String url;

                public String getUrl() {
                    return url;
                }

                public void setUrl(final String urlParam) {
                    this.url = urlParam;
                }
            }
        }

        // parameters for embedded document
        public class Embedded {
            private String saveUrl;
            private String embedUrl;
            private String shareUrl;
            private String toolbarDocked;

            public void setSaveUrl(final String saveUrlParam) {
                this.saveUrl = saveUrlParam;
            }

            public void setEmbedUrl(final String embedUrlParam) {
                this.embedUrl = embedUrlParam;
            }

            public void setShareUrl(final String shareUrlParam) {
                this.shareUrl = shareUrlParam;
            }

            public void setToolbarDocked(final String toolbarDockedParam) {
                this.toolbarDocked = toolbarDockedParam;
            }
        }
    }


    // turn java objects into json strings
    public static String serialize(final FileModel model) {
        Gson gson = new Gson();
        return gson.toJson(model);
    }

    public String getDocumentType() {
        return documentType;
    }
}
