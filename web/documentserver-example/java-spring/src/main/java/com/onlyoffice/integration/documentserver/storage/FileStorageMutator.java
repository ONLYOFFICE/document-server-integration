package com.onlyoffice.integration.documentserver.storage;

import org.springframework.core.io.Resource;

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
    File[] getStoredFiles();  // get a collection of all the stored files
    void createMeta(String fileName, String uid, String uname);  // create the file meta information
    boolean createOrUpdateFile(Path path, InputStream stream);  // create or update a file
}
