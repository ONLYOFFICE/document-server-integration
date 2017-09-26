<?php

$GLOBALS['FILE_SIZE_MAX'] = 5242880;
$GLOBALS['STORAGE_PATH'] = "";
$GLOBALS['ALONE'] = FALSE;

$GLOBALS['MODE'] = "";

$GLOBALS['DOC_SERV_VIEWD'] = array(".ppt",".pps",".odp",".fodp",".pdf",".djvu",".epub",".xps");
$GLOBALS['DOC_SERV_EDITED'] = array(".docx",".doc",".odt",".fodt",".xlsx",".xls",".ods",".fods",".csv",".pptx",".ppsx",".rtf",".txt",".mht",".html",".htm");
$GLOBALS['DOC_SERV_CONVERT'] = array(".doc",".odt",".fodt",".xls",".ods",".fods",".ppt",".pps",".odp",".fodp",".rtf",".mht",".html",".htm",".epub");

$GLOBALS['DOC_SERV_TIMEOUT'] = "120000";

$GLOBALS['DOC_SERV_STORAGE_URL'] = "https://doc.onlyoffice.com/FileUploader.ashx";
$GLOBALS['DOC_SERV_CONVERTER_URL'] = "https://doc.onlyoffice.com/ConvertService.ashx";
$GLOBALS['DOC_SERV_API_URL'] = "https://doc.onlyoffice.com/web-apps/apps/api/documents/api.js";

$GLOBALS['DOC_SERV_PRELOADER_URL'] = "https://doc.onlyoffice.com/web-apps/apps/api/documents/cache-scripts.html";

$GLOBALS['EXAMPLE_URL'] = "";

$GLOBALS['MOBILE_REGEX'] = "android|avantgo|playbook|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od|ad)|iris|kindle|lge |maemo|midp|mmp|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|symbian|treo|up\\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino";


$GLOBALS['ExtsSpreadsheet'] = array(".xls", ".xlsx",
                                    ".ods", ".fods", ".csv");

$GLOBALS['ExtsPresentation'] = array(".pps", ".ppsx",
                                    ".ppt", ".pptx",
                                    ".odp", ".fodp");

$GLOBALS['ExtsDocument'] = array(".docx", ".doc", ".odt", ".fodt", ".rtf", ".txt",
                                ".html", ".htm", ".mht", ".pdf", ".djvu",
                                ".fb2", ".epub", ".xps");

if ( !defined('ServiceConverterMaxTry') )
    define( 'ServiceConverterMaxTry', 3);


?>