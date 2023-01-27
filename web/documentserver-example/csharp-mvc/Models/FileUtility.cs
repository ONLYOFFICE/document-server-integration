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

using System.Collections.Generic;
using System.IO;

namespace OnlineEditorsExampleMVC.Models
{
    public static class FileUtility
    {
        public enum FileType
        {
            Word,
            Cell,
            Slide
        }

        // get file type
        public static FileType GetFileType(string fileName)
        {
            var ext = Path.GetExtension(fileName).ToLower();

            if (ExtsDocument.Contains(ext)) return FileType.Word;  // word type for document extensions
            if (ExtsSpreadsheet.Contains(ext)) return FileType.Cell;  // cell type for spreadsheet extensions
            if (ExtsPresentation.Contains(ext)) return FileType.Slide;  // slide type for presentation extensions

            return FileType.Word;  // the default type is word
        }

        // document extensions
        public static readonly List<string> ExtsDocument = new List<string>
            {
                ".doc", ".docx", ".docm",
                ".dot", ".dotx", ".dotm",
                ".odt", ".fodt", ".ott", ".rtf", ".txt",
                ".html", ".htm", ".mht", ".xml",
                ".pdf", ".djvu", ".fb2", ".epub", ".xps", ".oxps", ".oform"
            };

        // spreadsheet extensions
        public static readonly List<string> ExtsSpreadsheet = new List<string>
            {
                ".xls", ".xlsx", ".xlsm", ".xlsb",
                ".xlt", ".xltx", ".xltm",
                ".ods", ".fods", ".ots", ".csv"
            };

        // presentation extensions
        public static readonly List<string> ExtsPresentation = new List<string>
            {
                ".pps", ".ppsx", ".ppsm",
                ".ppt", ".pptx", ".pptm",
                ".pot", ".potx", ".potm",
                ".odp", ".fodp", ".otp"
            };
    }
}