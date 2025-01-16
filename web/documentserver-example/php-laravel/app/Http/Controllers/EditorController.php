<?php

/**
 * (c) Copyright Ascensio System SIA 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace App\Http\Controllers;

use App\Helpers\Path\PathInfo;
use App\Helpers\URL\URL;
use App\OnlyOffice\Managers\DocumentManager;
use App\OnlyOffice\Managers\JWTManager;
use App\OnlyOffice\Managers\SettingsManager;
use App\OnlyOffice\Services\CallbackService;
use App\UseCases\Common\Http\DownloadFileCommand;
use App\UseCases\Common\Http\DownloadFileRequest;
use App\UseCases\Document\Create\CreateDocumentCommand;
use App\UseCases\Document\Create\CreateDocumentFromTemplateCommand;
use App\UseCases\Document\Create\CreateDocumentFromTemplateRequest;
use App\UseCases\Document\Create\CreateDocumentRequest;
use App\UseCases\Document\Find\FindDocumentQuery;
use App\UseCases\Document\Find\FindDocumentQueryHandler;
use App\UseCases\Editor\Create\CreateConfigCommand;
use App\UseCases\Editor\Create\CreateConfigRequest;
use App\UseCases\File\Find\FileExistsQuery;
use App\UseCases\File\Find\FileExistsQueryHandler;
use App\UseCases\User\Find\FindAllUsersQuery;
use App\UseCases\User\Find\FindAllUsersQueryHandler;
use App\UseCases\User\Find\FindUserQuery;
use App\UseCases\User\Find\FindUserQueryHandler;
use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Onlyoffice\DocsIntegrationSdk\Models\Callback;
use Onlyoffice\DocsIntegrationSdk\Models\CallbackDocStatus;
use Onlyoffice\DocsIntegrationSdk\Models\CallbackForceSaveType;
use Onlyoffice\DocsIntegrationSdk\Models\History;

class EditorController extends Controller
{
    public function __construct(private SettingsManager $settings) {}

    public function index(Request $request, JWTManager $jwt)
    {
        $request->validate([
            'fileUrl' => 'required_without_all:fileID,fileExt|string',
            'fileID' => 'required_without_all:fileUrl,fileExt|string',
            'directUrl' => 'nullable|string',
            'user' => 'nullable|string',
            'fileExt' => 'required_without_all:fileUrl,fileID|string',
            'action' => 'nullable|string',
            'type' => 'nullable|string',
            'actionLink' => 'nullable|string',
        ]);

        $externalUrl = $request->input('fileUrl');
        $fileId = $request->input('fileID', '');
        $directUrlEnabled = $request->has('directUrl') && $request->directUrl === 'true';
        $userId = $request->user ?? 'uid-1';
        $type = $request->type ?? 'desktop';
        $actionLink = $request->input('actionLink', '');
        $lang = $request->cookie('ulang', 'en');
        $fileExt = $request->input('fileExt');
        $withSample = $request->has('sample') && $request->sample === 'true';
        $storagePublicUrl = $this->settings->getSetting('url.storage.public');
        $storagePrivateUrl = $this->settings->getSetting('url.storage.private');

        $user = app(FindUserQueryHandler::class)
            ->__invoke(new FindUserQuery($userId));

        if ($externalUrl) {
            try {
                $downloadedFile = app(DownloadFileCommand::class)
                    ->__invoke(new DownloadFileRequest(url: $externalUrl));

                $filename = PathInfo::basename($externalUrl);

                app(CreateDocumentCommand::class)
                    ->__invoke(
                        new CreateDocumentRequest(
                            filename: $filename,
                            userDirectory: $request->ip(),
                            fileType: PathInfo::extension($externalUrl),
                            fileSize: $downloadedFile['size'],
                            fileContent: $downloadedFile['content'],
                            user: $userId,
                        )
                    );
            } catch (Exception $e) {
                abort(500, $e->getMessage());
            }
        } elseif ($fileId) {
            $filename = PathInfo::basename($fileId);

            $fileExists = app(FileExistsQueryHandler::class)
                ->__invoke(new FileExistsQuery($filename, $request->ip()));

            if (! $fileExists) {
                abort(404, "The file $filename does not exist");
            }
        } elseif ($fileExt) {
            $file = app(CreateDocumentFromTemplateCommand::class)
                ->__invoke(new CreateDocumentFromTemplateRequest(
                    fileExtension: $fileExt,
                    userDirectory: $request->ip(),
                    user: $user['id'],
                    withSample: $withSample,
                ));

            return redirect()
                ->route('editor.index', [
                    'action' => 'edit',
                    'fileID' => $file['filename'],
                    'user' => $user['id'],
                ]);
        }

        $file = app(FindDocumentQueryHandler::class)
            ->__invoke(new FindDocumentQuery($filename, $request->ip()));

        if (! $file['format']->getType()) {
            $message = 'The format '.$file['format']->extension().' has undefined format.';
            Log::error($message);

            return view('error', [
                'code' => 500,
                'message' => $message,
            ]);
        }

        $file['user'] = $userId;
        $file['address'] = $request->ip();
        $fileId = $file['key'];

        $documentManager = new DocumentManager($file);

        $downloadUrl = $documentManager->getFileUrl($fileId);
        $imagesUrl = "$storagePublicUrl/images/";

        $mode = $request->action ?? 'edit';

        $config = app(CreateConfigCommand::class)
            ->__invoke(new CreateConfigRequest(
                filename: $filename,
                fileExtension: $file['format']->extension(),
                fileKey: $file['key'],
                fileUrl: $downloadUrl,
                user: $userId,
                mode: $mode,
                type: $type,
                lang: $lang,
                userAddress: $request->ip(),
                serverAddress: $storagePublicUrl,
                createUrl: $documentManager->getCreateUrl($fileId),
                templatesImageUrl: $documentManager->getTemplateImageUrl($fileId),
                actionLink: $actionLink,
                callbackUrl: $documentManager->getCallbackUrl($fileId),
                imagesUrl: $imagesUrl,
                directUrl: $directUrlEnabled ? $downloadUrl : '',
            ));

        // an image for inserting
        $dataInsertImage = [
            'fileType' => 'svg',
            'url' => "$storagePrivateUrl/images/logo.svg",
        ];

        // a document for comparing
        $dataDocument = [
            'fileType' => 'docx',
            'url' => "$storagePrivateUrl/assets/document-templates/sample/sample.docx",
        ];

        // recipients data for mail merging
        $dataSpreadsheet = [
            'fileType' => 'csv',
            'url' => "$storagePrivateUrl/assets/document-templates/sample/csv.csv",
        ];

        if ($directUrlEnabled) {
            $dataInsertImage['directUrl'] = URL::build($storagePrivateUrl, '/images/logo.svg');
            $dataDocument['directUrl'] = URL::build($storagePrivateUrl, '/document-templates/sample/sample.docx');
            $dataSpreadsheet['directUrl'] = URL::build($storagePrivateUrl, '/document-templates/sample/csv.csv');
        }

        $usersForMentions = $user['id'] !== 'uid-0'
            ? app(FindAllUsersQueryHandler::class)
                ->__invoke(new FindAllUsersQuery(id: $user['id'], forMentions: true))
            : null;
        $usersForProtect = $user['id'] !== 'uid-0'
            ? app(FindAllUsersQueryHandler::class)
                ->__invoke(new FindAllUsersQuery(id: $user['id'], forProtect: true))
            : null;

        $usersInfo = [];

        $users = app(FindAllUsersQueryHandler::class)
            ->__invoke(new FindAllUsersQuery);

        if ($user['id'] != 'uid-0') {
            foreach ($users as $userInfo) {
                $u = $userInfo;
                $u['image'] = $userInfo['avatar'] ? "$storagePublicUrl/images/".$userInfo['id'].'.png' : null;
                array_push($usersInfo, $u);
            }
        }

        // check if the secret key to generate token exists
        if ($this->settings->getSetting('jwt.enabled')) {
            // encode config into the token
            $config['token'] = $jwt->encode($config, $this->settings->getSetting('jwt.secret'));
            // encode the dataInsertImage object into the token
            $dataInsertImage['token'] = $jwt->encode($dataInsertImage, $this->settings->getSetting('jwt.secret'));
            // encode the dataDocument object into the token
            $dataDocument['token'] = $jwt->encode($dataDocument, $this->settings->getSetting('jwt.secret'));
            // encode the dataSpreadsheet object into the token
            $dataSpreadsheet['token'] = $jwt->encode($dataSpreadsheet, $this->settings->getSetting('jwt.secret'));
        }

        $historyLayout = '';

        if ($user['id'] == 'uid-3') {
            $historyLayout .= "config.events['onRequestHistoryClose'] = null;
                config.events['onRequestRestore'] = null;";
        }

        if ($user['id'] != 'uid-0') {
            $historyLayout .= "// add mentions for not anonymous users
                config.events['onRequestRefreshFile'] = onRequestRefreshFile;
                config.events['onRequestClose'] = onRequestClose;
                config.events['onRequestUsers'] = onRequestUsers;
                config.events['onRequestSaveAs'] = onRequestSaveAs;
                // the user is mentioned in a comment
                config.events['onRequestSendNotify'] = onRequestSendNotify;
                // prevent file renaming for anonymous users
                config.events['onRequestRename'] = onRequestRename;
                // prevent switch the document from the viewing into the editing mode for anonymous users
                config.events['onRequestEditRights'] = onRequestEditRights;
                config.events['onRequestHistory'] = onRequestHistory;
                config.events['onRequestHistoryData'] = onRequestHistoryData;
                config.events['onRequestReferenceSource'] = onRequestReferenceSource;";
            if ($user['id'] != 'uid-3') {
                $historyLayout .= "config.events['onRequestHistoryClose'] = onRequestHistoryClose;
                config.events['onRequestRestore'] = onRequestRestore;";
            }
        }

        $editorConfig = [
            'fileName' => $file['filename'],
            'docType' => $file['format']->getType(),
            'apiUrl' => $this->settings->getSetting('url.api'),
            'dataInsertImage' => mb_strimwidth(
                json_encode($dataInsertImage),
                1,
                mb_strlen(json_encode($dataInsertImage)) - 2
            ),
            'dataDocument' => json_encode($dataDocument),
            'dataSpreadsheet' => json_encode($dataSpreadsheet),
            'config' => json_encode($config),
            'history' => $historyLayout,
            'usersForMentions' => json_encode($usersForMentions),
            'usersInfo' => json_encode($usersInfo),
            'usersForProtect' => json_encode($usersForProtect),
        ];

        return view('editor', $editorConfig);
    }

    public function track(Request $request)
    {
        $request->validate([
            'fileName' => 'required|string',
            'userAddress' => 'required|string',
        ]);

        $filename = $request->input('fileName');
        $address = $request->input('userAddress');

        $fileExists = app(FileExistsQueryHandler::class)
            ->__invoke(new FileExistsQuery($filename, $address));

        if (! $fileExists) {
            Log::error("$filename does not exist.");

            return response()->json([
                'error' => 1,
            ]);
        }

        $data = $request->input('payload');
        $data['filename'] = $filename;
        $data['address'] = $address;
        $actions = array_key_exists('actions', $data) ? $data['actions'] : [];
        $changesUrl = array_key_exists('changesurl', $data) ? $data['changesurl'] : '';
        $formsDataUrl = array_key_exists('formsdataurl', $data) ? $data['formsdataurl'] : '';
        $fileType = array_key_exists('filetype', $data) ? $data['filetype'] : '';
        $forceSaveType = array_key_exists('forcesavetype', $data) ? new CallbackForceSaveType($data['forcesavetype']) : null;
        $history = array_key_exists('history', $data) && $data['history']
            ? new History($data['history']['serverVersion'], $data['history']['changes'])
            : null;
        $users = array_key_exists('users', $data) ? $data['users'] : [];
        $url = array_key_exists('url', $data) ? $data['url'] : '';

        $callback = new Callback(
            $actions,
            $changesUrl,
            $fileType,
            $forceSaveType,
            $history,
            $data['key'],
            new CallbackDocStatus($data['status']),
            $url,
            $users,
            '',
            $formsDataUrl
        );

        $callbackService = new CallbackService($this->settings, app(JWTManager::class), $data);
        $result = $callbackService->processCallback($callback, $filename);

        return response()->json($result);
    }
}
