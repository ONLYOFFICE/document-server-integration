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

package com.onlyoffice.integration.documentserver.managers.document;

import com.onlyoffice.integration.documentserver.storage.FileStorageMutator;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.documentserver.util.service.ServiceConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.*;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
@Primary
public class DefaultDocumentManager implements DocumentManager {

    @Value("${files.storage.folder}")
    private String storageFolder;
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

    public String getCreateUrl(String fileName, Boolean sample){
        String fileExt = fileName.substring(fileName.length() - 4);
        String url = storagePathBuilder.getServerUrl(true) + "/create?fileExt=" + fileExt + "&sample=" + sample;
        return url;
    }

    public String getCorrectName(String fileName)
    {
        String baseName = fileUtility.getFileNameWithoutExtension(fileName);
        String ext = fileUtility.getFileExtension(fileName);
        String name = baseName + ext;

        Path path = Paths.get(storagePathBuilder.getFileLocation(name));

        for (int i = 1; Files.exists(path); i++)
        {
            name = baseName + " (" + i + ")" + ext;
            path = Paths.get(storagePathBuilder.getFileLocation(name));
        }

        return name;
    }

    public String getFileUri(String fileName, Boolean forDocumentServer)
    {
        try
        {
            String serverPath = storagePathBuilder.getServerUrl(forDocumentServer);
            String hostAddress = storagePathBuilder.getStorageLocation();
            String filePathDownload = !fileName.contains(InetAddress.getLocalHost().getHostAddress()) ? fileName
                    : fileName.substring(fileName.indexOf(InetAddress.getLocalHost().getHostAddress()) + InetAddress.getLocalHost().getHostAddress().length() + 1);

            String filePath = serverPath + "/download?fileName=" + URLEncoder.encode(filePathDownload, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + "&userAddress" + URLEncoder.encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());
            return filePath;
        }
        catch (UnsupportedEncodingException | UnknownHostException e)
        {
            return "";
        }
    }

    public String getCallback(String fileName)
    {
        String serverPath = storagePathBuilder.getServerUrl(true);
        String storageAddress = storagePathBuilder.getStorageLocation();
        try
        {
            String query = trackUrl+"?fileName="+
                    URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + "&userAddress=" + URLEncoder.encode(storageAddress, java.nio.charset.StandardCharsets.UTF_8.toString());
            return serverPath + query;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public String getDownloadUrl(String fileName) {
        String serverPath = storagePathBuilder.getServerUrl(true);
        String storageAddress = storagePathBuilder.getStorageLocation();
        try
        {
            String query = downloadUrl+"?fileName="
                    + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + "&userAddress="
                    + URLEncoder.encode(storageAddress, java.nio.charset.StandardCharsets.UTF_8.toString());

            return serverPath + query;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public ArrayList<Map<String, Object>> getFilesInfo(){
        ArrayList<Map<String, Object>> files = new ArrayList<>();

        for(File file : storageMutator.getStoredFiles()){
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("version", storagePathBuilder.getFileVersion(file.getName(), false));
            map.put("id", serviceConverter
                    .generateRevisionId(storagePathBuilder.getStorageLocation() +
                            "/" + file.getName() + "/"
                            + Paths.get(storagePathBuilder.getFileLocation(file.getName()))
                            .toFile()
                            .lastModified()));
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

    public String versionDir(String path, Integer version, boolean historyPath) {
        if (!historyPath){
            return storagePathBuilder.getHistoryDir(storagePathBuilder.getFileLocation(path)) + version;
        }
        return path + File.separator + version;
    }

    public String createDemo(String fileExt,Boolean sample,String uid,String uname) {
        String demoName = (sample ?"sample.":"new." + fileExt);
        String demoPath = "assets" + File.separator  + (sample ? "sample" : "new") + File.separator + demoName;
        String fileName = getCorrectName(demoName);

        InputStream stream = Thread.currentThread()
                                    .getContextClassLoader()
                                    .getResourceAsStream(demoPath);

        if (stream == null) return null;

        storageMutator.createFile(Path.of(storagePathBuilder.getFileLocation(fileName)), stream);
        storageMutator.createMeta(fileName, uid, uname);

        return fileName;
    }
}
