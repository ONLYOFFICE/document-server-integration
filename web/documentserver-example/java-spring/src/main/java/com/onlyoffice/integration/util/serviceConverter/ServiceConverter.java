package com.onlyoffice.integration.util.serviceConverter;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;

public interface ServiceConverter {
    public String getConvertedUri(String documentUri, String fromExtension,
                                  String toExtension, String documentRevisionId,
                                  String filePass, Boolean isAsync) throws Exception;
    public String generateRevisionId(String expectedKey);
    public String convertStreamToString(InputStream stream) throws IOException;
    public JSONObject convertStringToJSON(String jsonString) throws ParseException;
}
