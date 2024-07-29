<?php

namespace App\UseCases\Document\Create;

use App\Helpers\Path\Path;
use App\Helpers\Path\PathInfo;
use App\Helpers\Path\TemplatePath;
use App\Helpers\UniqueFilename;
use App\Models\Version;
use App\Models\VersionInfo;
use App\Repositories\VersionRepository;
use Illuminate\Support\Str;

class CreateDocumentFromTemplateCommand
{
    public function __construct(
        private VersionRepository $versionRepository,
    ) {}

    public function __invoke(CreateDocumentFromTemplateRequest $request): array
    {
        $from = TemplatePath::for($request->fileExtension, $request->withSample);

        $filePath = Path::join($request->userDirectory, PathInfo::basename($from));
        $filePath = UniqueFilename::for($filePath);

        $to = storage_path(Path::join('app/public/files', $filePath));

        copy($from, $to);

        $versionInfo = VersionInfo::create(
            key: Str::uuid(),
            fileType: $request->fileExtension,
            version: 1,
            created: now(),
            userId: $request->user
        );

        $version = Version::create($filePath, $versionInfo);
        $this->versionRepository->save($version);

        return [
            'filename' => PathInfo::basename($filePath),
        ];
    }
}
