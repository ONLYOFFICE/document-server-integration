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
                $layout .= '<tr class="tableRow" title="'.$storeFile->name.' ['.getFileVersion(
                    getHistoryDir(
                        getStoragePath($storeFile->name)
                    )
                ).']">';
                $layout .= ' <td class="contentCells"><a class="stored-edit '.
                    $storeFile->documentType.'" href="editor?fileID='.
                    urlencode($storeFile->name).
                    '&user='.$user.
                    $directUrlArg .'" target="_blank">'.'<span>'.$storeFile->name.'</span></a></td>';
                if ($storeFile->canEdit) {
                    $layout .= ' <td class="contentCells contentCells-icon">   <a href="editor?fileID='.
                    urlencode($storeFile->name).'&user=' . htmlentities($user).$directUrlArg.
                     '&action=edit&type=desktop" target="_blank">'.
                     '<img src="assets/images/desktop.svg" alt="Open in editor for full size screens"'.
                     ' title="Open in editor for full size screens"/></a></td>'.
                     ' <td class="contentCells contentCells-icon">  <a href="editor?fileID='.
                        urlencode($storeFile->name).'&user=' . htmlentities($user).$directUrlArg.
                     '&action=edit&type=mobile" target="_blank">'.
                    '<img src="assets/images/mobile.svg" alt="Open in editor for mobile devices"'.
                    ' title="Open in editor for mobile devices" /></a></td>'.
                     ' <td class="contentCells contentCells-icon">  <a href="editor?fileID='.
                        urlencode($storeFile->name).'&user='.htmlentities($user).$directUrlArg.
                    '&action=comment&type=desktop" target="_blank">'.
                    '   <img src="assets/images/comment.svg" alt="Open in editor for comment"'.
                    ' title="Open in editor for comment" /></a></td>';
                    if ($storeFile->documentType == "word") {
                        $layout .= '<td class="contentCells contentCells-icon">   <a href="editor?fileID='.
                            urlencode($storeFile->name).'&user='.htmlentities($user).$directUrlArg.
                            '&action=review&type=desktop" target="_blank">'.
                        '   <img src="assets/images/review.svg" alt="Open in editor for review"'.
                        ' title="Open in editor for review" /></a></td>'.
                        ' <td class="contentCells contentCells-icon ">   <a href="editor?fileID='.
                        urlencode($storeFile->name).'&user='.htmlentities($user).$directUrlArg.
                            '&action=blockcontent&type=desktop" target="_blank">'.
                        '   <img src="assets/images/block-content.svg"'.
                            ' alt="Open in editor without content control modification"'.
                        ' title="Open in editor without content control modification"</a></td>';
                    } elseif ($storeFile->documentType == "cell") {
                        $layout .= '<td class="contentCells contentCells-icon">  <a href="editor?fileID='.
                            urlencode($storeFile->name).'&user='.htmlentities($user).$directUrlArg.
                            '&action=filter&type=desktop" target="_blank">'.
                        '   <img src="assets/images/filter.svg"'.
                        ' alt="Open in editor without access to change the filter"'.
                        ' title="Open in editor without access to change the filter" /></a></td>';
                    } else {
                        $layout .= '<td class="contentCells  contentCells-icon"></td>';
                        $layout .= '<td class="contentCells  contentCells-icon"></td>';
                    }
                    if ($storeFile->isFillFormDoc) {
                        $layout.= ' <td class="contentCells contentCells-shift contentCells-icon'.
                            ' firstContentCellShift">'.
                        '  <a href="editor?fileID='.urlencode($storeFile->name).
                            '&user='.htmlentities($user).$directUrlArg.
                            '&action=fillForms&type=desktop" target="_blank">'.
                        '   <img src="assets/images/fill-forms.svg" alt="Open in editor for filling in forms"'.
                        ' title="Open in editor for filling in forms" /></a></td>';
                    } else {
                        $layout .= '<td class="contentCells contentCells-shift contentCells-icon'.
                        'firstContentCellShift"></td>';
                    }
                } elseif ($storeFile->isFillFormDoc) {
                    $layout .= '<td class="contentCells contentCells-icon">   <a href="editor?fileID='.
                        urlencode($storeFile->name).'&user='.htmlentities($user).$directUrlArg.
                        '&action=fillForms&type=desktop" target="_blank">'.
                    '   <img src="assets/images/mobile-fill-forms.svg" alt="Open in editor for filling in forms'.
                    'for mobile devices" title="Open in editor for filling in forms for mobile devices" /></a></td>'.
                    '<td class="contentCells contentCells-icon"></td>'.
                    '<td class="contentCells contentCells-icon"></td>'.
                    '<td class="contentCells contentCells-icon"></td>'.
                    '<td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">'.
                    '<a href="editor?fileID='.urlencode($storeFile->name).'&user='.htmlentities($user).
                        $directUrlArg.'&action=fillForms&type=desktop" target="_blank">'.
                    '<img src="assets/images/fill-forms.svg" alt="Open in editor for filling in forms"'.
                    ' title="Open in editor for filling in forms"/></a></td>';
                } else {
                    $layout .= '<td class="contentCells contentCells-shift contentCells-icon' .
                        'contentCellsEmpty" colspan="6"></td>';
                }
                $layout .= '<td class="contentCells contentCells-icon firstContentCellViewers">'.
                    '  <a href="editor?fileID='.urlencode($storeFile->name).'&user='.htmlentities($user).
                        $directUrlArg.'&action=view&type=desktop" target="_blank">'.
                    '   <img src="assets/images/desktop.svg" alt="Open in viewer for full size screens"'.
                    ' title="Open in viewer for full size screens" /></a></td>'.
                    ' <td class="contentCells contentCells-icon">  <a href="editor?fileID='.
                        urlencode($storeFile->name).'&user='.htmlentities($user).$directUrlArg.
                        '&action=view&type=mobile" target="_blank">'.
                    '   <img src="assets/images/mobile.svg" alt="Open in viewer for mobile devices"'.
                    ' title="Open in viewer for mobile devices" /></a></td>'.
                    ' <td class="contentCells contentCells-icon contentCells-shift">'.
                    '  <a href="editor?fileID='.urlencode($storeFile->name).'&user='.htmlentities($user).
                        $directUrlArg.'&action=embedded&type=embedded" target="_blank">'.
                    '   <img src="assets/images/embeded.svg" alt="Open in embedded mode"'.
                    ' title="Open in embedded mode" /></a>'.
                    ' <td class="contentCells contentCells-icon contentCells-shift  downloadContentCellShift">'.
                    '<a href="download?fileName='.urlencode($storeFile->name).'">'.
                    '   <img class="icon-download" src="assets/images/download.svg"  alt="Download" title="Download"'.
                    ' /></a></td>'.
                    '<td class="contentCells contentCells-icon contentCells-shift">'.
                    '  <a class="delete-file" data="'.$storeFile->name.'">'.
                    '   <img class="icon-delete" src="assets/images/delete.svg" alt="Delete" title="Delete" /></a>'.
                    '</td></tr>';
            }
        }
        return $layout;
    }
}
