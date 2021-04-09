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

package helpers;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import entities.FileType;

import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;
import org.json.simple.JSONObject;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;

public class DocumentManager
{
    private static HttpServletRequest request;

    public static void Init(HttpServletRequest req, HttpServletResponse resp)
    {
        request = req;
    }

    public static long GetMaxFileSize()
    {
        long size;

        try
        {
            size = Long.parseLong(ConfigManager.GetProperty("filesize-max"));
        }
        catch (Exception ex)
        {
            size = 0;
        }

        return size > 0 ? size : 5 * 1024 * 1024;
    }

    public static List<String> GetFileExts()
    {
        List<String> res = new ArrayList<>();

        res.addAll(GetViewedExts());
        res.addAll(GetEditedExts());
        res.addAll(GetConvertExts());

        return res;
    }

    public static List<String> GetViewedExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.viewed-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static List<String> GetEditedExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.edited-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static List<String> GetConvertExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.convert-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static String CurUserHostAddress(String userAddress)
    {
        if(userAddress == null)
        {
            try
            {
                userAddress = InetAddress.getLocalHost().getHostAddress();
            }
            catch (Exception ex)
            {
                userAddress = "";
            }
        }

        return userAddress.replaceAll("[^0-9a-zA-Z.=]", "_");
    }

    public static String FilesRootPath(String userAddress)
    {
        String hostAddress = CurUserHostAddress(userAddress);
        String serverPath = request.getSession().getServletContext().getRealPath("");
        String storagePath = ConfigManager.GetProperty("storage-folder");
        String directory = serverPath + storagePath + File.separator + hostAddress + File.separator;

        File file = new File(directory);

        if (!file.exists())
        {
            file.mkdirs();
        }

        return directory;
    }

    public static String StoragePath(String fileName, String userAddress)
    {
        String directory = FilesRootPath(userAddress);
        return directory + FileUtility.GetFileName(fileName);
    }

    public static String ForcesavePath(String fileName, String userAddress, Boolean create)
    {
        String hostAddress = CurUserHostAddress(userAddress);
        String serverPath = request.getSession().getServletContext().getRealPath("");
        String storagePath = ConfigManager.GetProperty("storage-folder");

        String directory = serverPath + storagePath + File.separator + hostAddress + File.separator;

        File file = new File(directory);
        if (!file.exists()) return "";

        directory = directory + fileName + "-hist" + File.separator;
        file = new File(directory);
        if (!create && !file.exists()) return "";

        file.mkdirs();

        directory = directory + fileName;
        file = new File(directory);
        if (!create && !file.exists()) {
            return "";
        }

        return directory;
    }

    public static String HistoryDir(String storagePath)
    {
        return storagePath += "-hist";
    }

    public static String VersionDir(String histPath, Integer version)
    {
        return histPath + File.separator + Integer.toString(version);
    }

    public static String VersionDir(String fileName, String userAddress, Integer version)
    {
        return VersionDir(HistoryDir(StoragePath(fileName, userAddress)), version);
    }

    public static Integer GetFileVersion(String historyPath)
    {
        File dir = new File(historyPath);

        if (!dir.exists()) return 0;

        File[] dirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        return dirs.length + 1;
    }

    public static int GetFileVersion(String fileName, String userAddress)
    {
        return GetFileVersion(HistoryDir(StoragePath(fileName, userAddress)));
    }

    public static String GetCorrectName(String fileName, String userAddress)
    {
        String baseName = FileUtility.GetFileNameWithoutExtension(fileName);
        String ext = FileUtility.GetFileExtension(fileName);
        String name = baseName + ext;

        File file = new File(StoragePath(name, userAddress));

        for (int i = 1; file.exists(); i++)
        {
            name = baseName + " (" + i + ")" + ext;
            file = new File(StoragePath(name, userAddress));
        }

        return name;
    }

    public static void CreateMeta(String fileName, String uid, String uname, String userAddress) throws Exception
    {
        String histDir = HistoryDir(StoragePath(fileName, userAddress));

        File dir = new File(histDir);
        dir.mkdir();

        JSONObject json = new JSONObject();
        json.put("created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        json.put("id", (uid == null || uid.isEmpty()) ? "uid-1" : uid);
        if (json.get("id").equals("uid-0")) {
            json.put("name", null);
        } else {
            json.put("name", (uname == null || uname.isEmpty()) ? "John Smith" : uname);
        }

        File meta = new File(histDir + File.separator + "createdInfo.json");
        try (FileWriter writer = new FileWriter(meta)) {
            json.writeJSONString(writer);
        }
    }

    public static File[] GetStoredFiles(String userAddress)
    {
        String directory = FilesRootPath(userAddress);

        File file = new File(directory);
        return file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
    }

    public static String CreateDemo(String fileExt, Boolean sample, String uid, String uname) throws Exception
    {
        String demoName = (sample ? "sample." : "new.") + fileExt;
        String demoPath = "assets" + File.separator + (sample ? "sample" : "new") + File.separator;
        String fileName = GetCorrectName(demoName, null);

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(demoPath + demoName);

        File file = new File(StoragePath(fileName, null));

        try (FileOutputStream out = new FileOutputStream(file))
        {
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = stream.read(bytes)) != -1)
            {
                out.write(bytes, 0, read);
            }
            out.flush();
        }

        CreateMeta(fileName, uid, uname, null);

        return fileName;
    }

    public static String GetFileUri(String fileName, Boolean forDocumentServer)
    {
        try
        {
            String serverPath = GetServerUrl(forDocumentServer);
            String storagePath = ConfigManager.GetProperty("storage-folder");
            String hostAddress = CurUserHostAddress(null);

            String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/" + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()).replace("+", "%20");

            return filePath;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public static ArrayList<Map<String, Object>> GetFilesInfo(){
        ArrayList<Map<String, Object>> files = new ArrayList<>();

        for(File file : GetStoredFiles(null)){
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("version", GetFileVersion(file.getName(), null));
            map.put("id", ServiceConverter.GenerateRevisionId(CurUserHostAddress(null) + "/" + file.getName() + "/" + Long.toString(new File(StoragePath(file.getName(), null)).lastModified())));
            map.put("contentLength", new BigDecimal(String.valueOf((file.length()/1024.0))).setScale(2, RoundingMode.HALF_UP) + " KB");
            map.put("pureContentLength", file.length());
            map.put("title", file.getName());
            map.put("updated", String.valueOf(new Date(file.lastModified())));
            files.add(map);
        }

        return files;
    }

    public static ArrayList<Map<String, Object>> GetFilesInfo(String fileId){
        ArrayList<Map<String, Object>> file = new ArrayList<>();

        for (Map<String, Object> map : GetFilesInfo()){
            if (map.get("id").equals(fileId)){
                file.add(map);
                break;
            }
        }

        return file;
    }

    public static String GetPathUri(String path)
    {
        String serverPath = GetServerUrl(true);
        String storagePath = ConfigManager.GetProperty("storage-folder");
        String hostAddress = CurUserHostAddress(null);

        String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/" + path.replace(File.separator, "/").substring(FilesRootPath(null).length()).replace(" ", "%20");

        return filePath;
    }


    public static String GetServerUrl(Boolean forDocumentServer) {
        if (forDocumentServer && !ConfigManager.GetProperty("files.docservice.url.example").equals("")) {
            return ConfigManager.GetProperty("files.docservice.url.example");
        } else {
            return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        }
    }

    public static String GetCallback(String fileName)
    {
        String serverPath = GetServerUrl(true);
        String hostAddress = CurUserHostAddress(null);
        try
        {
            String query = "?type=track&fileName=" + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()) + "&userAddress=" + URLEncoder.encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());

            return serverPath + "/IndexServlet" + query;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public static String GetInternalExtension(FileType fileType)
    {
        if (fileType.equals(FileType.Word))
            return ".docx";

        if (fileType.equals(FileType.Cell))
            return ".xlsx";

        if (fileType.equals(FileType.Slide))
            return ".pptx";

        return ".docx";
    }

    public static String CreateToken(Map<String, Object> payloadClaims)
    {
        try
        {
            Signer signer = HMACSigner.newSHA256Signer(GetTokenSecret());
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet())
            {
                jwt.addClaim(key, payloadClaims.get(key));
            }
            return JWT.getEncoder().encode(jwt, signer);
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static JWT ReadToken(String token)
    {
        try
        {
            Verifier verifier = HMACVerifier.newVerifier(GetTokenSecret());
            return JWT.getDecoder().decode(token, verifier);
        }
        catch (Exception exception)
        {
            return null;
        }
    }

    public static Boolean TokenEnabled()
    {
        String secret = GetTokenSecret();
        return secret != null && !secret.isEmpty();
    }

    private static String GetTokenSecret()
    {
        return ConfigManager.GetProperty("files.docservice.secret");
    }
}