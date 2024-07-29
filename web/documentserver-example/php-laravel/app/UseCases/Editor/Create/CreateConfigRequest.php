<?php

namespace App\UseCases\Editor\Create;

class CreateConfigRequest
{
    public function __construct(
        public string $filename,
        public string $fileExtension,
        public string $fileKey,
        public string $fileUrl,
        public string $user,
        public string $mode,
        public string $type,
        public string $lang,
        public string $userAddress,
        public string $serverAddress,
        public string $createUrl,
        public string $templatesImageUrl,
        public string $actionLink,
        public string $callbackUrl,
        public string $imagesUrl,
        public string $directUrl,
    ) {}
}
