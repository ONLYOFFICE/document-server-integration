using System.Collections.Generic;
using System.IO;

namespace OnlineEditorsExampleNetCore.Models
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
                ".pdf", ".djvu", ".fb2", ".epub", ".xps"
            };

        // spreadsheet extensions
        public static readonly List<string> ExtsSpreadsheet = new List<string>
            {
                ".xls", ".xlsx", ".xlsm",
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
