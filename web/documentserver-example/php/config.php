<?php
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

$GLOBALS['version'] = "1.5.0";

$GLOBALS['FILE_SIZE_MAX'] = 5242880;
$GLOBALS['STORAGE_PATH'] = "";
$GLOBALS['ALONE'] = false;

$GLOBALS['DOC_SERV_FILLFORMS'] = [".oform", ".docx"];
$GLOBALS['DOC_SERV_VIEWD'] = [".pdf", ".djvu", ".xps", ".oxps"];
$GLOBALS['DOC_SERV_EDITED'] = [".docx", ".xlsx", ".csv", ".pptx", ".txt", ".docxf"];
$GLOBALS['DOC_SERV_CONVERT'] = [".docm", ".doc", ".dotx", ".dotm", ".dot", ".odt", ".fodt", ".ott", ".xlsm", ".xlsb", ".xls", ".xltx", ".xltm", ".xlt", ".ods", ".fods", ".ots", ".pptm", ".ppt", ".ppsx", ".ppsm", ".pps", ".potx", ".potm", ".pot", ".odp", ".fodp", ".otp", ".rtf", ".mht", ".html", ".htm", ".xml", ".epub", ".fb2"];

$GLOBALS['DOC_SERV_TIMEOUT'] = "120000";

$GLOBALS['DOC_SERV_SITE_URL'] = "http://documentserver/";

$GLOBALS['DOC_SERV_CONVERTER_URL'] = "ConvertService.ashx";
$GLOBALS['DOC_SERV_API_URL'] = "web-apps/apps/api/documents/api.js";
$GLOBALS['DOC_SERV_PRELOADER_URL'] = "web-apps/apps/api/documents/cache-scripts.html";
$GLOBALS['DOC_SERV_COMMAND_URL'] = "coauthoring/CommandService.ashx";

$GLOBALS['DOC_SERV_JWT_SECRET'] = "";
$GLOBALS['DOC_SERV_JWT_HEADER'] = "Authorization";

$GLOBALS['DOC_SERV_VERIFY_PEER_OFF'] = true;

$GLOBALS['EXAMPLE_URL'] = "";

$GLOBALS['MOBILE_REGEX'] = "android|avantgo|playbook|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od|ad)|iris|kindle|lge |maemo|midp|mmp|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|symbian|treo|up\\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino";

$GLOBALS['ExtsSpreadsheet'] = [".xls", ".xlsx", ".xlsm", ".xlsb",
    ".xlt", ".xltx", ".xltm",
    ".ods", ".fods", ".ots", ".csv"];

$GLOBALS['ExtsPresentation'] = [".pps", ".ppsx", ".ppsm",
    ".ppt", ".pptx", ".pptm",
    ".pot", ".potx", ".potm",
    ".odp", ".fodp", ".otp"];

$GLOBALS['ExtsDocument'] = [".doc", ".docx", ".docm",
    ".dot", ".dotx", ".dotm",
    ".odt", ".fodt", ".ott", ".rtf", ".txt",
    ".html", ".htm", ".mht", ".xml",
    ".pdf", ".djvu", ".fb2", ".epub", ".xps", ".oxps", ".oform"];

$GLOBALS['LANGUAGES'] = [
    'en' => 'English',
    'hy' => 'Armenian',
    'az' => 'Azerbaijani',
    'eu' => 'Basque',
    'be' => 'Belarusian',
    'bg' => 'Bulgarian',
    'ca' => 'Catalan',
    'zh' => 'Chinese (People\'s Republic of China)',
    'zh-TW' => 'Chinese (Traditional, Taiwan)',
    'cs' => 'Czech',
    'da' => 'Danish',
    'nl' => 'Dutch',
    'fi' => 'Finnish',
    'fr' => 'French',
    'gl' => 'Galego',
    'de' => 'German',
    'el' => 'Greek',
    'hu' => 'Hungarian',
    'id' => 'Indonesian',
    'it' => 'Italian',
    'ja' => 'Japanese',
    'ko' => 'Korean',
    'lv' => 'Latvian',
    'lo' => 'Lao',
    'ms' => 'Malay (Malaysia)',
    'nb' => 'Norwegian',
    'pl' => 'Polish',
    'pt' => 'Portuguese (Brazil)',
    'pt-PT' => 'Portuguese (Portugal)',
    'ro' => 'Romanian',
    'ru' => 'Russian',
    'sk' => 'Slovak',
    'sl' => 'Slovenian',
    'es' => 'Spanish',
    'sv' => 'Swedish',
    'tr' => 'Turkish',
    'uk' => 'Ukrainian',
    'vi' => 'Vietnamese',
    'aa-AA' => 'Test Language',
];
