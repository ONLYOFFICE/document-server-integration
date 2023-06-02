<?php

namespace OnlineEditorsExamplePhp\Helpers;

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

final class ConfigManager
{
    private mixed $config;

    public function __construct()
    {
        $this->config = json_decode($this->getConfigurationJson());
        if ($this->config === null) {
            return;
        }

        $fileSizeMax = getenv("FILE_SIZE_MAX");
        if ($fileSizeMax !== false) {
            $this->config->fileSizeMax = $fileSizeMax;
        }

        $storagePath = getenv("STORAGE_PATH");
        if ($storagePath !== false) {
            $this->config->storagePath = $storagePath;
        }

        $docServTimeout = getenv("DOC_SERV_TIMEOUT");
        if ($docServTimeout !== false) {
            $this->config->docServTimeout = $docServTimeout;
        }

        $docServSiteUrl = getenv("DOC_SERV_SITE_URL");
        if ($docServSiteUrl !== false) {
            $this->config->docServSiteUrl = $docServSiteUrl;
        }

        $docServJwtSecret = getenv("DOC_SERV_JWT_SECRET");
        if ($docServJwtSecret !== false) {
            $this->config->docServJwtSecret = $docServJwtSecret;
        }

        $exampleUrl = getenv("EXAMPLE_URL");
        if ($exampleUrl !== false) {
            $this->config->exampleUrl = $exampleUrl;
        }
    }

    private function getConfigurationJson(): bool|string
    {
        return file_exists("./config.json") ? file_get_contents("./config.json") : false;
    }

    /**
     * @param string|null $configName
     * @return mixed
     */
    public function getConfig(string $configName = null): mixed
    {
        if ($configName) {
            return $this->config->$configName ?? "";
        }
        return $this->config;
    }
}
