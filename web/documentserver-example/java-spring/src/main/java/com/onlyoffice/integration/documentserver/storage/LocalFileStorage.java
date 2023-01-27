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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.onlyoffice.integration.documentserver.util.Constants.KILOBYTE_SIZE;

// todo: Refactoring
@Component
@Primary
public class LocalFileStorage implements FileStorageMutator, FileStoragePathBuilder {

    @Getter
    private String storageAddress;

    @Value("${files.storage.folder}")
    private String storageFolder;

    @Value("${files.docservice.url.example}")
    private String docserviceUrlExample;

    @Value("${files.docservice.history.postfix}")
    private String historyPostfix;

    @Autowired
    private FileUtility fileUtility;

    @Autowired
    private HttpServletRequest request;

    /*
        This Storage configuration method should be called whenever a new storage folder is required
     */
    public void configure(final String address) {
        this.storageAddress = address;
        if (this.storageAddress == null) {
            try {
                this.storageAddress = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                this.storageAddress = "unknown_storage";
            }
        }
        this.storageAddress.replaceAll("[^0-9a-zA-Z.=]", "_");
        createDirectory(Paths.get(getStorageLocation()));
    }

    // get the storage directory
    public String getStorageLocation() {
        String serverPath = System.getProperty("user.dir");  // get the path to the server
        String directory;  // create the storage directory
        if (Paths.get(this.storageAddress).isAbsolute()) {
            directory = this.storageAddress + File.separator;
        } else {
            directory = serverPath
                    + File.separator + storageFolder
                    + File.separator + this.storageAddress
                    + File.separator;
        }
        if (!Files.exists(Paths.get(directory))) {
            createDirectory(Paths.get(directory));
        }

        return directory;
    }

    // get the directory of the specified file
    public String getFileLocation(final String fileName) {
        if (fileName.contains(File.separator)) {
            return getStorageLocation() + fileName;
        }
        return getStorageLocation() + fileUtility.getFileName(fileName);
    }

    // create a new directory if it does not exist
    public void createDirectory(final Path path) {
        if (Files.exists(path)) {
            return;
        }
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // create a new file if it does not exist
    public boolean createFile(final Path path, final InputStream stream) {
        if (Files.exists(path)) {
            return true;
        }
        try {
            File file = Files.createFile(path).toFile();  // create a new file in the specified path
            try (FileOutputStream out = new FileOutputStream(file)) {
                int read;
                final byte[] bytes = new byte[KILOBYTE_SIZE];
                while ((read = stream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);  // write bytes to the output stream
                }
                out.flush();  // force write data to the output stream that can be cached in the current thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // delete a file
    public boolean deleteFile(final String fileNameParam) {
        String fileName = URLDecoder
                .decode(fileNameParam, StandardCharsets.UTF_8);  // decode a x-www-form-urlencoded string
        if (fileName.isBlank()) {
            return false;
        }

        String filenameWithoutExt = fileUtility
                .getFileNameWithoutExtension(fileName);  // get file name without extension

        Path filePath = fileName.contains(File.separator)
                ? Paths.get(fileName) : Paths.get(getFileLocation(fileName));  // get the path to the file
        Path filePathWithoutExt = fileName.contains(File.separator)
                ? Paths.get(filenameWithoutExt) : Paths
                .get(getStorageLocation() + filenameWithoutExt);  // get the path to the file without extension

        // delete the specified file; for directories, recursively delete any nested directories or files as well
        boolean fileDeleted = FileSystemUtils.deleteRecursively(filePath.toFile());
        /* delete the specified file without extension; for directories,
         recursively delete any nested directories or files as well */
        boolean fileWithoutExtDeleted = FileSystemUtils.deleteRecursively(filePathWithoutExt.toFile());

        return fileDeleted && fileWithoutExtDeleted;
    }

    // delete file history
    public boolean deleteFileHistory(final String fileNameParam) {
        String fileName = URLDecoder
                .decode(fileNameParam, StandardCharsets.UTF_8);  // decode a x-www-form-urlencoded string
        if (fileName.isBlank()) {
            return false;
        }

        Path fileHistoryPath = Paths
                .get(getStorageLocation() + getHistoryDir(fileName));  // get the path to the history file
        Path fileHistoryPathWithoutExt = Paths.get(getStorageLocation() + getHistoryDir(fileUtility
                .getFileNameWithoutExtension(fileName)));  // get the path to the history file without extension

        /* delete the specified history file; for directories,
         recursively delete any nested directories or files as well */
        boolean historyDeleted = FileSystemUtils.deleteRecursively(fileHistoryPath.toFile());

        /* delete the specified history file without extension; for directories,
         recursively delete any nested directories or files as well */
        boolean historyWithoutExtDeleted = FileSystemUtils.deleteRecursively(fileHistoryPathWithoutExt.toFile());

        return historyDeleted || historyWithoutExtDeleted;
    }

    // update a file
    public String updateFile(final String fileName, final byte[] bytes) {
        Path path = fileUtility
                .generateFilepath(getStorageLocation(), fileName);  // generate the path to the specified file
        try {
            Files.write(path, bytes);  // write new information in the bytes format to the file
            return path.getFileName().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    // move a file to the specified destination
    public boolean moveFile(final Path source, final Path destination) {
        try {
            Files.move(source, destination,
                    new StandardCopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // write the payload to the file
    public boolean writeToFile(final String pathName, final String payload) {
        try (FileWriter fw = new FileWriter(pathName)) {
            fw.write(payload);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // get the path where all the forcely saved file versions are saved or create it
    public String getForcesavePath(final String fileName, final Boolean create) {
        String directory = getStorageLocation();

        Path path = Paths.get(directory);  // get the storage directory
        if (!Files.exists(path)) {
            return "";
        }

        directory = getFileLocation(fileName) + historyPostfix + File.separator;

        path = Paths.get(directory);   // get the history file directory
        if (!create && !Files.exists(path)) {
            return "";
        }

        createDirectory(path);  // create a new directory where all the forcely saved file versions will be saved

        directory = directory + fileName;
        path = Paths.get(directory);
        if (!create && !Files.exists(path)) {
            return "";
        }

        return directory;
    }

    // load file as a resource
    public Resource loadFileAsResource(final String fileName) {
        String fileLocation = getForcesavePath(fileName,
                false);  // get the path where all the forcely saved file versions are saved
        if (fileLocation.isBlank()) {  // if file location is empty
            fileLocation = getFileLocation(fileName);  // get it by the file name
        }
        try {
            Path filePath = Paths.get(fileLocation);  // get the path to the file location
            Resource resource = new UrlResource(filePath.toUri());  // convert the file path to URL
            if (resource.exists()) {
                return resource;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Resource loadFileAsResourceHistory(final String fileName, final String version, final String file) {

        String fileLocation = getStorageLocation() + fileName + "-hist" + File.separator + version
                + File.separator + file;  // get it by the file name

        try {
            Path filePath = Paths.get(fileLocation);  // get the path to the file location
            Resource resource = new UrlResource(filePath.toUri());  // convert the file path to URL
            if (resource.exists()) {
                return resource;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // get a collection of all the stored files
    public File[] getStoredFiles() {
        File file = new File(getStorageLocation());
        return file.listFiles(pathname -> pathname.isFile());
    }

    @SneakyThrows
    public void createMeta(final String fileName,
                           final String uid,
                           final String uname) {  // create the file meta information
        String histDir = getHistoryDir(getFileLocation(fileName));  // get the history directory

        Path path = Paths.get(histDir);  // get the path to the history directory
        createDirectory(path);  // create the history directory

        // create the json object with the file metadata
        JSONObject json = new JSONObject();
        json.put("created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date()));  // put the file creation date to the json object
        json.put("id", uid);  // put the user ID to the json object
        json.put("name", uname);  // put the user name to the json object

        File meta = new File(histDir + File.separator
                + "createdInfo.json");  // create the createdInfo.json file with the file meta information
        try (FileWriter writer = new FileWriter(meta)) {
            json.writeJSONString(writer);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // create or update a file
    public boolean createOrUpdateFile(final Path path, final ByteArrayInputStream stream) {
        if (!Files.exists(path)) { // if the specified file does not exist
            return createFile(path, stream);  // create it in the specified directory
        } else {
            try {
                Files.write(path, stream
                        .readAllBytes());  // otherwise, write new information in the bytes format to the file
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // get the server URL
    public String getServerUrl(final Boolean forDocumentServer) {
        if (forDocumentServer && !docserviceUrlExample.equals("")) {
            return docserviceUrlExample;
        } else {
            return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                    + request.getContextPath();
        }
    }

    // get the history directory
    public String getHistoryDir(final String path) {
        return path + historyPostfix;
    }

    // get the file version
    public int getFileVersion(final String historyPath, final Boolean ifIndexPage) {
        Path path;
        if (ifIndexPage) {  // if the start page is opened
            path = Paths.get(getStorageLocation()
                    + getHistoryDir(historyPath));  // get the storage directory and add the history directory to it
        } else {
            path = Paths.get(historyPath);  // otherwise, get the path to the history directory
            if (!Files.exists(path)) {
                return 1;  // if the history directory does not exist, then the file version is 1
        }
        }

        // run through all the files in the history directory
        try (Stream<Path> stream = Files.walk(path, 1)) {
            return stream
                    .filter(file -> Files.isDirectory(file))  // take only directories from the history folder
                    .map(Path::getFileName)  // get file names
                    .map(Path::toString)  // and convert them into strings
                    .collect(Collectors.toSet()).size();  /* convert stream into set
                     and get its size which specifies the file version */
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
