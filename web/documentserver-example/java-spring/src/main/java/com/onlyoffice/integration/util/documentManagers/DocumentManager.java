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

package com.onlyoffice.integration.util.documentManagers;

import org.springframework.core.io.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface DocumentManager {
    public long getMaxFileSize();
    public String curUserHostAddress(String userAddress);
    public String filesRootPath(String userAddress);
    public String storagePath(String fileName, String userAddress);
    public String forceSavePath(String fileName, String userAddress, Boolean create);
    public String historyDir(String storagePath);
    public String versionDir(String histPath, Integer version);
    public String versionDir(String fileName, String userAddress, Integer version);
    public int getFileVersion(String historyPath);
    public int getFileVersion(String fileName, String userAddress);
    public String getCorrectName(String fileName, String userAddress);
    public void createMeta(String fileName, String uid, String uname, String userAddress) throws IOException;
    public File[] getStoredFiles(String userAddress);
    public String getServerUrl(Boolean s);
    public String getFileUri(String fileName, Boolean forDocumentServer);
    public String getCallback(String fileName);
    public String getDownloadUrl(String fileName);
    public void deleteFilesRecursively(Path path);
    public Resource loadFileAsResource(String fileLocation);
    public ArrayList<Map<String, Object>> getFilesInfo();
    public ArrayList<Map<String, Object>> getFilesInfo(String fileId);
    public String createDemo(String fileExt,Boolean sample,String uid,String uname) throws Exception;
    public String getTemplateImageUrl(String fileName);
    public String getCreateUrl(String fileName, Boolean sample);
}
