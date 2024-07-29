<?php

namespace App\UseCases\Document\Save;

use App\Helpers\Path\Path;
use Illuminate\Support\Facades\Storage;

class ForceSaveDocumentCommand
{
    public function __invoke(ForceSaveDocumentRequest $request): void
    {
        $filePath = Path::join($request->userDirectory, $request->filename);

        Storage::disk('forcesaved')->put($filePath, $request->content);
    }
}
