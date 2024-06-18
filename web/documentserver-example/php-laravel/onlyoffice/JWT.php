<?php

namespace OnlyOffice;

use Firebase\JWT\JWT as FirebaseJWT;
use Firebase\JWT\Key;
use OnlyOffice\Config;

class JWT
{
    private string $secret;
    private string $algorithm;

    public function __construct(Config $config)
    {
        $this->secret = $config->get('jwt.secret');
        $this->algorithm = $config->get('jwt.algorithm');
    }
    /**
     * Encode a payload object into a token using a secret key
     *
     * @param array $payload
     *
     * @return string
     */
    public function encode(mixed $payload): string
    {
        return FirebaseJWT::encode($payload, $this->secret, $this->algorithm);
    }

    /**
     * Decode a token into a payload object using a secret key
     *
     * @param string $token
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
            $payload = "";
        }

        return $payload;
    }
}
