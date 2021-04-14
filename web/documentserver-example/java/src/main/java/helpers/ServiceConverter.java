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

package helpers;

import helpers.DocumentManager;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class ServiceConverter
{
    private static int ConvertTimeout = 120000;
    private static final String DocumentConverterUrl = ConfigManager.GetProperty("files.docservice.url.site") + ConfigManager.GetProperty("files.docservice.url.converter");
    private static final String DocumentJwtHeader = ConfigManager.GetProperty("files.docservice.header");

    public static class ConvertBody
    {
        public String url;
        public String outputtype;
        public String filetype;
        public String title;
        public String key;
        public Boolean async;
        public String token;
        public String password;
    }

    static
    {
        try
        {
            int timeout = Integer.parseInt(ConfigManager.GetProperty("files.docservice.timeout"));
            if (timeout > 0) 
            {
                ConvertTimeout = timeout;
            }
        }
        catch (Exception ex)
        {
        }
    }

    public static String GetConvertedUri(String documentUri, String fromExtension, String toExtension, String documentRevisionId, String filePass, Boolean isAsync) throws Exception
    {
        fromExtension = fromExtension == null || fromExtension.isEmpty() ? FileUtility.GetFileExtension(documentUri) : fromExtension;

        String title = FileUtility.GetFileName(documentUri);
        title = title == null || title.isEmpty() ? UUID.randomUUID().toString() : title;

        documentRevisionId = documentRevisionId == null || documentRevisionId.isEmpty() ? documentUri : documentRevisionId;

        documentRevisionId = GenerateRevisionId(documentRevisionId);

        ConvertBody body = new ConvertBody();
        body.url = documentUri;
        body.outputtype = toExtension.replace(".", "");
        body.filetype = fromExtension.replace(".", "");
        body.title = title;
        body.key = documentRevisionId;
        body.password = filePass;
        if (isAsync)
            body.async = true;

        String headerToken = "";
        if (DocumentManager.TokenEnabled())
        {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("url", body.url);
            map.put("outputtype", body.outputtype);
            map.put("filetype", body.filetype);
            map.put("title", body.title);
            map.put("key", body.key);
            map.put("password", body.password);
            if (isAsync)
                map.put("async", body.async);

            String token = DocumentManager.CreateToken(map);
            body.token = token;

            Map<String, Object> payloadMap = new HashMap<String, Object>();
            payloadMap.put("payload", map);
            headerToken = DocumentManager.CreateToken(payloadMap);
        }

        Gson gson = new Gson();
        String bodyString = gson.toJson(body);

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);

        URL url = new URL(DocumentConverterUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setFixedLengthStreamingMode(bodyByte.length);
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(ConvertTimeout);

        if (DocumentManager.TokenEnabled())
        {
            connection.setRequestProperty(DocumentJwtHeader.equals("") ? "Authorization" : DocumentJwtHeader, "Bearer " + headerToken);
        }

        connection.connect();
        try (OutputStream os = connection.getOutputStream()) {
            os.write(bodyByte);
        }

        InputStream stream = connection.getInputStream();

        if (stream == null)
            throw new Exception("Could not get an answer");

        String jsonString = ConvertStreamToString(stream);

        connection.disconnect();

        return GetResponseUri(jsonString);
    }

    public static String GenerateRevisionId(String expectedKey)
    {
        if (expectedKey.length() > 20)
            expectedKey = Integer.toString(expectedKey.hashCode());

        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), 20));
    }

    private static void ProcessConvertServiceResponceError(int errorCode) throws Exception
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
                errorMessage = errorMessageTemplate + "Incorrect password";
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

    private static String GetResponseUri(String jsonString) throws Exception
    {
        JSONObject jsonObj = ConvertStringToJSON(jsonString);

        Object error = jsonObj.get("error");
        if (error != null)
            ProcessConvertServiceResponceError(Math.toIntExact((long)error));

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

    public static String ConvertStreamToString(InputStream stream) throws IOException
    {
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

    public static JSONObject ConvertStringToJSON(String jsonString) throws ParseException
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(jsonString);
        JSONObject jsonObj = (JSONObject) obj;

        return jsonObj;
    }
}