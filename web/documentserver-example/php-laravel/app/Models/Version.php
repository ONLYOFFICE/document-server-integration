<?php

namespace App\Models;

class Version
{
    public function __construct(
        public string $filename,
        public VersionInfo $info,
        public mixed $history,
        public mixed $changes,
    ) {}

    public static function create(
        string $filename,
        VersionInfo $info,
        mixed $history = null,
        mixed $changes = null,
    ): self {
        return new self(
            $filename,
            $info,
            $history,
            $changes,
        );
    }

    public function version(): int
    {
        return $this->info->version;
    }

    public function fileType(): string
    {
        return $this->info->fileType;
    }

    public function key(): string
    {
        return $this->info->key;
    }

    public function user(): string
    {
        return $this->info->userId;
    }
}
