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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import entities.FileType;

import entities.User;
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

    // get max file size
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

    // get all the supported file extensions
    public static List<String> GetFileExts()
    {
        List<String> res = new ArrayList<>();

        res.addAll(GetViewedExts());
        res.addAll(GetEditedExts());
        res.addAll(GetConvertExts());

        return res;
    }

    // get file extensions that can be viewed
    public static List<String> GetViewedExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.viewed-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    // get file extensions that can be edited
    public static List<String> GetEditedExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.edited-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    // get file extensions that can be converted
    public static List<String> GetConvertExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.convert-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    // get current user host address
    public static String CurUserHostAddress(String userAddress)
    {
        if(userAddress == null)
        {
            try
            {
                // use InetAddress class to get the user address if it wasn't passed to the function
                userAddress = InetAddress.getLocalHost().getHostAddress();
            }
            catch (Exception ex)
            {
                userAddress = "";
            }
        }

        return userAddress.replaceAll("[^0-9a-zA-Z.=]", "_");
    }

    // get the root directory of the user host
    public static String FilesRootPath(String userAddress)
    {
        String hostAddress = CurUserHostAddress(userAddress);  // get current user host address
        String serverPath = request.getSession().getServletContext().getRealPath("");  // get the server url
        String storagePath = ConfigManager.GetProperty("storage-folder");  // get the storage directory
        File f = new File(storagePath);

        if (f.isAbsolute()) {
            if (!f.isDirectory()) {
                throw new SecurityException("The path to the file is specified instead of the folder");
            } else {
                if (!Files.isWritable(f.toPath())) {
                    throw new SecurityException("No write permission to path: " + f.toPath());
                }
            }
        }
        String directory = !f.isAbsolute() ? serverPath + storagePath + File.separator + hostAddress + File.separator : storagePath + File.separator;

        File file = new File(directory);

        // if the root directory doesn't exist
        if (!file.exists())
        {
            // create it
            file.mkdirs();
        }

        return directory;
    }

    // get the storage path of the file
    public static String StoragePath(String fileName, String userAddress)
    {
        String directory = FilesRootPath(userAddress);
        return directory + FileUtility.GetFileName(fileName);
    }

    // get the path to the forcesaved file version
    public static String ForcesavePath(String fileName, String userAddress, Boolean create)
    {
        String hostAddress = CurUserHostAddress(userAddress);
        String serverPath = request.getSession().getServletContext().getRealPath("");
        String storagePath = ConfigManager.GetProperty("storage-folder");

        // create the directory to this file version
        String directory = serverPath + storagePath + File.separator + hostAddress + File.separator;

        File file = new File(directory);
        if (!file.exists()) return "";

        // create the directory to the history of this file version
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

    // get the history directory
    public static String HistoryDir(String storagePath)
    {
        return storagePath += "-hist";
    }

    // get the path to the file version by the history path and file version
    public static String VersionDir(String histPath, Integer version)
    {
        return histPath + File.separator + Integer.toString(version);
    }

    // get the path to the file version by the file name, user address and file version
    public static String VersionDir(String fileName, String userAddress, Integer version)
    {
        return VersionDir(HistoryDir(StoragePath(fileName, userAddress)), version);
    }

    // get the file version by the history path
    public static Integer GetFileVersion(String historyPath)
    {
        File dir = new File(historyPath);

        if (!dir.exists()) return 1;  // if the history path doesn't exist, then the file version is 1

        File[] dirs = dir.listFiles(new FileFilter() {  // take only directories from the history folder
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        return dirs.length + 1;  // count the directories
    }

    // get the file version by the file name and user address
    public static int GetFileVersion(String fileName, String userAddress)
    {
        return GetFileVersion(HistoryDir(StoragePath(fileName, userAddress)));
    }

    // get a file name with an index if the file with such a name already exists
    public static String GetCorrectName(String fileName, String userAddress)
    {
        String baseName = FileUtility.GetFileNameWithoutExtension(fileName);
        String ext = FileUtility.GetFileExtension(fileName);
        String name = baseName + ext;

        File file = new File(StoragePath(name, userAddress));

        for (int i = 1; file.exists(); i++)  // run through all the files with such a name in the storage directory
        {
            name = baseName + " (" + i + ")" + ext;  // and add an index to the base name
            file = new File(StoragePath(name, userAddress));
        }

        return name;
    }

    // create meta information
    public static void CreateMeta(String fileName, String uid, String uname, String userAddress) throws Exception
    {
        String histDir = HistoryDir(StoragePath(fileName, userAddress));

        File dir = new File(histDir);  // create history directory
        dir.mkdir();

        // create json object and put there file information (creation time, user id and name)
        JSONObject json = new JSONObject();
        json.put("created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        json.put("id", uid);
        json.put("name", uname);

        // create createdInfo.json file with meta information in the history directory
        File meta = new File(histDir + File.separator + "createdInfo.json");
        try (FileWriter writer = new FileWriter(meta)) {
            json.writeJSONString(writer);  // write information from the json object into this file
        }
    }

    // get all the stored files from the user host address
    public static File[] GetStoredFiles(String userAddress)
    {
        String directory = FilesRootPath(userAddress);

        File file = new File(directory);
        return file.listFiles(new FileFilter() {  // take only files from the root directory
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
    }

    // create demo document
    public static String CreateDemo(String fileExt, Boolean sample, User user) throws Exception
    {
        String demoName = (sample ? "sample." : "new.") + fileExt;  // create sample or new template file with the necessary extension
        String demoPath = "assets" + File.separator + (sample ? "sample" : "new") + File.separator;  // get the path to the sample document
        String fileName = GetCorrectName(demoName, null);  // get a file name with an index if the file with such a name already exists

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(demoPath + demoName);  // get the input file stream

        File file = new File(StoragePath(fileName, null));

        try (FileOutputStream out = new FileOutputStream(file))
        {
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = stream.read(bytes)) != -1)
            {
                out.write(bytes, 0, read);  // write bytes to the output stream
            }
            // force write data to the output stream that can be cached in the current thread
            out.flush();
        }

        // create meta information of the demo file
        CreateMeta(fileName, user.id, user.name, null);

        return fileName;
    }

    // get file url
    public static String GetFileUri(String fileName, Boolean forDocumentServer)
    {
        try
        {
            String serverPath = GetServerUrl(forDocumentServer);
            String storagePath = ConfigManager.GetProperty("storage-folder");
            File f = new File(storagePath);
            String hostAddress = CurUserHostAddress(null);

            String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/" + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()).replace("+", "%20");
            if (f.isAbsolute()) {
                if (!f.isDirectory()) {
                    throw new SecurityException("The path to the file is specified instead of the folder");
                } else {
                    filePath = GetDownloadUrl(fileName);
                    if (!Files.isWritable(f.toPath())) {
                        throw new SecurityException("No write permission to path: " + f.toPath());
                    }
                }
            }

            return filePath;
        }
        catch (Exception e)
        {
            return "";
        }
    }

    // get file information
    public static ArrayList<Map<String, Object>> GetFilesInfo(){
        ArrayList<Map<String, Object>> files = new ArrayList<>();

        // run through all the stored files
        for(File file : GetStoredFiles(null)){
            Map<String, Object> map = new LinkedHashMap<>();  // write all the parameters to the map
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

    // get file information by its id
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

    // get the path url
    public static String GetPathUri(String path)
    {
        String serverPath = GetServerUrl(true);
        String storagePath = ConfigManager.GetProperty("storage-folder");
        String hostAddress = CurUserHostAddress(null);

        String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/" + path.replace(File.separator, "/").substring(FilesRootPath(null).length()).replace(" ", "%20");

        return filePath;
    }


    // get the server url
    public static String GetServerUrl(Boolean forDocumentServer) {
        if (forDocumentServer && !ConfigManager.GetProperty("files.docservice.url.example").equals("")) {
            return ConfigManager.GetProperty("files.docservice.url.example");
        } else {
            return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        }
    }

    // get the callback url
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

    // get url to the created file
    public static String GetCreateUrl (FileType fileType) {
        String serverPath = GetServerUrl(false);
        String fileExt = GetInternalExtension(fileType).replace(".", "");
        String query = "?fileExt=" + fileExt;

        return serverPath + "/EditorServlet" + query;
    }

    // get url to download a file
    public static String GetDownloadUrl(String fileName) {
        String serverPath = GetServerUrl(true);
        String hostAddress = CurUserHostAddress(null);
        try
        {
            String query = "?type=download&fileName=" + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()) + "&userAddress=" + URLEncoder.encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());

            return serverPath + "/IndexServlet" + query;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    // get an editor internal extension
    public static String GetInternalExtension(FileType fileType)
    {
        // .docx for word file type
        if (fileType.equals(FileType.Word))
            return ".docx";

        // .xlsx for cell file type
        if (fileType.equals(FileType.Cell))
            return ".xlsx";

        // .pptx for slide file type
        if (fileType.equals(FileType.Slide))
            return ".pptx";

        // the default file type is .docx
        return ".docx";
    }

    // get image url for templates
    public static String GetTemplateImageUrl(FileType fileType)
    {
        String path = GetServerUrl(true) + "/css/img/";
        // for word file type
        if (fileType.equals(FileType.Word))
            return path + "file_docx.svg";

        // .xlsx for cell file type
        if (fileType.equals(FileType.Cell))
            return path + "file_xlsx.svg";

        // .pptx for slide file type
        if (fileType.equals(FileType.Slide))
            return path + "file_pptx.svg";

        // the default file type
        return path + "file_docx.svg";
    }

    // create document token
    public static String CreateToken(Map<String, Object> payloadClaims)
    {
        try
        {
            // build a HMAC signer using a SHA-256 hash
            Signer signer = HMACSigner.newSHA256Signer(GetTokenSecret());
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet())  // run through all the keys from the payload
            {
                jwt.addClaim(key, payloadClaims.get(key));  // and write each claim to the jwt
            }
            return JWT.getEncoder().encode(jwt, signer);  // sign and encode the JWT to a JSON string representation
        }
        catch (Exception e)
        {
            return "";
        }
    }

    // read document token
    public static JWT ReadToken(String token)
    {
        try
        {
            // build a HMAC verifier using the token secret
            Verifier verifier = HMACVerifier.newVerifier(GetTokenSecret());
            return JWT.getDecoder().decode(token, verifier);  // verify and decode the encoded string JWT to a rich object
        }
        catch (Exception exception)
        {
            return null;
        }
    }

    // check if the token is enabled
    public static Boolean TokenEnabled()
    {
        String secret = GetTokenSecret();
        return secret != null && !secret.isEmpty();
    }

    // get token secret from the config parameters
    public static String GetTokenSecret()
    {
        return ConfigManager.GetProperty("files.docservice.secret");
    }
}