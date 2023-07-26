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
    private mixed $configFormats;

    public function __construct()
    {
        $this->configFormats = json_decode($this->getConfigurationFromatsJson());
    }

    private function getConfigurationFromatsJson(): bool|string
    {
        return file_exists("./assets/document-formats/onlyoffice-docs-formats.json")
            ? file_get_contents("./assets/document-formats/onlyoffice-docs-formats.json")
            : false;
    }

    public function getSuppotredFormats(): mixed
    {
        return $this->configFormats;
    }

    public function getSuppotredExtensions(): mixed
    {
        return array_reduce(
            $this->configFormats,
            function ($extensions, $format) {
                $extensions[] = $format->name;
                return $extensions;
            }
        );
    }

    public function getViewExtensions(): mixed
    {
        return array_reduce(
            $this->configFormats,
            function ($extensions, $format) {
                if (in_array("view", $format->actions)) {
                    $extensions[] = $format->name;
                }
                return $extensions;
            }
        );
    }

    public function getEditExtensions(): mixed
    {
        return array_reduce(
            $this->configFormats,
            function ($extensions, $format) {
                if (in_array("edit", $format->actions) || in_array("lossy-edit", $format->actions)) {
                    $extensions[] = $format->name;
                }
                return $extensions;
            }
        );
    }

    public function getFillExtensions(): mixed
    {
        return array_reduce(
            $this->configFormats,
            function ($extensions, $format) {
                if (in_array("fill", $format->actions)) {
                    $extensions[] = $format->name;
                }
                return $extensions;
            }
        );
    }

    public function getConvertExtensions(): mixed
    {
        return array_reduce(
            $this->configFormats,
            function ($extensions, $format) {
                if ($format->type === "word" && in_array("docx", $format->convert)
                    || $format->type === "cell" && in_array("xlsx", $format->convert)
                    || $format->type === "slide" && in_array("pptx", $format->convert)) {
                        $extensions[] = $format->name;
                }
                return $extensions;
            }
        );
    }
}
