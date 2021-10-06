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

public interface DocumentManager {
    String getCorrectName(String fileName);
    String getFileUri(String fileName, Boolean forDocumentServer);
    String getCallback(String fileName);
    String getDownloadUrl(String fileName);
    ArrayList<Map<String, Object>> getFilesInfo();
    ArrayList<Map<String, Object>> getFilesInfo(String fileId);
    String versionDir(String path, Integer version, boolean historyPath);
    String createDemo(String fileExt,Boolean sample,String uid,String uname) throws Exception;
    String getCreateUrl(String fileName, Boolean sample);
}
