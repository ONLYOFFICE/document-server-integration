<?php

$GLOBALS['FILE_SIZE_MAX'] = 5242880;
$GLOBALS['STORAGE_PATH'] = "";
$GLOBALS['ALONE'] = FALSE;

$GLOBALS['MODE'] = "";

$GLOBALS['DOC_SERV_VIEWD'] = array(".pdf", ".djvu", ".xps");
$GLOBALS['DOC_SERV_EDITED'] = array(".docx", ".xlsx", ".csv", ".pptx", ".ppsx", ".txt");
$GLOBALS['DOC_SERV_CONVERT'] = array(".docm", ".doc", ".dotx", ".dotm", ".dot", ".odt", ".fodt", ".ott", ".xlsm", ".xls", ".xltx", ".xltm", ".xlt", ".ods", ".fods", ".ots", ".pptm", ".ppt", ".ppsm", ".pps", ".potx", ".potm", ".pot", ".odp", ".fodp", ".otp", ".rtf", ".mht", ".html", ".htm", ".epub");

$GLOBALS['DOC_SERV_TIMEOUT'] = "120000";

$GLOBALS['DOC_SERV_CONVERTER_URL'] = "https://documentserver/ConvertService.ashx";
$GLOBALS['DOC_SERV_API_URL'] = "https://documentserver/web-apps/apps/api/documents/api.js";

$GLOBALS['DOC_SERV_PRELOADER_URL'] = "https://documentserver/web-apps/apps/api/documents/cache-scripts.html";

$GLOBALS['EXAMPLE_URL'] = "";

$GLOBALS['MOBILE_REGEX'] = "android|avantgo|playbook|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od|ad)|iris|kindle|lge |maemo|midp|mmp|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|symbian|treo|up\\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino";


$GLOBALS['ExtsSpreadsheet'] = array(".xls", ".xlsx", ".xlsm",
                                    ".xlt", ".xltx", ".xltm",
                                    ".ods", ".fods", ".ots", ".csv");

$GLOBALS['ExtsPresentation'] = array(".pps", ".ppsx", ".ppsm",
                                     ".ppt", ".pptx", ".pptm",
                                     ".pot", ".potx", ".potm",
                                     ".odp", ".fodp", ".otp");

$GLOBALS['ExtsDocument'] = array(".doc", ".docx", ".docm",
                                 ".dot", ".dotx", ".dotm",
                                 ".odt", ".fodt", ".ott", ".rtf", ".txt",
                                 ".html", ".htm", ".mht",
                                 ".pdf", ".djvu", ".fb2", ".epub", ".xps");


?>