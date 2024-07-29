<?php

namespace App\UseCases\Editor\Create;

use App\Models\Document;
use App\Models\Editor\Editor;
use App\Models\Editor\EditorConfig;
use App\Repositories\FormatRepository;
use App\Repositories\UserRepository;

class CreateConfigCommand
{
    public function __construct(
        private UserRepository $userRepository,
        private FormatRepository $formatRepository,
    ) {}

    public function __invoke(CreateConfigRequest $request): array
    {
        $format = $this->formatRepository->find($request->fileExtension);
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
