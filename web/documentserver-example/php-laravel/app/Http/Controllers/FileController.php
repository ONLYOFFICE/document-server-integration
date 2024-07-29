<?php

namespace App\Http\Controllers;

use App\Helpers\Path\Path;
use App\Helpers\Path\PathInfo;
use App\Helpers\URL\URL;
use App\Services\ServerConfig;
use App\Services\StorageConfig;
use App\UseCases\Common\Http\DownloadFileCommand;
use App\UseCases\Common\Http\DownloadFileRequest;
use App\UseCases\Docs\Conversion\ConvertCommand;
use App\UseCases\Docs\Conversion\ConvertRequest;
use App\UseCases\Document\Create\CreateDocumentCommand;
use App\UseCases\Document\Create\CreateDocumentRequest;
use App\UseCases\Document\Delete\DeleteAllDocumentsCommand;
use App\UseCases\Document\Delete\DeleteAllDocumentsRequest;
use App\UseCases\Document\Delete\DeleteDocumentCommand;
use App\UseCases\Document\Delete\DeleteDocumentRequest;
use App\UseCases\Document\Find\FindDocumentHistoryQuery;
use App\UseCases\Document\Find\FindDocumentHistoryQueryHandler;
use App\UseCases\Document\Find\FindDocumentQuery;
use App\UseCases\Document\Find\FindDocumentQueryHandler;
use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;

class FileController extends Controller
{
    public function __construct(
        private ServerConfig $serverConfig,
        private StorageConfig $storageConfig,
    ) {
    }

    public function upload(Request $request)
    {
        $request->validate([
            'file' => 'required|file',
            'user' => 'nullable|string',
        ]);

        $uploadedFile = $request->file('file');
        $fileType = $uploadedFile->getClientOriginalExtension();

        try {
            $file = app(CreateDocumentCommand::class)->__invoke(
                new CreateDocumentRequest(
                    $uploadedFile->getClientOriginalName(),
                    $request->ip(),
                    $fileType,
                    $uploadedFile->getSize(),
                    $uploadedFile->getContent(),
                    $request->user,
                )
            );
        } catch (Exception $e) {
            Log::error($e->getMessage());

            return response()
                ->json([
                    'error' => $e->getMessage(),
                ], 500);
        }

        return response()->json([
            'filename' => $file['filename'],
            'documentType' => $fileType,
        ]);
    }

    public function saveAs(Request $request)
    {
        $request->validate([
            'url' => 'required|string',
            'title' => 'required|string',
            'user' => 'nullable|string',
        ]);

        $user = $request->input('user', '');

        $url = Str::replace(URL::origin($request->url), $this->serverConfig->get('url.private'), $request->url);

        $downloadedFile = app(DownloadFileCommand::class)
            ->__invoke(new DownloadFileRequest(url: $url));

        $file = app(CreateDocumentCommand::class)->__invoke(
            new CreateDocumentRequest(
                $request->title,
                $request->ip(),
                PathInfo::extension($request->url),
                $downloadedFile['size'],
                $downloadedFile['content'],
                $user,
            )
        );

        return response()
            ->json(['filename' => $file['filename']]);
    }

    public function convert(Request $request)
    {
        $request->validate([
            'filename' => 'required|string',
            'fileUri' => 'nullable|string',
            'password' => 'nullable|string',
            'fileExt' => 'nullable|string',
        ]);

        try {
            $result = app(ConvertCommand::class)
                ->__invoke(new ConvertRequest(
                    filename: $request->filename,
                    fileType: PathInfo::extension($request->filename),
                    outputType: $request->input('fileExt', 'ooxml'),
                    url: $request->fileUri,
                    password: $request->password,
                    user: $request->user,
                    userAddress: $request->ip(),
                    lang: cache('lang', default: 'en'),
                ));

            if (array_key_exists('step', $result)) {
                return response()
                    ->json([
                        'step' => $result['step'],
                        'filename' => $result['filename'],
                        'fileUri' => $result['url'],
                    ], 500);
            } else if (array_key_exists('error', $result)) {
                return response()
                    ->json([
                        'error' => $result['error'],
                        'code' => $result['code'],
                    ], 500);
            }

            $convertedFileContent = app(DownloadFileCommand::class)
                ->__invoke(new DownloadFileRequest($result['fileUrl']));

            $file = app(CreateDocumentCommand::class)->__invoke(
                new CreateDocumentRequest(
                    filename: $result['filename'],
                    userDirectory: $request->ip(),
                    fileType: $result['fileType'],
                    fileSize: $convertedFileContent['size'],
                    fileContent: $convertedFileContent['content'],
                    user: $request->user,
                )
            );

            app(DeleteDocumentCommand::class)->__invoke(
                new DeleteDocumentRequest(
                    filename: $request->filename,
                    userDirectory: $request->ip(),
                )
            );
        } catch (Exception $e) {
            abort(500, $e->getMessage());
        }

        return response()->json([
            'filename' => $file['filename'],
        ]);
    }

    public function download(Request $request)
    {
        $request->validate([
            'fileName' => 'required|string',
            'userAddress' => 'nullable|string',
        ]);
        $filename = urldecode($request->fileName);
        $ip = $request->input('userAddress', $request->ip());

        $file = app(FindDocumentQueryHandler::class)
            ->__invoke(new FindDocumentQuery($filename, $ip));

        return response()->streamDownload(function () use ($file) {
            echo $file['content'];
        }, $filename, [
            'Content-Length' => $file['size'],
            'Content-Type' => $file['mimeType'],
            'Content-Disposition' => 'attachment; filename*=UTF-8\'\'' . str_replace('+', '%20', urlencode($filename)),
            'Access-Control-Allow-Origin' => '*',
        ]);
    }

    public function history(Request $request)
    {
        $filename = $request->filename;
        $filename = Path::join($request->ip(), $filename);
        $address = $request->ip();

        $history = app(FindDocumentHistoryQueryHandler::class)
            ->__invoke(new FindDocumentHistoryQuery($filename, $address));

        return response()->json($history);
    }

    public function destroy(Request $request)
    {
        $request->validate([
            'filename' => 'nullable|string',
        ]);

        try {
            if ($request->filename) {
                app(DeleteDocumentCommand::class)->__invoke(
                    new DeleteDocumentRequest(
                        filename: $request->filename,
                        userDirectory: $request->ip(),
                    )
                );
            } else {
                app(DeleteAllDocumentsCommand::class)->__invoke(
                    new DeleteAllDocumentsRequest(userDirectory: $request->ip())
                );
            }
        } catch (Exception $e) {
            return response()
                ->json([
                    'error' => $e->getMessage(),
                ], 500);
        }

        return response(status: 201);
    }
}
