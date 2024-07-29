<?php

namespace App\Models;

use UnexpectedValueException;

class File
{
    public function __construct(
        public string $filename,
        public mixed $content,
        public ?string $mime,
        public ?int $size,
        public ?int $modified,
    ) {}

    public static function create(
        string $filename,
        mixed $content = null,
        ?string $mime = null,
        ?int $size = null,
        ?int $modified = null,
    ): self {
        if ($size && $size <= 0) {
            throw new UnexpectedValueException('The file size cannot be less than zero');
        }

        return new self(
            $filename,
            $content,
            $mime,
            $size,
            $modified
        );
    }

    public function toArray(): array
    {
        return [
            'filename' => $this->filename,
            'content' => $this->content,
            'mimeType' => $this->mime,
            'size' => $this->size,
            'lastModified' => $this->modified,
        ];

    }
}
