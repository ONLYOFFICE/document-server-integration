/*
 *
 * (c) Copyright Ascensio System SIA 2019
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

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import entities.FileType;

import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;
import org.json.simple.JSONObject;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;

public class DocumentManager
{
    private static HttpServletRequest request;

    public static void Init(HttpServletRequest req, HttpServletResponse resp)
    {
        request = req;
    }

    public static long GetMaxFileSize()
    {
        long size;

        try
        {
            size = Long.parseLong(ConfigManager.GetProperty("filesize-max"));
        }
        catch (Exception ex)
        {
            size = 0;
        }

        return size > 0 ? size : 5 * 1024 * 1024;
    }

    public static List<String> GetFileExts()
    {
        List<String> res = new ArrayList<>();

        res.addAll(GetViewedExts());
        res.addAll(GetEditedExts());
        res.addAll(GetConvertExts());

        return res;
    }

    public static List<String> GetViewedExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.viewed-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static List<String> GetEditedExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.edited-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static List<String> GetConvertExts()
    {
        String exts = ConfigManager.GetProperty("files.docservice.convert-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static String CurUserHostAddress(String userAddress)
    {
        if(userAddress == null)
        {
            try
            {
                userAddress = InetAddress.getLocalHost().getHostAddress();
            }
            catch (Exception ex)
            {
                userAddress = "";
            }
        }

        return userAddress.replaceAll("[^0-9a-zA-Z.=]", "_");
    }

    public static String FilesRootPath(String userAddress)
    {
        String hostAddress = CurUserHostAddress(userAddress);
        String serverPath = request.getSession().getServletContext().getRealPath("");
        String storagePath = ConfigManager.GetProperty("storage-folder");
        String directory = serverPath + storagePath + File.separator + hostAddress + File.separator;

        File file = new File(directory);

        if (!file.exists())
        {
            file.mkdirs();
        }

        return directory;
    }

    public static String StoragePath(String fileName, String userAddress)
    {
        String directory = FilesRootPath(userAddress);
        return directory + fileName;
    }

    public static String HistoryDir(String storagePath)
    {
        return storagePath += "-hist";
    }

    public static String VersionDir(String histPath, Integer version)
    {
        return histPath + File.separator + Integer.toString(version);
    }

    public static String VersionDir(String fileName, String userAddress, Integer version)
    {
        return VersionDir(HistoryDir(StoragePath(fileName, userAddress)), version);
    }

    public static Integer GetFileVersion(String historyPath)
    {
        File dir = new File(historyPath);

        if (!dir.exists()) return 0;

        File[] dirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        return dirs.length;
    }

    public static int GetFileVersion(String fileName, String userAddress)
    {
        return GetFileVersion(HistoryDir(StoragePath(fileName, userAddress)));
    }

    public static String GetCorrectName(String fileName)
    {
        String baseName = FileUtility.GetFileNameWithoutExtension(fileName);
        String ext = FileUtility.GetFileExtension(fileName);
        String name = baseName + ext;

        File file = new File(StoragePath(name, null));

        for (int i = 1; file.exists(); i++)
        {
            name = baseName + " (" + i + ")" + ext;
            file = new File(StoragePath(name, null));
        }

        return name;
    }

    public static void CreateMeta(String fileName, String uid, String uname) throws Exception
    {
        String histDir = HistoryDir(StoragePath(fileName, null));

        File dir = new File(histDir);
        dir.mkdir();

        JSONObject json = new JSONObject();
        json.put("created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        json.put("id", (uid == null || uid.isEmpty()) ? "uid-1" : uid);
        json.put("name", (uname == null || uname.isEmpty()) ? "John Smith" : uname);

        File meta = new File(histDir + File.separator + "createdInfo.json");
        try (FileWriter writer = new FileWriter(meta)) {
            json.writeJSONString(writer);
        }
    }

    public static File[] GetStoredFiles(String userAddress)
    {
        String directory = FilesRootPath(userAddress);

        File file = new File(directory);
        return file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
    }

    public static String CreateDemo(String fileExt, Boolean sample, String uid, String uname) throws Exception
    {
        String demoName = (sample ? "sample." : "new.") + fileExt;
        String fileName = GetCorrectName(demoName);

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(demoName);

        File file = new File(StoragePath(fileName, null));

        try (FileOutputStream out = new FileOutputStream(file))
        {
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = stream.read(bytes)) != -1)
            {
                out.write(bytes, 0, read);
            }
            out.flush();
        }

        CreateMeta(fileName, uid, uname);

        return fileName;
    }

    public static String GetFileUri(String fileName)
    {
        try
        {
            String serverPath = GetServerUrl();
            String storagePath = ConfigManager.GetProperty("storage-folder");
            String hostAddress = CurUserHostAddress(null);

            String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/" + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()).replace("+", "%20");

            return filePath;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public static String GetPathUri(String path)
    {
        String serverPath = GetServerUrl();
        String storagePath = ConfigManager.GetProperty("storage-folder");
        String hostAddress = CurUserHostAddress(null);

        String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/" + path.replace(File.separator, "/").substring(FilesRootPath(null).length()).replace(" ", "%20");

        return filePath;
    }


    public static String GetServerUrl()
    {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }

    public static String GetCallback(String fileName)
    {
        String serverPath = GetServerUrl();
        String hostAddress = CurUserHostAddress(null);
        try
        {
            String query = "?type=track&fileName=" + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()) + "&userAddress=" + URLEncoder.encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());

            return serverPath + "/IndexServlet" + query;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public static String GetInternalExtension(FileType fileType)
    {
        if (fileType.equals(FileType.Text))
            return ".docx";

        if (fileType.equals(FileType.Spreadsheet))
            return ".xlsx";

        if (fileType.equals(FileType.Presentation))
            return ".pptx";

        return ".docx";
    }

    public static String CreateToken(Map<String, Object> payloadClaims)
    {
        try
        {
            Signer signer = HMACSigner.newSHA256Signer(GetTokenSecret());
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet())
            {
                jwt.addClaim(key, payloadClaims.get(key));
            }
            return JWT.getEncoder().encode(jwt, signer);
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static JWT ReadToken(String token)
    {
        try
        {
            Verifier verifier = HMACVerifier.newVerifier(GetTokenSecret());
            return JWT.getDecoder().decode(token, verifier);
        }
        catch (Exception exception)
        {
            return null;
        }
    }

    public static Boolean TokenEnabled()
    {
        String secret = GetTokenSecret();
        return secret != null && !secret.isEmpty();
    }

    private static String GetTokenSecret()
    {
        return ConfigManager.GetProperty("files.docservice.secret");
    }
}