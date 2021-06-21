package com.onlyoffice.integration.util.fileUtilities;

import com.onlyoffice.integration.entities.enums.DocumentType;

import java.nio.file.Path;

public interface FileUtility {
    public DocumentType getDocumentType(String fileName);
    public String getFileName(String url);
    public String getFileNameWithoutExtension(String url);
    public String getFileExtension(String url);
    public String getInternalExtension(DocumentType type);
    public Path generateFilepath(String directory, String fullFileName);
}
