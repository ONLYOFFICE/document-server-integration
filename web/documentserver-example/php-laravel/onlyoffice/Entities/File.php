<?php

namespace OnlyOffice\Entities;

class File {
    public string $filename;
    public string $basename;
    public string $extension;
    public string $path;
    public mixed $content;
    public User $author;
    public Format $format;
    public float $size;
    public string $lastModified;
    public string $mime;
    public string $key;

    public function setBasename(string $filename): void
    {
        $this->filename = pathinfo($filename, PATHINFO_FILENAME);
        $this->basename = $filename;
        $this->extension = pathinfo($filename, PATHINFO_EXTENSION);
    }
}

