import os

VERSION = '1.5.1'

FILE_SIZE_MAX = 5242880
STORAGE_PATH = 'app_data'

DOC_SERV_FILLFORMS = [".docx", ".oform"]
DOC_SERV_VIEWED = [".djvu", ".oxps", ".pdf", ".xps"]  # file extensions that can be viewed
DOC_SERV_EDITED = [                                             # file extensions that can be edited
    ".csv", ".docm", ".docx", ".docxf", ".dotm", ".dotx",
    ".epub", ".fb2", ".html", ".odp", ".ods", ".odt", ".otp",
    ".ots", ".ott", ".potm", ".potx", ".ppsm", ".ppsx", ".pptm",
    ".pptx", ".rtf", ".txt", ".xlsm", ".xlsx", ".xltm", ".xltx"
]
DOC_SERV_CONVERT = [                                            # file extensions that can be converted
    ".doc", ".dot", ".dps", ".dpt", ".epub", ".et", ".ett", ".fb2",
    ".fodp", ".fods", ".fodt", ".htm", ".html", ".mht", ".mhtml",
    ".odp", ".ods", ".odt", ".otp", ".ots", ".ott", ".pot", ".pps",
    ".ppt", ".rtf", ".stw", ".sxc", ".sxi", ".sxw", ".wps", ".wpt",
    ".xls", ".xlsb", ".xlt", ".xml"
]

DOC_SERV_TIMEOUT = 120000

DOC_SERV_SITE_URL = 'http://documentserver/'

DOC_SERV_CONVERTER_URL = 'ConvertService.ashx'
DOC_SERV_API_URL = 'web-apps/apps/api/documents/api.js'
DOC_SERV_PRELOADER_URL = 'web-apps/apps/api/documents/cache-scripts.html'
DOC_SERV_COMMAND_URL='coauthoring/CommandService.ashx'

EXAMPLE_DOMAIN = None

DOC_SERV_JWT_SECRET = ''  # the secret key for generating token
DOC_SERV_JWT_HEADER = 'Authorization'
DOC_SERV_JWT_USE_FOR_REQUEST = True

DOC_SERV_VERIFY_PEER = False

EXT_SPREADSHEET = [
    ".xls", ".xlsx", ".xlsm", ".xlsb",
    ".xlt", ".xltx", ".xltm",
    ".ods", ".fods", ".ots", ".csv"
]

EXT_PRESENTATION = [
    ".pps", ".ppsx", ".ppsm",
    ".ppt", ".pptx", ".pptm",
    ".pot", ".potx", ".potm",
    ".odp", ".fodp", ".otp"
]

EXT_DOCUMENT = [
    ".doc", ".docx", ".docm",
    ".dot", ".dotx", ".dotm",
    ".odt", ".fodt", ".ott", ".rtf", ".txt",
    ".html", ".htm", ".mht", ".xml",
    ".pdf", ".djvu", ".fb2", ".epub", ".xps", ".oxps", ".oform"
]

LANGUAGES = {
    'en': 'English',
    'hy': 'Armenian',
    'az': 'Azerbaijani',
    'eu': 'Basque',
    'be': 'Belarusian',
    'bg': 'Bulgarian',
    'ca': 'Catalan',
    'zh': 'Chinese',
    'zh-TW': 'Chinese (Taiwan)',
    'cs': 'Czech',
    'da': 'Danish',
    'nl': 'Dutch',
    'fi': 'Finnish',
    'fr': 'French',
    'gl': 'Galego',
    'de': 'German',
    'el': 'Greek',
    'hu': 'Hungarian',
    'id': 'Indonesian',
    'it': 'Italian',
    'ja': 'Japanese',
    'ko': 'Korean',
    'lo': 'Lao',
    'lv': 'Latvian',
    'ms': 'Malay (Malaysia)',
    'no': 'Norwegian',
    'pl': 'Polish',
    'pt': 'Portuguese (Brazil)',
    'pt-PT': 'Portuguese (Portugal)',
    'ro': 'Romanian',
    'ru': 'Russian',
    'si': 'Sinhala (Sri Lanka)',
    'sk': 'Slovak',
    'sl': 'Slovenian',
    'es': 'Spanish',
    'sv': 'Swedish',
    'tr': 'Turkish',
    'uk': 'Ukrainian',
    'vi': 'Vietnamese',
    'aa-AA': 'Test Language'
}

if os.environ.get("EXAMPLE_DOMAIN"):  # generates a link for example domain
    EXAMPLE_DOMAIN = os.environ.get("EXAMPLE_DOMAIN")
if os.environ.get("DOC_SERV"):  # generates links for document server
    DOC_SERV_SITE_URL = os.environ.get("DOC_SERV")