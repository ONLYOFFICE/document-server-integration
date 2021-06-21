package com.onlyoffice.integration.controllers.objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConverterBody {
    @JsonProperty("filename")
    private String fileName;
    @JsonProperty("filePass")
    private String filePass;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePass() {
        return filePass;
    }

    public void setFilePass(String filePass) {
        this.filePass = filePass;
    }
}
