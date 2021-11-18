package com.onlyoffice.integration.documentserver.storage;

// specify the file storage path builder functions
public interface FileStoragePathBuilder {
    void configure(String address);  // create a new storage folder
    String getStorageLocation();  // get the storage directory
    String getFileLocation(String fileName);  // get the directory of the specified file
    String getServerUrl(Boolean forDocumentServer);  // get the server URL
    String getHistoryDir(String fileName);  // get the history directory
    int getFileVersion(String historyPath, Boolean ifIndexPage);  // get the file version
    String getForcesavePath(String fileName, Boolean create);  // get the path where all the forcely saved file versions are saved or create it
}
