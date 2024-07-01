<?php

namespace App\Http\Controllers;

use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use OnlyOffice\Exceptions\Conversion\ConversionError;
use OnlyOffice\Exceptions\Conversion\ConversionNotComplete;
use OnlyOffice\Services\Editor;
use Illuminate\Support\Str;
use OnlyOffice\Config;
use OnlyOffice\Document;
use OnlyOffice\Editor\Key;
use OnlyOffice\Entities\File;
use OnlyOffice\Helpers\Path;
use OnlyOffice\Helpers\URL\File as FileURL;
use OnlyOffice\Helpers\URL\Template;
use OnlyOffice\Helpers\URL\URL;
use OnlyOffice\JWT;
use OnlyOffice\DocumentStorage;
use OnlyOffice\Users;

class EditorController extends Controller
{
    public function __construct(
        private DocumentStorage $storage,
        private Users $users,
        private JWT $jwt,
        private Config $config,
    ) {
    }

    public function index(Request $request)
    {
        $request->validate([
            'fileUrl' => 'nullable|string',
            'fileID' => 'nullable|string',
            'directUrl' => 'nullable|boolean',
            'user' => 'nullable|string',
            'fileExt' => 'nullable|string',
            'action' => 'nullable|string',
            'type' => 'nullable|string',
            'actionLink' => 'nullable|string'
        ]);

        $externalUrl = $request->input('fileUrl');
        $fileId = urldecode($request->input('fileID'));
        $directUrlEnabled = $request->input('directUrl', false);
        $user = $this->users->find($request->user ?? 'uid-1');
        $mode = $request->action ?? 'edit';
        $type = $request->type ?? 'desktop';
        $actionLink = $request->input('actionLink');
        $lang = cache('lang') ?? 'en';
        $fileExt = $request->input('fileExt');
        $withSample = $request->has('sample') && $request->sample === 'true';
        $storagePublicUrl = $this->config->get('url.storage.public');
        $storagePrivateUrl = $this->config->get('url.storage.private');

        if ($externalUrl) {
            // upload a file from the url
        } else {
            $filename = Path::basename($fileId);
        }

        $extension = Path::extension($filename);

        if ($fileExt) {
            $template = [
                'path' => $request->ip(),
                'user' => $user->id,
                'extension' => $fileExt,
            ];
            $file = $this->storage->createFromTemplate($template, $withSample);

            return redirect()
                ->route('editor.index', [
                    'fileID' => $file->basename,
                    'user' => $file->author->id,
                ]);
        }

        $fileMissing = false;
        try {
            Log::debug($filename);
            $file = $this->storage->find(Path::join($request->ip(), $filename));
        } catch (Exception $e) {
            $fileMissing = true;
        }

        $downloadUrl = FileURL::download($filename, $request->ip());

        if ((!$file->format->editable() && $mode == "edit"
                || $mode == "fillForms")
            && $file->format->fillable()
        ) {
            $mode = "fillForms";
        }

        $submitForm = $mode == "fillForms" && $user->id == "uid-1";
        $mode = $file->format->editable() && $mode != "view" ? "edit" : "view";

        $templatesImageUrl = Template::image($file->format->type); // templates image url in the "From Template" section
        $createUrl = FileURL::create($filename, $user->id);
        $templates = [
            [
                "image" => "",
                "title" => "Blank",
                "url" => $createUrl,
            ],
            [
                "image" => $templatesImageUrl,
                "title" => "With sample content",
                "url" => $createUrl . "&sample=true",
            ],
        ];

        if ($user->goback !== null) {
            $user->goback["url"] = $storagePublicUrl;
        }

        // specify the document config
        $config = [
            "type" => $type,
            "documentType" => $file->format->type,
            "document" => [
                "title" => $filename,
                "url" => $downloadUrl,
                "directUrl" => $directUrlEnabled ? $downloadUrl : "",
                "fileType" => $extension,
                "key" => Key::generate($file->basename, $file->lastModified),
                "info" => [
                    "owner" => "Me",
                    "uploaded" => date('d.m.y'),
                    "favorite" => $user->favorite,
                ],
                "permissions" => [  // the permission for the document to be edited and downloaded or not
                    "comment" => $mode != "view" && $mode
                        != "fillForms" && $mode != "embedded" && $mode != "blockcontent",
                    "copy" => !in_array("copy", $user->deniedPermissions),
                    "download" => !in_array("download", $user->deniedPermissions),
                    "edit" => $file->format->editable() && ($mode == "edit" ||
                        $mode == "view" || $mode == "filter" || $mode == "blockcontent"),
                    "print" => !in_array("print", $user->deniedPermissions),
                    "fillForms" => $mode != "view" && $mode != "comment"
                        && $mode != "blockcontent",
                    "modifyFilter" => $mode != "filter",
                    "modifyContentControl" => $mode != "blockcontent",
                    "review" => $file->format->editable() && ($mode == "edit" || $mode == "review"),
                    "chat" => $user->id != "uid-0",
                    "reviewGroups" => $user->reviewGroups,
                    "commentGroups" => $user->commentGroups,
                    "userInfoGroups" => $user->userInfoGroups,
                    "protect" => !in_array("protect", $user->deniedPermissions),
                ],
                "referenceData" => [
                    "fileKey" => $user->id != "uid-0" ? json_encode([
                        "fileName" => $filename,
                        "userAddress" =>  $request->ip()
                    ]) : null,
                    "instanceId" => $storagePublicUrl,
                ],
            ],
            "editorConfig" => [
                "actionLink" => $actionLink,
                "mode" => $mode,
                "lang" => $lang,
                "callbackUrl" => FileURL::callback($file->basename, $request->ip()),
                "coEditing" => $mode == "view" && $user->id == "uid-0" ? [
                    "mode" => "strict",
                    "change" => false,
                ] : null,
                "createUrl" => $user->id != "uid-0" ? $createUrl : null,
                "templates" => $user->templates ? $templates : null,
                "user" => [  // the user currently viewing or editing the document
                    "id" => $user->id != "uid-0" ? $user->id : null,
                    "name" => $user->name,
                    "group" => $user->group,
                    "image" => $user->avatar ? $storagePublicUrl . "/assets/images/" . $user->id . ".png" : null
                ],
                "embedded" => [  // the parameters for the embedded document type
                    // the absolute URL that will allow the document to be saved onto the user personal computer
                    "saveUrl" => $downloadUrl,
                    // the absolute URL to the document serving as a source file for the document embedded into
                    // the web page
                    "embedUrl" => $downloadUrl,
                    // the absolute URL that will allow other users to share this document
                    "shareUrl" => $downloadUrl,
                    "toolbarDocked" => "top",  // the place for the embedded viewer toolbar (top or bottom)
                ],
                "customization" => [  // the parameters for the editor interface
                    "about" => true,  // the About section display
                    "comments" => true,
                    "feedback" => true,  // the Feedback & Support menu button display
                    // adds the request for the forced file saving to the callback handler when saving the document
                    "forcesave" => true,
                    "submitForm" => $submitForm,  // if the Submit form button is displayed or not
                    // settings for the Open file location menu button and upper right corner button
                    "goback" => $user->goback !== null ? $user->goback : "",
                ],
            ],
        ];

        // an image for inserting
        $dataInsertImage = [
            "fileType" => "svg",
            "url" => URL::build($storagePrivateUrl, 'assets/images/logo.svg'),
        ];

        // a document for comparing
        $dataDocument = [
            "fileType" => "docx",
            "url" => URL::build($storagePrivateUrl, 'assets/document-templates/sample/sample.docx'),
        ];

        // recipients data for mail merging
        $dataSpreadsheet = [
            "fileType" => "csv",
            "url" => URL::build($storagePrivateUrl, 'assets/document-templates/sample/csv.csv'),
        ];

        if ($directUrlEnabled) {
            $dataInsertImage['directUrl'] = URL::build($storagePublicUrl, 'assets/images/logo.svg');
            $dataDocument['directUrl'] = URL::build($storagePublicUrl, 'assets/document-templates/sample/sample.docx');
            $dataSpreadsheet['directUrl'] = URL::build($storagePublicUrl, 'assets/document-templates/sample/csv.csv');
        }

        // users data for mentions
        $usersForMentions = $user->id != "uid-0" ? $this->users->getUsersForMentions($user->id) : null;

        // users data for protect
        $usersForProtect = $user->id != "uid-0" ? $this->users->getUsersForProtect($user->id) : null;

        $usersInfo = [];

        if ($user->id != 'uid-0') {
            foreach ($this->users->getAll() as $userInfo) {
                $u = $userInfo;
                $u->image = $userInfo->avatar ? $storagePublicUrl . "/assets/images/" . $userInfo->id . ".png" : null;
                array_push($usersInfo, $u);
            }
        }

        // check if the secret key to generate token exists
        if ($this->config->get('jwt.enabled')) {
            $config["token"] = $this->jwt->encode($config);  // encode config into the token
            // encode the dataInsertImage object into the token
            $dataInsertImage["token"] = $this->jwt->encode($dataInsertImage);
            // encode the dataDocument object into the token
            $dataDocument["token"] = $this->jwt->encode($dataDocument);
            // encode the dataSpreadsheet object into the token
            $dataSpreadsheet["token"] = $this->jwt->encode($dataSpreadsheet);
        }

        $historyLayout = "";

        if ($user->id == "uid-3") {
            $historyLayout .= "config.events['onRequestHistoryClose'] = null;
                config.events['onRequestRestore'] = null;";
        }

        if ($user->id != "uid-0") {
            $historyLayout .= "// add mentions for not anonymous users
                config.events['onRequestUsers'] = onRequestUsers;
                config.events['onRequestSaveAs'] = onRequestSaveAs;
                // the user is mentioned in a comment
                config.events['onRequestSendNotify'] = onRequestSendNotify;
                // prevent file renaming for anonymous users
                config.events['onRequestRename'] = onRequestRename;
                // prevent switch the document from the viewing into the editing mode for anonymous users
                config.events['onRequestEditRights'] = onRequestEditRights;
                config.events['onRequestHistory'] = onRequestHistory;
                config.events['onRequestHistoryData'] = onRequestHistoryData;";
            if ($user->id != "uid-3") {
                $historyLayout .= "config.events['onRequestHistoryClose'] = onRequestHistoryClose;
                config.events['onRequestRestore'] = onRequestRestore;";
            }
        }
        $editorConfig = [
            "docType" => $file->format->type,
            "apiUrl" => $this->config->get('url.server.api'),
            "dataInsertImage" => mb_strimwidth(
                json_encode($dataInsertImage),
                1,
                mb_strlen(json_encode($dataInsertImage)) - 2
            ),
            "dataDocument" => json_encode($dataDocument),
            "dataSpreadsheet" => json_encode($dataSpreadsheet),
            "fileNotFoundAlert" => $fileMissing ? "alert('File not found'); return;" : "",
            "config" => json_encode($config),
            "history" => $historyLayout,
            "usersForMentions" => json_encode($usersForMentions),
            "usersInfo" => json_encode($usersInfo),
            "usersForProtect" => json_encode($usersForProtect),
        ];

        return view('editor', $editorConfig);
    }

    public function track(Request $request, Document $document)
    {
        $request->validate([
            'fileName' => 'required|string',
            'userAddress' => 'required|string',
        ]);

        $filename = $request->input('fileName');
        $address = $request->input('userAddress');
        $data = $request->input('payload');

        switch ($data['status']) {
            case 1:
                if ($data['actions'] && $data['actions'][0]['type'] == 0) {
                    $user = $data['actions'][0]['userid'];
                    if (array_search($user, $data['users']) === false) {
                        $document->forceSave($data['key']);
                    }
                }
                break;
            case 2:
            case 3:
                $url = $data['url'];
                $url = Str::replace(URL::origin($url), $this->config->get('url.server.private'), $url);
                $changes = null;
                $history = null;

                $fileExtension = Path::extension($filename);
                $downloadExtension = Path::extension($url);

                $file = new File();
                $file->basename = $filename;
                $file->key = $data['key'];

                if ($fileExtension !== $downloadExtension) {
                    $data = [
                        'filename' => $filename,
                        'filetype' => $downloadExtension,
                        'outputtype' => $fileExtension,
                        'url' => $url,
                        'key' => Key::revision($url),
                    ];
                    try {
                        $url = $document->convert($data)['fileUrl'];
                    } catch (ConversionError | ConversionNotComplete $e) {
                        $url = Str::of(Path::filename($filename))->append('.' . $downloadExtension);
                    }
                }

                $file->content = $document->download($url);
                $file->path = Path::join($address, $filename);

                if (array_key_exists('changesurl', $data)) {
                    $changesUrl = $data['changesurl'];
                    $changesUrl = Str::replace(URL::origin($changesUrl), $this->config->get('url.server.private'), $changesUrl);
                    $changes = $document->download($changesUrl);
                }

                $history = array_key_exists('history', $data) ? json_encode($data['history'], JSON_PRETTY_PRINT) : null;

                $this->storage->update($file, $history, $changes);

                break;
            case 6:
            case 7:
                $isSubmitForm = $data['forcesavetype'] === 3;
                $url = $data['url'];
                $url = Str::replace(URL::origin($url), $this->config->get('url.server.private'), $url);
                $changes = null;
                $history = null;

                $fileExtension = Path::extension($filename);
                $downloadExtension = Path::extension($url);

                $file = new File();
                $file->basename = $filename;
                $file->key = $data['key'];
                $file->extension = $fileExtension;

                if ($fileExtension !== $downloadExtension) {
                    $data = [
                        'filename' => $filename,
                        'filetype' => $downloadExtension,
                        'outputtype' => $fileExtension,
                        'url' => $url,
                        'key' => Key::revision($url),
                    ];
                    try {
                        $url = $document->convert($data)['fileUrl'];
                    } catch (ConversionError | ConversionNotComplete $e) {
                        $filename = Str::of(Path::filename($filename))->append('.' . $downloadExtension);
                        $file->extension = $downloadExtension;
                    }
                }

                $file->content = $document->download($url);
                $file->path = Path::join($address, $filename);

                if ($isSubmitForm) {
                    $form = [
                        'filename' => $filename,
                        'path' => $file->path,
                        'content' => $file->content,
                        'user' => $data['actions'][0]['userid'],
                    ];

                    $this->storage->createForm($form);

                    if (array_key_exists('formsdataurl', $data)) {
                        $formsFilename = Str::of(Path::filename($filename))->append(".txt");
                        $formsData = [
                            'filename' => $formsFilename,
                            'path' => Path::join($address, $formsFilename),
                            'content' => $document->download($data['formsdataurl']),
                        ];
                        $this->storage->save($formsData);
                    }
                } else {
                    $this->storage->forceSave($file);
                }

                break;
        }

        return response()->json([
            'error' => 0,
        ]);
    }
}
