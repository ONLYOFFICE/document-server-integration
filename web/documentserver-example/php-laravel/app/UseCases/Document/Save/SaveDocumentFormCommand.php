<?php

namespace App\UseCases\Document\Save;

use App\Helpers\Path\Path;
use App\Helpers\Path\PathInfo;
use App\Helpers\UniqueFilename;
use App\Models\File;
use App\Models\Version;
use App\Models\VersionInfo;
use App\Repositories\FileRepository;
use App\Repositories\VersionRepository;
use Illuminate\Support\Str;

class SaveDocumentFormCommand
{
    public function __construct(
        private FileRepository $fileRepository,
        private VersionRepository $versionRepository,
    ) {}

    public function __invoke(SaveDocumentFormRequest $request): void
    {
        $this->saveForm($request);
        $this->createFormDataFile($request);
    }

    private function saveForm(SaveDocumentFormRequest $request): void
    {
        $formFileExtension = PathInfo::extension($request->filename);
        $formFilename = PathInfo::filename($request->filename)."-form.$formFileExtension";

        $formPath = Path::join($request->userDirectory, $formFilename);
        $formPath = UniqueFilename::for($formPath);

        $formFile = File::create($formPath, $request->formContent);

        $this->fileRepository->save($formFile);

        $versionInfo = VersionInfo::create(
            Str::uuid(),
            $formFileExtension,
            1,
            now(),
            $request->user,
        );

        $version = Version::create($formPath, $versionInfo);

        $this->versionRepository->save($version);
    }

    private function createFormDataFile(SaveDocumentFormRequest $request): void
    {
        $formFileExtension = 'txt';
        $formDataFilename = PathInfo::filename($request->filename)."-form.$formFileExtension";

        $formDataPath = Path::join($request->userDirectory, $formDataFilename);
        $formDataPath = UniqueFilename::for($formDataPath);

        $formDataFile = File::create($formDataPath, $request->formDataContent);

        $this->fileRepository->save($formDataFile);

        $versionInfo = VersionInfo::create(
            Str::uuid(),
            $formFileExtension,
            1,
            now(),
            $request->user,
        );

        $version = Version::create($formDataPath, $versionInfo);

        $this->versionRepository->save($version);
    }
}
