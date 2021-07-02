package com.onlyoffice.integration.util.documentManagers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DocumentManagerExtsImpl implements DocumentManagerExts {
    @Value("${files.docservice.viewed-docs}")
    private String docserviceViewedDocs;

    @Value("${files.docservice.edited-docs}")
    private String docserviceEditedDocs;

    @Value("${files.docservice.convert-docs}")
    private String docserviceConvertDocs;

    public List<String> getFileExts() {
        List<String> res = new ArrayList<>();

        res.addAll(getViewedExts());
        res.addAll(getEditedExts());
        res.addAll(getConvertExts());

        return res;
    }

    public List<String> getViewedExts()
    {
        return Arrays.asList(docserviceViewedDocs.split("\\|"));
    }

    public List<String> getEditedExts()
    {
        return Arrays.asList(docserviceEditedDocs.split("\\|"));
    }

    public List<String> getConvertExts()
    {
        return Arrays.asList(docserviceConvertDocs.split("\\|"));
    }
}
