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

package com.onlyoffice.integration.documentserver.util.file;

import com.onlyoffice.integration.documentserver.models.enums.DocumentType;

import java.nio.file.Path;
import java.util.List;

// specify the file utility functions
public interface FileUtility {
    DocumentType getDocumentType(String fileName);  // get the document type
    String getFileName(String url);  // get file name from its URL
    String getFileNameWithoutExtension(String url);  // get file name without extension
    String getFileExtension(String url);  // get file extension from URL
    String getInternalExtension(DocumentType type);  // get an editor internal extension
    List<String> getFileExts();  // get all the supported file extensions
    List<String> getFillExts();  // get file extensions that can be filled
    List<String> getViewedExts();  // get file extensions that can be viewed
    List<String> getEditedExts();  // get file extensions that can be edited
    List<String> getConvertExts();  // get file extensions that can be converted
    Path generateFilepath(String directory, String fullFileName);  /* generate the file path
     from file directory and name */
    long getMaxFileSize();  // get maximum file size
}
