package com.onlyoffice.integration.documentserver.storage;

import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public interface FileStorageMutator {
    void createDirectory(Path path);
    boolean createFile(Path path, InputStream stream);
    boolean deleteFile(String fileName);
    boolean deleteFileHistory(String fileName);
    String updateFile(String fileName, byte[] bytes);
    boolean writeToFile(String pathName, String payload);
    boolean moveFile(Path source, Path destination);
    Resource loadFileAsResource(String fileName);
    File[] getStoredFiles();
    void createMeta(String fileName, String uid, String uname);
}
