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

namespace App\Models\Editor;

use App\Models\User;
use App\OnlyOffice\Models\Format;

class EditorPermissions
{
    public function __construct(
        private User $user,
        private Format $format,
        private $action,
    ) {}

    public function canComment(): bool
    {
        return $this->action !== 'view'
            && $this->action !== 'fillForms'
            && $this->action !== 'embedded'
            && $this->action !== 'blockcontent';
    }

    public function canCopy(): bool
    {
        return ! in_array('copy', $this->user->deniedPermissions);
    }

    public function canDownload(): bool
    {
        return ! in_array('download', $this->user->deniedPermissions);
    }

    public function canEdit(): bool
    {
        return ($this->editable() || $this->fillable())
            && ($this->action === 'edit'
                || $this->action == 'view'
                || $this->action == 'filter'
                || $this->action == 'blockcontent');
    }

    public function canPrint(): bool
    {
        return ! in_array('print', $this->user->deniedPermissions);
    }

    public function canFillForms(): bool
    {
        return $this->action !== 'view'
                && $this->action !== 'comment'
                && $this->action != 'blockcontent';
    }

    public function canModifyFilter(): bool
    {
        return $this->action !== 'filter';
    }

    public function canModifyContentControl(): bool
    {
        return $this->action !== 'blockcontent';
    }

    public function canReview(): bool
    {
        return $this->fullyEditable()
            && ($this->action === 'edit' || $this->action === 'review');
    }

    public function canUseChat(): bool
    {
        return $this->user->id !== 'uid-0';
    }

    public function canBeProtected(): bool
    {
        return ! in_array('protect', $this->user->deniedPermissions);
    }

    private function fullyEditable(): bool
    {
        return $this->editable() || $this->fillable();
    }

    private function editable(): bool
    {
        return $this->action === 'edit' && $this->format->isEditable();
    }

    private function fillable(): bool
    {
        return (($this->action === 'edit' && ! $this->format->isEditable())
            || $this->action === 'fillForms') && $this->format->isFillable();
    }
}
