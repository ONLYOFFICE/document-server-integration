<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use OnlyOffice\DocumentServer;
use OnlyOffice\Entities\File;
use OnlyOffice\Formats;
use OnlyOffice\Helpers\Path;

class ForgottenController extends Controller
{
    public function __construct(private DocumentServer $documentServer, private Formats $formats)
    {

    }

    public function index()
    {
        $filesArray = $this->documentServer->getForgottenFiles();
        $files = [];
        
        foreach ($filesArray as $fileItem) {
            $file = new File();
            $file->key = $fileItem['key'];
            $file->basename = $fileItem['url'];
            $file->extension = Path::extension($file->basename);
            $file->format = $this->formats->find($file->extension);

            $files[] = $file;
        }

        return view('forgotten', compact('files'));
    }

    public function destroy(string $key)
    {
        $this->documentServer->deleteForgotten($key);

        return response(status: 204);
    }
}
