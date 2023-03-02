package com.onlyoffice.integration.documentserver.models.filemodel;

import java.util.HashMap;
import java.util.Map;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferenceData {
    @Autowired
    private FileStoragePathBuilder storagePathBuilder;
    private final String instanceId;
    private final Map<String, String> fileKey;
    public ReferenceData(final String fileName, final String curUserHostAddress, final User user) {
        instanceId = storagePathBuilder.getServerUrl(true);
        Map<String, String> fileKeyList = new HashMap<>();
        if (!user.getId().equals("uid-0")) {
            fileKeyList.put("fileName", fileName);
            fileKeyList.put("userAddress", curUserHostAddress);
        } else {
            fileKeyList = null;
        }
        fileKey = fileKeyList;
    }
}
