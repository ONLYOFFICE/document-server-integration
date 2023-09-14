/**
 *
 * (c) Copyright Ascensio System SIA 2023
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
import com.onlyoffice.integration.documentserver.models.enums.ConvertErrorType;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.dto.Convert;
import com.onlyoffice.integration.dto.ConvertedData;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.onlyoffice.integration.documentserver.util.Constants.CONVERTATION_ERROR_MESSAGE_TEMPLATE;
import static com.onlyoffice.integration.documentserver.util.Constants.CONVERT_TIMEOUT_MS;
import static com.onlyoffice.integration.documentserver.util.Constants.FULL_LOADING_IN_PERCENT;
import static com.onlyoffice.integration.documentserver.util.Constants.MAX_KEY_LENGTH;

// todo: Refactoring
@Component
public class DefaultServiceConverter implements ServiceConverter {
    @Value("${files.docservice.header}")
    private String documentJwtHeader;
    @Value("${files.docservice.url.site}")
    private String docServiceUrl;
    @Value("${files.docservice.url.converter}")
    private String docServiceUrlConverter;
    @Value("${files.docservice.timeout}")
    private String docserviceTimeout;
    private int convertTimeout;

    @Autowired
    private JwtManager jwtManager;
    @Autowired
    private FileUtility fileUtility;
    @Autowired
    private JSONParser parser;
    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        int timeout = Integer.parseInt(docserviceTimeout);  // parse the dcoument service timeout value
        convertTimeout = timeout > 0 ? timeout : CONVERT_TIMEOUT_MS;
    }

    @SneakyThrows
    private String postToServer(final Convert body, final String headerToken) {  // send the POST request to the server
        String bodyString = objectMapper
                .writeValueAsString(body);  // write the body request to the object mapper in the string format
        URL url = null;
        java.net.HttpURLConnection connection = null;
        InputStream response = null;
        String jsonString = null;

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);  // convert body string into bytes
        try {
            // set the request parameters
            url = new URL(docServiceUrl + docServiceUrlConverter);
            connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setFixedLengthStreamingMode(bodyByte.length);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(convertTimeout);

            // check if the token is enabled
            if (jwtManager.tokenEnabled()) {
                // set the JWT header to the request
                connection.setRequestProperty(documentJwtHeader.isBlank()
                        ? "Authorization" : documentJwtHeader, "Bearer " + headerToken);
            }

            connection.connect();

            try (OutputStream os = connection.getOutputStream()) {
                os.write(bodyByte);  // write bytes to the output stream
                os.flush();  // force write data to the output stream that can be cached in the current thread
            }

            int statusCode = connection.getResponseCode();
            if (statusCode != HttpStatus.OK.value()) {  // checking status code
                connection.disconnect();
                throw new RuntimeException("Convertation service returned status: " + statusCode);
            }

            response = connection.getInputStream();  // get the input stream
            jsonString = convertStreamToString(response);  // convert the response stream into a string
        } finally {
            connection.disconnect();
            return jsonString;
        }
    }

    // get the URL to the converted file
    public ConvertedData getConvertedData(final String documentUri, final String fromExtension,
                                          final String toExtension, final String documentRevisionId,
                                          final String filePass, final Boolean isAsync, final String lang) {
        // check if the fromExtension parameter is defined; if not, get it from the document url
        String fromExt = fromExtension == null || fromExtension.isEmpty()
                ? fileUtility.getFileExtension(documentUri) : fromExtension;

        // check if the file name parameter is defined; if not, get random uuid for this file
        String title = fileUtility.getFileName(documentUri);
        title = title == null || title.isEmpty() ? UUID.randomUUID().toString() : title;

        String documentRevId = documentRevisionId == null || documentRevisionId.isEmpty()
                ? documentUri : documentRevisionId;

        documentRevId = generateRevisionId(documentRevId);  // create document token

        // write all the necessary parameters to the body object
        Convert body = new Convert();
        body.setLang(lang);
        body.setUrl(documentUri);
        body.setOutputtype(toExtension);
        body.setFiletype(fromExt);
        body.setTitle(title);
        body.setKey(documentRevId);
        body.setFilePass(filePass);
        if (isAsync) {
            body.setAsync(true);
        }

        String headerToken = "";
        if (jwtManager.tokenEnabled() && jwtManager.tokenUseForRequest()) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("region", lang);
            map.put("url", body.getUrl());
            map.put("outputtype", body.getOutputtype());
            map.put("filetype", body.getFiletype());
            map.put("title", body.getTitle());
            map.put("key", body.getKey());
            map.put("password", body.getFilePass());
            if (isAsync) {
                map.put("async", body.getAsync());
            }

            // add token to the body if it is enabled
            String token = jwtManager.createToken(map);
            body.setToken(token);

            Map<String, Object> payloadMap = new HashMap<String, Object>();
            payloadMap.put("payload", map);  // create payload object
            headerToken = jwtManager.createToken(payloadMap);  // create header token
        }

        String jsonString = postToServer(body, headerToken);

        return getResponseData(jsonString);
    }

    // generate document key
    public String generateRevisionId(final String expectedKey) {
        /* if the expected key length is greater than 20
        then he expected key is hashed and a fixed length value is stored in the string format */
        String formatKey = expectedKey.length() > MAX_KEY_LENGTH
                ? Integer.toString(expectedKey.hashCode()) : expectedKey;
        String key = formatKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), MAX_KEY_LENGTH));  // the resulting key length is 20 or less
    }

    // todo: Replace with a registry (callbacks package for reference)
    // create an error message for an error code
    private void processConvertServiceResponceError(final int errorCode) {
        String errorMessage = CONVERTATION_ERROR_MESSAGE_TEMPLATE + ConvertErrorType.labelOfCode(errorCode);

        throw new RuntimeException(errorMessage);
    }

    // get the response URL
    @SneakyThrows
    private ConvertedData getResponseData(final String jsonString) {
        JSONObject jsonObj = convertStringToJSON(jsonString);

        Object error = jsonObj.get("error");
        if (error != null) {  // if an error occurs
            processConvertServiceResponceError(Math.toIntExact((long) error));  // then get an error message
        }

        // check if the conversion is completed and save the result to a variable
        Boolean isEndConvert = (Boolean) jsonObj.get("endConvert");

        Long resultPercent = 0L;
        String responseUri = null;
        String responseFileType = null;
        ConvertedData convertedData = new ConvertedData("", "");

        if (isEndConvert) {  // if the conversion is completed
            resultPercent = FULL_LOADING_IN_PERCENT;
            responseUri = (String) jsonObj.get("fileUrl");  // get the file URL
            responseFileType = (String) jsonObj.get("fileType");  // get the file type
            convertedData.setUri(responseUri);
            convertedData.setFileType(responseFileType);
        } else {  // if the conversion isn't completed
            resultPercent = (Long) jsonObj.get("percent");

            // get the percentage value of the conversion process
            resultPercent = resultPercent >= FULL_LOADING_IN_PERCENT ? FULL_LOADING_IN_PERCENT - 1 : resultPercent;
        }

        return convertedData;
    }

    // convert stream to string
    @SneakyThrows
    public String convertStreamToString(final InputStream stream) {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);  // create an object to get incoming stream
        StringBuilder stringBuilder = new StringBuilder();  // create a string builder object

        // create an object to read incoming streams
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();  // get incoming streams by lines

        while (line != null) {
            stringBuilder.append(line);  // concatenate strings using the string builder
            line = bufferedReader.readLine();
        }

        String result = stringBuilder.toString();

        return result;
    }

    // convert string to json
    @SneakyThrows
    public JSONObject convertStringToJSON(final String jsonString) {
        Object obj = parser.parse(jsonString);  // parse json string
        JSONObject jsonObj = (JSONObject) obj;  // and turn it into a json object

        return jsonObj;
    }
}
