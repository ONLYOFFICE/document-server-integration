import config

def getFileName(str):
    ind = str.rfind('/')
    return str[ind+1:]

def getFileNameWithoutExt(str):
    fn = getFileName(str)
    ind = fn.rfind('.')
    return fn[:ind]

def getFileExt(str):
    fn = getFileName(str)
    ind = fn.rfind('.')
    return fn[ind:]

def getFileType(str):
    ext = getFileExt(str)
    if ext in config.EXT_DOCUMENT:
        return 'text'
    if ext in config.EXT_SPREADSHEET:
        return 'spreadsheet'
    if ext in config.EXT_PRESENTATION:
        return 'presentation'

    return 'text'