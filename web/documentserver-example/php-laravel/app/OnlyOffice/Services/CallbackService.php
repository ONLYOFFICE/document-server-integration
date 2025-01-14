<?php

/**
 * (c) Copyright Ascensio System SIA 2024.
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

namespace App\OnlyOffice\Services;

use App\Helpers\Path\Path;
use App\Helpers\Path\PathInfo;
use App\Helpers\URL\URL;
use App\UseCases\Common\Http\DownloadFileCommand;
use App\UseCases\Common\Http\DownloadFileRequest;
use App\UseCases\Docs\Command\ForceSaveCommad;
use App\UseCases\Docs\Command\ForceSaveRequest;
use App\UseCases\Docs\Conversion\ConvertCommand;
use App\UseCases\Docs\Conversion\ConvertRequest;
use App\UseCases\Document\Save\ForceSaveDocumentCommand;
use App\UseCases\Document\Save\ForceSaveDocumentRequest;
use App\UseCases\Document\Save\SaveDocumentCommand;
use App\UseCases\Document\Save\SaveDocumentFormCommand;
use App\UseCases\Document\Save\SaveDocumentFormRequest;
use App\UseCases\Document\Save\SaveDocumentRequest;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;
use Onlyoffice\DocsIntegrationSdk\Models\CallbackForceSaveType;
use Onlyoffice\DocsIntegrationSdk\Service\Callback\CallbackService as OnlyOfficeCallbackService;

class CallbackService extends OnlyOfficeCallbackService
{
    private $data;

    public function __construct($settingsManager, $jwtManager, $data = [])
    {
        parent::__construct($settingsManager, $jwtManager);
        $this->data = $data;
    }

    public function processTrackerStatusEditing($callback, string $fileid)
    {
        return $this->processTrackerStatusEditingAndClosed($callback, $fileid);
    }

    public function processTrackerStatusMustsave($callback, string $fileid)
    {
        return $this->processTrackerStatusMustsaveAndCorrupted($callback, $fileid);
    }

    public function processTrackerStatusCorrupted($callback, string $fileid)
    {
        return $this->processTrackerStatusMustsaveAndCorrupted($callback, $fileid);
    }

    public function processTrackerStatusClosed($callback, string $fileid)
    {
        return $this->processTrackerStatusEditingAndClosed($callback, $fileid);
    }

    public function processTrackerStatusForcesave($callback, string $fileid)
    {
        $isSubmitForm = $callback->getForceSaveType()->getValue() === CallbackForceSaveType::SUBMIT_FORM;

        if ($isSubmitForm && ! $callback->getFormsDataUrl()) {
            Log::error('Document editing service did not return formsDataUrl');

            return ['error' => 1];
        }

        $url = $callback->getUrl();
        $key = $callback->getKey();
        $user = $callback->getUsers()[0];
        $filename = $this->data['filename'];
        $address = $this->data['address'];

        $url = $this->settingsManager->replaceDocumentServerUrlToInternal($url);

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
            $formsDataUrl = $this->settingsManager->replaceDocumentServerUrlToInternal($callback->getFormsDataUrl());

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

        $result['error'] = 0;

        return $result;
    }

    public function processTrackerStatusMustsaveAndCorrupted($callback, string $fileid)
    {
        $url = $callback->getUrl();
        $key = $callback->getKey();
        $user = $callback->getUsers()[0];
        $changes = null;
        $filename = $this->data['filename'];
        $address = $this->data['address'];

        $url = $this->settingsManager->replaceDocumentServerUrlToInternal($url);

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

        $changesUrl = $callback->getChangesurl();
        if ($changesUrl) {
            $changesUrl = Str::replace(URL::origin($changesUrl), $this->settingsManager->getSetting('url.server.private'), $changesUrl);

            $changes = app(DownloadFileCommand::class)
                ->__invoke(new DownloadFileRequest(url: $changesUrl))['content'];
        }

        $historyObject = $callback->getHistory();
        $history = $historyObject ? $historyObject->getChanges() : null;
        $serverVersion = $historyObject ? $historyObject->getServerVersion() : null;

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

        return [
            'error' => 0,
        ];
    }

    public function processTrackerStatusEditingAndClosed($callback, string $fileid)
    {
        $actions = $callback->getActions();
        if ($actions && $actions[0]['type'] == 0) {
            $user = $actions[0]['userid'];
            if (array_search($user, $callback->getUsers()) === false) {
                app(ForceSaveCommad::class)
                    ->__invoke(new ForceSaveRequest(key: $callback->getKey()));
            }
        }

        $result['error'] = 0;

        return $result;
    }
}
