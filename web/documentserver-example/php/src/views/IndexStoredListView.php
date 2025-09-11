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

use function Example\getStoredFiles;
use function Example\getFileVersion;
use function Example\getHistoryDir;
use function Example\getStoragePath;

class IndexStoredListView extends View
{
    private $request;

    public function __construct($request, $tempName = "storedList")
    {
        parent::__construct($tempName);
        $this->request = $request;
        $this->tagsValues = [
            "user" =>  isset($this->request["user"]) ? htmlentities($this->request["user"]) : "",
            "fileListTable" => $this->getStoredListLayout(),
        ];
    }

    public function getStoredListLayout()
    {
        $storedFiles = getStoredFiles();
        $layout = "";
        $user = isset($this->request["user"]) ? htmlentities($this->request["user"]) : "";
        $directUrlArg = isset($this->request["directUrl"]) ? "&directUrl=" . $this->request["directUrl"] : "";
        if (!empty($storedFiles)) {
            foreach ($storedFiles as &$storeFile) {
                $fileName = urlencode($storeFile->name);
                $userArg = '&user=' . htmlentities($user);
                $layout .= '<tr class="tableRow" title="'.$storeFile->name.' ['.getFileVersion(
                    getHistoryDir(
                        getStoragePath($storeFile->name)
                    )
                ).']">';
                $layout .= <<<TITLE
                    <td class="contentCells">
                        <a class="stored-edit {$storeFile->documentType}"
                           href="editor?fileID={$fileName}{$userArg}{$directUrlArg}"
                           target="_blank"
                        >
                            <span>{$storeFile->name}</span>
                        </a>
                    </td>
                TITLE;

                // 1-2
                if (in_array("edit", $storeFile->actions) || in_array("lossy-edit", $storeFile->actions)) {
                    $layout .= <<<EDIT
                        <td class="contentCells contentCells-icon" data-section="EDITOR">
                            <a href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=edit&type=desktop"
                               target="_blank"
                            >
                                <img src="assets/images/edit.svg"
                                     alt="Open for full size screens"
                                     title="Open for full size screens"
                                />
                            </a>
                        </td>
                        <td class="contentCells contentCells-icon">
                            <a href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=edit&type=mobile"
                               target="_blank"
                            >
                                <img src="assets/images/mobileEdit.svg"
                                     alt="Open for mobile devices"
                                     title="Open for mobile devices"
                                />
                            </a>
                        </td>
                    EDIT;
                } else {
                    $layout .= <<<EMPTY
                        <td class="contentCells contentCells-icon" data-section="EDITOR"></td>
                        <td class="contentCells contentCells-icon"></td>
                    EMPTY;
                }

                // 3
                if (in_array("comment", $storeFile->actions)) {
                    $layout .= <<<COMMENT
                        <td class="contentCells contentCells-icon">
                            <a href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=comment&type=desktop"
                               target="_blank"
                            >
                                <img src="assets/images/comment.svg"
                                     alt="Open for comment"
                                     title="Open for comment"
                                />
                            </a>
                        </td>
                    COMMENT;
                } else {
                    $layout .= '<td class="contentCells contentCells-icon"></td>';
                }

                // 4-5
                if (in_array("fill", $storeFile->actions)) {
                    $layout.= <<<FILL
                        <td class="contentCells contentCells-icon firstContentCellShift">
                            <a href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=fillForms&type=desktop"
                               target="_blank"
                            >
                                <img src="assets/images/formsubmit.svg"
                                     alt="Open for filling in forms"
                                     title="Open for filling in forms"
                                />
                            </a>
                        </td>
                        <td class="contentCells contentCells-icon contentCells-shift">
                            <a href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=fillForms&type=mobile"
                               target="_blank"
                            >
                                <img src="assets/images/mobile-fill-forms.svg"
                                     alt="Open for filling in forms for mobile devices"
                                     title="Open for filling in forms for mobile devices"
                                />
                            </a>
                        </td>
                    FILL;
                } else {
                    // 4
                    if (in_array("review", $storeFile->actions)) {
                        $layout .= <<<REVIEW
                            <td class="contentCells contentCells-icon">
                                <a href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=review&type=desktop"
                                   target="_blank"
                                >
                                    <img src="assets/images/review.svg"
                                         alt="Open for review"
                                         title="Open for review"
                                    />
                                </a>
                            </td>
                        REVIEW;
                    } elseif (in_array("customfilter", $storeFile->actions)) {
                        $layout .= <<<FILTER
                            <td class="contentCells contentCells-icon">
                                <a href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=filter&type=desktop"
                                   target="_blank"
                                >
                                    <img src="assets/images/filter.svg"
                                         alt="Open without access to change the filter"
                                         title="Open without access to change the filter"
                                    />
                                </a>
                            </td>
                        FILTER;
                    } else {
                        $layout .= '<td class="contentCells  contentCells-icon"></td>';
                    }

                    // 5
                    if (in_array("edit", $storeFile->actions) && $storeFile->documentType == "word") {
                        $layout .= <<<BLOCK
                            <td class="contentCells contentCells-icon contentCells-shift">
                                <a
                            href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=blockcontent&type=desktop"
                            target="_blank"
                                >
                                    <img src="assets/images/block-content.svg"
                                         alt="Open without content control modification"
                                         title="Open without content control modification"
                                    />
                                </a>
                            </td>
                        BLOCK;
                    } else {
                        $layout .= '<td class="contentCells contentCells-icon contentCells-shift"></td>';
                    }
                }

                $layout .= <<<VIEW
                    <td class="contentCells contentCells-icon firstContentCellViewers" data-section="VIEWERS">
                        <a href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=view&type=desktop"
                           target="_blank"
                        >
                            <img src="assets/images/view.svg"
                                 alt="Open for full size screens"
                                 title="Open for full size screens"
                            />
                        </a>
                    </td>
                    <td class="contentCells contentCells-icon">
                        <a href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=view&type=mobile"
                           target="_blank"
                        >
                            <img src="assets/images/mobileView.svg"
                                 alt="Open for mobile devices"
                                 title="Open for mobile devices"
                            />
                        </a>
                    </td>
                    <td class="contentCells contentCells-icon contentCells-shift">
                        <a href="editor?fileID={$fileName}{$userArg}{$directUrlArg}&action=embedded&type=embedded"
                           target="_blank"
                        >
                            <img src="assets/images/embedview.svg"
                                 alt="Open in embedded mode"
                                 title="Open in embedded mode"
                            />
                        </a>
                    </td>
                VIEW;

                if ($storeFile->documentType != null) {
                    $layout .= <<<CONVERT
                        <td class="contentCells contentCells-icon" data-section="ACTIONS">
                            <a class="convert-file" data="{$storeFile->name}" data-type="{$storeFile->documentType}">
                                <img class="icon-action" src="assets/images/convert.svg"
                                     alt="Convert" title="Convert"
                                />
                            </a>
                        </td>
                    CONVERT;
                } else {
                    $layout .= <<<EMPTY
                        <td class="contentCells contentCells-icon downloadContentCellShift"
                            data-section="ACTIONS"
                        ></td>
                    EMPTY;
                }
                $layout .= <<<ACTIONS
                    <td class="contentCells contentCells-icon downloadContentCellShift">
                        <a href="download?fileName={$fileName}">
                            <img class="icon-download" src="assets/images/download.svg"
                                 alt="Download" title="Download"
                            />
                        </a>
                    </td>
                    <td class="contentCells contentCells-icon contentCells-shift">
                        <a class="delete-file" data="{$storeFile->name}">
                            <img class="icon-action" src="assets/images/delete.svg" alt="Delete" title="Delete" />
                        </a>
                    </td>
                    <td class="contentCells contentCells-icon">
                        <a href="#" onclick="toggleContextMenu(event)">
                            <img src="assets/images/open-context.svg"
                                 alt="Open context menu" title="Open context menu"
                            />
                        </a>
                    </td>
                </tr>
                ACTIONS;
            }
        }
        return $layout;
    }
}
