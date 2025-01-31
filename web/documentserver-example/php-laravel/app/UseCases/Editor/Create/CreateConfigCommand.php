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

namespace App\UseCases\Editor\Create;

use App\Models\Document;
use App\Models\Editor\Editor;
use App\Models\Editor\EditorConfig;
use App\OnlyOffice\Managers\FormatManager;
use App\Repositories\UserRepository;

class CreateConfigCommand
{
    public function __construct(
        private UserRepository $userRepository,
        private FormatManager $formatManager,
    ) {}

    public function __invoke(CreateConfigRequest $request): array
    {
        $format = $this->formatManager->find($request->fileExtension);
        $user = $this->userRepository->find($request->user);

        if ($user->goback !== null) {
            $user->goback['url'] = $request->serverAddress;
        }

        $editor = new Editor(
            new Document($request->filename, $request->fileKey, $request->fileUrl, $format),
            $user,
            new EditorConfig(
                mode: $request->mode,
                type: $request->type,
                lang: $request->lang,
                userAddress: $request->userAddress,
                serverAddress: $request->serverAddress,
                createUrl: $request->createUrl,
                templatesImageUrl: $request->templatesImageUrl,
                actionLink: $request->actionLink,
                callbackUrl: $request->callbackUrl,
                imagesUrl: $request->imagesUrl,
                directUrl: $request->directUrl,
            ),
        );

        return $editor->open();
    }
}
