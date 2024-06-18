<?php

namespace App\Http\Controllers;

use OnlyOffice\Storage;
use OnlyOffice\Entities\File;
use Exception;
use Illuminate\Http\Request;

class FileController extends Controller
{
    public function __construct(
        private Storage $storage,
    ) {}

    public function destroy(Request $request)
    {
        $request->validate([
            'filename' => 'nullable|string'
        ]);

        $file = new File();
        $file->basename = $request->input('filename', '');

        try {
            $this->storage->delete($file);
        } catch (Exception $e) {
            return response()
                ->json([
                    'error' => $e->getMessage(),
                ], 500);
        }

        return response(status: 201);
    }
}
