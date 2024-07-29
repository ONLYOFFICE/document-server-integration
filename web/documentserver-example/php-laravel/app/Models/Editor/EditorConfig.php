<?php

namespace App\Models\Editor;

class EditorConfig
{
    public function __construct(
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
