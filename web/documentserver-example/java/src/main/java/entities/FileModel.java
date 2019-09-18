/*
 *
 * (c) Copyright Ascensio System SIA 2019
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
*/

package entities;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import helpers.DocumentManager;
import helpers.ServiceConverter;
import helpers.FileUtility;
import com.google.gson.Gson;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class FileModel
{
    public String type = "desktop";
    public String mode = "edit";
    public String documentType;
    public Document document;
    public EditorConfig editorConfig;
    public String token;

    public FileModel(String fileName, String lang, String uid, String uname)
    {
        if (fileName == null) fileName = "";
        fileName = fileName.trim();

        documentType = FileUtility.GetFileType(fileName).toString().toLowerCase();

        document = new Document();
        document.title = fileName;
        document.url = DocumentManager.GetFileUri(fileName);
        document.fileType = FileUtility.GetFileExtension(fileName).replace(".", "");
        document.key = ServiceConverter.GenerateRevisionId(DocumentManager.CurUserHostAddress(null) + "/" + fileName + "/" + Long.toString(new File(DocumentManager.StoragePath(fileName, null)).lastModified()));

        editorConfig = new EditorConfig();
        editorConfig.callbackUrl = DocumentManager.GetCallback(fileName);
        if (lang != null) editorConfig.lang = lang;

        if (uid != null) editorConfig.user.id = uid;
        if (uname != null) editorConfig.user.name = uname;

        editorConfig.customization.goback.url = DocumentManager.GetServerUrl() + "/IndexServlet";

        changeType(mode, type);
    }

    public void changeType(String _mode, String _type)
    {
        if (_mode != null) mode = _mode;
        if (_type != null) type = _type;

        Boolean canEdit = DocumentManager.GetEditedExts().contains(FileUtility.GetFileExtension(document.title));

        editorConfig.mode = canEdit && !mode.equals("view") ? "edit" : "view";

        document.permissions = new Permissions(mode, type, canEdit);

        if (type.equals("embedded")) InitDesktop();
    }

    public void InitDesktop()
    {
        editorConfig.InitDesktop(document.url);
    }

    public void BuildToken()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("documentType", documentType);
        map.put("document", document);
        map.put("editorConfig", editorConfig);

        token = DocumentManager.CreateToken(map);
    }

    public String[] GetHistory()
    {
        JSONParser parser = new JSONParser();
        String histDir = DocumentManager.HistoryDir(DocumentManager.StoragePath(document.title, null));
        if (DocumentManager.GetFileVersion(histDir) > 0) {
            Integer curVer = DocumentManager.GetFileVersion(histDir);

            Set<Object> hist = new HashSet<Object>();
            Map<String, Object> histData = new HashMap<String, Object>();

            for (Integer i = 0; i <= curVer; i++) {
                Map<String, Object> obj = new HashMap<String, Object>();
                Map<String, Object> dataObj = new HashMap<String, Object>();
                String verDir = DocumentManager.VersionDir(histDir, i + 1);

                try {
                    String key = null;

                    key = i == curVer ? document.key : readFileToEnd(new File(verDir + File.separator + "key.txt"));

                    obj.put("key", key);
                    obj.put("version", i);

                    if (i == 0) {
                        String createdInfo = readFileToEnd(new File(histDir + File.separator + "createdInfo.json"));
                        JSONObject json = (JSONObject) parser.parse(createdInfo);

                        obj.put("created", json.get("created"));
                        Map<String, Object> user = new HashMap<String, Object>();
                        user.put("id", json.get("id"));
                        user.put("name", json.get("name"));
                        obj.put("user", user);
                    }

                    dataObj.put("key", key);
                    dataObj.put("url", i == curVer ? document.url : DocumentManager.GetPathUri(verDir + File.separator + "prev" + FileUtility.GetFileExtension(document.title)));
                    dataObj.put("version", i);

                    if (i > 0) {
                        JSONObject changes = (JSONObject) parser.parse(readFileToEnd(new File(DocumentManager.VersionDir(histDir, i) + File.separator + "changes.json")));
                        JSONObject change = (JSONObject) ((JSONArray) changes.get("changes")).get(0);

                        obj.put("changes", changes.get("changes"));
                        obj.put("serverVersion", changes.get("serverVersion"));
                        obj.put("created", change.get("created"));
                        obj.put("user", change.get("user"));

                        Map<String, Object> prev = (Map<String, Object>) histData.get(Integer.toString(i - 1));
                        Map<String, Object> prevInfo = new HashMap<String, Object>();
                        prevInfo.put("key", prev.get("key"));
                        prevInfo.put("url", prev.get("url"));
                        dataObj.put("previous", prevInfo);
                        dataObj.put("changesUrl", DocumentManager.GetPathUri(DocumentManager.VersionDir(histDir, i) + File.separator + "diff.zip"));
                    }

                    hist.add(obj);
                    histData.put(Integer.toString(i), dataObj);

                } catch (Exception ex) { }
            }

            Map<String, Object> histObj = new HashMap<String, Object>();
            histObj.put("currentVersion", curVer);
            histObj.put("history", hist);

            Gson gson = new Gson();
            return new String[] { gson.toJson(histObj), gson.toJson(histData) };
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

    public class Document
    {
        public String title;
        public String url;
        public String fileType;
        public String key;
        public Permissions permissions;
    }

    public class Permissions
    {
        public Boolean comment;
        public Boolean download;
        public Boolean edit;
        public Boolean fillForms;
        public Boolean modifyFilter;
        public Boolean review;

        public Permissions(String mode, String type, Boolean canEdit)
        {
            comment = !mode.equals("view") && !mode.equals("fillForms") && !mode.equals("embedded");
            download = true;
            edit = canEdit && (mode.equals("edit") || mode.equals("filter"));
            fillForms = !mode.equals("view") && !mode.equals("comment") && !mode.equals("embedded");
            modifyFilter = !mode.equals("filter");
            review = mode.equals("edit") || mode.equals("review");
        }
    }

    public class EditorConfig
    {
        public String mode = "edit";
        public String callbackUrl;
        public String lang = "en";
        public User user;
        public Customization customization;
        public Embedded embedded;

        public EditorConfig()
        {
            user = new User();
            customization = new Customization();
        }

        public void InitDesktop(String url)
        {
            embedded = new Embedded();
            embedded.saveUrl = url;
            embedded.embedUrl = url;
            embedded.shareUrl = url;
            embedded.toolbarDocked = "top";
        }

        public class User
        {
            public String id = "uid-1";
            public String name = "John Smith";
        }

        public class Customization
        {
            public Goback goback;

            public Customization()
            {
                goback = new Goback();
            }

            public class Goback
            {
                public String url;
            }
        }

        public class Embedded
        {
            public String saveUrl;
            public String embedUrl;
            public String shareUrl;
            public String toolbarDocked;
        }
    }


    public static String Serialize(FileModel model)
    {
        Gson gson = new Gson();
        return gson.toJson(model);
    }
}
