<?php

$GLOBALS['FILE_SIZE_MAX'] = 5242880;
$GLOBALS['STORAGE_PATH'] = "";
$GLOBALS['ALONE'] = FALSE;

$GLOBALS['MODE'] = "";

$GLOBALS['DOC_SERV_VIEWD'] = array(".pdf",".djvu",".xps");
$GLOBALS['DOC_SERV_EDITED'] = array(".docx",".docm",".doc",".odt",".xlsx",".xlsm",".xls",".ods",".csv",".pptx",".pptm",".ppt",".ppsx",".ppsm",".pps",".odp",".rtf",".txt",".mht",".html",".htm");
$GLOBALS['DOC_SERV_CONVERT'] = array(".docm",".doc",".odt",".xlsm",".xls",".ods",".pptm",".ppt",".ppsm",".pps",".odp",".rtf",".mht",".html",".htm",".epub");

$GLOBALS['DOC_SERV_TIMEOUT'] = "120000";

$GLOBALS['DOC_SERV_STORAGE_URL'] = "https://doc.onlyoffice.com/FileUploader.ashx";
$GLOBALS['DOC_SERV_CONVERTER_URL'] = "https://doc.onlyoffice.com/ConvertService.ashx";
$GLOBALS['DOC_SERV_API_URL'] = "https://doc.onlyoffice.com/web-apps/apps/api/documents/api.js";

$GLOBALS['DOC_SERV_PRELOADER_URL'] = "https://doc.onlyoffice.com/web-apps/apps/api/documents/cache-scripts.html";

$GLOBALS['EXAMPLE_URL'] = "";

$GLOBALS['MOBILE_REGEX'] = "android|avantgo|playbook|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od|ad)|iris|kindle|lge |maemo|midp|mmp|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|symbian|treo|up\\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino";


$GLOBALS['ExtsSpreadsheet'] = array(".xls", ".xlsx", ".xlsm",
                                    ".ods", ".csv");

$GLOBALS['ExtsPresentation'] = array(".pps", ".ppsx", ".ppsm",
                                    ".ppt", ".pptx", ".pptm",
                                    ".odp");

$GLOBALS['ExtsDocument'] = array(".docx", ".docm", ".doc", ".odt", ".rtf", ".txt",
                                ".html", ".htm", ".mht", ".pdf", ".djvu",
                                ".fb2", ".epub", ".xps");

if ( !defined('ServiceConverterMaxTry') )
    define( 'ServiceConverterMaxTry', 3);


?>