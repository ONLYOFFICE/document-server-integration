<?php

namespace App\Http\Controllers\API\Files;

use App\Helpers\Path\PathInfo;
use App\Helpers\URL\URL;
use App\Http\Controllers\Controller;
use App\OnlyOffice\Managers\JWTManager;
use App\OnlyOffice\Managers\SettingsManager;
use App\UseCases\Document\Find\FindDocumentQuery;
use App\UseCases\Document\Find\FindDocumentQueryHandler;
use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;

class ReferenceController extends Controller
{
    public function get(Request $request, SettingsManager $settings, JWTManager $jwt)
    {
        $storagePrivateUrl = $settings->getSetting('url.storage.private');
        $storagePublicUrl = $settings->getSetting('url.storage.public');
        $referenceData = $request->input('referenceData');
        $link = $request->input('link');
        $path = $request->input('path');
        $filename = null;

        if ($referenceData && $referenceData['instanceId'] === $storagePrivateUrl) {
            $fileKey = json_decode(str_replace("'", '"', $referenceData['fileKey']));
            $userAddress = $fileKey->userAddress;
            if ($userAddress === $request->ip()) {
                $filename = $fileKey->fileName;
            }
        }

        if (! $filename && $link) {
            if (strpos($link, $storagePublicUrl) === false) {
                return response()->json([
                    'url' => $link,
                    'directUrl' => $link,
                ]);
            }

            $urlComponents = parse_url($link);
            parse_str($urlComponents['query'], $urlParams);
            $filename = $urlParams['fileID'];
        } elseif (! $filename && $path) {
            $filename = PathInfo::basename($path);
        }

        try {
            $file = app(FindDocumentQueryHandler::class)
                ->__invoke(new FindDocumentQuery(
                    filename: $filename,
                    userDirectory: $request->ip(),
                ));
        } catch (Exception $e) {
            Log::error($e->getMessage());

            return response()
                ->json(['error' => $e->getMessage()]);
        }

        $directDownloadUrl = route(
            'files.download',
            ['fileName' => urlencode($file['filename']), 'userAddress' => $request->ip()],
        );
        $directDownloadUrl = Str::replace(
            URL::origin($directDownloadUrl),
            $storagePublicUrl,
            $directDownloadUrl,
        );

        $downloadUrl = Str::replace(
            URL::origin($directDownloadUrl),
            $storagePrivateUrl,
            $directDownloadUrl,
        );

        $data = [
            'fileType' => $file['format']->extension(),
            'key' => $file['key'],
            'url' => $downloadUrl,
            'directUrl' => $request->input('directUrl') ? $directDownloadUrl : null,
            'referenceData' => [
                'fileKey' => json_encode([
                    'fileName' => $filename,
                    'userAddress' => $request->ip(),
                ]),
                'instanceId' => $storagePublicUrl,
            ],
            'path' => $filename,
            'link' => "$storagePublicUrl/editor?fileID=$filename",
        ];

        if ($settings->getSetting('jwt.enabled')) {
            $data['token'] = $jwt->encode($data, $settings->getSetting('jwt.secret'));
        }

        return response()->json($data);
    }
}
