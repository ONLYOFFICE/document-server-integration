package com.onlyoffice.integration.util.documentManagers;

import org.primeframework.jwt.domain.JWT;
import org.springframework.core.io.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//TODO: Segregate the interface
public interface DocumentManager {
    public long getMaxFileSize();
    public List<String> getFileExts();
    public List<String> getViewedExts();
    public List<String> getEditedExts();
    public List<String> getConvertExts();
    public String curUserHostAddress(String userAddress);
    public String filesRootPath(String userAddress);
    public String storagePath(String fileName, String userAddress);
    public String forcesavePath(String fileName, String userAddress, Boolean create);
    public String historyDir(String storagePath);
    public String versionDir(String histPath, Integer version);
    public String versionDir(String fileName, String userAddress, Integer version);
    public int getFileVersion(String historyPath);
    public int getFileVersion(String fileName, String userAddress);
    public String getCorrectName(String fileName, String userAddress);
    public void createMeta(String fileName, String uid, String uname, String userAddress) throws IOException;
    public File[] getStoredFiles(String userAddress);
    public String getServerUrl(Boolean s);
    public String getFileUri(String fileName, Boolean forDocumentServer);
    public boolean tokenEnabled();
    public String createToken(Map<String, Object> payloadClaims);
    public String getCallback(String fileName);
    public JWT readToken(String token);
    public String getDownloadUrl(String fileName);
    public void deleteFilesRecursively(Path path);
    public Resource loadFileAsResource(String fileLocation);
    public ArrayList<Map<String, Object>> getFilesInfo();
    public ArrayList<Map<String, Object>> getFilesInfo(String fileId);
}
