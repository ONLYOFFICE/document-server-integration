import config
import json

from django.shortcuts import render

from src.utils import users
from src.utils import docManager

def default(request):
    context = {
        'users': users.USERS,
        'languages': docManager.LANGUAGES,
        'preloadurl': config.DOC_SERV_PRELOADER_URL,
        'editExt': json.dumps(config.DOC_SERV_EDITED),
        'convExt': json.dumps(config.DOC_SERV_CONVERT),
        'files': docManager.getStoredFiles(request)
    }
    return render(request, 'index.html', context)