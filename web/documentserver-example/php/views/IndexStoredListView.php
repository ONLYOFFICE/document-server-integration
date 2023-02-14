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

namespace OnlineEditorsExamplePhp\Views;

use function OnlineEditorsExamplePhp\getStoredFiles;
use function OnlineEditorsExamplePhp\getFileVersion;
use function OnlineEditorsExamplePhp\getHistoryDir;
use function OnlineEditorsExamplePhp\getStoragePath;

class IndexStoredListView extends View
{
    private $tagsValues;

    public function __construct($tempName = "storedList")
    {
        parent::__construct($tempName);
        $this->tagsValues = [
            "user" => htmlentities($_GET["user"]) ?? "",
        ];
    }

    public function getParsedTemplate()
    {
        return $this->parseTemplate($this->tagsValues);
    }

    public function getStoredListLayout()
    {
        $storedFiles = getStoredFiles();
        $layout = "";
        $user = htmlentities($_GET["user"]) ?? "";
        $directUrlArg = isset($_GET["directUrl"]) ? "&directUrl=" . $_GET["directUrl"] : "";
        if (!empty($storedFiles)) {
            foreach ($storedFiles as &$storeFile) {
                $layout .= '<tr class="tableRow" title="'.$storeFile->name.' ['.getFileVersion(
                    getHistoryDir(
                        getStoragePath($storeFile->name)
                    )
                ).']">';
                $layout .= ' <td class="contentCells"><br><a class="stored-edit '.
                    $storeFile->documentType.'" href="doceditor.php?fileID='.
                    urlencode($storeFile->name).
                    '&user='.$user.
                    $directUrlArg .'" target="_blank">'.'   <span>'.$storeFile->name.'</span>  </a><br></td>';
            }
        }
    }
}