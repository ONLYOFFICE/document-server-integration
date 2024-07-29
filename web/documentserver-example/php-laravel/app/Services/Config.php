<?php

namespace App\Services;

use Exception;

abstract class Config
{
    protected array $config;

    public function get(string $key, mixed $default = null): mixed
    {
        $keys = explode('.', $key);
        $result = $this->config;

        try {
            foreach ($keys as $key) {
                $result = $result[$key];
            }
        } catch (Exception $e) {
            $result = $default;
        }

        return $result;
    }
}
