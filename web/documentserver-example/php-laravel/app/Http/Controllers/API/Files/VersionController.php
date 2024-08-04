<?php

namespace App\Http\Controllers\API\Files;

use App\Http\Controllers\Controller;
use App\UseCases\Document\Update\ChangeDocumentVersionCommand;
use App\UseCases\Document\Update\ChangeDocumentVersionRequest;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class VersionController extends Controller
{
    public function restore(Request $request)
    {
        $request->validate([
            'filename' => 'required|string',
            'version' => 'required|int',
            'fileType' => 'required|string',
            'userId' => 'required|string',
        ]);

        app(ChangeDocumentVersionCommand::class)
            ->__invoke(new ChangeDocumentVersionRequest(
                filename: $request->filename,
                userDirectory: $request->ip(),
                fileType: $request->fileType,
                version: $request->version,
                userId: $request->userId,
            ));

        return response(status: 200);
    }
}
