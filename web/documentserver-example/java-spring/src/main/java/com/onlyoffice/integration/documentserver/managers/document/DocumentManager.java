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

import java.util.ArrayList;
import java.util.Map;

// specify the document manager functions
public interface DocumentManager {
    String getCorrectName(String fileName);  // get a file name with an index if the file with such a name already exists
    String getFileUri(String fileName, Boolean forDocumentServer);  // get file URL
    String getCallback(String fileName);  // get the callback URL
    String getDownloadUrl(String fileName);  // get URL to download a file
    ArrayList<Map<String, Object>> getFilesInfo();  // get file information
    ArrayList<Map<String, Object>> getFilesInfo(String fileId);  // get file information by its ID
    String versionDir(String path, Integer version, boolean historyPath);  // get the path to the file version by the history path and file version
    String createDemo(String fileExt,Boolean sample,String uid,String uname) throws Exception;  // create demo document
    String getCreateUrl(String fileName, Boolean sample);  // get URL to the created file
}
