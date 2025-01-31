<?php

/**
 * (c) Copyright Ascensio System SIA 2024.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace App\OnlyOffice\Services;

use Exception;
use GuzzleHttp\Client;
use GuzzleHttp\Exception\RequestException;
use Onlyoffice\DocsIntegrationSdk\Service\Request\HttpClientInterface;

class HttpClient implements HttpClientInterface
{
    private $status;

    private $body;

    public function __construct()
    {
        $this->status = null;
        $this->body = null;
    }

    /**
     * Request to Document Server with turn off verification.
     *
     * @param  string  $url  - request address
     * @param  string  $method  - request method
     * @param  array  $opts  - request options
     */
    public function request($url, $method = 'GET', $opts = [])
    {
        $httpClient = new Client(['base_uri' => $url]);
        try {
            $response = $httpClient->request($method, $url, $opts);
            $this->body = $response->getBody()->getContents();
            $this->status = $response->getStatusCode();
        } catch (RequestException $requestException) {
            throw new Exception($requestException->getMessage());
        }
    }

    /**
     * Get the status code
     */
    public function getStatusCode()
    {
        return $this->status;
    }

    /**
     * Get the response body.
     */
    public function getBody()
    {
        return $this->body;
    }
}
