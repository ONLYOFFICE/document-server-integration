package com.onlyoffice.integration.util.documentManagers;

import java.util.List;

public interface DocumentManagerExts {
    public List<String> getFileExts();
    public List<String> getViewedExts();
    public List<String> getEditedExts();
    public List<String> getConvertExts();
}
