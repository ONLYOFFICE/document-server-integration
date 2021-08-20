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

import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

//TODO: Refactoring
public interface IntegrationStorage {
    void configure(String address);
    String getStorageLocation();
    String getFileLocation(String fileName);
    String getServerUrl(Boolean forDocumentServer);
    void createDirectory(Path path);
    boolean createFile(String fileName, InputStream stream);
    boolean createFile(Path path, InputStream stream);
    boolean createFile(String fileName);
    boolean deleteFile(String fileName);
    String updateFile(String fileName, byte[] bytes);
    boolean writeToFile(String pathName, String payload);
    boolean moveFile(Path source, Path destination);
    Resource loadFileAsResource(String fileName);
    File[] getStoredFiles();
    void createMeta(String fileName, String uid, String uname);
    String historyDir(String fileName);
    int getFileVersion(String historyPath);
    //TODO: Remove getForcesave
    String getForcesavePath(String fileName, Boolean create);
}
