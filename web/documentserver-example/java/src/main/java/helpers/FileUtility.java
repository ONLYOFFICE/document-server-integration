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

import entities.FileType;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FileUtility {

    private FileUtility() { }

    // get file type
    public static FileType getFileType(final String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();

        // word type for document extensions
        if (extsDocument.contains(ext)) {
            return FileType.Word;
        }

        // cell type for spreadsheet extensions
        if (extsSpreadsheet.contains(ext)) {
            return FileType.Cell;
        }

        // slide type for presentation extensions
        if (extsPresentation.contains(ext)) {
            return FileType.Slide;
        }

        // default file type is word
        return FileType.Word;
    }

    // document extensions
    private static List<String> extsDocument = Arrays.asList(
            ".doc", ".docx", ".docm",
            ".dot", ".dotx", ".dotm",
            ".odt", ".fodt", ".ott", ".rtf", ".txt",
            ".html", ".htm", ".mht", ".xml",
            ".pdf", ".djvu", ".fb2", ".epub", ".xps", ".oxps", ".oform"
    );

    // spreadsheet extensions
    private static List<String> extsSpreadsheet = Arrays.asList(
            ".xls", ".xlsx", ".xlsm", ".xlsb",
            ".xlt", ".xltx", ".xltm",
            ".ods", ".fods", ".ots", ".csv"
    );

    // presentation extensions
    private static List<String> extsPresentation = Arrays.asList(
            ".pps", ".ppsx", ".ppsm",
            ".ppt", ".pptx", ".pptm",
            ".pot", ".potx", ".potm",
            ".odp", ".fodp", ".otp"
    );


    // get file name from the url
    public static String getFileName(final String url) {
        if (url == null) {
            return "";
        }

        // get file name from the last part of url
        String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());
        fileName = fileName.split("\\?")[0];
        return fileName;
    }

    // get file name without extension
    public static String getFileNameWithoutExtension(final String url) {
        String fileName = getFileName(url);
        if (fileName == null) {
            return null;
        }
        String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        return fileNameWithoutExt;
    }

    // get file extension from url
    public static String getFileExtension(final String url) {
        String fileName = getFileName(url);
        if (fileName == null) {
            return null;
        }
        String fileExt = fileName.substring(fileName.lastIndexOf("."));
        return fileExt.toLowerCase();
    }

    // get url parameters
    public static Map<String, String> getUrlParams(final String url) {
        try {
            // take all the parameters which are placed after ? sign in the file url
            String query = new URL(url).getQuery();
            String[] params = query.split("&");  // parameters are separated by & sign
            Map<String, String> map = new HashMap<>();
            for (String param : params) {  // write parameters and their values to the map dictionary
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            }
            return map;
        } catch (Exception ex) {
            return null;
        }
    }
}
