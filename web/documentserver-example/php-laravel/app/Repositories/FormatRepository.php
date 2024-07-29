<?php

namespace App\Repositories;

use App\Enums\FormatType;
use App\Exceptions\FileNotFound;
use App\Models\Format;
use Exception;

class FormatRepository
{
    private array $formats;

    public function __construct(private string $path)
    {
        if (! is_file($path)) {
            throw new FileNotFound($path);
        }

        $data = file_get_contents($path);

        if (! $data) {
            throw new Exception(sprintf('Could not read <%s>', $this->path));
        }

        $formats = json_decode($data, associative: true, flags: JSON_THROW_ON_ERROR);

        foreach ($formats as $format) {
            $this->formats[] = new Format(
                $format['name'],
                $format['type'] ? FormatType::from($format['type']) : null,
                $format['actions'],
                $format['convert'],
                $format['mime']
            );
        }
    }

    public function find(string $extension): ?Format
    {
        foreach ($this->formats as $format) {
            if ($format->name === $extension) {
                return $format;
            }
        }

        return null;
    }

    public function all(): array
    {
        return $this->formats;
    }
}
