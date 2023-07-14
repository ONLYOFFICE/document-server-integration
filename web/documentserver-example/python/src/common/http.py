#
# (c) Copyright Ascensio System SIA 2023
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# TODO: add types for parameters.
# https://github.com/python/typing/discussions/946r

from http import HTTPStatus, HTTPMethod
from django.http import HttpRequest, HttpResponse

def GET():
    return method(HTTPMethod.GET)

def POST():
    return method(HTTPMethod.POST)

def PUT():
    return method(HTTPMethod.PUT)

def method(meth: HTTPMethod):
    def wrapper(func):
        def inner(request: HttpRequest, *args, **kwargs):
            if request.method is None:
                return HttpResponse(
                    status=HTTPStatus.METHOD_NOT_ALLOWED
                )

            if request.method.upper() != meth.name:
                return HttpResponse(
                    status=HTTPStatus.METHOD_NOT_ALLOWED
                )

            return func(request, *args, **kwargs)

        return inner

    return wrapper
