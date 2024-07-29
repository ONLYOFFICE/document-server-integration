<?php

namespace App\Http\Controllers;

use App\Helpers\Path\Path;
use App\Repositories\VersionRepository;
use Illuminate\Http\Request;

class VersionController extends Controller
{
    public function __construct(private VersionRepository $versionRepository) {}

    public function changes(Request $request)
    {
        $filename = Path::join($request->userAddress, $request->filename);

        $changes = $this->versionRepository->changes($filename, $request->version);

        return response()->streamDownload(function () use ($changes) {
            echo $changes['content'];
        }, $changes['filename'], [
            'Content-Length' => $changes['size'],
            'Content-Type' => $changes['mime'],
            'Content-Disposition' => 'attachment; filename*=UTF-8\'\''.str_replace('+', '%20', urlencode($changes['filename'])),
            'Access-Control-Allow-Origin' => '*',
        ]);
    }

    public function previous(Request $request)
    {
        $filename = Path::join($request->userAddress, $request->filename);

        $file = $this->versionRepository->file($filename, $request->version - 1);

        return response()->streamDownload(function () use ($file) {
            echo $file['content'];
        }, $file['filename'], [
            'Content-Length' => $file['size'],
            'Content-Type' => $file['mime'],
            'Content-Disposition' => 'attachment; filename*=UTF-8\'\''.str_replace('+', '%20', urlencode($file['filename'])),
            'Access-Control-Allow-Origin' => '*',
        ]);
    }
}
