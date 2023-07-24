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

use OnlineEditorsExamplePhp\Configuration\ConfigurationManager;
use OnlineEditorsExamplePhp\Helpers\ConfigManager;
use OnlineEditorsExamplePhp\Helpers\ExampleUsers;
use function OnlineEditorsExamplePhp\getStoredFiles;

final class IndexView extends View
{

    public function __construct($request, $tempName = "index")
    {
        parent::__construct($tempName);
        $storedList = new IndexStoredListView($request);
        $configManager = new ConfigManager();
        $portalInfo = $this->getPortalInfoStyleDisplay();

        $this->tagsValues = [
            "user" => isset($request["user"]) ? htmlentities($request["user"]) : "",
            "userOpts" => $this->getUserListOptionsLayout(),
            "langs" => $this->getLanguageListOptionsLayout(),
            "portalInfoDisplay" => $portalInfo,
            "userDescr" => $this->getUserDescriptionLayout(),
            "storedList" => $portalInfo == "none" ? $storedList->getParsedTemplate() : "",
            "editButton" => $this->getEditButton(),
            "dataDocs" => $this->getPreloaderUrl(),
            "date" => date("Y"),
            "fillFormsExtList" => implode(",", $configManager->getFillExtensions()),
            "converExtList" => implode(",", $configManager->getConvertExtensions()),
            "editedExtList" => implode(",", $configManager->getEditExtensions()),
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
        $config_manager = new ConfigurationManager();
        foreach ($config_manager->languages() as $key => $language) {
            $layout .= '<option value="'.$key.'">'.$language.'</option>'.PHP_EOL;
        }
        return $layout;
    }

    private function getPortalInfoStyleDisplay()
    {
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

    private function getPreloaderUrl()
    {
        $config_manager = new ConfigurationManager();
        return $config_manager->document_server_preloader_url()->string();
    }

    private function getEditButton()
    {
        return '<div id="beginEdit" class="button orange disable">Edit</div>';
    }
}
