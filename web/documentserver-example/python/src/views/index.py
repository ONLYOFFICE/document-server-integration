"""

 (c) Copyright Ascensio System SIA 2024

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

from django.shortcuts import render

from src.configuration import ConfigurationManager
from src.utils import users
from src.utils import docManager

config_manager = ConfigurationManager()


def getDirectUrlParam(request):
    if 'directUrl' in request.GET:
        return request.GET['directUrl'].lower() in ("true")

    return False


def default(request):  # default parameters that will be passed to the template
    context = {
        'users': users.USERS,
        'languages': config_manager.languages(),
        'preloadurl': config_manager.document_server_preloader_url().geturl(),
        'files': docManager.getStoredFiles(request),  # information about stored files
        'directUrl': str(getDirectUrlParam(request)).lower,
        'serverVersion': config_manager.getVersion(),
        'enableForgotten': config_manager.enable_forgotten(),
    }
    # execute the "index.html" template with context data and return http response in json format
    return render(request, 'index.html', context)
