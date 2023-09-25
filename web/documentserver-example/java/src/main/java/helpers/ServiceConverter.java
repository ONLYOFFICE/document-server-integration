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

package helpers;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utils.ConvertErrorType;

import javax.servlet.http.HttpServletResponse;

import static utils.Constants.CONVERTATION_ERROR_MESSAGE_TEMPLATE;
import static utils.Constants.CONVERT_TIMEOUT_MS;
import static utils.Constants.FULL_LOADING_IN_PERCENT;
import static utils.Constants.MAX_KEY_LENGTH;


public final class ServiceConverter {
    private static int convertTimeout;
    private static final String DOCUMENT_CONVERTER_URL = ConfigManager
            .getProperty("files.docservice.url.site") + ConfigManager.getProperty("files.docservice.url.converter");
    private static final String DOCUMENT_JWT_HEADER = ConfigManager.getProperty("files.docservice.header");

    private ServiceConverter() { }

    public static class ConvertBody {
        private String region;
        private String url;
        private String outputtype;
        private String filetype;
        private String title;
        private String key;
        private Boolean async;
        private String token;
        private String password;

        public void setRegion(final String regionParam) {
            this.region = regionParam;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(final String urlParam) {
            this.url = urlParam;
        }

        public String getOutputtype() {
            return outputtype;
        }

        public void setOutputtype(final String outputtypeParam) {
            this.outputtype = outputtypeParam;
        }

        public String getFiletype() {
            return filetype;
        }

        public void setFiletype(final String filetypeParam) {
            this.filetype = filetypeParam;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(final String titleParam) {
            this.title = titleParam;
        }

        public String getKey() {
            return key;
        }

        public void setKey(final String keyParam) {
            this.key = keyParam;
        }

        public Boolean getAsync() {
            return async;
        }

        public void setAsync(final Boolean asyncParam) {
            this.async = asyncParam;
        }

        public void setToken(final String tokenParam) {
            this.token = tokenParam;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String passwordParam) {
            this.password = passwordParam;
        }
    }

    static {
        try {
            // get timeout value from the settings.properties
            int timeout = Integer.parseInt(ConfigManager.getProperty("files.docservice.timeout"));
            // if it's greater than 0 then value to a convert timeout
            convertTimeout = timeout > 0 ? timeout : CONVERT_TIMEOUT_MS;
        } catch (Exception ex) {
        }
    }

    // get the url of the converted file
    public static Map<String, String> getConvertedData(final String documentUri, final String fromExtension,
                                                       final String toExtension, final String documentRevisionId,
                                                       final String filePass, final Boolean isAsync,
                                                       final String lang) throws Exception {
        // check if the fromExtension parameter is defined; if not, get it from the document url
        String fromExt = fromExtension == null || fromExtension.isEmpty()
                ? FileUtility.getFileExtension(documentUri) : fromExtension;

        // check if the file name parameter is defined; if not, get random uuid for this file
        String title = FileUtility.getFileName(documentUri);
        title = title == null || title.isEmpty() ? UUID.randomUUID().toString() : title;

        String documentRevId = documentRevisionId == null || documentRevisionId.isEmpty()
                ? documentUri : documentRevisionId;

        documentRevId = generateRevisionId(documentRevId);  // create document token

        // write all the necessary parameters to the body object
        ConvertBody body = new ConvertBody();
        body.setRegion(lang);
        body.setUrl(documentUri);
        body.setOutputtype(toExtension);
        body.setFiletype(fromExt);
        body.setTitle(title);
        body.setKey(documentRevId);
        body.setPassword(filePass);
        if (isAsync) {
            body.setAsync(true);
        }

        String headerToken = "";
        if (DocumentManager.tokenEnabled() && DocumentManager.tokenUseForRequest()) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("region", lang);
            map.put("url", body.getUrl());
            map.put("outputtype", body.getOutputtype());
            map.put("filetype", body.getFiletype());
            map.put("title", body.getTitle());
            map.put("key", body.getKey());
            map.put("password", body.getPassword());
            if (isAsync) {
                map.put("async", body.getAsync());
            }

            // add token to the body if it is enabled
            String token = DocumentManager.createToken(map);
            body.setToken(token);

            Map<String, Object> payloadMap = new HashMap<String, Object>();
            payloadMap.put("payload", map);  // create payload object
            headerToken = DocumentManager.createToken(payloadMap);  // create header token
        }

        Gson gson = new Gson();
        String bodyString = gson.toJson(body);

        byte[] bodyByte = bodyString.getBytes(StandardCharsets.UTF_8);

        // specify request parameters
        URL url = new URL(DOCUMENT_CONVERTER_URL);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setFixedLengthStreamingMode(bodyByte.length);
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(convertTimeout);

        // write header token to the request
        if (DocumentManager.tokenEnabled()) {
            connection.setRequestProperty(DOCUMENT_JWT_HEADER.equals("")
                    ? "Authorization" : DOCUMENT_JWT_HEADER, "Bearer " + headerToken);
        }

        connection.connect();


        try (OutputStream os = connection.getOutputStream()) {
            os.write(bodyByte);
        }
        int statusCode = connection.getResponseCode();
        if (statusCode != HttpServletResponse.SC_OK) {  // checking status code
            connection.disconnect();
            throw new Exception("Conversion service returned status: " + statusCode);
        }

        InputStream stream = connection.getInputStream();

        if (stream == null) {
            throw new Exception("Could not get an answer");
        }

        // convert string to json
        String jsonString = convertStreamToString(stream);

        connection.disconnect();

        return getResponseData(jsonString);
    }

    // generate document key
    public static String generateRevisionId(final String expectedKey) {
        /* if the expected key length is greater than 20 then
         he expected key is hashed and a fixed length value is stored in the string format */
        String formatKey = expectedKey.length() > MAX_KEY_LENGTH
                ? Integer.toString(expectedKey.hashCode()) : expectedKey;
        String key = formatKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), MAX_KEY_LENGTH));  // the resulting key length is 20 or less
    }

    // create an error message for an error code
    private static void processConvertServiceResponceError(final int errorCode) throws Exception {
        String errorMessage = CONVERTATION_ERROR_MESSAGE_TEMPLATE + ConvertErrorType.labelOfCode(errorCode);

        throw new Exception(errorMessage);
    }

    // get the response data
    private static Map<String, String> getResponseData(final String jsonString) throws Exception {
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
        Map<String, String> responseData = new HashMap<>();

        if (isEndConvert) {  // if the conversion is completed
            resultPercent = FULL_LOADING_IN_PERCENT;
            responseUri = (String) jsonObj.get("fileUrl");  // get the file url
            responseFileType = (String) jsonObj.get("fileType");  // get the file type
            responseData.put("fileUrl", responseUri);
            responseData.put("fileType", responseFileType);
        } else {  // if the conversion isn't completed
            resultPercent = (Long) jsonObj.get("percent");
            responseData.put("fileUrl", "");
            resultPercent = resultPercent >= FULL_LOADING_IN_PERCENT
                    ? FULL_LOADING_IN_PERCENT - 1 : resultPercent;  // get the percentage value
        }

        return responseData;
    }

    // convert stream to string
    public static String convertStreamToString(final InputStream stream) throws IOException {
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
    public static JSONObject convertStringToJSON(final String jsonString) throws ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(jsonString);  // parse json string
        JSONObject jsonObj = (JSONObject) obj;  // and turn it into a json object

        return jsonObj;
    }
}
