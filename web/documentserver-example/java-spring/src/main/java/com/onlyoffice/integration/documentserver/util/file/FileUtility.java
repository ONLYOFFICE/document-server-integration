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

package com.onlyoffice.integration.documentserver.util.file;

import com.onlyoffice.integration.documentserver.models.enums.DocumentType;

import java.nio.file.Path;
import java.util.List;

public interface FileUtility {
    DocumentType getDocumentType(String fileName);
    String getFileName(String url);
    String getFileNameWithoutExtension(String url);
    String getFileExtension(String url);
    String getInternalExtension(DocumentType type);
    List<String> getFileExts();
    List<String> getViewedExts();
    List<String> getEditedExts();
    List<String> getConvertExts();
    Path generateFilepath(String directory, String fullFileName);
    long getMaxFileSize();
}
