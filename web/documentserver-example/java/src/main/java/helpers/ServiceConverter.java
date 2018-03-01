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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class ServiceConverter
{
    private static int ConvertTimeout = 120000;
    private static final String DocumentConverterUrl = ConfigManager.GetProperty("files.docservice.url.converter");
    private static final MessageFormat ConvertParams = new MessageFormat("?url={0}&outputtype={1}&filetype={2}&title={3}&key={4}");

    static
    {
        try
        {
            int timeout = Integer.parseInt(ConfigManager.GetProperty("files.docservice.timeout"));
            if(timeout > 0) 
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

        Object[] args = {
                            URLEncoder.encode(documentUri, java.nio.charset.StandardCharsets.UTF_8.toString()),
                            toExtension.replace(".", ""),
                            fromExtension.replace(".", ""),
                            title,
                            documentRevisionId
                        };

        String urlToConverter = DocumentConverterUrl +  ConvertParams.format(args);

        if (isAsync)
            urlToConverter += "&async=true";

        URL url = new URL(urlToConverter);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(ConvertTimeout);

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

        String error = (String) jsonObj.get("error");
        if (error != null && error != "")
            ProcessConvertServiceResponceError(Integer.parseInt(error));

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

        while(line != null)
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