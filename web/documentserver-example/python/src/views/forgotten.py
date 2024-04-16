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
from src.utils import trackManager, fileUtils
from django.http import HttpResponse
import json

config_manager = ConfigurationManager()


def default(request):  # default parameters that will be passed to the template
    context = {
        'files': getForgottenFiles(),  # information about stored files
        'serverVersion': config_manager.getVersion()
    }
    # execute the "index.html" template with context data and return http response in json format
    return render(request, 'forgotten.html', context)


def getForgottenFiles():
    files = []

    try:
        forgottenList = trackManager.commandRequest('getForgottenList', '').json()

        for key in forgottenList["keys"]:
            file = trackManager.commandRequest('getForgotten', key).json()
            file["type"] = fileUtils.getFileType(file["url"])
            files.append(file)
    except (Exception, ValueError):
        pass  # TODO: write to logger

    return files


# delete a forgotten file from the document server
def delete(request):
    response = {}
    status = 204

    try:
        filename = request.GET.get('filename', '')
        if filename:
            trackManager.commandRequest('deleteForgotten', filename)
    except Exception as e:
        response.setdefault('error', str(e.args[0]))
        status = 500

    return HttpResponse(
        json.dumps(response),
        content_type='application/json',
        status=status)