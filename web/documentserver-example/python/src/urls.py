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

from django.urls import path, re_path

from src.views import index, actions
from django.contrib.staticfiles.urls import staticfiles_urlpatterns

urlpatterns = [
    path('', index.default),
    path('upload', actions.upload),
    path('download', actions.download),
    path('downloadhistory', actions.downloadhistory),
    path('convert', actions.convert),
    path('create', actions.createNew),
    path('edit', actions.edit),
    path('track', actions.track),
    path('remove', actions.remove),
    path('csv', actions.csv),
    path('files', actions.files),
    path('saveas', actions.saveAs),
    path('rename', actions.rename)
]

urlpatterns += staticfiles_urlpatterns()