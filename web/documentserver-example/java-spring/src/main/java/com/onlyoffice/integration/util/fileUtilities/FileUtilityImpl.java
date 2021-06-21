package com.onlyoffice.integration.util.fileUtilities;

import com.onlyoffice.integration.entities.enums.DocumentType;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Component
public class FileUtilityImpl implements FileUtility {
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

        String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());
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
        // .docx for word file type
        if (type.equals(DocumentType.word))
            return ".docx";

        // .xlsx for cell file type
        if (type.equals(DocumentType.cell))
            return ".xlsx";

        // .pptx for slide file type
        if (type.equals(DocumentType.slide))
            return ".pptx";

        // the default file type is .docx
        return ".docx";
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
}
