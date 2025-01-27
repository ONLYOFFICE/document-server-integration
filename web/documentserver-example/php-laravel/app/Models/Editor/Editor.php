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

use App\Models\Document;
use App\Models\User;

class Editor
{
    public function __construct(
        private Document $document,
        private User $user,
        private EditorConfig $config,
    ) {}

    public function open(): array
    {
        return [
            'document' => [
                'fileType' => $this->document->format->extension(),
                'info' => $this->buildInfo(),
                'key' => $this->document->key,
                'permissions' => $this->buildPermissions(),
                'referenceData' => $this->buildReferenceData(),
                'title' => $this->document->title,
                'url' => $this->document->url,
                'directUrl' => $this->config->directUrl,
            ],
            'documentType' => $this->document->format->type,
            'editorConfig' => $this->buildEditorConfig(),
            'type' => $this->config->type,
        ];
    }

    private function buildInfo(): array
    {
        return [
            'owner' => 'Me',
            'uploaded' => date('d.m.y'),
            'favorite' => $this->user->favorite,
        ];
    }

    private function buildPermissions(): array
    {
        $permission = new EditorPermissions($this->user, $this->document->format, $this->config->mode);

        return [
            'chat' => $permission->canUseChat(),
            'comment' => $permission->canComment(),
            'commentGroups' => $this->user->commentGroups,
            'copy' => $permission->canCopy(),
            'download' => $permission->canDownload(),
            'edit' => $permission->canEdit(),
            'fillForms' => $permission->canFillForms(),
            'modifyContentControl' => $permission->canModifyContentControl(),
            'modifyFilter' => $permission->canModifyFilter(),
            'print' => $permission->canPrint(),
            'protect' => $permission->canBeProtected(),
            'review' => $permission->canReview(),
            'reviewGroups' => $this->user->reviewGroups,
            'userInfoGroups' => $this->user->userInfoGroups,
        ];
    }

    private function buildReferenceData(): array
    {
        return [
            'fileKey' => $this->user->id != 'uid-0' ? json_encode([
                'fileName' => $this->document->title,
                'userAddress' => $this->config->userAddress,
            ]) : null,
            'instanceId' => $this->config->serverAddress,
        ];
    }

    private function buildEditorConfig(): array
    {
        $templates = [
            [
                'image' => '',
                'title' => 'Blank',
                'url' => $this->config->createUrl,
            ],
            [
                'image' => $this->config->templatesImageUrl,
                'title' => 'With sample content',
                'url' => $this->config->createUrl.'&sample=true',
            ],
        ];

        return [
            'actionLink' => $this->config->actionLink ? json_decode($this->config->actionLink) : null,
            'mode' => $this->config->mode,
            'lang' => $this->config->lang,
            'callbackUrl' => $this->config->callbackUrl,
            'coEditing' => $this->config->mode === 'view' && $this->user->id === 'uid-0' ? [
                'mode' => 'strict',
                'change' => false,
            ] : null,
            'createUrl' => $this->user->id !== 'uid-0' ? $this->config->createUrl : null,
            'templates' => $this->user->templates ? $templates : null,
            'user' => [
                'id' => $this->user->id != 'uid-0' ? $this->user->id : null,
                'name' => $this->user->name,
                'group' => $this->user->group,
                'image' => $this->user->avatar ? $this->config->imagesUrl.$this->user->id.'.png' : null,
            ],
            'embedded' => [
                'saveUrl' => $this->config->directUrl,
                'embedUrl' => $this->config->directUrl,
                'shareUrl' => $this->config->directUrl,
                'toolbarDocked' => 'top',
            ],
            'customization' => [
                'about' => true,
                'comments' => true,
                'feedback' => true,
                'forcesave' => false,
                'submitForm' => $this->user->id === 'uid-1',
                'goback' => $this->user->goback !== null ? $this->user->goback : '',
                'close' => $this->user->close !== null ? $this->user->close : '',
            ],
        ];
    }
}
