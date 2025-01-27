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

namespace App\Repositories;

use App\Helpers\Path\Path;
use App\Helpers\Path\PathInfo;
use App\Models\Version;
use App\Models\VersionInfo;
use Illuminate\Support\Facades\Storage;

class VersionRepository
{
    private const VERSIONS_DIRECTORY_SUFFIX = 'history';

    private string $infoFilename = 'info.json';

    private string $changesFilename = 'changes.zip';

    private string $historyFilename = 'history.json';

    public function save(Version $version): void
    {
        $versionDirectory = $this->getVersionDirectory($version->filename, $version->version());

        $infoFilename = Path::join($versionDirectory, $this->infoFilename);
        Storage::disk('files')->put($infoFilename, json_encode($version->info->toArray(), JSON_PRETTY_PRINT));

        if ($version->history) {
            $historyFilename = Path::join($versionDirectory, $this->historyFilename);
            Storage::disk('files')->put($historyFilename, json_encode($version->history, JSON_PRETTY_PRINT));
        }

        if ($version->changes) {
            $changesFilename = Path::join($versionDirectory, $this->changesFilename);
            Storage::disk('files')->put($changesFilename, $version->changes);
        }

        $versionFilename = Path::join($versionDirectory, $version->version().'.'.PathInfo::extension($version->filename));
        Storage::disk('files')->copy($version->filename, $versionFilename);
    }

    public function changes(string $filename, int $version): array
    {
        $path = Path::join($this->getVersionDirectory($filename, $version), $this->changesFilename);

        $changes = [
            'filename' => $this->changesFilename,
            'content' => Storage::disk('files')->get($path),
            'mime' => Storage::disk('files')->mimeType($path),
            'size' => Storage::disk('files')->size($path),
        ];

        return $changes;
    }

    public function file(string $filename, int $version): array
    {
        $extension = PathInfo::extension($filename);
        $path = Path::join($this->getVersionDirectory($filename, $version), "$version.$extension");
        $absPath = Storage::disk('files')->path($path);

        $file = [
            'path' => $absPath,
            'filename' => "$version.$extension",
            'content' => Storage::disk('files')->get($path),
            'mime' => mime_content_type($absPath),
            'size' => Storage::disk('files')->size($path),
        ];

        return $file;
    }

    public function find(string $filename, int $version): Version
    {
        $versionDirectory = $this->getVersionDirectory($filename, $version);
        $infoPath = Path::join($versionDirectory, $this->infoFilename);
        $historyPath = Path::join($versionDirectory, $this->historyFilename);
        $changesPath = Path::join($versionDirectory, $this->changesFilename);

        $infoArray = Storage::disk('files')->json($infoPath);
        $info = VersionInfo::create(
            $infoArray['key'],
            $infoArray['fileType'],
            $version,
            $infoArray['created'],
            $infoArray['user'],
            $infoArray['serverVersion'],
        );

        $history = Storage::disk('files')->exists($historyPath)
            ? Storage::disk('files')->json($historyPath)
            : null;

        $changes = Storage::disk('files')->exists($changesPath)
            ? Storage::disk('files')->get($changesPath)
            : null;

        return Version::create($filename, $info, $history, $changes);
    }

    public function all(string $filename): array
    {
        $current = $this->current($filename);
        $versions = [];

        for ($version = 1; $version <= $current; $version++) {
            $versions[] = $this->find($filename, $version);
        }

        return $versions;
    }

    public function current(string $filename): int
    {
        $directories = Storage::disk('files')->directories($this->getVersionsRootDirectory($filename));

        return count($directories);
    }

    public function deleteAll(string $filename): void
    {
        Storage::disk('files')->deleteDirectory($this->getVersionsRootDirectory($filename));
    }

    private function getVersionsRootDirectory(string $filename): string
    {
        return "$filename-".self::VERSIONS_DIRECTORY_SUFFIX;
    }

    private function getVersionDirectory(string $filename, int $version): string
    {
        return Path::join($this->getVersionsRootDirectory($filename), $version);
    }
}
