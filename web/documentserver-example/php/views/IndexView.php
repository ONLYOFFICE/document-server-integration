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

use OnlineEditorsExamplePhp\Helpers\ConfigManager;
use OnlineEditorsExamplePhp\Helpers\ExampleUsers;
use function OnlineEditorsExamplePhp\getStoredFiles;
use function OnlineEditorsExamplePhp\getFileVersion;
use function OnlineEditorsExamplePhp\getHistoryDir;
use function OnlineEditorsExamplePhp\getStoragePath;

final class IndexView extends View
{
    private $tagsValues;

    public function __construct($tempName = "index")
    {
        parent::__construct($tempName);
        $this->tagsValues = [
            "user" => htmlentities($_GET["user"]) ?? "",
            "userOpts" => $this->getUserListOptionsLayout(),
            "langs" => $this->getLanguageListOptionsLayout(),
            "portalInfoDisplay" => $this->getPortalInfoStyleDisplay(),
            "userDescr" => $this->getUserDescriptionLayout(),
        ];
    }

    private function getUserListOptionsLayout()
    {
        $layout = "";
        $userList = new ExampleUsers();
        foreach ($userList->getAllUsers() as $userL) {
            $name = $userL->name ?: "Anonymous";
            $layout .= '<option value="'.$userL->id.'">'.$name.'</option>'.PHP_EOL;
        }
        return $layout;
    }

    private function getLanguageListOptionsLayout()
    {
        $layout = "";
        $configManager = new ConfigManager();
        foreach ($configManager->getConfig("languages") as $key => $language) {
            $layout .= '<option value="'.$key.'">'.$language.'</option>'.PHP_EOL;
        }
        return $layout;
    }

    private function getPortalInfoStyleDisplay()
    {
        $layout = "";
        $storedFiles = getStoredFiles();
        if (!empty($storedFiles)) {
            return "none";
        }
        return "table-cell";
    }

    private function getUserDescriptionLayout()
    {
        $layout = "";
        $userList = new ExampleUsers();
        foreach ($userList->getAllUsers() as $userL) {
            $name = $userL->name ?: "Anonymous";
            $layout .= '<div class="user-descr"><br><b>'.$name.'</b><br><ul>';
            foreach ($userL->descriptions as $description) {
                $layout .= '<li>'.$description.'</li>';
            }
            $layout .= '</ul><br></div>';
        }
        return $layout;
    }

    private function getStoredListLayout()
    {
        $storedList = new IndexStoredListView();
        return $storedList->getParsedTemplate();
    }

    public function render()
    {
        $this->renderTemplate($this->tagsValues);
    }
}
