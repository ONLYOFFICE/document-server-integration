<?php
/*
 *
 * (c) Copyright Ascensio System SIA 2019
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
*/
?>

<?php

require_once( dirname(__FILE__) . '/config.php' );

function isJwtEnabled() {
    return !empty($GLOBALS['DOC_SERV_JWT_SECRET']);
}

function jwtEncode($payload) {
    $header = [
        "alg" => "HS256",
        "typ" => "JWT"
    ];
    $encHeader = base64UrlEncode(json_encode($header));
    $encPayload = base64UrlEncode(json_encode($payload));
    $hash = base64UrlEncode(calculateHash($encHeader, $encPayload));

    return "$encHeader.$encPayload.$hash";
}

function jwtDecode($token) {
    if (!isJwtEnabled()) return "";

    $split = explode(".", $token);
    if (count($split) != 3) return "";

    $hash = base64UrlEncode(calculateHash($split[0], $split[1]));

    if (strcmp($hash, $split[2]) != 0) return "";
    return base64UrlDecode($split[1]);
}

function calculateHash($encHeader, $encPayload) {
    return hash_hmac("sha256", "$encHeader.$encPayload", $GLOBALS['DOC_SERV_JWT_SECRET'], true);
}

function base64UrlEncode($str) {
    return str_replace("/", "_", str_replace("+", "-", trim(base64_encode($str), "=")));
}

function base64UrlDecode($payload) {
    $b64 = str_replace("_", "/", str_replace("-", "+", $payload));
    switch (strlen($b64) % 4) {
        case 2:
            $b64 = $b64 . "=="; break;
        case 3:
            $b64 = $b64 . "="; break;
    }
    return base64_decode($b64);
}

?>