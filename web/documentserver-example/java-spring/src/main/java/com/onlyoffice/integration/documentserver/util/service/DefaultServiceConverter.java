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
import com.onlyoffice.integration.documentserver.models.enums.ConvertErrorType;
import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.onlyoffice.integration.documentserver.util.Constants.CONVERTATION_ERROR_MESSAGE_TEMPLATE;
import static com.onlyoffice.integration.documentserver.util.Constants.CONVERT_TIMEOUT_MS;
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
