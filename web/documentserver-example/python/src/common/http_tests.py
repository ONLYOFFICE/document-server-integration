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

from http import HTTPMethod, HTTPStatus
from unittest import TestCase
from django.conf import settings
from django.http import HttpRequest, HttpResponse
from . import http

# Under the hood, HttpRequest uses a settings object.
settings.configure()

@http.GET()
def endpoint(_: HttpRequest) -> HttpResponse:
    return HttpResponse()

class HTTPMethodTests(TestCase):
    def test_returns_a_response_from_the_endpoint(self):
        request = HttpRequest()
        request.method = HTTPMethod.GET
        response = endpoint(request)
        self.assertEqual(response.status_code, HTTPStatus.OK)

    def test_returns_an_error_response(self):
        request = HttpRequest()
        request.method = HTTPMethod.POST
        response = endpoint(request)
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)
