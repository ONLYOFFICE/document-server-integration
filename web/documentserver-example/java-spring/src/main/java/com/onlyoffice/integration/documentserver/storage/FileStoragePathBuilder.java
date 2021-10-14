package com.onlyoffice.integration.documentserver.storage;

public interface FileStoragePathBuilder {
    void configure(String address);
    String getStorageLocation();
    String getFileLocation(String fileName);
    String getServerUrl(Boolean forDocumentServer);
    String getHistoryDir(String fileName);
    int getFileVersion(String historyPath, Boolean ifIndexPage);
    String getForcesavePath(String fileName, Boolean create);
}
