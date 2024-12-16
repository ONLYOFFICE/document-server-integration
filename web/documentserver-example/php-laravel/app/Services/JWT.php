<?php

/**
 * (c) Copyright Ascensio System SIA 2024
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

namespace App\Services;

use Firebase\JWT\JWT as FirebaseJWT;
use Firebase\JWT\Key;

class JWT
{
    private string $secret;

    private string $algorithm;

    public function __construct(ServerConfig $config)
    {
        $this->secret = $config->get('jwt.secret');
        $this->algorithm = $config->get('jwt.algorithm');
    }

    /**
     * Encode a payload object into a token using a secret key
     *
     * @param  array  $payload
     */
    public function encode(mixed $payload): string
    {
        return FirebaseJWT::encode($payload, $this->secret, $this->algorithm);
    }

    /**
     * Decode a token into a payload object using a secret key
     *
     *
     * @return string
     */
    public function decode(string $token)
    {
        try {
            $payload = FirebaseJWT::decode(
                $token,
                new Key($this->secret, $this->algorithm),
            );
        } catch (\UnexpectedValueException $e) {
            $payload = '';
        }

        return $payload;
    }
}
