<?php

namespace App\Models;

class VersionInfo
{
    public function __construct(
        public string $key,
        public string $fileType,
        public int $version,
        public string $created,
        public string $userId,
        public ?string $serverVersion,
    ) {}

    public static function create(
        string $key,
        string $fileType,
        int $version,
        string $created,
        string $userId,
        ?string $serverVersion = null,
    ): self {
        return new self(
            $key,
            $fileType,
            $version,
            $created,
            $userId,
            $serverVersion,
        );
    }

    public function toArray(): array
    {
        return [
            'key' => $this->key,
            'fileType' => $this->fileType,
            'version' => $this->version,
            'created' => $this->created,
            'user' => $this->userId,
            'serverVersion' => $this->serverVersion,
        ];
    }
}
