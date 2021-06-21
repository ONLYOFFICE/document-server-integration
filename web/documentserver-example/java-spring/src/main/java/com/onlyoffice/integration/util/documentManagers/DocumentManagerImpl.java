package com.onlyoffice.integration.util.documentManagers;

import com.onlyoffice.integration.util.fileUtilities.FileUtility;
import com.onlyoffice.integration.util.serviceConverter.ServiceConverter;
import org.json.simple.JSONObject;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DocumentManagerImpl implements DocumentManager {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private FileUtility fileUtility;

    @Autowired
    private ServiceConverter serviceConverter;

    @Value("${storage-folder}")
    private String storageFolder;

    @Value("${files.docservice.secret}")
    private String tokenSecret;

    @Value("${files.docservice.url.example}")
    private String docserviceUrlExample;

    @Value("${filesize-max}")
    private String filesizeMax;

    @Value("${files.docservice.viewed-docs}")
    private String docserviceViewedDocs;

    @Value("${files.docservice.edited-docs}")
    private String docserviceEditedDocs;

    @Value("${files.docservice.convert-docs}")
    private String docserviceConvertDocs;

    @Value("${files.docservice.history.postfix}")
    private String historyPostfix;

    @Value("${url.track}")
    private String trackUrl;

    @Value("${url.download}")
    private String downloadUrl;

    public long getMaxFileSize(){
        long size = Long.parseLong(filesizeMax);
        return size > 0 ? size : 5 * 1024 * 1024;
    }

    public List<String> getFileExts()
    {
        List<String> res = new ArrayList<>();

        res.addAll(getViewedExts());
        res.addAll(getEditedExts());
        res.addAll(getConvertExts());

        return res;
    }

    public List<String> getViewedExts()
    {
        return Arrays.asList(docserviceViewedDocs.split("\\|"));
    }

    public List<String> getEditedExts()
    {
        return Arrays.asList(docserviceEditedDocs.split("\\|"));
    }

    public List<String> getConvertExts()
    {
        return Arrays.asList(docserviceConvertDocs.split("\\|"));
    }

    public String curUserHostAddress(String userAddress){
        if(userAddress == null){
            try{
                userAddress = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e){
                userAddress = "";
            }
        }
        return userAddress.replaceAll("[^0-9a-zA-Z.=]", "_");
    }

    public void createDirectory(Path path){
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteFilesRecursively(Path path) {
        FileSystemUtils.deleteRecursively(path.toFile());
    }

    public Resource loadFileAsResource(String fileLocation){
        try {
            Path filePath = Paths.get(fileLocation);
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) return resource;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String filesRootPath(String userAddress)
    {
        String directory = buildDirectoryLocation(userAddress);
        createDirectory(Paths.get(directory));

        return directory;
    }

    public String storagePath(String fileName, String userAddress)
    {
        String directory = filesRootPath(userAddress);

        return directory + fileUtility.getFileName(fileName);
    }

    public String forcesavePath(String fileName, String userAddress, Boolean create)
    {
        String directory = buildDirectoryLocation(userAddress);

        Path path = Paths.get(directory);
        if (!Files.exists(path)) return "";

        directory = directory + fileName + historyPostfix + File.separator;

        path = Paths.get(directory);
        if (!create && !Files.exists(path)) return "";

        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        directory = directory + fileName;
        path = Paths.get(directory);
        if (!create && !Files.exists(path)) {
            return "";
        }

        return directory;
    }

    public String historyDir(String storagePath)
    {
        return storagePath += historyPostfix;
    }

    public String versionDir(String histPath, Integer version)
    {
        return histPath + File.separator + Integer.toString(version);
    }

    public String versionDir(String fileName, String userAddress, Integer version)
    {
        return versionDir(historyDir(storagePath(fileName, userAddress)), version);
    }

    //TODO: NOT TESTED
    public int getFileVersion(String historyPath)
    {
        Path path = Paths.get(historyPath);
        if (!Files.exists(path)) return 0;

        try (Stream<Path> stream = Files.walk(path, 1)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet()).size();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getFileVersion(String fileName, String userAddress)
    {
        return getFileVersion(historyDir(storagePath(fileName, userAddress)));
    }

    // get a file name with an index if the file with such a name already exists
    public String getCorrectName(String fileName, String userAddress)
    {
        String baseName = fileUtility.getFileNameWithoutExtension(fileName);
        String ext = fileUtility.getFileExtension(fileName);
        String name = baseName + ext;

        Path path = Paths.get(storagePath(name, userAddress));

        for (int i = 1; Files.exists(path); i++)
        {
            name = baseName + " (" + i + ")" + ext;
            path = Paths.get(storagePath(name, userAddress));
        }

        return name;
    }

    public void createMeta(String fileName, String uid, String uname, String userAddress) throws IOException {
        String histDir = historyDir(storagePath(fileName, userAddress));

        Path path = Paths.get(histDir);
        createDirectory(path);

        JSONObject json = new JSONObject();
        json.put("created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        json.put("id", uid);
        json.put("name", uname);

        File meta = Files.createFile(Paths.get(histDir + File.separator + "createdInfo.json")).toFile();
        try (FileWriter writer = new FileWriter(meta)) {
            json.writeJSONString(writer);
        } catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public File[] getStoredFiles(String userAddress)
    {
        String directory = filesRootPath(userAddress);

        File file = new File(directory);
        return file.listFiles(pathname -> pathname.isFile());
    }

    public String getServerUrl(Boolean forDocumentServer) {
        if (forDocumentServer && !docserviceUrlExample.equals("")) {
            return docserviceUrlExample;
        } else {
            return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        }
    }

    public String getFileUri(String fileName, Boolean forDocumentServer)
    {
        try
        {
            String serverPath = getServerUrl(forDocumentServer);
            String hostAddress = curUserHostAddress(null);

            String filePath = serverPath + "/" + storageFolder
                    + "/" + hostAddress + "/"
                    + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()).replace("+", "%20");

            return filePath;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public String createToken(Map<String, Object> payloadClaims)
    {
        try
        {
            Signer signer = HMACSigner.newSHA256Signer(tokenSecret);
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

    public boolean tokenEnabled()
    {
        return tokenSecret != null && !tokenSecret.isEmpty();
    }

    public String getCallback(String fileName)
    {
        String serverPath = getServerUrl(true);
        String hostAddress = curUserHostAddress(null);
        try
        {
            String query = trackUrl+"?fileName="+
                    URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + "&userAddress=" + URLEncoder.encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());
            return serverPath + query;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public JWT readToken(String token)
    {
        try
        {
            Verifier verifier = HMACVerifier.newVerifier(tokenSecret);
            return JWT.getDecoder().decode(token, verifier);
        }
        catch (Exception exception)
        {
            return null;
        }
    }

    public String getDownloadUrl(String fileName) {
        String serverPath = getServerUrl(true);
        String hostAddress = curUserHostAddress(null);
        try
        {
            String query = downloadUrl+"?fileName="
                    + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + "&userAddress="
                    + URLEncoder.encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());

            return serverPath + query;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public ArrayList<Map<String, Object>> getFilesInfo(){
        ArrayList<Map<String, Object>> files = new ArrayList<>();

        for(File file : getStoredFiles(null)){
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("version", getFileVersion(file.getName(), null));
            map.put("id", serviceConverter
                    .generateRevisionId(curUserHostAddress(null) +
                            "/" + file.getName() + "/"
                            + Long.toString(Paths.get(storagePath(file.getName(), null))
                            .toFile()
                            .lastModified())));
            map.put("contentLength", new BigDecimal(String.valueOf((file.length()/1024.0)))
                    .setScale(2, RoundingMode.HALF_UP) + " KB");
            map.put("pureContentLength", file.length());
            map.put("title", file.getName());
            map.put("updated", String.valueOf(new Date(file.lastModified())));
            files.add(map);
        }

        return files;
    }

    public ArrayList<Map<String, Object>> getFilesInfo(String fileId){
        ArrayList<Map<String, Object>> file = new ArrayList<>();

        for (Map<String, Object> map : getFilesInfo()){
            if (map.get("id").equals(fileId)){
                file.add(map);
                break;
            }
        }

        return file;
    }

    private String buildDirectoryLocation(String userAddress){
        String hostAddress = curUserHostAddress(userAddress);
        String serverPath = System.getProperty("user.dir");
        if(userAddress != null){
            serverPath = request.getSession().getServletContext().getRealPath("");
        }
        String directory = serverPath + File.separator + storageFolder + File.separator + hostAddress + File.separator;

        return directory;
    }
}
