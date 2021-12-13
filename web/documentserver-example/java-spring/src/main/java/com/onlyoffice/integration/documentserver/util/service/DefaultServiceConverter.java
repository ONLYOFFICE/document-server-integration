/**
 *
 * (c) Copyright Ascensio System SIA 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.onlyoffice.integration.documentserver.util.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.integration.documentserver.managers.jwt.JwtManager;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.dto.Convert;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//TODO: Refactoring
@Component
public class DefaultServiceConverter implements ServiceConverter
{
    @Value("${files.docservice.header}")
    private String documentJwtHeader;
    @Value("${files.docservice.url.site}")
    private String docServiceUrl;
    @Value("${files.docservice.url.converter}")
    private String docServiceUrlConverter;
    @Value("${files.docservice.timeout}")
    private String docserviceTimeout;
    private int convertTimeout = 120000;

    @Autowired
    private JwtManager jwtManager;
    @Autowired
    private FileUtility fileUtility;
    @Autowired
    private JSONParser parser;
    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init(){
        int timeout = Integer.parseInt(docserviceTimeout);  // parse the dcoument service timeout value
        if (timeout > 0) convertTimeout = timeout;
    }

    @SneakyThrows
    private String postToServer(Convert body, String headerToken){  // send the POST request to the server
        String bodyString = objectMapper.writeValueAsString(body);  // write the body request to the object mapper in the string format
        URL url = null;
        java.net.HttpURLConnection connection = null;
        InputStream response = null;
        String jsonString = null;

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);  // convert body string into bytes
        try{
            // set the request parameters
            url = new URL(docServiceUrl+docServiceUrlConverter);
            connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setFixedLengthStreamingMode(bodyByte.length);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(convertTimeout);

            // check if the token is enabled
            if (jwtManager.tokenEnabled())
            {
                // set the JWT header to the request
                connection.setRequestProperty(documentJwtHeader.isBlank() ?
                        "Authorization" : documentJwtHeader, "Bearer " + headerToken);
            }

            connection.connect();
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bodyByte);  // write bytes to the output stream
                os.flush();  // force write data to the output stream that can be cached in the current thread
            }

            response = connection.getInputStream();  // get the input stream
            jsonString = convertStreamToString(response);  // convert the response stream into a string
        } finally {
            connection.disconnect();
            return jsonString;
        }
    }

    // get the URL to the converted file
    public String getConvertedUri(String documentUri, String fromExtension,
                                  String toExtension, String documentRevisionId,
                                  String filePass, Boolean isAsync, String lang)
    {
        // check if the fromExtension parameter is defined; if not, get it from the document url
        fromExtension = fromExtension == null || fromExtension.isEmpty() ?
                fileUtility.getFileExtension(documentUri) : fromExtension;

        // check if the file name parameter is defined; if not, get random uuid for this file
        String title = fileUtility.getFileName(documentUri);
        title = title == null || title.isEmpty() ? UUID.randomUUID().toString() : title;

        documentRevisionId = documentRevisionId == null || documentRevisionId.isEmpty() ? documentUri : documentRevisionId;

        documentRevisionId = generateRevisionId(documentRevisionId);  // create document token

        // write all the necessary parameters to the body object
        Convert body = new Convert();
        body.setLang(lang);
        body.setUrl(documentUri);
        body.setOutputtype(toExtension.replace(".", ""));
        body.setFiletype(fromExtension.replace(".", ""));
        body.setTitle(title);
        body.setKey(documentRevisionId);
        body.setFilePass(filePass);
        if (isAsync)
            body.setAsync(true);

        String headerToken = "";
        if (jwtManager.tokenEnabled())
        {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("region", lang);
            map.put("url", body.getUrl());
            map.put("outputtype", body.getOutputtype());
            map.put("filetype", body.getFiletype());
            map.put("title", body.getTitle());
            map.put("key", body.getKey());
            map.put("password", body.getFilePass());
            if (isAsync)
                map.put("async", body.getAsync());

            // add token to the body if it is enabled
            String token = jwtManager.createToken(map);
            body.setToken(token);

            Map<String, Object> payloadMap = new HashMap<String, Object>();
            payloadMap.put("payload", map);  // create payload object
            headerToken = jwtManager.createToken(payloadMap);  // create header token
        }

        String jsonString = postToServer(body, headerToken);

        return getResponseUri(jsonString);
    }

    // generate document key
    public String generateRevisionId(String expectedKey)
    {
        if (expectedKey.length() > 20)  // if the expected key length is greater than 20
            expectedKey = Integer.toString(expectedKey.hashCode());  // the expected key is hashed and a fixed length value is stored in the string format

        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), 20));  // the resulting key length is 20 or less
    }

    //TODO: Replace with a registry (callbacks package for reference)
    private void processConvertServiceResponceError(int errorCode)  // create an error message for an error code
    {
        String errorMessage = "";
        String errorMessageTemplate = "Error occurred in the ConvertService: ";

        // add the error message to the error message template depending on the error code
        switch (errorCode)
        {
            case -8:
                errorMessage = errorMessageTemplate + "Error document VKey";
                break;
            case -7:
                errorMessage = errorMessageTemplate + "Error document request";
                break;
            case -6:
                errorMessage = errorMessageTemplate + "Error database";
                break;
            case -5:
                errorMessage = errorMessageTemplate + "Error unexpected guid";
                break;
            case -4:
                errorMessage = errorMessageTemplate + "Error download error";
                break;
            case -3:
                errorMessage = errorMessageTemplate + "Error convertation error";
                break;
            case -2:
                errorMessage = errorMessageTemplate + "Error convertation timeout";
                break;
            case -1:
                errorMessage = errorMessageTemplate + "Error convertation unknown";
                break;
            case 0:  // if the error code is equal to 0, the error message is empty
                break;
            default:
                errorMessage = "ErrorCode = " + errorCode;  // default value for the error message
                break;
        }

        throw new RuntimeException(errorMessage);
    }

    @SneakyThrows
    private String getResponseUri(String jsonString)  // get the response URL
    {
        JSONObject jsonObj = convertStringToJSON(jsonString);

        Object error = jsonObj.get("error");
        if (error != null)  // if an error occurs
            processConvertServiceResponceError(Math.toIntExact((long)error));  // then get an error message

        // check if the conversion is completed and save the result to a variable
        Boolean isEndConvert = (Boolean) jsonObj.get("endConvert");

        Long resultPercent = 0l;
        String responseUri = null;

        if (isEndConvert)  // if the conversion is completed
        {
            resultPercent = 100l;
            responseUri = (String) jsonObj.get("fileUrl");  // get the file URL
        }
        else  // if the conversion isn't completed
        {
            resultPercent = (Long) jsonObj.get("percent");
            resultPercent = resultPercent >= 100l ? 99l : resultPercent;  // get the percentage value of the conversion process
        }

        return resultPercent >= 100l ? responseUri : "";
    }

    @SneakyThrows
    public String convertStreamToString(InputStream stream)  // convert stream to string
    {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);  // create an object to get incoming stream
        StringBuilder stringBuilder = new StringBuilder();  // create a string builder object
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);  // create an object to read incoming streams
        String line = bufferedReader.readLine();  // get incoming streams by lines

        while (line != null)
        {
            stringBuilder.append(line);  // concatenate strings using the string builder
            line = bufferedReader.readLine();
        }

        String result = stringBuilder.toString();

        return result;
    }

    @SneakyThrows
    public JSONObject convertStringToJSON(String jsonString)  // convert string to json
    {
        Object obj = parser.parse(jsonString);  // parse json string
        JSONObject jsonObj = (JSONObject) obj;  // and turn it into a json object

        return jsonObj;
    }
}