<?php
/**
 * (c) Copyright Ascensio System SIA 2023
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

require_once dirname(__FILE__) . '/lib/jwt/BeforeValidException.php';
require_once dirname(__FILE__) . '/lib/jwt/ExpiredException.php';
require_once dirname(__FILE__) . '/lib/jwt/SignatureInvalidException.php';
require_once dirname(__FILE__) . '/lib/jwt/JWT.php';
require_once dirname(__FILE__) . '/config.php';

/**
 * Check if a secret key to generate token exists or not.
 *
 * @return bool
 */
function isJwtEnabled()
{
    return !empty($GLOBALS['DOC_SERV_JWT_SECRET']);
}

/**
 * Check if a secret key use for request
 *
 * @return bool
 */
function tokenUseForRequest()
{
    return $GLOBALS['DOC_SERV_JWT_USE_FOR_REQUEST'] ?: false;
}

/**
 * Encode a payload object into a token using a secret key
 *
 * @param array $payload
 *
 * @return string
 */
function jwtEncode($payload)
{
    return \Firebase\JWT\JWT::encode($payload, $GLOBALS['DOC_SERV_JWT_SECRET']);
}

/**
 * Decode a token into a payload object using a secret key
 *
 * @param string $token
 *
 * @return string
 */
function jwtDecode($token)
{
    try {
        $payload = \Firebase\JWT\JWT::decode($token, $GLOBALS['DOC_SERV_JWT_SECRET'], ['HS256']);
    } catch (\UnexpectedValueException $e) {
        $payload = "";
    }

    return $payload;
}
