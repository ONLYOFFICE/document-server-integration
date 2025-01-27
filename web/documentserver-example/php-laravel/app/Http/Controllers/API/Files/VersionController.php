<?php

namespace App\Http\Controllers\API\Files;

use App\Http\Controllers\Controller;
use App\UseCases\Document\Update\ChangeDocumentVersionCommand;
use App\UseCases\Document\Update\ChangeDocumentVersionRequest;
use Illuminate\Http\Request;

class VersionController extends Controller
{
    public function restore(Request $request)
    {
        $request->validate([
            'filename' => 'required|string',
            'version' => 'required|int',
            'url' => 'nullable|string',
            'fileType' => 'required|string',
            'userId' => 'required|string',
        ]);

        app(ChangeDocumentVersionCommand::class)
            ->__invoke(new ChangeDocumentVersionRequest(
                filename: $request->filename,
                userDirectory: $request->ip(),
                fileType: $request->fileType,
                version: $request->version,
                url: $request->url,
                userId: $request->userId,
            ));

        return response(status: 200);
    }
}
