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

use App\Helpers\Path\Path;
use App\Helpers\Path\PathInfo;
use App\Helpers\URL\FileURL;
use App\Helpers\URL\TemplateURL;
use App\Helpers\URL\URL;
use App\Services\JWT;
use App\Services\ServerConfig;
use App\Services\StorageConfig;
use App\UseCases\Common\Http\DownloadFileCommand;
use App\UseCases\Common\Http\DownloadFileRequest;
use App\UseCases\Docs\Command\ForceSaveCommad;
use App\UseCases\Docs\Command\ForceSaveRequest;
use App\UseCases\Docs\Conversion\ConvertCommand;
use App\UseCases\Docs\Conversion\ConvertRequest;
use App\UseCases\Document\Create\CreateDocumentCommand;
use App\UseCases\Document\Create\CreateDocumentFromTemplateCommand;
use App\UseCases\Document\Create\CreateDocumentFromTemplateRequest;
use App\UseCases\Document\Create\CreateDocumentRequest;
use App\UseCases\Document\Find\FindDocumentQuery;
use App\UseCases\Document\Find\FindDocumentQueryHandler;
use App\UseCases\Document\Save\ForceSaveDocumentCommand;
use App\UseCases\Document\Save\ForceSaveDocumentRequest;
use App\UseCases\Document\Save\SaveDocumentCommand;
use App\UseCases\Document\Save\SaveDocumentFormCommand;
use App\UseCases\Document\Save\SaveDocumentFormRequest;
use App\UseCases\Document\Save\SaveDocumentRequest;
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
use Illuminate\Support\Str;

class EditorController extends Controller
{
    public function __construct(
        private StorageConfig $storageConfig,
        private ServerConfig $serverConfig,
    ) {}

    public function index(Request $request, JWT $jwt)
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
        $storagePublicUrl = $this->storageConfig->get('url.public');
        $storagePrivateUrl = $this->storageConfig->get('url.private');

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

        if (! $file['format']->type) {
            $message = 'The format '.$file['format']->extension().' has undefined format.';
            Log::error($message);

            return view('error', [
                'code' => 500,
                'message' => $message,
            ]);
        }

        $downloadUrl = FileURL::download($filename, $request->ip());
        $templatesImageUrl = TemplateURL::image($file['format']->type->value);
        $createUrl = FileURL::create($file['format']->extension(), $user['id']);
        $callbackUrl = FileURL::callback($filename, $request->ip());
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
                createUrl: $createUrl,
                templatesImageUrl: $templatesImageUrl,
                actionLink: $actionLink,
                callbackUrl: $callbackUrl,
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
        if ($this->serverConfig->get('jwt.enabled')) {
            $config['token'] = $jwt->encode($config);  // encode config into the token
            // encode the dataInsertImage object into the token
            $dataInsertImage['token'] = $jwt->encode($dataInsertImage);
            // encode the dataDocument object into the token
            $dataDocument['token'] = $jwt->encode($dataDocument);
            // encode the dataSpreadsheet object into the token
            $dataSpreadsheet['token'] = $jwt->encode($dataSpreadsheet);
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
            'docType' => $file['format']->type,
            'apiUrl' => $this->serverConfig->get('url.api'),
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
        $status = $data['status'];

        switch ($status) {
            case 1:
                if ($data['actions'] && $data['actions'][0]['type'] == 0) {
                    $user = $data['actions'][0]['userid'];
                    if (array_search($user, $data['users']) === false) {
                        app(ForceSaveCommad::class)
                            ->__invoke(new ForceSaveRequest(key: $data['key']));
                    }
                }
                break;
            case 2:
            case 3:
                $url = $data['url'];
                $key = $data['key'];
                $user = $data['users'][0];
                $changes = null;

                $url = Str::replace(URL::origin($url), $this->serverConfig->get('url.private'), $url);

                $fileExtension = PathInfo::extension($filename);
                $downloadExtension = PathInfo::extension($url);

                if ($fileExtension !== $downloadExtension) {
                    $result = app(ConvertCommand::class)
                        ->__invoke(new ConvertRequest(
                            filename: $filename,
                            fileType: $downloadExtension,
                            outputType: $fileExtension,
                            url: $url,
                            password: null,
                            user: $user,
                            userAddress: $address,
                        ));

                    if (array_key_exists('step', $result) || array_key_exists('error', $result)) {
                        $filename = PathInfo::filename($filename).".$downloadExtension";
                    } else {
                        $url = $result['fileUrl'];
                    }
                }

                $content = app(DownloadFileCommand::class)
                    ->__invoke(new DownloadFileRequest(url: $url))['content'];

                if (array_key_exists('changesurl', $data)) {
                    $changesUrl = $data['changesurl'];
                    $changesUrl = Str::replace(URL::origin($changesUrl), $this->serverConfig->get('url.private'), $changesUrl);

                    $changes = app(DownloadFileCommand::class)
                        ->__invoke(new DownloadFileRequest(url: $changesUrl))['content'];
                }

                $history = array_key_exists('history', $data) ? $data['history']['changes'] : null;
                $serverVersion = array_key_exists('history', $data) ? $data['history']['serverVersion'] : null;
                $user = $data['users'][0];

                app(SaveDocumentCommand::class)->__invoke(
                    new SaveDocumentRequest(
                        Path::join($address, $filename),
                        $fileExtension,
                        $key,
                        $content,
                        $user,
                        $serverVersion,
                        $history,
                        $changes,
                    )
                );

                break;
            case 6:
            case 7:
                $isSubmitForm = $data['forcesavetype'] === 3;

                if ($isSubmitForm && ! array_key_exists('formsdataurl', $data)) {
                    abort(500, 'Document editing service did not return formsDataUrl');
                }

                $url = $data['url'];
                $key = $data['key'];
                $user = $data['users'][0];

                $url = Str::replace(URL::origin($url), $this->serverConfig->get('url.private'), $url);

                $fileExtension = PathInfo::extension($filename);
                $downloadExtension = PathInfo::extension($url);

                if ($fileExtension !== $downloadExtension) {
                    $result = app(ConvertCommand::class)
                        ->__invoke(new ConvertRequest(
                            filename: $filename,
                            fileType: $downloadExtension,
                            outputType: $fileExtension,
                            url: $url,
                            password: null,
                            user: $user,
                            userAddress: $address,
                        ));

                    if (array_key_exists('step', $result) || array_key_exists('error', $result)) {
                        $filename = PathInfo::filename($filename).".$downloadExtension";
                    } else {
                        $url = $result['fileUrl'];
                    }
                }

                $content = app(DownloadFileCommand::class)
                    ->__invoke(new DownloadFileRequest(url: $url))['content'];

                if ($isSubmitForm) {
                    $formsDataUrl = $data['formsdataurl'];
                    $formsDataUrl = Str::replace(
                        URL::origin($formsDataUrl),
                        $this->serverConfig->get('url.private'),
                        $formsDataUrl
                    );

                    $formData = app(DownloadFileCommand::class)
                        ->__invoke(new DownloadFileRequest(url: $formsDataUrl))['content'];

                    app(SaveDocumentFormCommand::class)
                        ->__invoke(new SaveDocumentFormRequest(
                            filename: $filename,
                            userDirectory: $address,
                            user: $user,
                            formContent: $content,
                            formDataContent: $formData,
                        ));
                } else {
                    app(ForceSaveDocumentCommand::class)
                        ->__invoke(
                            new ForceSaveDocumentRequest(
                                filename: $filename,
                                userDirectory: $address,
                                content: $content
                            )
                        );
                }

                break;
        }

        return response()->json([
            'error' => 0,
        ]);
    }
}
