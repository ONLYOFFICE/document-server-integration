<?php

namespace OnlyOffice\Repositories\Files;

use App\Helpers\DocEditorKey;
use App\Helpers\URL\URL;
use OnlyOffice\Entities\File;
use Carbon\Carbon;
use Exception;
use Illuminate\Support\Facades\Log;
use SplFileInfo;
use Illuminate\Support\Facades\Storage;
use App\Helpers\URL\Storage as StorageURL;
use OnlyOffice\Formats;
use Illuminate\Support\Str;
use OnlyOffice\Config;
use OnlyOffice\Helpers\Path;

class LocalFileRepository implements FileRepository
{
    public function __construct(private Config $config, private Formats $formats)
    {
    }

    public function get(string $path = ''): array
    {
        $files = [];

        if (!Storage::disk('files')->directoryExists($path)) {
            return $files;
        }

        $filesList = Storage::disk('files')->files($path);

        foreach ($filesList as $filePath) {
            $file = new File();
            $fileInfo = new SplFileInfo(Storage::disk('files')->path($filePath));
            $file->basename = $fileInfo->getFilename();
            $file->lastModified = $fileInfo->getMTime();
            $key = Str::of($this->config->get('client.ip'))->append($this->config->virtualPath() . rawurlencode($file->basename));
            $key = Str::of($key)->append($file->lastModified);
            $file->key = DocEditorKey::generate($key);
            $file->format = $this->formats->find($fileInfo->getExtension());
            $files[] = $file;
        }

        return $files;
    }

    public function find(string $path, bool $withContent = false): File
    {
        if (!Storage::disk('files')->fileExists($path)) {
            throw new Exception('The file does not exist');
        }

        $absPath = Storage::disk('files')->path($path);

        $file = new File();
        $fileInfo = new SplFileInfo($absPath);
        $file->basename = $fileInfo->getFilename();
        $file->lastModified = $fileInfo->getMTime();
        $file->size = $fileInfo->getSize();
        $file->mime = mime_content_type($absPath);
        $key = Str::of($this->config->get('client.ip'))->append($this->config->virtualPath() . rawurlencode($file->basename));
        $key = Str::of($key)->append($file->lastModified);
        $file->key = DocEditorKey::generate($key);
        $file->format = $this->formats->find($fileInfo->getExtension());

        if ($withContent) {
            $file->content = Storage::disk('files')->get($path);
        }

        return $file;
    }

    public function save(File $file): void
    {
        $tmpfile = tmpfile();
        fwrite($tmpfile, $file->content);

        $this->setProperFileName($file);
        $path = Storage::disk('files')->put($file->path, $tmpfile);

        fclose($tmpfile);

        if ($path === false) {
            throw new Exception('Could not save the file.');
        }

        $this->createMeta($file);
    }

    public function update(File $file): void
    {
        
    }

    public function delete(File $file): void
    {
        if (Storage::disk('files')->fileExists($file->path)) {
            Storage::disk('files')->delete($file->path);
            $this->deleteMeta($file);
        } else if (Storage::disk('files')->directoryExists($file->path)) {
            Storage::disk('files')->deleteDirectory($file->path);
        } else {
            throw new Exception('File not found.');
        }
    }

    public function copy(File $file): void
    {
        $this->setProperFileName($file);
        $to = Storage::disk('files')->path($this->getFilePath($file->basename));
        Storage::copy($file->path, $to);
        $this->createMeta($file);
    }

    private function createMeta(File $file): void
    {
        $filename = $file->path . '-hist/createdInfo.json';

        $meta = [
            "created" => Carbon::now(),
            "uid" => $file->author->id,
            "name" => $file->basename,
        ];

        Storage::disk('files')->put(
            $filename,
            json_encode($meta, JSON_PRETTY_PRINT),
        );
    }

    private function deleteMeta(File $file)
    {
        $path = $file->path . '-hist';

        if (Storage::disk('files')->directoryExists($path)) {
            Storage::disk('files')->deleteDirectory($path);
        }
    }

    private function setProperFileName(File $file): void
    {
        $filename = pathinfo($file->basename, PATHINFO_FILENAME);
        for ($i = 1; Storage::disk('files')->fileExists($file->path); $i++) {
            $file->basename = $filename . " (" . $i . ")." . $file->format->extension();
            $file->path = pathinfo($file->path, PATHINFO_DIRNAME) . '/' . $file->basename;
        }
    }

    private function getHistoryDirectory(File $file): string
    {
        return $file->path . '-history';
    }

    private function determineVersion(File $file): int
    {
        $version = 1;
        $historyDirectory = $this->getHistoryDirectory($file);

        if (!Storage::disk('files')->directoryExists($historyDirectory)) {
            return $version;
        }

        $directories = Storage::disk('files')->directories($historyDirectory);
        $version += count($directories); 

        return $version;
    }

    private function getVersionDirectory(File $file): string
    {
        return Path::join($this->getHistoryDirectory($file), $this->determineVersion($file));
    }
}
