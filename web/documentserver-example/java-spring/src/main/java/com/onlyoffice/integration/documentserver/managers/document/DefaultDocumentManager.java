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

package com.onlyoffice.integration.documentserver.managers.document;

import com.onlyoffice.integration.documentserver.storage.FileStorageMutator;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.documentserver.util.service.ServiceConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.onlyoffice.integration.documentserver.util.Constants.KILOBYTE_SIZE;

@Component
@Primary
public class DefaultDocumentManager implements DocumentManager {

    @Value("${files.storage.folder}")
    private String storageFolder;
    @Value("${files.storage}")
    private String filesStorage;
    @Value("${url.track}")
    private String trackUrl;
    @Value("${url.download}")
    private String downloadUrl;

    @Autowired
    private FileStorageMutator storageMutator;
    @Autowired
    private FileStoragePathBuilder storagePathBuilder;
    @Autowired
    private FileUtility fileUtility;
    @Autowired
    private ServiceConverter serviceConverter;

    // get URL to the created file
    public String getCreateUrl(final String fileName, final Boolean sample) {
        String fileExt = fileUtility.getFileExtension(fileName);
        String url = storagePathBuilder.getServerUrl(true)
                + "/create?fileExt=" + fileExt + "&sample=" + sample;
        return url;
    }

    // get a file name with an index if the file with such a name already exists
    public String getCorrectName(final String fileName) {
        String baseName = fileUtility.getFileNameWithoutExtension(fileName);  // get file name without extension
        String ext = fileUtility.getFileExtension(fileName);  // get file extension
        String name = baseName + "." + ext;  // create a full file name

        Path path = Paths.get(storagePathBuilder.getFileLocation(name));

        // run through all the files with such a name in the storage directory
        for (int i = 1; Files.exists(path); i++) {
            name = baseName + " (" + i + ")." + ext;  // and add an index to the base name
            path = Paths.get(storagePathBuilder.getFileLocation(name));
        }

        return name;
    }

    // get file URL
    public String getFileUri(final String fileName, final Boolean forDocumentServer) {
        try {
            String serverPath = storagePathBuilder.getServerUrl(forDocumentServer);  // get server URL
            String hostAddress = storagePathBuilder.getStorageLocation();  // get the storage directory
            String filePathDownload = !fileName.contains(InetAddress.getLocalHost().getHostAddress()) ? fileName
                    : fileName.substring(fileName.indexOf(InetAddress.getLocalHost()
                    .getHostAddress()) + InetAddress.getLocalHost().getHostAddress().length() + 1);
            if (!filesStorage.isEmpty() && filePathDownload.contains(filesStorage)) {
                filePathDownload = filePathDownload.substring(filesStorage.length() + 1);
            }

            String filePath = serverPath + "/download?fileName=" + URLEncoder
                    .encode(filePathDownload, java.nio.charset.StandardCharsets.UTF_8.toString()) + "&userAddress"
                    + URLEncoder.encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());
            return filePath;
        } catch (UnsupportedEncodingException | UnknownHostException e) {
            return "";
        }
    }

    // get file URL
    public String getHistoryFileUrl(final String fileName, final Integer version, final String file,
                                    final Boolean forDocumentServer) {
        try {
            String serverPath = storagePathBuilder.getServerUrl(forDocumentServer);  // get server URL
            String hostAddress = storagePathBuilder.getStorageLocation();  // get the storage directory
            String filePathDownload = !fileName.contains(InetAddress.getLocalHost().getHostAddress()) ? fileName
                    : fileName.substring(fileName.indexOf(InetAddress.getLocalHost().getHostAddress())
                    + InetAddress.getLocalHost().getHostAddress().length() + 1);
            String userAddress = forDocumentServer ? "&userAddress" + URLEncoder
                    .encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString()) : "";
            String filePath = serverPath + "/downloadhistory?fileName=" + URLEncoder
                    .encode(filePathDownload, java.nio.charset.StandardCharsets.UTF_8.toString())
                + "&ver=" + version + "&file=" + file
                + userAddress;
            return filePath;
        } catch (UnsupportedEncodingException | UnknownHostException e) {
            return "";
        }
    }

    // get the callback URL
    public String getCallback(final String fileName) {
        String serverPath = storagePathBuilder.getServerUrl(true);
        String storageAddress = storagePathBuilder.getStorageLocation();
        try {
            String query = trackUrl + "?fileName="
                    + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + "&userAddress=" + URLEncoder
                    .encode(storageAddress, java.nio.charset.StandardCharsets.UTF_8.toString());
            return serverPath + query;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    // get URL to download a file
    public String getDownloadUrl(final String fileName, final Boolean isServer) {
        String serverPath = storagePathBuilder.getServerUrl(isServer);
        String storageAddress = storagePathBuilder.getStorageLocation();
        try {
            String userAddress = isServer ? "&userAddress=" + URLEncoder
                    .encode(storageAddress, java.nio.charset.StandardCharsets.UTF_8.toString()) : "";
            String query = downloadUrl + "?fileName="
                    + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + userAddress;

            return serverPath + query;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    // get file information
    public ArrayList<Map<String, Object>> getFilesInfo() {
        ArrayList<Map<String, Object>> files = new ArrayList<>();

        // run through all the stored files
        for (File file : storageMutator.getStoredFiles()) {
            Map<String, Object> map = new LinkedHashMap<>();  // write all the parameters to the map
            map.put("version", storagePathBuilder.getFileVersion(file.getName(), false));
            map.put("id", serviceConverter
                    .generateRevisionId(storagePathBuilder.getStorageLocation()
                            + "/" + file.getName() + "/"
                            + Paths.get(storagePathBuilder.getFileLocation(file.getName()))
                            .toFile()
                            .lastModified()));
            map.put("contentLength", new BigDecimal(String.valueOf((file.length() / Double.valueOf(KILOBYTE_SIZE))))
                    .setScale(2, RoundingMode.HALF_UP) + " KB");
            map.put("pureContentLength", file.length());
            map.put("title", file.getName());
            map.put("updated", String.valueOf(new Date(file.lastModified())));
            files.add(map);
        }

        return files;
    }

    // get file information by its ID
    public ArrayList<Map<String, Object>> getFilesInfo(final String fileId) {
        ArrayList<Map<String, Object>> file = new ArrayList<>();

        for (Map<String, Object> map : getFilesInfo()) {
            if (map.get("id").equals(fileId)) {
                file.add(map);
                break;
            }
        }

        return file;
    }

    // get the path to the file version by the history path and file version
    public String versionDir(final String path, final Integer version, final boolean historyPath) {
        if (!historyPath) {
            return storagePathBuilder.getHistoryDir(storagePathBuilder.getFileLocation(path)) + version;
        }
        return path + File.separator + version;
    }

    // create demo document
    public String createDemo(final String fileExt, final Boolean sample, final String uid, final String uname) {
        String demoName = (sample ? "sample." : "new.")
                + fileExt;  // create sample or new template file with the necessary extension
        String demoPath =
            "assets"
            + File.separator
            + "document-templates"
            + File.separator
            + (sample ? "sample" : "new")
            + File.separator
            + demoName;

        // get a file name with an index if the file with such a name already exists
        String fileName = getCorrectName(demoName);

        InputStream stream = Thread.currentThread()
                                    .getContextClassLoader()
                                    .getResourceAsStream(demoPath);  // get the input file stream

        if (stream == null) {
            return null;
        }

        storageMutator.createFile(Path.of(storagePathBuilder
                .getFileLocation(fileName)), stream);  // create a file in the specified directory
        storageMutator.createMeta(fileName, uid, uname);  // create meta information of the demo file

        return fileName;
    }
}
