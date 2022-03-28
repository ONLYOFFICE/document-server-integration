import os

VERSION = '1.1.0'

FILE_SIZE_MAX = 5242880
STORAGE_PATH = 'app_data'

DOC_SERV_FILLFORMS = [".oform", ".docx"]
DOC_SERV_VIEWED = [".pdf", ".djvu", ".xps", ".oxps"]  # file extensions that can be viewed
DOC_SERV_EDITED = [".docx", ".xlsx", ".csv", ".pptx", ".txt", ".docxf"]  # file extensions that can be edited
DOC_SERV_CONVERT = [                                           # file extensions that can be converted
    ".docm", ".doc", ".dotx", ".dotm", ".dot", ".odt",
    ".fodt", ".ott", ".xlsm", ".xlsb", ".xls", ".xltx", ".xltm",
    ".xlt", ".ods", ".fods", ".ots", ".pptm", ".ppt",
    ".ppsx", ".ppsm", ".pps", ".potx", ".potm", ".pot",
    ".odp", ".fodp", ".otp", ".rtf", ".mht", ".html", ".htm", ".xml", ".epub", ".fb2"
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
    'be': 'Belarusian',
    'bg': 'Bulgarian',
    'ca': 'Catalan',
    'zh': 'Chinese',
    'cs': 'Czech',
    'da': 'Danish',
    'nl': 'Dutch',
    'fi': 'Finnish',
    'fr': 'French',
    'de': 'German',
    'el': 'Greek',
    'hu': 'Hungarian',
    'id': 'Indonesian',
    'it': 'Italian',
    'ja': 'Japanese',
    'ko': 'Korean',
    'lv': 'Latvian',
    'lo': 'Lao',
    'nb': 'Norwegian',
    'pl': 'Polish',
    'pt': 'Portuguese',
    'ro': 'Romanian',
    'ru': 'Russian',
    'sk': 'Slovak',
    'sl': 'Slovenian',
    'es': 'Spanish',
    'sv': 'Swedish',
    'tr': 'Turkish',
    'uk': 'Ukrainian',
    'vi': 'Vietnamese',
    'az': 'Azerbaijani',
    'gl': 'Galego'
}

if os.environ.get("EXAMPLE_DOMAIN"):  # generates a link for example domain
    EXAMPLE_DOMAIN = os.environ.get("EXAMPLE_DOMAIN")
if os.environ.get("DOC_SERV"):  # generates links for document server
    DOC_SERV_SITE_URL = os.environ.get("DOC_SERV")