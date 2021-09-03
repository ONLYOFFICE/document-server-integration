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

package com.onlyoffice.integration.documentserver.storage;

import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import lombok.Getter;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO: Refactoring
@Component
@Primary
public class LocalIntegrationStorage implements IntegrationStorage {

    @Getter
    private String storageAddress;

    @Value("${files.storage.folder}")
    private String storageFolder;

    @Value("${files.docservice.url.example}")
    private String docserviceUrlExample;

    @Value("${files.docservice.history.postfix}")
    private String historyPostfix;

    //TODO: Get rid of HttpServletRequest
    @Autowired
    private HttpServletRequest request;

    @Autowired
    private FileUtility fileUtility;

    /*
        This Storage configuration method should be called whenever a new storage folder is required
     */
    public void configure(String address) {
        this.storageAddress = address;
        if(this.storageAddress == null){
            try{
                this.storageAddress = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e){
                this.storageAddress = "";
            }
        }
        this.storageAddress.replaceAll("[^0-9a-zA-Z.=]", "_");
        createDirectory(Paths.get(getStorageLocation()));
    }

    public String getStorageLocation(){
        String serverPath = System.getProperty("user.dir");
        String directory = serverPath
                + File.separator + storageFolder
                + File.separator + this.storageAddress
                + File.separator;

        return directory;
    }

    public String getFileLocation(String fileName){
        return getStorageLocation() + fileUtility.getFileName(fileName);
    }

    public void createDirectory(Path path){
        if (Files.exists(path)) return;
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean createFile(String fileName, InputStream stream) {
        String fileLocation = getFileLocation(fileName);

        if (Files.exists(Path.of(fileLocation))){
            return true;
        }

        File convertedFile = new File(fileLocation);
        try (FileOutputStream out = new FileOutputStream(convertedFile))
        {
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = stream.read(bytes)) != -1)
            {
                out.write(bytes, 0, read);
            }

            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean createFile(String fileName){
        if (fileName.isBlank()) return false;

        String fileLocation = getFileLocation(fileName);
        Path filePath = Path.of(fileLocation);

        if (Files.exists(filePath)) return true;

        try{
            Files.createFile(filePath);
            return true;
        } catch (IOException exception){
            return false;
        }
    }

    public boolean createFile(Path path, InputStream stream){
        try {
            File file = Files.createFile(path).toFile();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteFile(String fileName){
        if (fileName.isBlank()) return false;

        String filenameWithoutExt = fileUtility.getFileNameWithoutExtension(fileName);

        Path filePath = Paths.get(getFileLocation(fileName));
        Path filePathWithoutExt = Paths.get(getStorageLocation() + filenameWithoutExt);
        Path fileHistoryPath = Paths.get(getStorageLocation() + historyDir(fileName));
        Path fileHistoryPathWithoutExt = Paths.get(getStorageLocation() + historyDir(filenameWithoutExt));

        boolean fileDeleted = FileSystemUtils.deleteRecursively(filePath.toFile());
        boolean fileWithoutExtDeleted = FileSystemUtils.deleteRecursively(filePathWithoutExt.toFile());
        boolean historyDeleted = FileSystemUtils.deleteRecursively(fileHistoryPath.toFile());
        boolean historyWithoutExtDeleted = FileSystemUtils.deleteRecursively(fileHistoryPathWithoutExt.toFile());

        return fileDeleted && fileWithoutExtDeleted && (historyDeleted || historyWithoutExtDeleted);
    }

    public String updateFile(String fileName, byte[] bytes) {
        Path path = fileUtility.generateFilepath(getStorageLocation(), fileName);
        try {
            Files.write(path, bytes);
            return path.getFileName().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean moveFile(Path source, Path destination){
        try {
            Files.move(source, destination,
                    new StandardCopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean writeToFile(String pathName, String payload){
        try (FileWriter fw = new FileWriter(pathName)) {
            fw.write(payload);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getForcesavePath(String fileName, Boolean create) {
        String directory = getStorageLocation();

        Path path = Paths.get(directory);
        if (!Files.exists(path)) return "";

        directory = getFileLocation(fileName) + historyPostfix + File.separator;

        path = Paths.get(directory);
        if (!create && !Files.exists(path)) return "";

        createDirectory(path);

        directory = directory + fileName;
        path = Paths.get(directory);
        if (!create && !Files.exists(path)) {
            return "";
        }

        return directory;
    }

    public Resource loadFileAsResource(String fileName){
        String fileLocation = getForcesavePath(fileName, false);
        if (fileLocation.isBlank()){
            fileLocation = getFileLocation(fileName);
        }
        try {
            Path filePath = Paths.get(fileLocation);
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) return resource;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public File[] getStoredFiles()
    {
        File file = new File(getStorageLocation());
        return file.listFiles(pathname -> pathname.isFile());
    }

    @SneakyThrows
    public void createMeta(String fileName, String uid, String uname) {
        String histDir = historyDir(getFileLocation(fileName));

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

    public String getServerUrl(Boolean forDocumentServer) {
        if (forDocumentServer && !docserviceUrlExample.equals("")) {
            return docserviceUrlExample;
        } else {
            return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        }
    }

    public String historyDir(String path)
    {
        return path + historyPostfix;
    }

    public int getFileVersion(String historyPath, Boolean ifIndexPage)
    {
        Path path;
        if (ifIndexPage) {
            path = Paths.get(getStorageLocation() + historyDir(historyPath));
        } else {
            path = Paths.get(historyPath);
            if (!Files.exists(path)) return 1;
        }

        try (Stream<Path> stream = Files.walk(path, 1)) {
            return stream
                    .filter(file -> Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet()).size();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
