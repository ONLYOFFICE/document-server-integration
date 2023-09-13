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

import com.onlyoffice.integration.documentserver.models.Format;
import com.onlyoffice.integration.documentserver.models.enums.DocumentType;
import com.onlyoffice.integration.documentserver.util.service.FormatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.onlyoffice.integration.documentserver.util.Constants.MAX_FILE_SIZE;

@Component
@Qualifier("default")
public class DefaultFileUtility implements FileUtility {
    @Value("${filesize-max}")
    private String filesizeMax;

    @Autowired
    private FormatService formatService;

    // get the document type
    public DocumentType getDocumentType(final String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();  // get file extension from its name

        List<Format> formats = formatService.getFormats();
        for (Format format : formats) {
            if (format.getName().equals(ext)) {
                return format.getType();
            }
        }

        // default file type is word
        return DocumentType.word;
    }

    // get file name from its URL
    public String getFileName(final String url) {
        if (url == null) {
            return "";
        }

        // get file name from the last part of URL
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        fileName = fileName.split("\\?")[0];
        return fileName;
    }

    // get file name without extension
    public String getFileNameWithoutExtension(final String url) {
        String fileName = getFileName(url);
        if (fileName == null) {
            return null;
        }
        String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        return fileNameWithoutExt;
    }

    // get file extension from URL
    public String getFileExtension(final String url) {
        String fileName = getFileName(url);
        if (fileName == null) {
            return null;
        }
        String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1);
        return fileExt.toLowerCase();
    }

    // get an editor internal extension
    public String getInternalExtension(final DocumentType type) {
        // .docx for word file type
        if (type.equals(DocumentType.word)) {
            return ".docx";
        }

        // .xlsx for cell file type
        if (type.equals(DocumentType.cell)) {
            return ".xlsx";
        }

        // .pptx for slide file type
        if (type.equals(DocumentType.slide)) {
            return ".pptx";
        }

        // the default file type is .docx
        return ".docx";
    }

    public List<String> getFillExts() {
        return formatService.fillableExtensions();
    }

    // get file extensions that can be viewed
    public List<String> getViewedExts() {
        return formatService.viewableExtensions();
    }

    // get file extensions that can be edited
    public List<String> getEditedExts() {
        return formatService.editableExtensions();
    }

    // get file extensions that can be converted
    public List<String> getConvertExts() {
        return formatService.autoConvertExtensions();
    }

    // get all the supported file extensions
    public List<String> getFileExts() {
        return formatService.allExtensions();
    }

    // generate the file path from file directory and name
    public Path generateFilepath(final String directory, final String fullFileName) {
        String fileName = getFileNameWithoutExtension(fullFileName);  // get file name without extension
        String fileExtension = getFileExtension(fullFileName);  // get file extension
        Path path = Paths.get(directory + fullFileName);  // get the path to the files with the specified name

        for (int i = 1; Files.exists(path); i++) {  // run through all the files with the specified name
            // get a name of each file without extension and add an index to it
            fileName = getFileNameWithoutExtension(fullFileName) + "(" + i + ")";

            // create a new path for this file with the correct name and extension
            path = Paths.get(directory + fileName + "." + fileExtension);
        }

        path = Paths.get(directory + fileName + "." + fileExtension);
        return path;
    }

    // get maximum file size
    public long getMaxFileSize() {
        long size = Long.parseLong(filesizeMax);
        return size > 0 ? size : MAX_FILE_SIZE;
    }
}
