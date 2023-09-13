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

package helpers;

import entities.FileType;
import entities.User;
import format.FormatManager;

import org.json.simple.JSONObject;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static utils.Constants.KILOBYTE_SIZE;
import static utils.Constants.MAX_FILE_SIZE;

public final class DocumentManager {
    private static HttpServletRequest request;
    private static FormatManager formatManager = new FormatManager();

    private DocumentManager() { }

    public static void init(final HttpServletRequest req, final HttpServletResponse resp) {
        request = req;
    }

    // get max file size
    public static long getMaxFileSize() {
        long size;

        try {
            size = Long.parseLong(ConfigManager.getProperty("filesize-max"));
        } catch (Exception ex) {
            size = 0;
        }

        return size > 0 ? size : MAX_FILE_SIZE;
    }

    // get all the supported file extensions
    public static List<String> getFileExts() {
        return DocumentManager.formatManager.allExtensions();
    }

    public static List<String> getFillExts() {
        return DocumentManager.formatManager.fillableExtensions();
    }

    // get file extensions that can be viewed
    public static List<String> getViewedExts() {
        return DocumentManager.formatManager.viewableExtensions();
    }

    // get file extensions that can be edited
    public static List<String> getEditedExts() {
        return DocumentManager.formatManager.editableExtensions();
    }

    // get file extensions that can be converted
    public static List<String> getConvertExts() {
        return DocumentManager.formatManager.autoConvertExtensions();
    }

    // get current user host address
    public static String curUserHostAddress(final String userAddress) {
        String userAddr = userAddress;
        if (userAddr == null) {
            try {
                // use InetAddress class to get the user address if it wasn't passed to the function
                userAddr = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ex) {
                userAddr = "";
            }
        }

        return userAddr.replaceAll("[^0-9a-zA-Z.=]", "_");
    }

    // get the root directory of the user host
    public static String filesRootPath(final String userAddress) {
        String hostAddress = curUserHostAddress(userAddress);  // get current user host address
        String serverPath = request.getSession().getServletContext().getRealPath("");  // get the server url
        String storagePath = ConfigManager.getProperty("storage-folder");  // get the storage directory
        File f = new File(storagePath);

        if (f.isAbsolute()) {
            if (!f.exists()) {
                if (Files.isWritable(Paths.get(storagePath.substring(0, storagePath.lastIndexOf(File.separator))))) {
                    f.mkdirs();
                } else {
                    throw new SecurityException("No write permission to path: " + f.toPath());
                }
            } else if (f.exists() && f.isFile()) {
                throw new SecurityException("The path to the file is specified instead of the folder");
            }
        }
        String directory = !f.isAbsolute() ? serverPath + storagePath
                + File.separator + hostAddress + File.separator : storagePath + File.separator;

        File file = new File(directory);

        // if the root directory doesn't exist
        if (!file.exists()) {
            // create it
            file.mkdirs();
        }

        return directory;
    }

    // get the storage path of the file
    public static String storagePath(final String fileName, final String userAddress) {
        String directory = filesRootPath(userAddress);
        return directory + FileUtility.getFileName(fileName);
    }

    // get the path to history file
    public static String historyPath(final String fileName, final String userAddress, final String version,
                                     final String file) {
        String hostAddress = curUserHostAddress(userAddress);
        String serverPath = request.getSession().getServletContext().getRealPath("");
        String storagePath = ConfigManager.getProperty("storage-folder");
        String directory = serverPath + storagePath + File.separator + hostAddress + File.separator;
        if (new File(storagePath).isAbsolute()) {
            directory = filesRootPath(userAddress);
        }

        directory = directory + fileName + "-hist" + File.separator + version + File.separator + file;

        return directory;
    }

    // get the path to the forcesaved file version
    public static String forcesavePath(final String fileName, final String userAddress, final Boolean create) {
        String hostAddress = curUserHostAddress(userAddress);
        String serverPath = request.getSession().getServletContext().getRealPath("");
        String storagePath = ConfigManager.getProperty("storage-folder");

        // create the directory to this file version
        String directory = serverPath + storagePath + File.separator + hostAddress + File.separator;

        File file = new File(directory);
        if (!file.exists()) {
            return "";
        }

        // create the directory to the history of this file version
        directory = directory + fileName + "-hist" + File.separator;
        file = new File(directory);
        if (!create && !file.exists()) {
            return "";
        }

        file.mkdirs();

        directory = directory + fileName;
        file = new File(directory);
        if (!create && !file.exists()) {
            return "";
        }

        return directory;
    }

    // get the history directory
    public static String historyDir(final String storagePath) {
        return storagePath + "-hist";
    }

    // get the path to the file version by the history path and file version
    public static String versionDir(final String histPath, final Integer version) {
        return histPath + File.separator + Integer.toString(version);
    }

    // get the path to the file version by the file name, user address and file version
    public static String versionDir(final String fileName, final String userAddress, final Integer version) {
        return versionDir(historyDir(storagePath(fileName, userAddress)), version);
    }

    // get the file version by the history path
    public static Integer getFileVersion(final String historyPath) {
        File dir = new File(historyPath);

        if (!dir.exists()) {
            return 1;  // if the history path doesn't exist, then the file version is 1
        }

        File[] dirs = dir.listFiles(new FileFilter() {  // take only directories from the history folder
            @Override
            public boolean accept(final File pathname) {
                return pathname.isDirectory();
            }
        });

        return dirs.length + 1;  // count the directories
    }

    // get the file version by the file name and user address
    public static int getFileVersion(final String fileName, final String userAddress) {
        return getFileVersion(historyDir(storagePath(fileName, userAddress)));
    }

    // get a file name with an index if the file with such a name already exists
    public static String getCorrectName(final String fileName, final String userAddress) {
        String baseName = FileUtility.getFileNameWithoutExtension(fileName);
        String ext = FileUtility.getFileExtension(fileName);
        String name = baseName + "." + ext;

        File file = new File(storagePath(name, userAddress));

        for (int i = 1; file.exists(); i++) {  // run through all the files with such a name in the storage directory
            name = baseName + " (" + i + ")." + ext;  // and add an index to the base name
            file = new File(storagePath(name, userAddress));
        }

        return name;
    }

    // create meta information
    public static void createMeta(final String fileName, final String uid, final String uname,
                                  final String userAddress) throws Exception {
        String histDir = historyDir(storagePath(fileName, userAddress));

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
    public static File[] getStoredFiles(final String userAddress) {
        String directory = filesRootPath(userAddress);

        File file = new File(directory);
        return file.listFiles(new FileFilter() {  // take only files from the root directory
            @Override
            public boolean accept(final File pathname) {
                return pathname.isFile();
            }
        });
    }

    // create demo document
    public static String createDemo(final String fileExt, final Boolean sample, final User user) throws Exception {
        // create sample or new template file with the necessary extension
        String demoName = (sample ? "sample." : "new.") + fileExt;

        // get the path to the sample document
        String demoPath = "assets"
            + File.separator
            + "document-templates"
            + File.separator
            + (sample ? "sample" : "new")
            + File.separator;

        // get a file name with an index if the file with such a name already exists
        String fileName = getCorrectName(demoName, null);

        // get the input file stream
        InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(demoPath + demoName);

        createFile(Paths.get(storagePath(fileName, null)), stream);

        // create meta information of the demo file
        createMeta(fileName, user.getId(), user.getName(), null);

        return fileName;
    }

    public static boolean createFile(final Path path, final InputStream stream) {
        if (Files.exists(path)) {
            return true;
        }
        try {
            File file = Files.createFile(path).toFile();
            try (FileOutputStream out = new FileOutputStream(file)) {
                int read;
                final byte[] bytes = new byte[KILOBYTE_SIZE];
                while ((read = stream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // get file url
    public static String getFileUri(final String fileName, final Boolean forDocumentServer) {
        try {
            String serverPath = getServerUrl(forDocumentServer);
            String storagePath = ConfigManager.getProperty("storage-folder");
            File f = new File(storagePath);
            String hostAddress = curUserHostAddress(null);

            String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/"
                    + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
            if (f.isAbsolute() && f.isFile()) {
                filePath = getDownloadUrl(fileName, true);
                if (!Files.isWritable(f.toPath())) {
                    throw new SecurityException("No write permission to path: " + f.toPath());
                }
            }

            return filePath;
        } catch (Exception e) {
            return "";
        }
    }

    // get file information
    public static ArrayList<Map<String, Object>> getFilesInfo() {
        ArrayList<Map<String, Object>> files = new ArrayList<>();

        // run through all the stored files
        for (File file : getStoredFiles(null)) {
            Map<String, Object> map = new LinkedHashMap<>();  // write all the parameters to the map
            map.put("version", getFileVersion(file.getName(), null));
            map.put("id", ServiceConverter
                    .generateRevisionId(curUserHostAddress(null) + "/" + file.getName() + "/"
                            + Long.toString(new File(storagePath(file.getName(), null)).lastModified())));
            map.put("contentLength", new BigDecimal(String.valueOf((file.length() / Double.valueOf(KILOBYTE_SIZE))))
                    .setScale(2, RoundingMode.HALF_UP) + " KB");
            map.put("pureContentLength", file.length());
            map.put("title", file.getName());
            map.put("updated", String.valueOf(new Date(file.lastModified())));
            files.add(map);
        }

        return files;
    }

    // get file information by its id
    public static ArrayList<Map<String, Object>> getFilesInfo(final String fileId) {
        ArrayList<Map<String, Object>> file = new ArrayList<>();

        for (Map<String, Object> map : getFilesInfo()) {
            if (map.get("id").equals(fileId)) {
                file.add(map);
                break;
            }
        }

        return file;
    }

    // get the path url
    public static String getPathUri(final String path) {
        String serverPath = getServerUrl(true);
        String storagePath = ConfigManager.getProperty("storage-folder");
        String hostAddress = curUserHostAddress(null);

        String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/"
                + path.replace(File.separator, "/").substring(filesRootPath(null).length())
                .replace(" ", "%20");

        return filePath;
    }


    // get the server url
    public static String getServerUrl(final Boolean forDocumentServer) {
        if (forDocumentServer && !ConfigManager.getProperty("files.docservice.url.example").equals("")) {
            return ConfigManager.getProperty("files.docservice.url.example");
        } else {
            return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                    + request.getContextPath();
        }
    }

    // get the callback url
    public static String getCallback(final String fileName) {
        String serverPath = getServerUrl(true);
        String hostAddress = curUserHostAddress(null);
        try {
            String query = "?type=track&fileName="
                    + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + "&userAddress=" + URLEncoder
                    .encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());

            return serverPath + "/IndexServlet" + query;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    // get url to the created file
    public static String getCreateUrl(final FileType fileType) {
        String serverPath = getServerUrl(false);
        String fileExt = getInternalExtension(fileType).replace(".", "");
        String query = "?fileExt=" + fileExt;

        return serverPath + "/EditorServlet" + query;
    }

    // get url to download a file
    public static String getDownloadUrl(final String fileName, final Boolean forDocumentServer) {
        String serverPath = getServerUrl(forDocumentServer);
        String hostAddress = curUserHostAddress(null);
        try {
            String userAddress = forDocumentServer ? "&userAddress=" + URLEncoder
                    .encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString()) : "";
            String query = "?type=download&fileName=" + URLEncoder
                    .encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()) + userAddress;

            return serverPath + "/IndexServlet" + query;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    // get url to download a file to History prev.*
    public static String getDownloadHistoryUrl(final String fileName, final Integer version, final String file,
                                               final Boolean forDocumentServer) {
        String serverPath = getServerUrl(forDocumentServer);
        String hostAddress = curUserHostAddress(null);
        try {
            String userAddress = forDocumentServer ? "&userAddress=" + URLEncoder
                    .encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString()) : "";
            String query = "?type=downloadhistory&fileName=" + URLEncoder
                    .encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()) + userAddress;
            query = query + "&ver=" + version + "&file=" + URLEncoder.
                    encode(file, java.nio.charset.StandardCharsets.UTF_8.toString());

            return serverPath + "/IndexServlet" + query;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    // get an editor internal extension
    public static String getInternalExtension(final FileType fileType) {
        // .docx for word file type
        if (fileType.equals(FileType.WORD)) {
            return ".docx";
        }

        // .xlsx for cell file type
        if (fileType.equals(FileType.CELL)) {
            return ".xlsx";
        }

        // .pptx for slide file type
        if (fileType.equals(FileType.SLIDE)) {
            return ".pptx";
        }

        // the default file type is .docx
        return ".docx";
    }

    // get image url for templates
    public static String getTemplateImageUrl(final FileType fileType) {
        String path = getServerUrl(true) + "/css/img/";
        // for word file type
        if (fileType.equals(FileType.WORD)) {
            return path + "file_docx.svg";
        }

        // .xlsx for cell file type
        if (fileType.equals(FileType.CELL)) {
            return path + "file_xlsx.svg";
        }

        // .pptx for slide file type
        if (fileType.equals(FileType.SLIDE)) {
            return path + "file_pptx.svg";
        }

        // the default file type
        return path + "file_docx.svg";
    }

    // create document token
    public static String createToken(final Map<String, Object> payloadClaims) {
        try {
            // build a HMAC signer using a SHA-256 hash
            Signer signer = HMACSigner.newSHA256Signer(getTokenSecret());
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet()) {  // run through all the keys from the payload
                jwt.addClaim(key, payloadClaims.get(key));  // and write each claim to the jwt
            }
            return JWT.getEncoder().encode(jwt, signer);  // sign and encode the JWT to a JSON string representation
        } catch (Exception e) {
            return "";
        }
    }

    // read document token
    public static JWT readToken(final String token) {
        try {
            // build a HMAC verifier using the token secret
            Verifier verifier = HMACVerifier.newVerifier(getTokenSecret());

            // verify and decode the encoded string JWT to a rich object
            return JWT.getDecoder().decode(token, verifier);
        } catch (Exception exception) {
            return null;
        }
    }

    // check if the token is enabled
    public static Boolean tokenEnabled() {
        String secret = getTokenSecret();
        return secret != null && !secret.isEmpty();
    }

    // check if the token is enabled for request
    public static Boolean tokenUseForRequest() {
        String tokenUseForRequest = getTokenUseForRequest();
        return Boolean.parseBoolean(tokenUseForRequest) && !tokenUseForRequest.isEmpty();
    }

    // get token secret from the config parameters
    public static String getTokenSecret() {
        return ConfigManager.getProperty("files.docservice.secret");
    }

    // get config request jwt
    public static String getTokenUseForRequest() {
        return ConfigManager.getProperty("files.docservice.token-use-for-request");
    }

    // get languages
    public static Map<String, String> getLanguages() {
        String langs = ConfigManager.getProperty("files.docservice.languages");
        List<String> langsAndKeys = Arrays.asList(langs.split("\\|"));

        Map<String, String> languages = new LinkedHashMap<>();

        langsAndKeys.forEach((str) -> {
            String[] couple = str.split(":");
            languages.put(couple[0], couple[1]);
        });
        return languages;
    }

    public static String readFileToEnd(final File file) {
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
}
