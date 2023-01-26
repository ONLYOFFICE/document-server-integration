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

import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

// specify the file storage mutator functions
public interface FileStorageMutator {
    void createDirectory(Path path);  // create a new directory if it does not exist
    boolean createFile(Path path, InputStream stream);  // create a new file if it does not exist
    boolean deleteFile(String fileName);  // delete a file
    boolean deleteFileHistory(String fileName);  // delete file history
    String updateFile(String fileName, byte[] bytes);  // update a file
    boolean writeToFile(String pathName, String payload);  // write the payload to the file
    boolean moveFile(Path source, Path destination);  // move a file to the specified destination
    Resource loadFileAsResource(String fileName);  // load file as a resource
    Resource loadFileAsResourceHistory(String fileName, String version, String file);  // load file as a resource
    File[] getStoredFiles();  // get a collection of all the stored files
    void createMeta(String fileName, String uid, String uname);  // create the file meta information
    boolean createOrUpdateFile(Path path, ByteArrayInputStream stream);  // create or update a file
}
