<?php

namespace OnlyOffice\Storage;

interface Storage
{
    public function size(string $path): int;
    public function lastModified(string $path): int;
    public function mimeType(string $path): string;
    public function put(string $path, mixed $content): string;
    public function move(string $from, string $to): void;
    public function copy(string $from, string $to): string;
    public function exists(string $path): bool;
    public function fileExists(string $path): bool;
    public function directoryExists(string $path): bool;
    public function get(string $path): mixed;
    public function files(string $path): array;
    public function directories(string $path): array;
    public function delete(string $path): void;
    public function deleteDirectory(string $path): void;
}