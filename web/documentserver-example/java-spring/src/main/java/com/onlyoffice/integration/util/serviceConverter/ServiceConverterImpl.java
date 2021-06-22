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

package com.onlyoffice.integration.util.serviceConverter;

import com.google.gson.Gson;
import com.onlyoffice.integration.util.documentManagers.DocumentManager;
import com.onlyoffice.integration.util.fileUtilities.FileUtility;
import com.onlyoffice.integration.util.objects.ConvertBody;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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

@Component
public class ServiceConverterImpl implements ServiceConverter
{

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private FileUtility fileUtility;

    private int convertTimeout = 120000;

    @Value("${files.docservice.header}")
    private String documentJwtHeader;

    @Value("${files.docservice.url.site}")
    private String docServiceUrl;

    @Value("${files.docservice.url.converter}")
    private String docServiceUrlConverter;

    @Value("${files.docservice.timeout}")
    private String docserviceTimeout;


    @PostConstruct
    public void init(){
        int timeout = Integer.parseInt(docserviceTimeout);
        if(timeout > 0) convertTimeout = timeout;
    }

    private String postToServer(ConvertBody body, String headerToken){
        Gson gson = new Gson();
        String bodyString = gson.toJson(body);
        URL url = null;
        java.net.HttpURLConnection connection = null;
        InputStream response = null;
        String jsonString = null;

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);
        try{
            url = new URL(docServiceUrl+docServiceUrlConverter);
            connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setFixedLengthStreamingMode(bodyByte.length);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(convertTimeout);

            if (documentManager.tokenEnabled())
            {
                connection.setRequestProperty(documentJwtHeader.equals("") ?
                        "Authorization" : documentJwtHeader, "Bearer " + headerToken);
            }

            connection.connect();
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bodyByte);
                os.flush(); //
            }

            response = connection.getInputStream();
            jsonString = convertStreamToString(response);
        } catch (java.io.IOException e){
            System.out.println(e);
        } finally {
            connection.disconnect();
            return jsonString;
        }
    }

    public String getConvertedUri(String documentUri, String fromExtension,
                                  String toExtension, String documentRevisionId,
                                  String filePass, Boolean isAsync) throws Exception
    {
        fromExtension = fromExtension == null || fromExtension.isEmpty() ?
                fileUtility.getFileExtension(documentUri) : fromExtension;

        // check if the file name parameter is defined; if not, get random uuid for this file
        String title = fileUtility.getFileName(documentUri);
        title = title == null || title.isEmpty() ? UUID.randomUUID().toString() : title;

        documentRevisionId = documentRevisionId == null || documentRevisionId.isEmpty() ? documentUri : documentRevisionId;

        documentRevisionId = generateRevisionId(documentRevisionId);  // create document token

        // write all the necessary parameters to the body object
        ConvertBody body = new ConvertBody();
        body.setUrl(documentUri);
        body.setOutputtype(toExtension.replace(".", ""));
        body.setFiletype(fromExtension.replace(".", ""));
        body.setTitle(title);
        body.setKey(documentRevisionId);
        body.setFilePass(filePass);
        if (isAsync)
            body.setAsync(true);

        String headerToken = "";
        if (documentManager.tokenEnabled())
        {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("url", body.getUrl());
            map.put("outputtype", body.getOutputtype());
            map.put("filetype", body.getFiletype());
            map.put("title", body.getTitle());
            map.put("key", body.getKey());
            map.put("password", body.getFilePass());
            if (isAsync)
                map.put("async", body.getAsync());

            // add token to the body if it is enabled
            String token = documentManager.createToken(map);
            body.setToken(token);

            Map<String, Object> payloadMap = new HashMap<String, Object>();
            payloadMap.put("payload", map);  // create payload object
            headerToken = documentManager.createToken(payloadMap);  // create header token
        }

        String jsonString = postToServer(body, headerToken);

        return getResponseUri(jsonString);
    }

    public String generateRevisionId(String expectedKey)
    {
        if (expectedKey.length() > 20)
            expectedKey = Integer.toString(expectedKey.hashCode());

        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), 20));
    }

    private void processConvertServiceResponceError(int errorCode) throws Exception
    {
        String errorMessage = "";
        String errorMessageTemplate = "Error occurred in the ConvertService: ";

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
            case 0:
                break;
            default:
                errorMessage = "ErrorCode = " + errorCode;
                break;
        }

        throw new Exception(errorMessage);
    }

    private String getResponseUri(String jsonString) throws Exception
    {
        JSONObject jsonObj = convertStringToJSON(jsonString);

        Object error = jsonObj.get("error");
        if (error != null)
            processConvertServiceResponceError(Math.toIntExact((long)error));

        Boolean isEndConvert = (Boolean) jsonObj.get("endConvert");

        Long resultPercent = 0l;
        String responseUri = null;

        if (isEndConvert)
        {
            resultPercent = 100l;
            responseUri = (String) jsonObj.get("fileUrl");
        }
        else
        {
            resultPercent = (Long) jsonObj.get("percent");
            resultPercent = resultPercent >= 100l ? 99l : resultPercent;
        }

        return resultPercent >= 100l ? responseUri : "";
    }

    public String convertStreamToString(InputStream stream) throws IOException
    {
        if (stream == null)
            throw new IOException("The stream is empty");
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();

        while (line != null)
        {
            stringBuilder.append(line);
            line = bufferedReader.readLine();
        }

        String result = stringBuilder.toString();

        return result;
    }

    public JSONObject convertStringToJSON(String jsonString) throws ParseException
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(jsonString);
        JSONObject jsonObj = (JSONObject) obj;

        return jsonObj;
    }
}