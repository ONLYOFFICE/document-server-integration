/*
 *
 * (c) Copyright Ascensio System Limited 2010-2018
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
    private static final String DocumentConverterUrl = ConfigManager.GetProperty("files.docservice.url.converter");

    public static class ConvertBody
    {
        public String url;
        public String outputtype;
        public String filetype;
        public String title;
        public String key;
        public Boolean async;
        public String token;
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

    public static String GetConvertedUri(String documentUri, String fromExtension, String toExtension, String documentRevisionId, Boolean isAsync) throws Exception
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
        if (isAsync)
            body.async = true;

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
            Map<String, Object> map = new HashMap<>();
            map.put("payload", body);
            String token = DocumentManager.CreateToken(map);
            connection.setRequestProperty("Authorization", "Bearer " + token);
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

    private static String ConvertStreamToString(InputStream stream) throws IOException
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

    private static JSONObject ConvertStringToJSON(String jsonString) throws ParseException
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(jsonString);
        JSONObject jsonObj = (JSONObject) obj;

        return jsonObj;
    }
}