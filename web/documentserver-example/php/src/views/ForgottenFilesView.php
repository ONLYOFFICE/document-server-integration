<?php
/**
 * (c) Copyright Ascensio System SIA 2025
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

use Example\Configuration\ConfigurationManager;
use function Example\getForgottenFiles;

final class ForgottenFilesView extends View
{

    public function __construct($request, $tempName = "forgotten")
    {
        parent::__construct($tempName);
        $configManager = new ConfigurationManager();

        $this->tagsValues = [
            "files" => $this->getForgottenFilesLayout(),
            "date" => date("Y"),
            "serverVersion" => $configManager->getVersion(),
        ];
    }

    private function getForgottenFilesLayout()
    {
        $layout = "";
        $files = getForgottenFiles();
        foreach ($files as $file) {
            $layout .= <<<EOT
            <tr class="tableRow" title="$file->key">
                <td>
                    <a class="stored-edit action-link $file->type" href="$file->url" target="_blank">
                        <span>$file->key</span>
                    </a>
                </td>
                <td>
                    <a href="$file->url">
                        <img class="icon-download" src="assets/images/download.svg" alt="Download" title="Download" />
                    </a>
                    <a class="delete-file" data="$file->key">
                        <img class="icon-delete" src="assets/images/delete.svg" alt="Delete" title="Delete" /></a>
                </td>
            </tr>
            EOT;
        }
        return $layout;
    }
}
