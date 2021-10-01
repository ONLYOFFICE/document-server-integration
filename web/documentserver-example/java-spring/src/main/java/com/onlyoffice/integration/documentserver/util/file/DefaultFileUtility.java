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
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Qualifier("default")
public class DefaultFileUtility implements FileUtility {
    @Value("${filesize-max}")
    private String filesizeMax;

    @Value("${files.docservice.viewed-docs}")
    private String docserviceViewedDocs;

    @Value("${files.docservice.edited-docs}")
    private String docserviceEditedDocs;

    @Value("${files.docservice.convert-docs}")
    private String docserviceConvertDocs;

    private List<String> ExtsDocument = Arrays.asList(
                            ".doc", ".docx", ".docm",
                            ".dot", ".dotx", ".dotm",
                            ".odt", ".fodt", ".ott", ".rtf", ".txt",
                            ".html", ".htm", ".mht", ".xml",
                            ".pdf", ".djvu", ".fb2", ".epub", ".xps");

    private List<String> ExtsSpreadsheet = Arrays.asList(
                            ".xls", ".xlsx", ".xlsm",
                            ".xlt", ".xltx", ".xltm",
                            ".ods", ".fods", ".ots", ".csv");

    private List<String> ExtsPresentation = Arrays.asList(
                            ".pps", ".ppsx", ".ppsm",
                            ".ppt", ".pptx", ".pptm",
                            ".pot", ".potx", ".potm",
                            ".odp", ".fodp", ".otp");

    public DocumentType getDocumentType(String fileName)
    {
        String ext = getFileExtension(fileName).toLowerCase();
        if (ExtsDocument.contains(ext))
            return DocumentType.word;

        if (ExtsSpreadsheet.contains(ext))
            return DocumentType.cell;

        if (ExtsPresentation.contains(ext))
            return DocumentType.slide;

        return DocumentType.word;
    }

    public String getFileName(String url)
    {
        if (url == null) return "";

        String fileName = url.substring(url.lastIndexOf('/') + 1);
        fileName = fileName.split("\\?")[0];
        return fileName;
    }

    public String getFileNameWithoutExtension(String url)
    {
        String fileName = getFileName(url);
        if (fileName == null) return null;
        String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        return fileNameWithoutExt;
    }

    public String getFileExtension(String url)
    {
        String fileName = getFileName(url);
        if (fileName == null) return null;
        String fileExt = fileName.substring(fileName.lastIndexOf("."));
        return fileExt.toLowerCase();
    }

    public String getInternalExtension(DocumentType type)
    {
        if (type.equals(DocumentType.word))
            return ".docx";

        if (type.equals(DocumentType.cell))
            return ".xlsx";

        if (type.equals(DocumentType.slide))
            return ".pptx";

        return ".docx";
    }

    public List<String> getViewedExts()
    {
        return Arrays.asList(docserviceViewedDocs.split("\\|"));
    }

    public List<String> getEditedExts()
    {
        return Arrays.asList(docserviceEditedDocs.split("\\|"));
    }

    public List<String> getConvertExts()
    {
        return Arrays.asList(docserviceConvertDocs.split("\\|"));
    }

    public List<String> getFileExts() {
        List<String> res = new ArrayList<>();

        res.addAll(getViewedExts());
        res.addAll(getEditedExts());
        res.addAll(getConvertExts());

        return res;
    }

    public Path generateFilepath(String directory, String fullFileName){
        String fileName = getFileNameWithoutExtension(fullFileName);
        String fileExtension = getFileExtension(fullFileName);
        Path path = Paths.get(directory+fullFileName);

        for(int i = 1; Files.exists(path); i++){
            fileName = getFileNameWithoutExtension(fullFileName) + "("+i+")";
            path = Paths.get(directory+fileName+fileExtension);
        }

        path = Paths.get(directory+fileName+fileExtension);
        return path;
    }

    public long getMaxFileSize(){
        long size = Long.parseLong(filesizeMax);
        return size > 0 ? size : 5 * 1024 * 1024;
    }
}
