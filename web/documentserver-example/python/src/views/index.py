"""

 (c) Copyright Ascensio System SIA 2023

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

"""

import re
import sys
import config
import json

from django.shortcuts import render

from src.utils import users
from src.utils import docManager

def getDirectUrlParam(request):
    if ('directUrl' in request.GET): 
        return request.GET['directUrl'].lower() in ("true")
    else:
        return False;    

def default(request):  # default parameters that will be passed to the template
    context = {
        'users': users.USERS,
        'languages': config.LANGUAGES,
        'preloadurl': config.DOC_SERV_SITE_URL + config.DOC_SERV_PRELOADER_URL,
        'editExt': json.dumps(config.DOC_SERV_EDITED),  # file extensions that can be edited
        'convExt': json.dumps(config.DOC_SERV_CONVERT),  # file extensions that can be converted
        'files': docManager.getStoredFiles(request),  # information about stored files
        'fillExt': json.dumps(config.DOC_SERV_FILLFORMS),
        'directUrl': str(getDirectUrlParam(request)).lower
    }
    return render(request, 'index.html', context)  # execute the "index.html" template with context data and return http response in json format