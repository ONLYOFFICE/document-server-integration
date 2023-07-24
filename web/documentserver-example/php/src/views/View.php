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

namespace Example\Views;

class View
{
    private $template;
    protected $tagsValues;

    public function __construct($tempName)
    {
        $pathToTemplate = "./templates/".$tempName.".tpl";
        if (file_exists($pathToTemplate)) {
            $this->template = file_get_contents($pathToTemplate);
        } else {
            $this->template = "";
        }
    }

    private function parseTemplate($tagsValues = []): array|bool|string
    {
        $parsedTemplate = $this->template;
        foreach ($tagsValues as $tag => $value) {
            $parsedTemplate = str_replace("{".$tag."}", $value, $parsedTemplate);
        }
        return $parsedTemplate;
    }

    protected function getParsedTemplate()
    {
        return $this->parseTemplate($this->tagsValues);
    }

    private function renderTemplate($tagsValues)
    {
        echo ($this->parseTemplate($tagsValues));
    }

    public function render()
    {
        $this->renderTemplate($this->tagsValues);
    }
}
