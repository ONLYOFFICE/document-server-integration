<?php

namespace App\Http\Controllers;

use OnlyOffice\Helpers\Path;
use OnlyOffice\Storage;
use OnlyOffice\Document;
use OnlyOffice\Entities\File;
use Exception;
use Illuminate\Http\Request;
use OnlyOffice\Config;
use OnlyOffice\Exceptions\Conversion\ConversionNotComplete;
use OnlyOffice\Exceptions\Conversion\ConversionError;
use OnlyOffice\Formats;
use OnlyOffice\Helpers\URL\URL;
use OnlyOffice\JWT;
use OnlyOffice\Users;

class FileController extends Controller
{
    public function __construct(
        private Config $config,
        private Storage $storage,
        private Document $document,
        private Formats $formats,
        private Users $users
    ) {
    }

    public function upload(Request $request)
    {
        $request->validate([
            'file' => 'required|file',
            'user' => 'nullable|string'
        ]);

        $uploadedFile = $request->file('file');

        try {
            $file = new File();
            $file->basename = $uploadedFile->getClientOriginalName();
            $file->size = $uploadedFile->getSize();
            $file->content = $uploadedFile->getContent();
            $file->format = $this->formats->find($uploadedFile->getClientOriginalExtension());
            $file->author = $this->users->find($request->user);
            $file->path = Path::join($request->ip(), $file->basename);

            $this->storage->save($file);
        } catch (Exception $e) {
            return response()
                ->json([
                    'error' => $e->getMessage(),
                ], 500);
        }

        return response()->json([
            'filename' => $file->basename,
            'documentType' => $file->format->type
        ]);
    }

    public function convert(Request $request)
    {
        $request->validate([
            'filename' => 'required|string',
            'fileUri' => 'nullable|string',
            'password' => 'nullable|string',
            'fileExt' => 'nullable|string',
        ]);

        $file = $this->storage->find(Path::join($request->ip(), $request->filename));

        $url = $request->fileUri;

        if (!$url) {
            $url = URL::build($this->config->get('url.storage.private'), 'files.download', [
                'fileName' => urlencode($request->filename),
                'userAddress' => request()->ip()
            ]);
        }

        $data = [
            'filename' => $request->filename,
            'filetype' => Path::extension($request->filename),
            'outputtype' => $request->input('fileExt', 'ooxml'),
            'password' => $request->password,
            'url' => $url,
            'key' => $file->key,
            'user' => $request->user,
            'lang' => cache('lang', default: 'en'),
        ];
        try {
            $file = $this->document->convert($data);
            $file->path = $request->ip() . '/' . $file->basename;
            $this->storage->save($file);
        } catch (ConversionNotComplete $e) {
            return response()
                ->json([
                    'step' => $e->step,
                    'filename' => $e->filename,
                    'fileUri' => $e->url,
                ], 500);
        } catch (ConversionError $e) {
            return response()
                ->json([
                    'error' => $e->getMessage(),
                    'code' => $e->getCode(),
                ], 500);
        }

        return response()->json([
            'filename' => $file->basename
        ]);
    }

    public function download(Request $request, JWT $jwt)
    {
        $request->validate([
            'fileName' => 'required|string',
            'userAddress' => 'nullable|string',
        ]);
        $filename = urldecode($request->fileName);
        $ip = $request->input('userAddress', $request->ip());
        $isEmbedded = $request->input('dmode');

        if ($this->config->get('jwt.enabled') && !$isEmbedded && $request->input('userAddress')) {
            if ($request->hasHeader($this->config->get('jwt.header'))) {
                $token = $jwt->decode($request->bearerToken());
                if (empty($token)) {
                    return abort(500, 'Invalid JWT signature');
                }
            }
        }

        // todo get force save file path

        $path = Path::join($ip, $filename);
        $file = $this->storage->find($path, true);

        return response()->streamDownload(function() use ($file) {
            echo $file->content;
        }, $filename, [
            'Content-Length' => $file->size,
            'Content-Type' => $file->mime,
            'Content-Disposition' => 'attachment; filename*=UTF-8\'\'' . str_replace("+", "%20", urlencode($filename)),
            'Access-Control-Allow-Origin' => '*'
        ]);
    }

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
